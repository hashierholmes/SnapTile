package com.snaptile.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 64, 48, 64);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setBackgroundColor(0xFF0D1B2A);

        // Title
        TextView title = new TextView(this);
        title.setText("SnapTile");
        title.setTextSize(28);
        title.setTextColor(0xFF42A5F5);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 8);
        layout.addView(title);

        // Subtitle
        TextView subtitle = new TextView(this);
        subtitle.setText("Quick Settings Screenshot Tool");
        subtitle.setTextSize(14);
        subtitle.setTextColor(0xFF90CAF9);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 48);
        layout.addView(subtitle);

        // Step cards
        addCard(layout, "Step 1", "Add the tile to Quick Settings",
                "Swipe down twice to open the full Quick Settings panel, tap the pencil/edit icon, find the \"Screenshot\" tile and drag it into your active tiles.", 0xFF1565C0);

        addCard(layout, "Step 2", "Tap the tile",
                "Tap the Screenshot tile — a small menu appears with three options.", 0xFF1B5E20);

        addCard(layout, "Step 3", "Pick a mode",
                "• Full Screen — saves the entire display instantly as a lossless PNG\n• Snip Region — screen freezes, drag a rectangle to crop, then annotate and save\n• Full Screen + Paint — captures full screen and drops you straight into the paint editor", 0xFF4A148C);

        addCard(layout, "Paint editor",
                "After snipping or in Full Screen + Paint mode:\n• Save icon — writes the final PNG to your gallery\n• Undo / Redo — per-stroke history\n• Brush icon — tap to cycle Small / Medium / Large\n• Color dots — 12 colors, tap to select", 0xFF0D47A1);

        addCard(layout, "Where are screenshots saved?",
                "Pictures/Screenshots — shows up in your Gallery app immediately. Files are named Screenshot_YYYYMMDD_HHmmss.png and are always lossless.", 0xFF1A237E);

        TextView ready = new TextView(this);
        ready.setText("✓  All set. Tap the tile, tap Start now once, and you're capturing.");
        ready.setTextSize(13);
        ready.setTextColor(0xFF66BB6A);
        ready.setGravity(Gravity.CENTER);
        ready.setPadding(0, 48, 0, 0);
        layout.addView(ready);

        scrollView.addView(layout);
        setContentView(scrollView);
    }

    private void addCard(LinearLayout parent, String heading, String body, String note, int accentColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(32, 28, 32, 28);
        card.setBackgroundColor(0xFF16213E);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 20);
        card.setLayoutParams(params);

        // Accent bar
        View accent = new View(this);
        LinearLayout.LayoutParams accentParams = new LinearLayout.LayoutParams(6, ViewGroup.LayoutParams.MATCH_PARENT);
        // Draw accent as background tint on heading instead
        TextView h = new TextView(this);
        h.setText(heading);
        h.setTextSize(15);
        h.setTextColor(accentColor | 0xFF000000);
        h.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        h.setPadding(0, 0, 0, 10);
        card.addView(h);

        if (!body.isEmpty()) {
            TextView b = new TextView(this);
            b.setText(body);
            b.setTextSize(13);
            b.setTextColor(0xFFB0BEC5);
            b.setLineSpacing(4, 1.2f);
            card.addView(b);
        }

        if (note != null && !note.isEmpty()) {
            TextView n = new TextView(this);
            n.setText(note);
            n.setTextSize(12);
            n.setTextColor(0xFF78909C);
            n.setPadding(0, 8, 0, 0);
            card.addView(n);
        }

        parent.addView(card);
    }

    private void addCard(LinearLayout parent, String heading, String body, int accentColor) {
        addCard(parent, heading, body, null, accentColor);
    }

    private Button makeButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(0xFF000000);
        btn.setBackgroundColor(0xFF42A5F5);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 8, 0, 8);
        btn.setLayoutParams(p);
        return btn;
    }
}
