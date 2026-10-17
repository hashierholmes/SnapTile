package com.snaptile.a11yprobe;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

/**
 * Does nothing, on purpose.
 *
 * A control app for the SnapTile bank/UPI investigation: the payload is
 * res/xml/probe_service.xml, because that file is what a risk SDK reads when it
 * decides whether to warn about an app. Keeping the code empty means any
 * difference in what gets flagged has to come from the declared configuration
 * (or from the app's provenance), never from behaviour.
 */
public class ProbeService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Deliberately empty.
    }

    @Override
    public void onInterrupt() {
        // Deliberately empty.
    }
}
