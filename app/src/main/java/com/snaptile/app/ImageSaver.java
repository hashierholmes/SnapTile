package com.snaptile.app;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Persists completed screenshots to the device's public Pictures/Screenshots directory.
 *
 * Android 10+ uses MediaStore so the app can write to shared media without requesting
 * broad storage access. Older Android versions use the public Pictures directory and
 * trigger a media scan so Gallery apps can discover the file immediately.
 */
public class ImageSaver {

    public static void save(Context ctx, Bitmap bitmap) {
        String name = "SnapTile_"
                + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date())
                + ".png";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.Images.Media.DISPLAY_NAME, name);
                cv.put(MediaStore.Images.Media.MIME_TYPE,    "image/png");
                cv.put(MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/Screenshots");
                cv.put(MediaStore.Images.Media.IS_PENDING, 1);

                Uri uri = ctx.getContentResolver()
                        .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
                if (uri == null) {
                    toast(ctx, "Save failed: MediaStore insert returned null");
                    return;
                }
                try (OutputStream out = ctx.getContentResolver().openOutputStream(uri)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                }
                cv.put(MediaStore.Images.Media.IS_PENDING, 0);
                ctx.getContentResolver().update(uri, cv, null, null);
                toast(ctx, "Screenshot saved ✓");
            } else {
                File dir = new File(
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES), "Screenshots");
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
                File f = new File(dir, name);
                try (FileOutputStream out = new FileOutputStream(f)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                }
                Intent scan = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                scan.setData(Uri.fromFile(f));
                ctx.sendBroadcast(scan);
                toast(ctx, "Saved: " + f.getName());
            }
        } catch (Exception e) {
            toast(ctx, "Save error: " + e.getMessage());
        }
    }

    private static void toast(Context ctx, String msg) {
        // Must be called from main thread or post to main looper
        Toast.makeText(ctx.getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }
}
