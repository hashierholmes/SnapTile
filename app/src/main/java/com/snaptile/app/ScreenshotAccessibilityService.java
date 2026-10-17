package com.snaptile.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
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
 *              This needs CAPABILITY_CAN_TAKE_SCREENSHOT, which the XML grants.
 *              A build that withholds it (tested 2026-10-17, bought nothing) is
 *              detected at request time and routed to the MediaProjection path.
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
     * The *capabilities* (read window content, filter key events, inject gestures) are
     * not settable through the public API — they come from
     * res/xml/accessibility_service_config.xml, which declares nothing but
     * canTakeScreenshot="true". These three fields are settable, so pin them here to
     * keep the live AccessibilityServiceInfo identical to the XML on every OEM
     * (banking/UPI risk SDKs read the live info, not just the declared XML).
     */
    private void enforceMinimalCapabilities() {
        try {
            android.accessibilityservice.AccessibilityServiceInfo info = getServiceInfo();
            if (info == null) return;
            info.eventTypes = android.view.accessibility.AccessibilityEvent.TYPE_WINDOWS_CHANGED;
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
                // takeScreenshot() can never succeed. Route to the
                // MediaProjection path instead — all three modes keep working,
                // each capture needs one consent dialog.
                startLegacyCapture();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            boolean ok = performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
            if (!ok) {
                toast("Screenshot failed");
            }
            // NOTE: on API 28-29 the system owns save/share UI.
            // Region / Paint modes cannot receive a bitmap here —
            // they fall back to MediaProjection (see pick() fallback).
        } else {
            startLegacyCapture();
        }
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
        Intent i = new Intent(this, CaptureActivity.class);
        i.putExtra(ScreenshotChooserActivity.EXTRA_MODE, sPendingMode);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    private void toast(String msg) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(ScreenshotAccessibilityService.this,
                        msg, Toast.LENGTH_SHORT).show());
    }
}
