package com.snaptile.a11yprobe;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Tells the user which configuration this variant declares and jumps straight to
 * Accessibility settings so the service can be enabled.
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView text = new TextView(this);
        text.setText(getString(R.string.probe_hint, getString(R.string.probe_config)));
        text.setTextSize(15f);
        root.addView(text);

        Button open = new Button(this);
        open.setText(R.string.probe_open);
        open.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(open);

        setContentView(root);
    }
}
