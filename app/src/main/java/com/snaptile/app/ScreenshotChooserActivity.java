package com.snaptile.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ScreenshotChooserActivity extends Activity {

    public static final String EXTRA_MODE      = "mode";
    public static final String MODE_FULL       = "full";
    public static final String MODE_REGION     = "region";
    public static final String MODE_FULL_PAINT = "full_paint";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window w = getWindow();
        w.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        w.setGravity(Gravity.CENTER);
        w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        w.setDimAmount(0.65f);
        setContentView(buildUI());
    }

    private View buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF0D1B2A);
        bg.setCornerRadius(dp(24));
        bg.setStroke(1, 0xFF1E3A5F);
        root.setBackground(bg);
        root.setClipToOutline(true);
        root.setLayoutParams(new LinearLayout.LayoutParams(dp(280), ViewGroup.LayoutParams.WRAP_CONTENT));

        // Header
        TextView hdr = new TextView(this);
        hdr.setText("Screenshot");
        hdr.setTextSize(17); hdr.setTextColor(0xFFECEFF1);
        hdr.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        hdr.setGravity(Gravity.CENTER);
        hdr.setPadding(0, dp(22), 0, dp(18));
        root.addView(hdr);

        root.addView(div());
        root.addView(row("Full Screen",        "Capture the entire display",          MODE_FULL));
        root.addView(div());
        root.addView(row("Snip Region",        "Draw a region like Snipping Tool",    MODE_REGION));
        root.addView(div());
        root.addView(row("Full Screen + Paint","Capture full screen and annotate",    MODE_FULL_PAINT));
        root.addView(div());

        // Cancel
        TextView cancel = new TextView(this);
        cancel.setText("Cancel");
        cancel.setTextColor(0xFF78909C); cancel.setTextSize(14);
        cancel.setGravity(Gravity.CENTER);
        cancel.setPadding(0, dp(14), 0, dp(14));
        cancel.setOnClickListener(v -> finish());
        root.addView(cancel);

        return root;
    }

    private View row(String title, String sub, String mode) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(20), dp(14), dp(20), dp(14));
        row.setClickable(true); row.setFocusable(true);
        row.setOnClickListener(v -> pick(mode));

        TextView t = new TextView(this);
        t.setText(title); t.setTextSize(15); t.setTextColor(0xFFECEFF1);
        t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        row.addView(t);

        TextView s = new TextView(this);
        s.setText(sub); s.setTextSize(12); s.setTextColor(0xFF78909C);
        row.addView(s);

        return row;
    }

    private void pick(String mode) {
        finish();
        getWindow().getDecorView().postDelayed(() -> {
            Intent i = new Intent(this, CaptureActivity.class);
            i.putExtra(EXTRA_MODE, mode);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        }, 50);
    }

    private View div() {
        View v = new View(this);
        v.setBackgroundColor(0xFF1E3A5F);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        return v;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
