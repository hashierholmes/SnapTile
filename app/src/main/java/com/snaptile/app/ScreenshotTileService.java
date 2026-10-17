package com.snaptile.app;

import android.content.Intent;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * Entry point exposed to Android's Quick Settings panel.
 *
 * The tile deliberately stays stateless: tapping it launches the chooser and the
 * capture flow owns the temporary state needed for the actual screenshot.
 *
 * If the accessibility screenshot service is not enabled yet, the tile deep-links
 * to system Accessibility settings (PowerMenu-style onboarding) instead.
 */
public class ScreenshotTileService extends TileService {

    @Override
    public void onStartListening() {
        Tile t = getQsTile();
        if (t != null) { t.setState(Tile.STATE_INACTIVE); t.updateTile(); }
    }

    @Override
    public void onClick() {
        if (!ScreenshotAccessibilityService.isEnabled()) {
            Intent s = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            s.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityAndCollapse(s);
            return;
        }
        Intent i = new Intent(this, ScreenshotChooserActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityAndCollapse(i);
    }
}
