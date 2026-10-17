package com.snaptile.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;

/**
 * The one place that knows which capture method SnapTile is set to use, and the
 * one place that routes a capture request accordingly.
 *
 * Two methods, both real, and the choice is the user's:
 *
 *  - NATIVE (default) goes through {@link ScreenshotAccessibilityService}. No
 *    consent dialog, no screen-recording indicator: one tap on the tile and the
 *    bitmap is in the app, which is what makes the region and paint modes
 *    possible. It requires an enabled accessibility service, and every bank and
 *    UPI app treats an enabled accessibility service as a risk signal and warns
 *    about it — including for apps that plainly only take screenshots. That is a
 *    property of the whole category, not of SnapTile's configuration: the
 *    otherwise-minimal, widely used ScreenshotTile app is flagged the same way.
 *
 *  - LEGACY goes through {@link CaptureActivity} and Android's MediaProjection
 *    consent dialog, exactly like any screen recorder. Nothing about it needs an
 *    accessibility service, so with the service off the warnings stop. The cost
 *    is the system consent dialog before each capture.
 *
 * Because "Legacy" is only worth anything with the service off, selecting it also
 * disables SnapTile's accessibility service (reversible at any time; Android
 * always requires the user to re-enable it).
 */
public final class CapturePrefs {

    private static final String PREFS = "snaptile";
    private static final String KEY_METHOD = "capture_method";

    public static final String NATIVE = "native";
    public static final String LEGACY = "legacy";

    private CapturePrefs() {
    }

    public static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String method(Context c) {
        String m = prefs(c).getString(KEY_METHOD, NATIVE);
        return LEGACY.equals(m) ? LEGACY : NATIVE;
    }

    public static void setMethod(Context c, String m) {
        prefs(c).edit().putString(KEY_METHOD, LEGACY.equals(m) ? LEGACY : NATIVE).apply();
    }

    public static boolean isLegacy(Context c) {
        return LEGACY.equals(method(c));
    }

    /**
     * True when SnapTile has an accessibility service enabled — either entry.
     *
     * Both entries behave identically (the second one exists only for the naming
     * test), so the question is never "which class", always "does this package
     * have one enabled". Asking the secure setting rather than the live static
     * instance also covers a fresh boot, before the service has been bound.
     */
    public static boolean isAccessibilityEnabled(Context c) {
        if (ScreenshotAccessibilityService.isEnabled()) return true;
        String enabled = Settings.Secure.getString(
                c.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabled != null && enabled.contains(c.getPackageName() + "/");
    }

    /**
     * Take one screenshot, by whichever method is selected.
     *
     * Native and not enabled is deliberately not a silent fallback: the user
     * picked the prompt-free method, and quietly asking for screen-recording
     * consent instead would be a different method than the one they chose. They
     * get the setup screen, which says what is missing and how to fix it.
     */
    public static void request(Context c, String mode) {
        if (!isLegacy(c)) {
            if (ScreenshotAccessibilityService.requestScreenshot(mode)) return;
            startSetup(c, AccessibilitySetupActivity.REASON_NOT_ENABLED);
            return;
        }
        startLegacy(c, mode);
    }

    /** MediaProjection path: consent dialog, then a one-shot capture. */
    public static void startLegacy(Context c, String mode) {
        Intent i = new Intent(c, CaptureActivity.class);
        i.putExtra(ScreenshotChooserActivity.EXTRA_MODE, mode);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        c.startActivity(i);
    }

    /** The how-to-enable screen, also used as the "you asked for this" detour above. */
    public static void startSetup(Context c, String reason) {
        Intent i = new Intent(c, AccessibilitySetupActivity.class);
        i.putExtra(AccessibilitySetupActivity.EXTRA_REASON, reason);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        c.startActivity(i);
    }
}
