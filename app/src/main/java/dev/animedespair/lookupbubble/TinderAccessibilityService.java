package dev.animedespair.lookupbubble;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Optional on-device text suggestions from the foreground Tinder screen only. */
public class TinderAccessibilityService extends AccessibilityService {
    private static final Pattern NAME_WITH_AGE = Pattern.compile(
            "^([\\p{L}][\\p{L}'’.-]{1,24}(?: +[\\p{L}][\\p{L}'’.-]{1,24}){0,2}),? +(1[89]|[2-6][0-9]|7[0-9])$");
    private static final Pattern CITY = Pattern.compile(
            "(?i)^(?:lives in|from|located in|living in) +([\\p{L}][\\p{L} .,'-]{1,50})$");
    private static volatile Snapshot lastSnapshot;

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

    public static Snapshot recentSnapshot() {
        Snapshot value = lastSnapshot;
        if (value == null || System.currentTimeMillis() - value.capturedAt > 60000L) return null;
        return value;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null
                || !"com.tinder".contentEquals(event.getPackageName())) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        try {
            LinkedHashSet<String> texts = new LinkedHashSet<>();
            ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
            queue.add(root);
            int visited = 0;
            while (!queue.isEmpty() && visited++ < 180) {
                AccessibilityNodeInfo node = queue.removeFirst();
                // Avoid reading text typed into editable controls (e.g. chats or search).
                if (!node.isEditable() && node.isVisibleToUser()) {
                    CharSequence raw = node.getText();
                    if (raw != null) store(texts, raw);
                    CharSequence description = node.getContentDescription();
                    if (description != null) store(texts, description);
                }
                int count = Math.min(node.getChildCount(), 40);
                for (int i = 0; i < count; i++) {
                    AccessibilityNodeInfo child = node.getChild(i);
                    if (child != null) queue.addLast(child);
                }
            }
            String name = "";
            String city = "";
            for (String text : texts) {
                if (name.isEmpty()) {
                    Matcher m = NAME_WITH_AGE.matcher(text);
                    if (m.matches()) name = m.group(1);
                }
                if (city.isEmpty()) {
                    Matcher m = CITY.matcher(text);
                    if (m.matches()) city = m.group(1);
                }
            }
            if (!name.isEmpty()) lastSnapshot = new Snapshot(name, city, System.currentTimeMillis());
        } finally {
            // Service stores no raw UI text or search history.
        }
    }

    private static void store(LinkedHashSet<String> texts, CharSequence content) {
        if (texts.size() >= 160) return;
        String s = content.toString().trim().replaceAll("\\s+", " ");
        if (s.length() <= 80 && !s.isEmpty()) texts.add(s);
    }

    @Override public void onInterrupt() {}
    @Override public void onDestroy() { super.onDestroy(); lastSnapshot = null; }
}
