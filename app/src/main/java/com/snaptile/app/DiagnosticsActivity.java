package com.snaptile.app;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
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
 * risk SDK is clearly not reading declared capabilities; removing
 * canTakeScreenshot entirely (1.1.9-probe) changed nothing, and disabling the
 * service removed the warning.
 *
 * What is left is provenance versus the event type, so this screen shows both
 * signals per service, side by side: read the "installer" line of every ENABLED
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
        sb.append("Verified against super.money, 2026-10-17:\n");
        sb.append(" - only ENABLED services matter: disabling\n");
        sb.append("   SnapTile's service makes the warning go away\n");
        sb.append(" - the declared accessibility info does not\n");
        sb.append("   decide it. 1.1.11-1.1.13 walked it down to\n");
        sb.append("   nothing (caps 0x80 -> 0x0, events -> 0x0)\n");
        sb.append("   and the warning stayed; every live field of\n");
        sb.append("   SnapTile's info is now a subset of an\n");
        sb.append("   unflagged app's. Twilight is ENABLED with\n");
        sb.append("   CAN_TAKE_SCREENSHOT and is not flagged.\n");
        sb.append("   Lock Screen is ENABLED with byte-identical\n");
        sb.append("   info (caps 0x0, events 0x0, feedback 0x10,\n");
        sb.append("   flags 0x1) and is not flagged either.\n");
        sb.append(" - the manifest does not decide it: 1.1.14\n");
        sb.append("   removed the mediaProjection permission and\n");
        sb.append("   the foregroundServiceType (see \"declares\"\n");
        sb.append("   below, mediaProjection=no) and the warning\n");
        sb.append("   stayed.\n");
        sb.append(" - provenance and install dates do not decide\n");
        sb.append("   it: Key Mapper (10-15) and Shizuku-Next\n");
        sb.append("   (10-16) are side-loaded, ENABLED, installed\n");
        sb.append("   this week, and not flagged.\n\n");
        sb.append("Where that leaves it, as of 1.2.0:\n");
        sb.append(" the trigger is not a declared field. Every\n");
        sb.append(" candidate was tested in both directions and\n");
        sb.append(" the warning never changed (see the table in\n");
        sb.append(" accessibility_service_config.xml). The\n");
        sb.append(" external check is ScreenshotTile,\n");
        sb.append(" github.com/cvzi/ScreenshotTile: flagged too,\n");
        sb.append(" and its accessibility config declares less\n");
        sb.append(" than any SnapTile build ever has.\n");
        sb.append(" So 1.2.0 stops hunting for a setting that\n");
        sb.append(" clears it and offers two capture methods\n");
        sb.append(" instead: native (this service, no prompt,\n");
        sb.append(" banks warn) or legacy (screen recording, no\n");
        sb.append(" accessibility service, no warning). The\n");
        sb.append(" method is chosen on the main screen; the\n");
        sb.append(" \"method\" line below shows which is set.\n");
        sb.append(" Still unanswered, both readable here: does\n");
        sb.append(" the scanner read what the app says about\n");
        sb.append(" itself (enable exactly ONE of the two\n");
        sb.append(" SnapTile entries, check super.money, swap),\n");
        sb.append(" and is \"unknown app + enabled accessibility\n");
        sb.append(" service\" enough on its own (enable an\n");
        sb.append(" a11yprobe app instead).\n\n");
        sb.append("this app  : ").append(getPackageName()).append('\n');
        sb.append("version   : ").append(versionName()).append('\n');
        sb.append("method    : ").append(CapturePrefs.method(this)).append('\n');
        sb.append("a11y on   : ").append(CapturePrefs.isAccessibilityEnabled(this)).append('\n');
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
            String text = serviceText(pm, info);
            if (!text.isEmpty()) {
                sb.append(" text     ").append(text).append('\n');
            }
            // The class name is the other half of what a scanner reads straight
            // out of the accessibility list, without any package visibility.
            sb.append(" service  ").append(shortComponent(info)).append('\n');
        }

        // Ground truth that package visibility cannot hide: the secure setting
        // lists every enabled service, including ones this app is not allowed to
        // inspect. It is the same set a risk SDK bases its warning on, so it is
        // the honest answer to "which services are live right now".
        sb.append("\n----------------------------------------\n");
        sb.append("ENABLED per Settings.Secure (unfiltered)\n");
        String raw = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (raw == null || raw.trim().isEmpty()) {
            sb.append(" (none)\n");
        } else {
            for (String component : raw.split(":")) {
                component = component.trim();
                if (component.isEmpty()) continue;
                int slash = component.indexOf('/');
                String pkg = slash > 0 ? component.substring(0, slash) : component;
                sb.append(" ").append(component).append('\n');
                sb.append("   installer ").append(installerOf(pm, pkg)).append('\n');
                sb.append("   declares ").append(appFacts(pm, pkg)).append('\n');
            }
        }

        sb.append("\n----------------------------------------\n");
        sb.append("CAN_TAKE_SCREENSHOT = this service may capture\n");
        sb.append("the screen itself (not just trigger the system\n");
        sb.append("screenshot). It is NOT what the warning keys on:\n");
        sb.append("1.1.13 declared no capabilities at all and was\n");
        sb.append("still flagged, while Twilight is ENABLED with\n");
        sb.append("exactly this capability and is not flagged.\n\n");
        sb.append("mediaProjection=yes = the app declares that it\n");
        sb.append("may capture the screen through MediaProjection.\n");
        sb.append("1.1.14 removed it and the warning stayed, so\n");
        sb.append("this is no longer a live question; it stays in\n");
        sb.append("the dump as a control.\n\n");
        sb.append("service = the class name the scanner reads out\n");
        sb.append("of the accessibility list. names = every\n");
        sb.append("activity and service the app declares, so a\n");
        sb.append("rule keying on names would read \"Screenshot\"\n");
        sb.append("and \"Capture\" straight off SnapTile's list.\n");
        sb.append("perms = requested permissions, so SnapTile's\n");
        sb.append("set can be diffed against the apps that are\n");
        sb.append("not flagged. source = API 34 install source:\n");
        sb.append("STORE versus a file opened by hand.\n");
        return sb.toString();
    }

    /** Version code + name, so a dump identifies the build it came from. */
    private String versionName() {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pi.versionName + " (" + pi.versionCode + ")";
        } catch (PackageManager.NameNotFoundException e) {
            return "?";
        }
    }

    /** The service's own description text — the string Settings shows the user. */
    private static String serviceText(PackageManager pm, AccessibilityServiceInfo info) {
        CharSequence d = null;
        try {
            d = info.loadDescription(pm);
        } catch (Throwable ignored) {
            // Fall through to the raw fields below.
        }
        if (d == null || d.length() == 0) d = info.getDescription();
        if (d == null) return "";
        String s = d.toString().replace('\n', ' ').trim();
        return s.length() > 110 ? s.substring(0, 110) + "…" : s;
    }

    /**
     * What an app declares that a screen-capture rule could key on, plus when it
     * was last written — the app-level things the accessibility info cannot
     * explain. Falls back to a note when package visibility still hides it.
     *
     * 1.1.15 adds the two remaining app-level signals nothing has ruled out: the
     * names of every component the app declares (a keyword rule would read
     * "Screenshot"/"Capture" straight off this list) and its full permission
     * set, so SnapTile's can be diffed against the enabled apps that are not
     * flagged.
     */
    private String appFacts(PackageManager pm, String pkg) {
        try {
            PackageInfo pi = pm.getPackageInfo(pkg,
                    PackageManager.GET_PERMISSIONS
                            | PackageManager.GET_ACTIVITIES
                            | PackageManager.GET_SERVICES);
            String[] req = pi.requestedPermissions;
            StringBuilder sb = new StringBuilder();
            sb.append("mediaProjection=")
                    .append(has(req, "android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION") ? "yes" : "no")
                    .append(" captureVideo=")
                    .append(has(req, "android.permission.CAPTURE_VIDEO_OUTPUT")
                            || has(req, "android.permission.CAPTURE_SECURE_VIDEO_OUTPUT") ? "yes" : "no")
                    .append(" overlay=")
                    .append(has(req, "android.permission.SYSTEM_ALERT_WINDOW") ? "yes" : "no");
            sb.append("\n   source   ").append(packageSource(pm, pkg));
            sb.append("\n   dates    installed ").append(date(pi.firstInstallTime))
                    .append(" updated ").append(date(pi.lastUpdateTime));
            sb.append("\n   names    ").append(componentNames(pi));
            sb.append("\n   perms    ").append(permissionNames(req));
            return sb.toString();
        } catch (PackageManager.NameNotFoundException e) {
            return "(package not visible, or not installed)";
        }
    }

    /**
     * API 34 tells an app where its packages came from in a form install-source
     * alone cannot: a store install and a downloaded APK both show a package
     * installer, but only this distinguishes them. Kept as a named string so a
     * dump stays readable on platforms that predate it.
     */
    private String packageSource(PackageManager pm, String pkg) {
        if (Build.VERSION.SDK_INT < 34) return "(needs API 34)";
        try {
            InstallSourceInfo si = pm.getInstallSourceInfo(pkg);
            switch (si.getPackageSource()) {
                case PackageInstaller.PACKAGE_SOURCE_STORE:
                    return "STORE";
                case PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE:
                    return "LOCAL_FILE (opened from a file: side-loaded)";
                case PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE:
                    return "DOWNLOADED_FILE";
                case PackageInstaller.PACKAGE_SOURCE_OTHER:
                    return "OTHER";
                default:
                    return "UNSPECIFIED (" + si.getPackageSource() + ")";
            }
        } catch (Exception e) {
            return "(not visible to SnapTile)";
        }
    }

    /** Every activity and service class the app declares, sorted, short form. */
    private static String componentNames(PackageInfo pi) {
        List<String> names = new java.util.ArrayList<>();
        // Component names make long lists; only the class matters for eyeballing
        // a keyword, so the package prefix is dropped.
        String pkg = pi.packageName;
        if (pi.activities != null) {
            for (android.content.pm.ActivityInfo a : pi.activities) {
                if (a != null && a.name != null) names.add(trim(pkg, a.name));
            }
        }
        if (pi.services != null) {
            for (android.content.pm.ServiceInfo s : pi.services) {
                if (s != null && s.name != null) names.add(trim(pkg, s.name));
            }
        }
        if (names.isEmpty()) return "(none)";
        java.util.Collections.sort(names);
        return join(names);
    }

    /** Requested permissions, sorted, with the android.permission. prefix dropped. */
    private static String permissionNames(String[] req) {
        if (req == null || req.length == 0) return "(none)";
        List<String> out = new java.util.ArrayList<>(req.length);
        for (String p : req) {
            if (p == null) continue;
            out.add(p.startsWith("android.permission.")
                    ? p.substring("android.permission.".length()) : p);
        }
        java.util.Collections.sort(out);
        return join(out);
    }

    private static String trim(String pkg, String cls) {
        return cls.startsWith(pkg + ".") ? cls.substring(pkg.length() + 1) : cls;
    }

    private static String join(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(p);
        }
        return sb.toString();
    }

    private static boolean has(String[] values, String needle) {
        if (values == null) return false;
        for (String v : values) {
            if (needle.equals(v)) return true;
        }
        return false;
    }

    private static String date(long millis) {
        return android.text.format.DateFormat.format("yyyy-MM-dd", millis).toString();
    }

    /** Package + class of a service, used to match installed against enabled. */
    private static String key(AccessibilityServiceInfo info) {
        ResolveInfo ri = info.getResolveInfo();
        if (ri == null || ri.serviceInfo == null) return "?";
        return ri.serviceInfo.applicationInfo.packageName + "/" + ri.serviceInfo.name;
    }

    /** "com.example.app/.FooService" — how the secure setting spells a component. */
    private static String shortComponent(AccessibilityServiceInfo info) {
        ResolveInfo ri = info.getResolveInfo();
        if (ri == null || ri.serviceInfo == null) return "?";
        String pkg = ri.serviceInfo.applicationInfo.packageName;
        String cls = ri.serviceInfo.name;
        if (cls != null && cls.startsWith(pkg + ".")) {
            cls = "." + cls.substring(pkg.length() + 1);
        }
        return pkg + "/" + cls;
    }

    @SuppressWarnings("deprecation")
    private String installerOf(PackageManager pm, String pkg) {
        try {
            String installer;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                InstallSourceInfo si = pm.getInstallSourceInfo(pkg);
                installer = si != null ? si.getInstallingPackageName() : null;
            } else {
                installer = pm.getInstallerPackageName(pkg);
            }
            if (installer == null || installer.isEmpty()) return "(none) -> SIDELOADED";
            if ("com.android.vending".equals(installer)) return installer + " -> PLAY (trusted)";
            return installer + " -> not Play";
        } catch (Exception e) {
            // Package visibility: not allowed to inspect this one. Saying
            // "sideloaded" here would be a guess, so say what we actually know.
            return "(not visible to SnapTile)";
        }
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
