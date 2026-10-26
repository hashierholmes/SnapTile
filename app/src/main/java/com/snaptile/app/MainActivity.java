package com.snaptile.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final int BG = Color.rgb(13, 17, 23);
    private static final int CARD = Color.rgb(23, 28, 35);
    private static final int BLUE = Color.rgb(77, 163, 255);
    private static final int TEXT = Color.rgb(241, 244, 248);
    private static final int SECONDARY = Color.rgb(170, 180, 192);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySystemBarColors();
        setContentView(R.layout.activity_main);
        setupAccessCard();

        View menu = findViewById(R.id.btnAbout);
        if (menu != null) menu.setOnClickListener(v -> showAboutDialog());
    }

    private void applySystemBarColors() {
        Window window = getWindow();
        window.setStatusBarColor(BG);
        window.setNavigationBarColor(BG);
        window.setBackgroundDrawableResource(android.R.color.transparent);
        if (Build.VERSION.SDK_INT >= 29) {
            window.setNavigationBarContrastEnforced(false);
            window.setStatusBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= 26) {
            window.getDecorView().setSystemUiVisibility(0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAccessCard();
    }

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
        if (ScreenshotAccessibilityService.isEnabled()) return true;
        String enabled = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabled != null
                && enabled.contains(getPackageName() + "/.ScreenshotAccessibilityService");
    }

    private void showAboutDialog() {
        Dialog dialog = new Dialog(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(24), dp(22), dp(20));
        root.setBackground(roundRect(CARD, dp(26), 0xFF2B3541, dp(1)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_screenshot);
        logo.setContentDescription("SnapTile logo");
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(76), dp(76));
        logoParams.gravity = Gravity.CENTER;
        content.addView(logo, logoParams);

        TextView title = text("SnapTile", 25, TEXT, true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = wrapCenter();
        titleParams.topMargin = dp(12);
        content.addView(title, titleParams);

        String version = "Version " + getInstalledVersion();
        TextView versionView = text(version, 13, BLUE, true);
        versionView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams versionParams = wrapCenter();
        versionParams.topMargin = dp(5);
        content.addView(versionView, versionParams);

        TextView description = text(
                "A quick, flexible screenshot tool for Android. Capture your full screen, snip a region, or open the paint editor directly from Quick Settings. Choose Accessibility capture or use MediaProjection without enabling Accessibility.",
                14, SECONDARY, false);
        description.setGravity(Gravity.CENTER);
        description.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        descParams.topMargin = dp(16);
        content.addView(description, descParams);

        View divider = new View(this);
        divider.setBackgroundColor(0xFF303A46);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        dividerParams.topMargin = dp(22);
        dividerParams.bottomMargin = dp(16);
        content.addView(divider, dividerParams);

        TextView devHeading = text("DEVELOPER", 11, BLUE, true);
        devHeading.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(devHeading, matchWrap());

        LinearLayout devCard = linkCard("Hashier Holmes", "hash.is-a.dev", "↗",
                "https://hash.is-a.dev");
        LinearLayout.LayoutParams cardParams = matchWrap();
        cardParams.topMargin = dp(9);
        content.addView(devCard, cardParams);

        TextView projectHeading = text("PROJECT", 11, BLUE, true);
        LinearLayout.LayoutParams projectHeadingParams = matchWrap();
        projectHeadingParams.topMargin = dp(18);
        content.addView(projectHeading, projectHeadingParams);

        LinearLayout projectCard = linkCard("View project on GitHub",
                "github.com/hashierholmes/SnapTile", "↗",
                "https://github.com/hashierholmes/SnapTile");
        LinearLayout.LayoutParams projectParams = matchWrap();
        projectParams.topMargin = dp(9);
        content.addView(projectCard, projectParams);

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView close = text("DONE", 13, BLUE, true);
        close.setGravity(Gravity.CENTER);
        close.setBackground(roundRect(0xFF182D43, dp(14), 0xFF284E73, dp(1)));
        close.setPadding(dp(18), dp(13), dp(18), dp(13));
        close.setClickable(true);
        close.setFocusable(true);
        close.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams closeParams = matchWrap();
        closeParams.topMargin = dp(18);
        root.addView(close, closeParams);

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(32), dp(420));
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.dimAmount = 0.65f;
            window.setAttributes(lp);
        }
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    Math.min(getResources().getDisplayMetrics().widthPixels - dp(32), dp(420)),
                    Math.min((int) (getResources().getDisplayMetrics().heightPixels * 0.82),
                            dp(720)));
        }
    }

    private LinearLayout linkCard(String titleText, String subtitleText, String trailing,
                                  String url) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(13), dp(14), dp(13));
        card.setBackground(roundRect(0xFF111720, dp(16), 0xFF2B3541, dp(1)));
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception ignored) { }
        });

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        labels.addView(text(titleText, 14, TEXT, true));
        TextView subtitle = text(subtitleText, 12, SECONDARY, false);
        LinearLayout.LayoutParams subParams = wrap();
        subParams.topMargin = dp(4);
        labels.addView(subtitle, subParams);
        card.addView(labels);

        TextView arrow = text(trailing, 21, BLUE, true);
        arrow.setGravity(Gravity.CENTER);
        card.addView(arrow, new LinearLayout.LayoutParams(dp(28), dp(32)));
        return card;
    }

    private TextView text(String value, int sizeSp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private GradientDrawable roundRect(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(radius);
        if (strokeWidth > 0) shape.setStroke(strokeWidth, strokeColor);
        return shape;
    }

    private String getInstalledVersion() {
        try {
            if (Build.VERSION.SDK_INT >=  P_VERSION_CODE) {
                return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            }
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private static final int P_VERSION_CODE = 28;

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams wrapCenter() {
        LinearLayout.LayoutParams params = wrap();
        params.gravity = Gravity.CENTER_HORIZONTAL;
        return params;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }
}
