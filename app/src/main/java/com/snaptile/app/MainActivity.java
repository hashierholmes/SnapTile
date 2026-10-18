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
import android.widget.RadioButton;
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

        setupMethodCard();
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
        refreshMethodCard();
        refreshAccessCard();
    }

    /**
     * Capture method card — native (accessibility) or legacy (screen recording).
     *
     * The two methods are not a preference about style: one of them requires an
     * enabled accessibility service, and that is the thing banks and UPI apps
     * warn about. So the choice is presented with its cost written next to it,
     * and picking legacy also turns the service off, because a legacy method with
     * the service still enabled buys nothing.
     */
    private void setupMethodCard() {
        RadioButton nativeBtn = findViewById(R.id.rbMethodNative);
        RadioButton legacyBtn = findViewById(R.id.rbMethodLegacy);
        if (nativeBtn == null || legacyBtn == null) return;

        nativeBtn.setOnClickListener(v -> chooseMethod(CapturePrefs.NATIVE));
        legacyBtn.setOnClickListener(v -> chooseMethod(CapturePrefs.LEGACY));

        Button setup = findViewById(R.id.btnMethodSetup);
        if (setup != null) {
            setup.setOnClickListener(v -> CapturePrefs.startSetup(
                    this, AccessibilitySetupActivity.REASON_NOT_ENABLED));
        }

        refreshMethodCard();
    }

    private void chooseMethod(String method) {
        boolean wasLegacy = CapturePrefs.isLegacy(this);
        CapturePrefs.setMethod(this, method);

        if (CapturePrefs.LEGACY.equals(method) && !wasLegacy) {
            // Legacy exists to keep the service off; leaving it on would mean the
            // user paid the warning for nothing.
            ScreenshotAccessibilityService.disableForBanking();
            prefs().edit().putBoolean(KEY_BANKING_OFF, true).apply();
            Toast.makeText(this, R.string.method_legacy_toast, Toast.LENGTH_LONG).show();
        } else if (CapturePrefs.NATIVE.equals(method) && wasLegacy) {
            prefs().edit().putBoolean(KEY_BANKING_OFF, false).apply();
            Toast.makeText(this, R.string.method_native_toast, Toast.LENGTH_LONG).show();
        }

        refreshMethodCard();
        refreshAccessCard();
    }

    private void refreshMethodCard() {
        RadioButton nativeBtn = findViewById(R.id.rbMethodNative);
        RadioButton legacyBtn = findViewById(R.id.rbMethodLegacy);
        Button setup = findViewById(R.id.btnMethodSetup);
        if (nativeBtn == null || legacyBtn == null) return;

        boolean legacy = CapturePrefs.isLegacy(this);
        nativeBtn.setChecked(!legacy);
        legacyBtn.setChecked(legacy);

        // Nothing to set up while legacy is selected (it needs no service) or
        // while the service is already on.
        if (setup != null) {
            setup.setVisibility(legacy || isAccessServiceOn() ? View.GONE : View.VISIBLE);
        }
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

        refreshAccessCard();
    }

    /**
     * Banking mode: disable SnapTile's own accessibility service so bank/UPI apps
     * stop warning. Capture keeps working through the MediaProjection consent
     * dialog; re-enabling is the user's call, as Android requires.
     */
    private void onBankingOffClicked() {
        // One tap for "stop warning me": switch to the legacy method and take the
        // accessibility service down with it. The method change is unconditional -
        // captures must route to MediaProjection either way - but only a real
        // disableSelf() is allowed to claim the warning is gone.
        CapturePrefs.setMethod(this, CapturePrefs.LEGACY);
        boolean disabled = ScreenshotAccessibilityService.disableForBanking();
        prefs().edit().putBoolean(KEY_BANKING_OFF, disabled).apply();
        refreshMethodCard();
        refreshAccessCard();

        if (disabled) {
            Toast.makeText(this, R.string.banking_mode_toast, Toast.LENGTH_LONG).show();
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
        // The legacy method implies "service off" — that is the point of choosing
        // it — so the card should read as banking-safe as soon as it is selected,
        // whether or not this process was the one that disabled the service.
        boolean bankingOff = !on
                && (CapturePrefs.isLegacy(this) || prefs().getBoolean(KEY_BANKING_OFF, false));

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
        // If we just disabled the service ourselves, ENABLED_ACCESSIBILITY_SERVICES
        // lags for a moment. Trust our own action so the card flips immediately
        // instead of briefly claiming the service is still on.
        if (prefs().getBoolean(KEY_BANKING_OFF, false)
                && !ScreenshotAccessibilityService.isEnabled()) {
            return false;
        }
        // Live instance is fastest; the secure setting covers fresh boot and
        // process death, and matches either SnapTile entry.
        return CapturePrefs.isAccessibilityEnabled(this);
    }

}