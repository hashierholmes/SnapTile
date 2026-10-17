package com.snaptile.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.snaptile.app.R;

/**
 * Small launcher activity that presents SnapTile's setup/information screen.
 *
 * Screenshot capture itself is intentionally not owned by this activity. The Quick
 * Settings tile starts the capture flow so the app can stay out of the user's way.
 */
public class MainActivity extends Activity {

    private static final String PREFS = "snaptile";
    /** Set when the user turns accessibility off from here (banking mode). */
    private static final String KEY_BANKING_OFF = "banking_mode_off";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(0xFF0B0B0B);
        window.setNavigationBarColor(0xFF000000);

        setContentView(R.layout.activity_main);

        setupAccessCard();

        TextView tvDeveloperCredit =
                findViewById(R.id.tvDeveloperCredit);
                
        String credit = "Made with 💙 by @hashierholmes";
        SpannableString creditText = new SpannableString(credit);

        int start = credit.indexOf("@hashierholmes");
        int end = start + "@hashierholmes".length();

        creditText.setSpan(
                new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        Intent intent = new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://hashierholmes.vercel.app")
                        );
                        startActivity(intent);
                    }

                    @Override
                    public void updateDrawState(TextPaint ds) {
                        ds.setColor(Color.rgb(96, 165, 250));
                        ds.setUnderlineText(false);
                    }
                },
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        tvDeveloperCredit.setText(creditText);
        tvDeveloperCredit.setMovementMethod(LinkMovementMethod.getInstance());
        tvDeveloperCredit.setHighlightColor(Color.TRANSPARENT);        
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAccessCard();
    }

    /** PowerMenu-style onboarding: one tap to system Accessibility settings. */
    private void setupAccessCard() {
        Button enable = findViewById(R.id.btnEnableAccess);
        if (enable != null) {
            enable.setOnClickListener(v -> startActivity(
                    new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        }

        Button banking = findViewById(R.id.btnBankingOff);
        if (banking != null) {
            banking.setOnClickListener(v -> onBankingOffClicked());
        }

        Button diag = findViewById(R.id.btnDiagnostics);
        if (diag != null) {
            diag.setOnClickListener(v -> startActivity(
                    new Intent(MainActivity.this, DiagnosticsActivity.class)));
        }

        refreshAccessCard();
    }

    /**
     * Banking mode: disable SnapTile's own accessibility service so bank/UPI apps
     * stop warning. Capture keeps working through the MediaProjection consent
     * dialog; re-enabling is the user's call, as Android requires.
     */
    private void onBankingOffClicked() {
        if (ScreenshotAccessibilityService.disableForBanking()) {
            prefs().edit().putBoolean(KEY_BANKING_OFF, true).apply();
            Toast.makeText(this, R.string.banking_mode_toast, Toast.LENGTH_LONG).show();
            refreshAccessCard();
            return;
        }
        // Not bound yet, or Android 6 or older where disableSelf() does not exist.
        Toast.makeText(this, R.string.banking_mode_settings, Toast.LENGTH_LONG).show();
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    private void refreshAccessCard() {
        TextView title = findViewById(R.id.tvAccessTitle);
        TextView text = findViewById(R.id.tvAccessText);
        Button enable = findViewById(R.id.btnEnableAccess);
        Button banking = findViewById(R.id.btnBankingOff);
        if (title == null || enable == null) return;

        boolean bound = ScreenshotAccessibilityService.isEnabled();
        if (bound) {
            // Service is genuinely bound again — banking mode is no longer in effect.
            prefs().edit().putBoolean(KEY_BANKING_OFF, false).apply();
        }

        boolean on = isAccessServiceOn();
        boolean bankingOff = !on && prefs().getBoolean(KEY_BANKING_OFF, false);

        if (on) {
            title.setText(R.string.accessibility_on);
        } else {
            title.setText(bankingOff
                    ? R.string.banking_mode_title
                    : R.string.enable_accessibility_title);
        }
        if (text != null) {
            text.setText(bankingOff
                    ? R.string.banking_mode_text
                    : R.string.enable_accessibility_text);
        }

        enable.setVisibility(on ? View.GONE : View.VISIBLE);
        if (banking != null) banking.setVisibility(on ? View.VISIBLE : View.GONE);
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private boolean isAccessServiceOn() {
        // Live instance is fastest (service connected at least once this boot).
        if (ScreenshotAccessibilityService.isEnabled()) return true;
        // If we just disabled ourselves, ENABLED_ACCESSIBILITY_SERVICES lags for a
        // moment. Trust our own action so the card flips immediately instead of
        // briefly claiming the service is still on.
        if (prefs().getBoolean(KEY_BANKING_OFF, false)) return false;
        // Otherwise read the secure setting (covers fresh boot / process death).
        String enabled = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabled != null
                && enabled.contains(getPackageName() + "/.ScreenshotAccessibilityService");
    }

}