package com.snaptile.app;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import java.nio.ByteBuffer;

/**
 * Coordinates the one-shot screen capture lifecycle.
 *
 * The activity obtains the user's capture consent first, then starts the required
 * media-projection foreground service, creates the {MediaProjection}, and reads
 * exactly one frame through an {ImageReader}. Consent-before-service is an Android 14
 * (targetSdk 34) requirement. Keeping projection creation here avoids passing the
 * projection result token through the service boundary.
 *
 * After the frame is obtained, the projection is released immediately. Full-screen
 * captures are handed to {ImageSaver}; region and full-paint modes pass the
 * bitmap to {SnipActivity} for editing.
 */
public class CaptureActivity extends Activity {

    private static final String TAG = "CaptureActivity";
    private static final int    REQ = 1001;

    /** 40 polls x 50 ms = 2 s ceiling for the mediaProjection FGS to reach the foreground. */
    private static final int MAX_FGS_WAIT_TRIES = 40;

    static Bitmap pendingBitmap = null;

    private String        mMode;
    private HandlerThread mThread;
    private Handler       mHandler;
    private MediaProjection  mProjection;
    private VirtualDisplay   mVirtualDisplay;
    private ImageReader      mImageReader;
    private boolean          mCaptured = false;
    private int mW, mH, mDpi;

    /** Consent result/data parked while the foreground service comes up. */
    private int    mConsentResult;
    private Intent mConsentData;
    private int    mFgsTries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMode = getIntent().getStringExtra(ScreenshotChooserActivity.EXTRA_MODE);
        if (mMode == null) mMode = ScreenshotChooserActivity.MODE_FULL;

        // This activity only exists to drive MediaProjection. In a build that does
        // not declare ProjectionService / the mediaProjection foreground-service
        // type (1.1.14 removes both), asking for consent would end in a service
        // that can never start, so refuse up front instead of hanging on it.
        try {
            getPackageManager().getServiceInfo(
                    new ComponentName(this, ProjectionService.class), 0);
        } catch (PackageManager.NameNotFoundException e) {
            toast("No capture path in this build — turn SnapTile's accessibility service on");
            finish();
            return;
        }

        // Capture at the physical display size rather than the activity window size.
        // The chooser/editor may not occupy the whole screen, but the screenshot must.
        WindowManager  wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(dm);
        mW = dm.widthPixels; mH = dm.heightPixels; mDpi = dm.densityDpi;

        // ImageReader callbacks run on this handler so bitmap conversion and projection
        // cleanup do not block the main UI thread.
        mThread = new HandlerThread("CaptureThread");
        mThread.start();
        mHandler = new Handler(mThread.getLooper());

