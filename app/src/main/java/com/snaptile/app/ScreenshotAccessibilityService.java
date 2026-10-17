package com.snaptile.app;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

/**
 * Native capture path — prompt-free screenshots through the accessibility API.
 *
 * MediaProjection needs a consent dialog and a foreground service; this does not:
 *
 *  - API 30+ : performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT) makes the
 *              system take and save the shot with no consent dialog. That needs
 *              no capability, and since 1.2.1 it is the only full-screen route
 *              here: the config declares no capability at all, matching the
 *              ScreenshotTile minimum. Region and paint modes need the bitmap in
 *              the app, so they go through MediaProjection (consent) instead.
 *  - API 28-29 : the same global action; same global action since API 28.
 *  - API <28 : no accessibility screenshot API — MediaProjection (CaptureActivity).
 *
 * Whether this path runs at all is the user's choice in {@link CapturePrefs}: with
 * the legacy method selected, capture never reaches this service. When it does
 * run, an enabled accessibility service is what bank and UPI apps warn about, and
 * that warning is a property of the category rather than of this config — the
 * otherwise-minimal ScreenshotTile app is flagged the same way.
 *
 * Tile / chooser never call the platform APIs directly; only the bound
 * AccessibilityService instance can. They go through requestScreenshot().
 */
public class ScreenshotAccessibilityService extends AccessibilityService {

    private static ScreenshotAccessibilityService sInstance;
    private static String sPendingMode = ScreenshotChooserActivity.MODE_FULL;

    /** Called from Tile / Chooser. Returns false when the service is not enabled. */
    public static boolean requestScreenshot(String mode) {
        if (sInstance == null) return false;
        if (mode != null) sPendingMode = mode;
        sInstance.handleRequest();
        return true;
    }

    public static boolean isEnabled() {
        return sInstance != null;
    }

    /**
     * Banking mode — turn SnapTile's own service off so bank/UPI apps stop warning.
     *
     * Bank and UPI security SDKs warn about enabled third-party accessibility
     * services. Not every one of them: Key Mapper, Shizuku, Bitwarden and Twilight
     * are all enabled on the reference device and none of them is flagged. What is
     * flagged is screenshot apps — SnapTile in every configuration it has ever
     * shipped, and ScreenshotTile, whose accessibility config is even emptier. So
     * the one reliable fix while paying is for SnapTile's service not to be
     * enabled at all. disableSelf() is the one route an app is allowed to take;
     * re-enabling always requires the user, which is by design in Android's
     * security model.
     *
     * @return true if the service disabled itself; false when it was not bound or
     *         the platform is too old (API < 24), in which case the caller must
     *         send the user to Settings.
     */
    public static boolean disableForBanking() {
        // disableSelf() was added in API 24 (Nougat).
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false;
        ScreenshotAccessibilityService svc = sInstance;
        if (svc == null) return false;
        try {
            svc.disableSelf();
            sInstance = null;   // reflect the new state immediately; onUnbind() is the backstop
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        enforceMinimalCapabilities();
        sInstance = this;
    }

    /**
     * Re-assert the minimal event/feedback/flags set at runtime.
     *
     * The *capabilities* (read window content, filter key events, inject gestures,
     * take screenshots) are not settable through the public API, and as of 1.2.1
     * the config declares none of them either — it is the ScreenshotTile minimum:
     * an event type, a feedback type and a description. These three fields are
     * settable, so pin them here to keep the live AccessibilityServiceInfo
     * identical to the XML on every OEM (risk SDKs read the live info, not just
     * the declared XML).
     * eventTypes is pinned to 0 because the service consumes no events (see
     * onAccessibilityEvent), so requesting none is honest even though the XML
     * mirrors the reference app's typeWindowStateChanged. It also has to be pinned
     * here: leaving a declared value in place would re-impose it at runtime on top
     * of the XML, and the live info is what a scanner reads.
     */
    private void enforceMinimalCapabilities() {
        try {
            android.accessibilityservice.AccessibilityServiceInfo info = getServiceInfo();
            if (info == null) return;
            info.eventTypes = 0;
            info.feedbackType = android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC;
            info.flags = android.accessibilityservice.AccessibilityServiceInfo.DEFAULT;
            info.notificationTimeout = 0;
            setServiceInfo(info);
        } catch (Exception ignored) {
            // Capability tightening is best-effort; capture must never break here.
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        sInstance = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        sInstance = null;
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // No event processing needed — service exists only for screenshot actions.
    }

    @Override
    public void onInterrupt() {
    }

    private void handleRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (hasScreenshotCapability()) {
                takeScreenshotCompat();
            } else if (ScreenshotChooserActivity.MODE_FULL.equals(sPendingMode)) {
                // No capability: 1.2.1's config declares none, and an OEM could
                // withhold it even if one were declared. A global action needs no
                // capability, so let the system take the shot and own the result —
                // the same route as API 28-29. Region and paint modes need the
                // bitmap in-app, so they go to MediaProjection instead of silently
                // degrading to a system screenshot.
                systemScreenshot();
            } else {
                startLegacyCapture();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (ScreenshotChooserActivity.MODE_FULL.equals(sPendingMode)) {
                systemScreenshot();
            } else {
                startLegacyCapture();
            }
        } else {
            startLegacyCapture();
        }
    }

    /**
     * System-owned screenshot: performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT).
     *
     * Needs no capability and no permission, so it works in this build (whose
     * config declares no capability by design) and on API 28-29, where the
     * accessibility screenshot API does not exist. The system saves the image and
     * shows its own UI; SnapTile only gets a bitmap through takeScreenshot(),
     * which is why region/paint modes take the MediaProjection path instead.
     *
     * The message is deliberately plain: this is the normal route for full-screen
     * captures, not a degraded one, so there is nothing for the user to fix.
     */
    private void systemScreenshot() {
        boolean ok = performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
        toast(ok ? "Screenshot taken" : "Screenshot failed");
    }

    /**
     * True when the platform actually granted CAPABILITY_CAN_TAKE_SCREENSHOT.
     *
     * The capability comes from res/xml/accessibility_service_config.xml and has
     * no runtime setter, so this reads the live AccessibilityServiceInfo rather
     * than assuming: if the config stops declaring it, takeScreenshot() cannot
     * succeed and the caller must use another route. Checking first avoids
     * relying on the framework's behaviour when the capability is absent.
     */
    private boolean hasScreenshotCapability() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false;
        android.accessibilityservice.AccessibilityServiceInfo info = getServiceInfo();
        return info != null
                && (info.getCapabilities()
                & android.accessibilityservice.AccessibilityServiceInfo.CAPABILITY_CAN_TAKE_SCREENSHOT) != 0;
    }

