package dev.animedespair.lookupbubble;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.accessibility.AccessibilityManager;
import android.accessibilityservice.AccessibilityServiceInfo;
import java.util.List;

public class MainActivity extends Activity {
    private TextView overlayStatus;
    private TextView accessibilityStatus;
    private static final int NOTIFY_REQUEST = 31;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.BG);
        LinearLayout layout = Ui.vertical(this, 20);
        scroll.addView(layout);
        setContentView(scroll);

        layout.addView(Ui.text(this, "🔎 Lookup Bubble", 28, true, Ui.INK));
        Ui.space(this, layout, 12);
        layout.addView(Ui.text(this, "Search public Facebook profiles from Tinder without leaving your screen.", 16, false, Ui.MUTED));
        Ui.space(this, layout, 24);

        layout.addView(Ui.text(this, "1. Floating window access", 20, true, Ui.INK));
        overlayStatus = Ui.text(this, "", 14, false, Ui.MUTED);
        Ui.space(this, layout, 8);
        layout.addView(overlayStatus);
        Ui.space(this, layout, 10);
        Button overlay = Ui.button(this, "Allow Display over other apps", true);
        layout.addView(overlay);
        overlay.setOnClickListener(v -> showOverlaySettings());

        Ui.space(this, layout, 24);
        layout.addView(Ui.text(this, "2. Tinder text suggestion (optional)", 20, true, Ui.INK));
        Ui.space(this, layout, 8);
        layout.addView(Ui.text(this, "Accessibility can read visible Tinder name and city on your device. Enable it only if you want suggestions. No data is sent or stored.", 14, false, Ui.MUTED));
        accessibilityStatus = Ui.text(this, "", 14, false, Ui.MUTED);
        Ui.space(this, layout, 8);
        layout.addView(accessibilityStatus);
        Button accessibility = Ui.button(this, "Accessibility settings", false);
        Ui.space(this, layout, 10);
        layout.addView(accessibility);
        accessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        Ui.space(this, layout, 28);
        layout.addView(Ui.text(this, "3. Start", 20, true, Ui.INK));
        Ui.space(this, layout, 12);
        Button start = Ui.button(this, "Start floating bubble", true);
        layout.addView(start);
        start.setOnClickListener(v -> startBubble());
        Ui.space(this, layout, 12);
        Button stop = Ui.button(this, "Stop floating bubble", false);
        layout.addView(stop);
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, OverlayService.class));
            Toast.makeText(this, "Bubble stopped", Toast.LENGTH_SHORT).show();
        });
        Ui.space(this, layout, 24);
        layout.addView(Ui.text(this, "Search results are not identity verification. No account login or hidden profile access.", 13, false, Ui.MUTED));
    }

    @Override public void onResume() {
        super.onResume();
        if (overlayStatus != null)
            overlayStatus.setText(Settings.canDrawOverlays(this) ? "✓ Overlay allowed" : "○ Permission required");
        if (accessibilityStatus != null)
            accessibilityStatus.setText(isCaptureEnabled() ? "✓ Tinder suggestion enabled" : "○ Disabled (manual lookup works)");
    }

    private void showOverlaySettings() {
        startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
    }

    private void startBubble() {
        if (!Settings.canDrawOverlays(this)) {
            showOverlaySettings();
            return;
        }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFY_REQUEST);
            return;
        }
        launch();
    }

    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(code, permissions, grants);
        if (code == NOTIFY_REQUEST) launch();
    }

    private void launch() {
        try {
            Intent intent = new Intent(this, OverlayService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent);
            else startService(intent);
            Toast.makeText(this, isCaptureEnabled() ? "Open Tinder and tap the floating 🔎"
                    : "Open Tinder and tap the floating 🔎. Enable Tinder capture (step 2) to auto-fill details.",
                    Toast.LENGTH_LONG).show();
        } catch (RuntimeException error) {
            Toast.makeText(this, "Could not start overlay: " + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean isCaptureEnabled() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) return false;
        ComponentName expected = new ComponentName(this, TinderAccessibilityService.class);
        List<AccessibilityServiceInfo> infos = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo info : infos) {
            if (info.getResolveInfo() != null && info.getResolveInfo().serviceInfo != null) {
                ComponentName found = new ComponentName(info.getResolveInfo().serviceInfo.packageName,
                    info.getResolveInfo().serviceInfo.name);
                if (expected.equals(found)) return true;
            }
        }
        return false;
    }
}
