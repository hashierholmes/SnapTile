package com.snaptile.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Minimal foreground service whose only purpose is to satisfy Android's
 * requirement that a foregroundServiceType="mediaProjection" service is
 * running before MediaProjection is used.
 *
 * All actual capture logic lives in CaptureActivity (same process).
 * CaptureActivity starts this service, captures, then stops it.
 */
public class ProjectionService extends Service {

    static final String CHANNEL_ID = "ss_tile";
    static final int    NOTIF_ID   = 42;

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Screenshot Tile", NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
        startForeground(NOTIF_ID, buildNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Nothing — CaptureActivity drives everything
        return START_NOT_STICKY;
    }

    @Override public IBinder onBind(Intent i) { return null; }

    private Notification buildNotification() {
        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Screenshot Tile")
                .setContentText("Capturing…")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setOngoing(true)
                .build();
    }
}