    @RequiresApi(30)
    private void takeScreenshotCompat() {
        try {
            // Handler-post executor avoids getMainExecutor() (API 28+) entirely.
            android.os.Handler mainHandler = new android.os.Handler(Looper.getMainLooper());
            java.util.concurrent.Executor executor = mainHandler::post;
            takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    executor,
                    new TakeScreenshotCallback() {
                        @Override
                        public void onSuccess(ScreenshotResult screenshot) {
                            Bitmap hw = Bitmap.wrapHardwareBuffer(
                                    screenshot.getHardwareBuffer(),
                                    screenshot.getColorSpace());
                            if (hw == null) {
                                toast("Capture failed");
                                return;
                            }
                            // Copy to a mutable software bitmap so SnipActivity
                            // can edit / recycle freely (matches old ImageReader path).
                            Bitmap sw = hw.copy(Bitmap.Config.ARGB_8888, true);
                            if (sw == null) {
                                toast("Capture failed");
                                return;
                            }
                            routeBitmap(sw);
                        }

                        @Override
                        public void onFailure(int errorCode) {
                            toast("Capture failed: " + errorCode);
                        }
                    });
        } catch (Exception e) {
            // Missing capability / OEM weirdness: never leave the user with a
            // dead tile — the MediaProjection path also handles all 3 modes.
            startLegacyCapture();
        }
    }

    private void routeBitmap(Bitmap bmp) {
        String mode = sPendingMode != null ? sPendingMode : ScreenshotChooserActivity.MODE_FULL;
        if (ScreenshotChooserActivity.MODE_FULL_PAINT.equals(mode)) {
            CaptureActivity.pendingBitmap = bmp;
            startActivity(new Intent(this, SnipActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(SnipActivity.EXTRA_FULL_PAINT, true));
        } else if (ScreenshotChooserActivity.MODE_REGION.equals(mode)) {
            CaptureActivity.pendingBitmap = bmp;
            startActivity(new Intent(this, SnipActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else {
            ImageSaver.save(this, bmp);
            bmp.recycle();
        }
    }

    private void startLegacyCapture() {
        if (!projectionPathDeclared()) {
            // Only reachable on a build without the MediaProjection declaration;
            // 1.2.0 has it back, so this exists to fail loudly rather than hang
            // waiting for a foreground service that can never start.
            toast("SnapTile cannot capture on this build — reinstall it");
            return;
        }
        Intent i = new Intent(this, CaptureActivity.class);
        i.putExtra(ScreenshotChooserActivity.EXTRA_MODE, sPendingMode);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    /**
     * True when this build declares ProjectionService (and therefore the
     * mediaProjection foreground-service type and permission that go with it).
     *
     * Asking the PackageManager rather than asserting a build-time constant keeps
     * one source of truth: the manifest. 1.1.14 removed the declaration to test
     * whether it was what bank/UPI scanners key on, and that test came back
     * negative, so 1.2.0 declares it again — the legacy capture method and the
     * pre-API-28 fallback both need it.
     */
    private boolean projectionPathDeclared() {
        try {
            getPackageManager().getServiceInfo(
                    new ComponentName(this, ProjectionService.class), 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void toast(String msg) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(ScreenshotAccessibilityService.this,
                        msg, Toast.LENGTH_SHORT).show());
    }
}
