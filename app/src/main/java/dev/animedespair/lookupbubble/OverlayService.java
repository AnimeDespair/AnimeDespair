package dev.animedespair.lookupbubble;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.net.Uri;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class OverlayService extends Service {
    private static final String CHANNEL_ID = "lookup_bubble_running";
    private static final int NOTIFICATION_ID = 2104;
    private WindowManager windowManager;
    private TextView bubble;
    private WindowManager.LayoutParams bubbleParams;
    private ScrollView panel;
    private EditText nameField;
    private EditText cityField;
    private TextView hintText;

    @Override public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!Settings.canDrawOverlays(this) || windowManager == null) {
            Toast.makeText(this, "Allow Display over other apps first", Toast.LENGTH_LONG).show();
            stopSelf();
            return START_NOT_STICKY;
        }
        makeForeground();
        if (bubble == null) {
            try { showBubble(); }
            catch (RuntimeException error) {
                Toast.makeText(this, "Overlay could not be displayed", Toast.LENGTH_LONG).show();
                stopSelf();
                return START_NOT_STICKY;
            }
        }
        return START_NOT_STICKY;
    }

    private void makeForeground() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Lookup Bubble active", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("The floating lookup window is active");
            nm.createNotificationChannel(channel);
        }
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_search)
                .setContentTitle("Lookup Bubble is running")
                .setContentText("Tap to adjust or stop floating search")
                .setContentIntent(pending).setOngoing(true).build();
        if (Build.VERSION.SDK_INT >= 34)
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(NOTIFICATION_ID, notification);
    }

    private void showBubble() {
        bubble = Ui.text(this, "🔎", 29, true, Color.WHITE);
        bubble.setGravity(Gravity.CENTER);
        bubble.setContentDescription("Open floating Facebook search");
        bubble.setBackground(Ui.shape(Ui.PURPLE, this, 32));
        bubble.setElevation(Ui.dp(this, 9));
        bubbleParams = new WindowManager.LayoutParams(
                Ui.dp(this, 60), Ui.dp(this, 60),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = Ui.dp(this, 10);
        bubbleParams.y = Ui.dp(this, 180);
        bubble.setOnClickListener(v -> togglePanel());
        final int threshold = ViewConfiguration.get(this).getScaledTouchSlop();
        bubble.setOnTouchListener(new View.OnTouchListener() {
            private float downX, downY;
            private int originX, originY;
            private boolean dragging;

            @Override public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getRawX();
                        downY = event.getRawY();
                        originX = bubbleParams.x;
                        originY = bubbleParams.y;
                        dragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = Math.round(event.getRawX() - downX);
                        int dy = Math.round(event.getRawY() - downY);
                        if (Math.abs(dx) > threshold || Math.abs(dy) > threshold) dragging = true;
                        if (dragging) {
                            int width = getResources().getDisplayMetrics().widthPixels;
                            int height = getResources().getDisplayMetrics().heightPixels;
                            bubbleParams.x = Math.max(0, Math.min(width - Ui.dp(OverlayService.this, 60), originX + dx));
                            bubbleParams.y = Math.max(0, Math.min(height - Ui.dp(OverlayService.this, 90), originY + dy));
                            windowManager.updateViewLayout(bubble, bubbleParams);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!dragging) view.performClick();
                        return true;
                    case MotionEvent.ACTION_CANCEL: return true;
                    default: return false;
                }
            }
        });
        windowManager.addView(bubble, bubbleParams);
    }

    private void togglePanel() {
        if (panel != null) { closePanel(); return; }
        showPanel();
    }

    private void showPanel() {
        panel = new ScrollView(this);
        panel.setFillViewport(false);
        panel.setBackground(Ui.shape(Color.WHITE, this, 18));
        panel.setElevation(Ui.dp(this, 14));
        LinearLayout content = Ui.vertical(this, 17);
        panel.addView(content);
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = Ui.text(this, "Find on Facebook", 19, true, Ui.INK);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        Button close = Ui.button(this, "✕", false);
        header.addView(close, new LinearLayout.LayoutParams(Ui.dp(this, 45), Ui.dp(this, 45)));
        close.setOnClickListener(v -> closePanel());
        content.addView(header);
        content.addView(Ui.text(this, "Review details before searching. Matches are not verified.", 13, false, Ui.MUTED));
        Ui.space(this, content, 12);
        content.addView(Ui.text(this, "Name", 13, true, Ui.INK));
        Ui.space(this, content, 5);
        nameField = Ui.input(this, "e.g. Alex");
        content.addView(nameField, new LinearLayout.LayoutParams(-1, Ui.dp(this, 48)));
        Ui.space(this, content, 12);
        content.addView(Ui.text(this, "City (optional)", 13, true, Ui.INK));
        Ui.space(this, content, 5);
        cityField = Ui.input(this, "e.g. Sioux City");
        content.addView(cityField, new LinearLayout.LayoutParams(-1, Ui.dp(this, 48)));
        Ui.space(this, content, 12);

        Button capture = Ui.button(this, "✨ Suggest from Tinder screen", false);
        content.addView(capture, new LinearLayout.LayoutParams(-1, Ui.dp(this, 48)));
        hintText = Ui.text(this, "Optional: enable Tinder text capture in app settings.", 12, false, Ui.MUTED);
        Ui.space(this, content, 8);
        content.addView(hintText);
        capture.setOnClickListener(v -> captureSuggestion(false));
        Ui.space(this, content, 14);

        Button facebook = Ui.button(this, "Search Facebook People", true);
        content.addView(facebook, new LinearLayout.LayoutParams(-1, Ui.dp(this, 48)));
        facebook.setOnClickListener(v -> openSearch(false));
        Ui.space(this, content, 10);
        Button google = Ui.button(this, "Search public Facebook via Google", false);
        content.addView(google, new LinearLayout.LayoutParams(-1, Ui.dp(this, 52)));
        google.setOnClickListener(v -> openSearch(true));
        Ui.space(this, content, 10);
        Button stop = Ui.button(this, "Stop floating bubble", false);
        content.addView(stop, new LinearLayout.LayoutParams(-1, Ui.dp(this, 45)));
        stop.setOnClickListener(v -> stopSelf());

        captureSuggestion(true);

        int width = Math.min(Ui.dp(this, 350), getResources().getDisplayMetrics().widthPixels - Ui.dp(this, 20));
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width, Math.max(Ui.dp(this, 170), Math.min(Ui.dp(this, 600),
                    getResources().getDisplayMetrics().heightPixels - Ui.dp(this, 90))),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = Ui.dp(this, 10);
        params.y = Ui.dp(this, 70);
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        try { windowManager.addView(panel, params); }
        catch (RuntimeException e) {
            panel = null;
            Toast.makeText(this, "Can't open search panel", Toast.LENGTH_LONG).show();
        }
    }

    private void captureSuggestion(boolean automatic) {
        TinderAccessibilityService.Snapshot suggestion = TinderAccessibilityService.recentSnapshot();
        if (suggestion == null || suggestion.name.isEmpty()) {
            if (!TinderAccessibilityService.isConnected()) {
                hintText.setText("Tinder capture is off. Enable it in the Lookup Bubble app to auto-fill, or enter the name.");
                return;
            }
            if (automatic) return;
            hintText.setText("No recent Tinder profile detected. Open a profile in Tinder and try again, or enter the name.");
            return;
        }
        nameField.setText(suggestion.name);
        if (!suggestion.city.isEmpty()) cityField.setText(suggestion.city);
        hintText.setText("Auto-filled from Tinder text. Verify before searching.");
    }

    private void openSearch(boolean google) {
        String destination;
        try {
            destination = google ? SearchUrls.google(nameField.getText().toString(), cityField.getText().toString())
                    : SearchUrls.facebook(nameField.getText().toString(), cityField.getText().toString());
        } catch (IllegalArgumentException e) {
            nameField.setError(e.getMessage());
            return;
        }
        Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(destination));
        browser.addCategory(Intent.CATEGORY_BROWSABLE);
        browser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(browser);
            closePanel();
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No browser installed", Toast.LENGTH_LONG).show();
        }
    }

    private void closePanel() {
        if (panel != null && windowManager != null) {
            try { windowManager.removeView(panel); } catch (IllegalArgumentException ignored) {}
            panel = null;
            nameField = null;
            cityField = null;
            hintText = null;
        }
    }

    @Override public void onDestroy() {
        closePanel();
        if (bubble != null && windowManager != null) {
            try { windowManager.removeView(bubble); } catch (IllegalArgumentException ignored) {}
            bubble = null;
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
