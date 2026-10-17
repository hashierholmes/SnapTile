package com.snaptile.app;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.accessibility.AccessibilityManager;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Read-only dump of what a bank / UPI risk SDK can see about accessibility on
 * this device — the same data those SDKs read before deciding to warn about an
 * app.
 *
 * It exists to settle an open question. SnapTile declared the most minimal
 * service config possible (no window content, no gestures, no key filtering)
 * and was still flagged, while other accessibility apps on the same device were
 * not. Those other apps came from Play and declare *more* capabilities, so the
 * discriminator is either the one capability SnapTile declared that they don't
 * (CAN_TAKE_SCREENSHOT), or the install source (risk SDKs scan sideloaded apps
 * and give trusted-store apps a badge that skips the scan).
 *
 * This screen shows both signals per service, side by side, so the answer is
 * visible rather than guessed: compare the "installer" line of every ENABLED
 * entry.
 */
public class DiagnosticsActivity extends Activity {

    /**
     * Capability bits with readable names. These are compile-time constants, so
     * referencing the newer ones is safe on older platforms.
     */
    private static final Object[][] CAPS = {
            {AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT, "CAN_RETRIEVE_WINDOW_CONTENT"},
            {AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_TOUCH_EXPLORATION, "CAN_REQUEST_TOUCH_EXPLORATION"},
            {AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_ENHANCED_WEB_ACCESSIBILITY, "CAN_REQUEST_ENHANCED_WEB_A11Y"},
            {AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_FILTER_KEY_EVENTS, "CAN_REQUEST_FILTER_KEY_EVENTS"},
            {AccessibilityServiceInfo.CAPABILITY_CAN_CONTROL_MAGNIFICATION, "CAN_CONTROL_MAGNIFICATION"},
            {AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES, "CAN_PERFORM_GESTURES"},
            {AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_FINGERPRINT_GESTURES, "CAN_REQUEST_FINGERPRINT_GESTURES"},
            {AccessibilityServiceInfo.CAPABILITY_CAN_TAKE_SCREENSHOT, "CAN_TAKE_SCREENSHOT"},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.diagnostics_title);

        TextView tv = new TextView(this);
        tv.setTypeface(android.graphics.Typeface.MONOSPACE);
        tv.setTextSize(11f);
        tv.setTextIsSelectable(true);
        tv.setMovementMethod(new ScrollingMovementMethod());
        int pad = Math.round(14 * getResources().getDisplayMetrics().density);
        tv.setPadding(pad, pad, pad, pad);
        tv.setText(buildReport());

        ScrollView sc = new ScrollView(this);
        sc.addView(tv);
        setContentView(sc);
    }

    private String buildReport() {
        PackageManager pm = getPackageManager();
        AccessibilityManager am =
                (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);

        StringBuilder sb = new StringBuilder();
        sb.append("What bank/UPI risk SDKs can read\n");
        sb.append("================================\n\n");
        sb.append("Question being tested:\n");
        sb.append(" (a) is CAN_TAKE_SCREENSHOT the trigger?\n");
        sb.append(" (b) is the install source the trigger?\n");
        sb.append("     (trusted store = skipped, sideloaded = scanned)\n\n");
        sb.append("Read the 'installer' line of every ENABLED entry.\n");
        sb.append("If the only non-Play entries are SnapTile and\n");
        sb.append("SnapTile Debug, the source is the discriminator.\n\n");
        sb.append("this app  : ").append(getPackageName()).append('\n');
        sb.append("installer : ").append(installerOf(pm, getPackageName())).append('\n');
        sb.append("sdk       : ").append(Build.VERSION.SDK_INT).append('\n');

        Set<String> enabled = new HashSet<>();
        List<AccessibilityServiceInfo> installed = null;
        if (am != null) {
            installed = am.getInstalledAccessibilityServiceList();
            List<AccessibilityServiceInfo> on =
                    am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
            if (on != null) {
                for (AccessibilityServiceInfo s : on) {
                    enabled.add(key(s));
                }
            }
        }

        if (installed == null || installed.isEmpty()) {
            sb.append("\n(no accessibility services installed)\n");
            return sb.toString();
        }

        for (AccessibilityServiceInfo info : installed) {
            ResolveInfo ri = info.getResolveInfo();
            String pkg = (ri != null && ri.serviceInfo != null)
                    ? ri.serviceInfo.applicationInfo.packageName : "?";
            String label = ri != null ? String.valueOf(ri.loadLabel(pm)) : pkg;

            sb.append("\n----------------------------------------\n");
            sb.append(label).append(enabled.contains(key(info)) ? "   [ENABLED]" : "   [off]").append('\n');
            sb.append(" package  ").append(pkg).append('\n');
            sb.append(" installer ").append(installerOf(pm, pkg)).append('\n');
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                sb.append(" caps     0x").append(hex(info.getCapabilities()))
                        .append(' ').append(capNames(info.getCapabilities())).append('\n');
            }
            sb.append(" events   0x").append(hex(info.eventTypes)).append('\n');
            sb.append(" feedback 0x").append(hex(info.feedbackType))
                    .append("  flags 0x").append(hex(info.flags)).append('\n');
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                sb.append(" a11yTool ").append(info.isAccessibilityTool()).append('\n');
            }
        }

        sb.append("\n----------------------------------------\n");
        sb.append("CAN_TAKE_SCREENSHOT = this service may capture\n");
        sb.append("the screen itself (not just trigger the system\n");
        sb.append("screenshot), which is what OTP-stealing trojans\n");
        sb.append("want. 1.1.9-probe no longer declares it.\n");
        return sb.toString();
    }

    /** Package + class of a service, used to match installed against enabled. */
    private static String key(AccessibilityServiceInfo info) {
        ResolveInfo ri = info.getResolveInfo();
        if (ri == null || ri.serviceInfo == null) return "?";
        return ri.serviceInfo.applicationInfo.packageName + "/" + ri.serviceInfo.name;
    }

    @SuppressWarnings("deprecation")
    private String installerOf(PackageManager pm, String pkg) {
        String installer = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                InstallSourceInfo si = pm.getInstallSourceInfo(pkg);
                installer = si != null ? si.getInstallingPackageName() : null;
            } else {
                installer = pm.getInstallerPackageName(pkg);
            }
        } catch (Exception ignored) {
            // Package visibility / unknown installer — treated as sideloaded.
        }
        if (installer == null || installer.isEmpty()) return "(none) -> SIDELOADED";
        if ("com.android.vending".equals(installer)) return installer + " -> PLAY (trusted)";
        return installer + " -> not Play";
    }

    private static String hex(int v) {
        return String.format(Locale.US, "%08x", v);
    }

    private static String capNames(int caps) {
        StringBuilder sb = new StringBuilder();
        for (Object[] row : CAPS) {
            int bit = (Integer) row[0];
            if ((caps & bit) != 0) {
                if (sb.length() > 0) sb.append(" | ");
                sb.append((String) row[1]);
            }
        }
        return sb.length() == 0 ? "(none)" : sb.toString();
    }
}
