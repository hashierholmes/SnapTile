package com.snaptile.app;

/**
 * Diagnostic twin of {@link ScreenshotAccessibilityService}, added in 1.1.15.
 *
 * Everything the warning could still be keying on has been eliminated except the
 * service's own identity as the platform reports it to other apps: the bound
 * service's class name ("ScreenshotAccessibilityService") and the description
 * text underneath it ("...only to take screenshots..."). Both are readable
 * without any package-visibility permission, because they ship inside the
 * {@link android.view.accessibility.AccessibilityServiceInfo} that every app can
 * list with getEnabledAccessibilityServiceList().
 *
 * 1.1.15 declares this second service so the test runs *inside one app*: same
 * package, same label, same permissions, same installer, same install dates, same
 * accessibility config (no capabilities, no event types, feedbackGeneric,
 * flagDefault, canRetrieveWindowContent=false). The only differences between the
 * two entries are the class name and the description string.
 *
 * The class body is deliberately empty: it inherits onServiceConnected(), so a
 * bound TileAssistService sets the same static instance that requestScreenshot()
 * uses, and capture behaves identically. Enable exactly one of the two services
 * at a time while testing; with both enabled the last one bound wins.
 *
 * Disposable: once the question is answered this class, its config XML and its
 * manifest entry come out again.
 */
public class TileAssistService extends ScreenshotAccessibilityService {
}
