package com.snaptile.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Short-lived foreground service required by Android for media projection capture.
 *
 * The service intentionally contains no capture logic. {CaptureActivity} owns
 * the projection and frame lifecycle; this service only establishes the foreground
 * execution context required by the platform and is stopped after the one-shot
 * capture finishes.
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
