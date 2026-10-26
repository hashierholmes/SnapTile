package com.snaptile.app;

import android.app.PendingIntent;
import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * Entry point exposed to Android's Quick Settings panel.
 *
 * Always opens the capture-mode chooser. The chooser selects the available
 * screenshot path: Accessibility when enabled, or MediaProjection consent when
 * Accessibility is disabled. Accessibility is optional, never a hard requirement.
 */
public class ScreenshotTileService extends TileService {

    @Override
    public void onStartListening() {
        Tile t = getQsTile();
        if (t != null) {
            t.setState(Tile.STATE_INACTIVE);
            t.updateTile();
        }
    }

    @Override
    public void onClick() {
        Intent intent = new Intent(this, ScreenshotChooserActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Android 14+ requires a PendingIntent when collapsing the Quick Settings
        // panel to launch an Activity. The Intent overload throws on these versions.
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            startActivityAndCollapse(pendingIntent);
        } else {
            startActivityAndCollapse(intent);
        }
    }
}
