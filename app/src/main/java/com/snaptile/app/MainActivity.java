package com.snaptile.app;

import android.app.Activity;
import android.content.Intent;
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

import com.snaptile.app.R;

/**
 * Small launcher activity that presents SnapTile's setup/information screen.
 *
 * Screenshot capture itself is intentionally not owned by this activity. The Quick
 * Settings tile starts the capture flow so the app can stay out of the user's way.
 */
public class MainActivity extends Activity {

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
        Button btn = findViewById(R.id.btnEnableAccess);
        if (btn != null) {
            btn.setOnClickListener(v -> startActivity(
                    new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        }
        refreshAccessCard();
    }

    private void refreshAccessCard() {
        TextView title = findViewById(R.id.tvAccessTitle);
        Button btn = findViewById(R.id.btnEnableAccess);
        if (title == null || btn == null) return;
        if (isAccessServiceOn()) {
            title.setText(R.string.accessibility_on);
            btn.setVisibility(View.GONE);
        } else {
            title.setText(R.string.enable_accessibility_title);
            btn.setVisibility(View.VISIBLE);
        }
    }

    private boolean isAccessServiceOn() {
        // Live instance is fastest (service connected at least once this boot).
        if (ScreenshotAccessibilityService.isEnabled()) return true;
        // Otherwise read the secure setting (covers fresh boot / process death).
        String enabled = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabled != null
                && enabled.contains(getPackageName() + "/.ScreenshotAccessibilityService");
    }

}