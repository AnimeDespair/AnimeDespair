package dev.animedespair.lookupbubble;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static final int PURPLE = Color.rgb(115, 84, 233);
    static final int INK = Color.rgb(32, 25, 61);
    static final int MUTED = Color.rgb(107, 102, 121);
    static final int BG = Color.rgb(247, 246, 252);

    private Ui() {}

    static int dp(Context context, float number) {
        return (int) (context.getResources().getDisplayMetrics().density * number + 0.5f);
    }

    static GradientDrawable shape(int color, Context context, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(context, radius));
        return d;
    }

    static LinearLayout vertical(Context c, int padding) {
        LinearLayout layout = new LinearLayout(c);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(c, padding), dp(c, padding), dp(c, padding), dp(c, padding));
        return layout;
    }

    static TextView text(Context c, String content, int sp, boolean bold, int color) {
        TextView t = new TextView(c);
        t.setText(content);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    static Button button(Context c, String name, boolean primary) {
        Button b = new Button(c);
        b.setText(name);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTextColor(primary ? Color.WHITE : PURPLE);
        b.setBackground(shape(primary ? PURPLE : BG, c, 12));
        b.setPadding(dp(c, 12), dp(c, 8), dp(c, 12), dp(c, 8));
        return b;
    }

    static EditText input(Context c, String hint) {
        EditText e = new EditText(c);
        e.setSingleLine(true);
        e.setHint(hint);
        e.setTextSize(16);
        e.setTextColor(INK);
        e.setHintTextColor(MUTED);
        e.setPadding(dp(c, 14), dp(c, 10), dp(c, 14), dp(c, 10));
        e.setBackground(shape(BG, c, 12));
        e.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        return e;
    }

    static void space(Context c, LinearLayout parent, int height) {
        View v = new View(c);
        parent.addView(v, new LinearLayout.LayoutParams(1, dp(c, height)));
    }
}
