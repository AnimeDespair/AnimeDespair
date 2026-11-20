package dev.animedespair.lookupbubble;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Optional on-device text suggestions from the foreground Tinder screen only. */
public class TinderAccessibilityService extends AccessibilityService {
    private static final Pattern NAME_WITH_AGE = Pattern.compile(
            "^([\\p{L}][\\p{L}'’.-]{1,24}(?: +[\\p{L}][\\p{L}'’.-]{1,24}){0,2}),? +(1[89]|[2-6][0-9]|7[0-9])$");
    private static final Pattern AGE_ONLY = Pattern.compile("^(1[89]|[2-9][0-9])$");
    private static final Pattern NAME_ONLY = Pattern.compile(
            "^([\\p{L}][\\p{L}'’.-]{1,24}(?: +[\\p{L}][\\p{L}'’.-]{1,24}){0,2})$");
    private static final Pattern CITY = Pattern.compile(
            "(?i)^(?:lives in|from|located in|living in|📍) *([\\p{L}][\\p{L} .,'-]{1,50})$");
    private static volatile Snapshot lastSnapshot;
    private static volatile boolean connected;

    public static final class Snapshot {
        public final String name;
        public final String city;
        public final long capturedAt;

        Snapshot(String name, String city, long capturedAt) {
            this.name = name;
            this.city = city;
            this.capturedAt = capturedAt;
        }
    }

    public static boolean isConnected() { return connected; }

    @Override protected void onServiceConnected() { super.onServiceConnected(); connected = true; }

    @Override public boolean onUnbind(android.content.Intent intent) { connected = false; lastSnapshot = null; return super.onUnbind(intent); }

    private static volatile TinderAccessibilityService instance;

    /** Scans the Tinder window now (it may no longer be the active window once the panel is open). */
    public static Snapshot recentSnapshot() {
        TinderAccessibilityService service = instance;
        if (service != null) service.scanTinder();
        Snapshot value = lastSnapshot;
        if (value == null || System.currentTimeMillis() - value.capturedAt > 600000L) return null;
        return value;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null
                || !"com.tinder".contentEquals(event.getPackageName())) return;
        scanTinder();
    }

    private void scanTinder() {
        AccessibilityNodeInfo root = null;
        try {
            List<AccessibilityWindowInfo> windows = getWindows();
            if (windows != null) {
                for (AccessibilityWindowInfo w : windows) {
                    AccessibilityNodeInfo candidate = w.getRoot();
                    if (candidate == null) continue;
                    if (candidate.getPackageName() != null
                            && "com.tinder".contentEquals(candidate.getPackageName())) {
                        root = candidate;
                        break;
                    }
                }
            }
        } catch (RuntimeException ignored) {}
        if (root == null) root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null
                || !"com.tinder".contentEquals(root.getPackageName())) return;
        List<String> texts = new ArrayList<>();
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        int visited = 0;
        while (!queue.isEmpty() && visited++ < 600) {
            AccessibilityNodeInfo node = queue.removeFirst();
            // Avoid reading text typed into editable controls (e.g. chats or search).
            if (!node.isEditable()) {
                CharSequence raw = node.getText();
                if (raw != null) store(texts, raw);
                CharSequence description = node.getContentDescription();
                if (description != null) store(texts, description);
            }
            int count = Math.min(node.getChildCount(), 60);
            for (int i = 0; i < count; i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) queue.addLast(child);
            }
        }
        String name = "";
        String city = "";
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            if (name.isEmpty()) {
                Matcher m = NAME_WITH_AGE.matcher(text);
                if (m.matches()) name = m.group(1);
                else if (AGE_ONLY.matcher(text).matches() && i > 0) {
                    Matcher prev = NAME_ONLY.matcher(texts.get(i - 1));
                    if (prev.matches()) name = prev.group(1);
                }
            }
            if (city.isEmpty()) {
                Matcher m = CITY.matcher(text);
                if (m.matches()) city = m.group(1).trim();
            }
        }
        if (!name.isEmpty()) lastSnapshot = new Snapshot(name, city, System.currentTimeMillis());
    }

    @Override protected void onServiceConnected() { super.onServiceConnected(); connected = true; instance = this; }

    @Override public boolean onUnbind(android.content.Intent intent) {
        connected = false; instance = null; lastSnapshot = null; return super.onUnbind(intent);
    }

    private static void store(List<String> texts, CharSequence content) {
        if (texts.size() >= 400) return;
        String s = content.toString().trim().replaceAll("\\s+", " ");
        if (s.length() <= 80 && !s.isEmpty() && !texts.contains(s)) texts.add(s);
    }

    @Override public void onInterrupt() {}
    @Override public void onDestroy() { super.onDestroy(); connected = false; instance = null; lastSnapshot = null; }
}