        // Consent comes first. Android 14 (targetSdk 34) requires that
        // createScreenCaptureIntent() is called and granted BEFORE the mediaProjection
        // foreground service is created; starting the service up front is rejected on
        // Android 14+. The service is started in onActivityResult() instead.
        requestProjection();
    }

    private void requestProjection() {
        runOnUiThread(() -> {
            MediaProjectionManager mgr =
                    (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            startActivityForResult(mgr.createScreenCaptureIntent(), REQ);
        });
    }

    @Override
    protected void onActivityResult(int req, int result, Intent data) {
        if (req != REQ) return;
        if (result != RESULT_OK || data == null) {
            toast("Cancelled");
            done(); return;
        }

        // Consent granted -> now the mediaProjection foreground service may start.
        // getMediaProjection() is only valid once that service is actually running,
        // so start it and poll ProjectionService.isRunning() rather than sleeping.
        try {
            startForegroundService(new Intent(this, ProjectionService.class));
        } catch (Exception e) {
            Log.e(TAG, "startForegroundService failed", e);
            toast("Capture error: " + e.getMessage());
            done(); return;
        }

        mConsentResult = result;
        mConsentData   = data;
        mFgsTries      = 0;
        Handler h = mHandler;
        if (h != null) h.postDelayed(this::waitForProjectionService, 50);
    }

    /**
     * Bridges consent -> running foreground service -> MediaProjection.
     * Re-polls every 50 ms (max 2 s) so a slow system_server never breaks capture.
     */
    private void waitForProjectionService() {
        Handler h = mHandler;
        if (h == null || isFinishing()) return;

        if (!ProjectionService.isRunning()) {
            if (++mFgsTries > MAX_FGS_WAIT_TRIES) {
                Log.e(TAG, "mediaProjection foreground service never started");
                toast("Capture error: service failed to start");
                runOnUiThread(this::done);
                return;
            }
            h.postDelayed(this::waitForProjectionService, 50);
            return;
        }

        runOnUiThread(() -> {
            // Handler may be gone if the activity was torn down while we hopped threads.
            Handler ui = mHandler;
            if (ui == null || isFinishing()) { done(); return; }

            MediaProjectionManager mgr =
                    (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            try {
                mProjection = mgr.getMediaProjection(mConsentResult, mConsentData);
            } catch (Exception e) {
                Log.e(TAG, "getMediaProjection failed", e);
                toast("Could not create projection");
                done(); return;
            }
            if (mProjection == null) {
                toast("Could not create projection");
                done(); return;
            }

            mProjection.registerCallback(new MediaProjection.Callback() {
                @Override public void onStop() { releaseCapture(); }
            }, ui);

            // The projection can otherwise capture the consent/chooser UI itself. Wait
            // until those windows have been dismissed before creating the VirtualDisplay.
            ui.postDelayed(this::doCapture, 650);
        });
    }

    private void doCapture() {
        try {
            mImageReader = ImageReader.newInstance(mW, mH, PixelFormat.RGBA_8888, 2);
            mImageReader.setOnImageAvailableListener(reader -> {
                if (mCaptured) return;
                mCaptured = true;

                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    if (image == null) { mCaptured = false; return; }

                    Bitmap bmp = toBitmap(image);
                    image.close(); image = null;

                    // SnapTile only needs one frame, so release the projection as soon as the bitmap
                    // has been copied. This also removes the projection indicator promptly.
                    releaseCapture();

                    String mode = mMode;
                    if (ScreenshotChooserActivity.MODE_REGION.equals(mode)) {
                        pendingBitmap = bmp;
                        runOnUiThread(() -> {
                            startActivity(new Intent(this, SnipActivity.class)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                            done();
                        });
                    } else if (ScreenshotChooserActivity.MODE_FULL_PAINT.equals(mode)) {
                        pendingBitmap = bmp;
                        runOnUiThread(() -> {
                            startActivity(new Intent(this, SnipActivity.class)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    .putExtra(SnipActivity.EXTRA_FULL_PAINT, true));
                            done();
                        });
                    } else {
                        ImageSaver.save(this, bmp);
                        bmp.recycle();
                        runOnUiThread(this::done);
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Frame error", e);
                    if (image != null) image.close();
                    toast("Capture error: " + e.getMessage());
                    runOnUiThread(this::done);
                }
            }, mHandler);

            mVirtualDisplay = mProjection.createVirtualDisplay(
                    "SnapVD", mW, mH, mDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mImageReader.getSurface(), null, mHandler);

            Log.d(TAG, "VirtualDisplay created " + mW + "x" + mH);

        } catch (Exception e) {
            Log.e(TAG, "doCapture error", e);
            toast("Error: " + e.getMessage());
            done();
        }
    }

    /**
     * Converts an ImageReader frame into a tightly cropped ARGB bitmap.
     *
     * ImageReader rows may contain padding bytes, so the buffer width can be wider
     * than the requested display width. The temporary bitmap preserves that stride
     * while copying, then the padding is cropped away before the image is returned.
     */
    private Bitmap toBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer    buf    = planes[0].getBuffer();
        int pxStride  = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int pad  = rowStride - pxStride * mW;
        int bmpW = mW + pad / pxStride;

        Bitmap full = Bitmap.createBitmap(bmpW, mH, Bitmap.Config.ARGB_8888);
        full.copyPixelsFromBuffer(buf);
        if (bmpW != mW) {
            Bitmap c = Bitmap.createBitmap(full, 0, 0, mW, mH);
            full.recycle();
            return c;
        }
        return full;
    }

    /**
     * Releases every resource tied to the current projection.
     *
     * This method is synchronized because it can be reached from both the projection
     * callback and normal activity cleanup paths.
     */
    private synchronized void releaseCapture() {
        if (mVirtualDisplay != null) { mVirtualDisplay.release(); mVirtualDisplay = null; }
        if (mImageReader    != null) { mImageReader.close();      mImageReader    = null; }
        if (mProjection     != null) { mProjection.stop();        mProjection     = null; }
    }

    private void done() {
        stopService(new Intent(this, ProjectionService.class));
        if (mThread != null) { mThread.quitSafely(); mThread = null; }
        mHandler = null; // stops any in-flight consent/projection polling chain
        finish();
    }

    private void toast(String msg) {
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
    }

    @Override
    protected void onDestroy() {
        releaseCapture();
        if (mThread != null) { mThread.quitSafely(); mThread = null; }
        mHandler = null;
        super.onDestroy();
    }
}
