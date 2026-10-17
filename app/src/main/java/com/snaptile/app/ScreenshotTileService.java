package com.snaptile.app;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * Entry point exposed to Android's Quick Settings panel.
 *
 * The tile deliberately stays stateless: tapping it launches the chooser and the
 * capture flow owns the temporary state needed for the actual screenshot.
 *
 * The tile never forces the user into system Accessibility settings: when the
 * screenshot service is off, ScreenshotChooserActivity falls back to the
 * MediaProjection path. That keeps accessibility switchable to OFF for
 * banking/UPI apps without breaking any capture mode.
 */
public class ScreenshotTileService extends TileService {

    @Override
    public void onStartListening() {
        Tile t = getQsTile();
        if (t != null) { t.setState(Tile.STATE_INACTIVE); t.updateTile(); }
    }

    @Override
    public void onClick() {
        Intent i = new Intent(this, ScreenshotChooserActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityAndCollapse(i);
    }
}
