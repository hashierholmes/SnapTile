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

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        sInstance = this;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        sInstance = null;
        return super.onUnbind(intent);
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
            takeScreenshotCompat();
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
            toast("Capture failed: " + e.getMessage());
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
