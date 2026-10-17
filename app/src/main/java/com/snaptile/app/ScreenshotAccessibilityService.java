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
 * Clean screenshot path — PowerMenu-style.
 *
 * Instead of MediaProjection (screen-record consent dialog + recording dot +
 * foreground service + VirtualDisplay), this service uses the platform
 * accessibility screenshot APIs:
 *
 *  - API 30+ : takeScreenshot(displayId, executor, callback) returns a Bitmap
 *              directly to the app. All 3 SnapTile modes keep working.
 *              This needs CAPABILITY_CAN_TAKE_SCREENSHOT, which the XML grants
 *              as of 1.1.12 and withholds in 1.1.13. The 1.1.9 probe tested the
 *              same thing and appeared to exonerate the capability, but that
 *              build still declared accessibilityEventTypes="typeWindowsChanged",
 *              so its result could not be attributed. It is the last declared
 *              value the flagged service has that no enabled, unflagged service
 *              on the test device has (Key Mapper: window content + key
 *              filtering + gestures; Shizuku-Next: window content). A build
 *              without the capability detects it at request time and routes to
 *              the MediaProjection path — all three modes still work, one
 *              consent dialog per capture.
 *  - API 28-29 : performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT) triggers
 *              the system screenshot UI. System owns save/share.
 *  - API <28 : no accessibility screenshot API — falls back to CaptureActivity
 *              (MediaProjection).
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
     * Banking and UPI security SDKs flag *any* enabled third-party accessibility
     * service (they cannot tell a legitimate tool from malware), so the only fix
     * is for the service to not be enabled while the user pays. disableSelf() is
     * the one route an app is allowed to take; re-enabling always requires the
     * user, which is by design in Android's security model.
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
     * The *capabilities* (read window content, filter key events, inject gestures)
     * are not settable through the public API — they come from
     * res/xml/accessibility_service_config.xml, which in 1.1.13 declares nothing at
     * all. These three fields are settable, so pin them here to
     * keep the live AccessibilityServiceInfo identical to the XML on every OEM
     * (banking/UPI risk SDKs read the live info, not just the declared XML).
     * eventTypes is pinned to 0 for the same reason the XML omits it: the service
     * consumes no events (see onAccessibilityEvent), so requesting none is honest.
     * It has to be pinned here too — leaving the old TYPE_WINDOWS_CHANGED value
     * would re-impose it at runtime on top of the XML, and the live info is what
     * a risk SDK actually reads. Dropping it is the last declarative test left
     * against the bank/UPI warning; if it does not clear it, provenance is the
     * only explanation remaining.
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
            } else {
                // The config does not declare canTakeScreenshot, so
                // takeScreenshot() can never succeed. A global action does not
                // need the capability, so let the system take the shot and own
                // the result — same route as API 28-29. Region/Paint modes
                // cannot receive that bitmap; they fall through to
                // startLegacyCapture() where the MediaProjection path exists.
                systemScreenshot();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            systemScreenshot();
        } else {
            startLegacyCapture();
        }
    }

    /**
     * System-owned screenshot: performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT).
     *
     * Needs no capability and no permission, so it also works in builds that
     * withhold CAPABILITY_CAN_TAKE_SCREENSHOT. The system saves the image and
     * shows its own UI; SnapTile only gets a bitmap through takeScreenshot(),
     * which is why region/paint modes need either the capability or the
     * MediaProjection path.
     */
    private void systemScreenshot() {
        boolean ok = performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
        toast(ok
                ? "System screenshot saved (SnapTile has no screenshot capability in this build)"
                : "Screenshot failed");
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
            // 1.1.14 removes the mediaProjection declaration on purpose, so the
            // MediaProjection route is dead here and must not be entered: it
            // would start an undeclared service and hang waiting for it.
            toast("No capture path in this build — turn SnapTile's accessibility service on");
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
     * one source of truth: the manifest. A build without the declaration — like
     * 1.1.14, which removes it to test whether the capture declaration is what
     * bank/UPI scanners key on — reports false and capture is refused instead of
     * failing halfway through a consent flow.
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
