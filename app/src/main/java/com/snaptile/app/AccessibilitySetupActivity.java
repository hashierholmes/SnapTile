package com.snaptile.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

/**
 * How to turn the native capture method on, including the step Android hides.
 *
 * Reached two ways: from the main screen's "Set up the accessibility method"
 * button, and automatically when a capture is requested while the method is
 * native but the accessibility service is off — including from the Quick
 * Settings tile, where a silent switch to the Legacy method would be answering a
 * different question than the one the user asked.
 *
 * The screen is deliberately actionable rather than a wall of text: two buttons
 * that open the two system screens involved, plus a drawn schematic of the taps
 * in between, because "Allow restricted settings" only appears in the App info
 * page's overflow menu and is easy to miss.
 */
public class AccessibilitySetupActivity extends Activity {

    public static final String EXTRA_REASON = "reason";
    /** A capture was requested with the native method selected but nothing enabled. */
    public static final String REASON_NOT_ENABLED = "not_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(0xFF0B0B0B);
        window.setNavigationBarColor(0xFF000000);

        setContentView(R.layout.activity_accessibility_setup);

        Button allow = findViewById(R.id.btnAllowRestricted);
        if (allow != null) allow.setOnClickListener(v -> openAppInfo());

        Button open = findViewById(R.id.btnOpenA11y);
        if (open != null) open.setOnClickListener(v -> startActivity(
                new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        Button close = findViewById(R.id.btnSetupClose);
        if (close != null) close.setOnClickListener(v -> finish());

        refreshState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Coming back from Settings is the whole point of this screen, so the
        // status line has to be current the moment it is visible again.
        refreshState();
    }

    private void refreshState() {
        TextView tv = findViewById(R.id.tvSetupState);
        if (tv == null) return;
        if (CapturePrefs.isAccessibilityEnabled(this)) {
            tv.setText(R.string.setup_enabled);
            return;
        }
        // The wording depends on why the user is here: a capture they asked for
        // did not happen, or they came to set the method up on purpose.
        boolean blocked = REASON_NOT_ENABLED.equals(getIntent().getStringExtra(EXTRA_REASON));
        tv.setText(blocked ? R.string.setup_not_enabled : R.string.setup_intro);
    }

    /**
     * SnapTile's own App info page — where the overflow menu with "Allow
     * restricted settings" lives. There is no public intent that performs the
     * two taps, and there should not be: the point of the setting is that the
     * user performs it.
     */
    private void openAppInfo() {
        Intent i = new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null));
        startActivity(i);
    }
}
