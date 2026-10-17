# SnapTile

**Lightweight screenshot and snipping tool for Android.**  
*Capture your screen from Quick Settings, select a region, annotate it, and save it as a lossless PNG.*

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Platform: Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Language: Java](https://img.shields.io/badge/Language-Java-orange.svg)]()
[![Min SDK](https://img.shields.io/badge/Min%20SDK-21-blue.svg)]()

<p align="center">
  <a href="https://github.com/hashierholmes/SnapTile/releases/latest">
    <img src="https://img.shields.io/badge/Download-SnapTile-blue?style=for-the-badge&logo=android" alt="Download SnapTile Latest" />
  </a>
</p>

---

## Overview

**SnapTile** is a lightweight Android screenshot utility built around the system Quick Settings panel.

It provides three capture modes:

- **Full Screen** — captures the entire display and saves it as a lossless PNG.
- **Snip Region** — captures the display, allows the user to select a specific region, and opens the paint editor.
- **Full Screen + Paint** — captures the entire display and opens the paint editor immediately.

The project prefers Android's accessibility screenshot APIs (`takeScreenshot()` on API 30+, system screenshot action on API 28–29) and keeps the entire image-processing pipeline local to the device. `MediaProjection` remains only as a pre-API-28 / service-disabled fallback.

SnapTile does not require a backend, network connection, account, or third-party service.

---

## Screenshots

<p align="center">
  <img src="docs/screenshots/1.jpg" width="24%" />
  &nbsp;&nbsp;
  <img src="docs/screenshots/2.jpg" width="24%" />
  &nbsp;&nbsp;
  <img src="docs/screenshots/3.jpg" width="24%" />
</p>

---

## Features

- **Quick Settings Integration** — Launch screenshot capture directly from Android Quick Settings.
- **Full Screen Capture** — Capture the entire display as a lossless PNG.
- **Interactive Snipping Tool** — Select, move, resize, or replace a screenshot region before editing.
- **Paint Annotation** — Draw directly over captured screenshots using configurable brush sizes and colors.
- **Undo / Redo** — Reverse or restore individual paint strokes without modifying the original bitmap.
- **Multiple Capture Modes** — Choose between full-screen capture, region selection, or full-screen capture with immediate editing.
- **Native Android Implementation** — Built primarily with Android framework APIs and Java.
- **Lossless PNG Output** — Screenshots are saved as PNG files to preserve image quality.
- **Native MediaStore Support** — Uses Android's modern shared-media APIs on supported Android versions.
- **Offline Operation** — Core screenshot and editing functionality does not require network access.
- **Minimal Permissions** — SnapTile does not request unnecessary network or account-related permissions.

---

## Architecture

```text
[ Android Quick Settings ]
            │
            ▼
[ ScreenshotTileService ]
            │
            ▼
[ ScreenshotChooserActivity ]
            │
       Capture Mode
            │
            ▼
[ CaptureActivity ]
            │
     MediaProjection
       Authorization
            │
            ▼
[ ProjectionService ]
            │
    Foreground Service
            │
            ▼
[ VirtualDisplay + ImageReader ]
            │
       Single Frame
            │
            ▼
         [ Bitmap ]
            │
      ┌─────┼───────────────┐
      │     │               │
      ▼     ▼               ▼
 Full Screen │        Full + Paint
      │     │               │
      │     ▼               │
      │ [ SnipActivity ] ◄──┘
      │     │
      │     ├─ Region Selection
      │     │
      │     └─ Paint Editor
      │
      └──────────┬────────────┘
                 ▼
          [ ImageSaver ]
                 │
                 ▼
      Pictures/Screenshots/
```

---

## Components

| Component | Responsibility |
| :--- | :--- |
| `MainActivity` | Provides the launcher/setup screen and developer credit. |
| `ScreenshotTileService` | Exposes SnapTile through Android Quick Settings. |
| `ScreenshotChooserActivity` | Allows the user to select the desired capture mode. |
| `CaptureActivity` | Handles MediaProjection authorization, screen capture, bitmap creation, and capture-mode routing. |
| `ProjectionService` | Provides the short-lived foreground service required while MediaProjection is active. |
| `SnipActivity` | Provides region selection and the current screenshot paint editor. |
| `ImageSaver` | Writes captured or edited screenshots to shared storage as PNG files. |
| `PaintToolbar` | Standalone drawing-toolbar helper kept separate from the current `SnipActivity` editor implementation. |

---

## Project Structure

```text
src/main/
├── AndroidManifest.xml
├── java/
│   └── com/snaptile/app/
│       ├── CaptureActivity.java
│       ├── ImageSaver.java
│       ├── MainActivity.java
│       ├── PaintToolbar.java
│       ├── ProjectionService.java
│       ├── ScreenshotChooserActivity.java
│       ├── ScreenshotTileService.java
│       └── SnipActivity.java
└── res/
    ├── drawable/
    ├── layout/
    ├── mipmap-*/
    └── values/
```

---

## Android APIs

SnapTile relies primarily on native Android framework APIs:

- `MediaProjection` — obtains the user's explicit authorization for screen capture.
- `VirtualDisplay` — mirrors the device display into an `ImageReader`.
- `ImageReader` — receives individual display frames from the virtual display.
- `Image` / `Plane` — provides access to the raw captured pixel buffer.
- `Bitmap` — stores captured frames and provides the base image for editing.
- `Canvas` — renders screenshot content and annotation strokes.
- `Path` — records individual drawing strokes for reversible editing.
- `Paint` — stores brush appearance and rendering properties.
- `TileService` — integrates SnapTile with Android Quick Settings.
- `Service` — provides the foreground-service execution required during screen projection.
- `MediaStore` — stores screenshots in the system's shared media collection on modern Android versions.
- `ContentResolver` — writes screenshot data through Android's storage APIs.

---

## Permissions

| Permission | Purpose |
| :--- | :--- |
| `android.permission.FOREGROUND_SERVICE` | Allows the capture service to run as a foreground service. |
| `android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION` | Required for foreground services using MediaProjection on supported Android versions. |

SnapTile does **not** require the `INTERNET` permission.

Screen capture itself also requires explicit authorization through Android's `MediaProjection` consent dialog.

---

## Requirements

- Android device running **Android 5.0 (API 21)** or higher.
- Android SDK and Gradle.
- [Android Studio](https://developer.android.com/studio) or [AndroidIDE](https://androidide.com/).
- A device or emulator that supports the required Android screen-capture APIs.

No API key, backend server, account, or internet connection is required.

---

## Configuration & Usage

### Initial Setup

1. Install the SnapTile APK.
2. Open **SnapTile** once, tap **Open accessibility settings**, and enable **SnapTile** (one-time, PowerMenu-style — no screen-recording prompt afterwards).
3. Open the Android Quick Settings panel.
4. Tap the **Edit** / pencil button.
5. Find **SnapTile** in the available tiles.
6. Drag the tile into the active Quick Settings area.

On Android 11+ captures run through the accessibility screenshot API (no consent dialog, no recording indicator). On Android 9–10 the system screenshot action is used. `MediaProjection` remains only as a fallback for older devices or when the accessibility service is off.

Accessibility access is controlled by Android and cannot be silently granted by SnapTile.

---

## Banking / UPI apps and accessibility

Some banking and UPI apps warn (or block) when *any* third-party accessibility service is
enabled. Those checks are **presence-based**, not capability-based: the app lists every
enabled service and warns about it, because it cannot tell a legitimate tool from
banking malware. That is why minimising SnapTile's declared capabilities does not, on
its own, silence the warning.

SnapTile's service is still deliberately screenshot-only, which keeps it honest and
future-proof if a checker ever does look at capabilities:

- `accessibilityEventTypes="typeWindowsChanged"` instead of `typeAllMask`
- `canRetrieveWindowContent="false"` — cannot read any app's views or text
- `canPerformGestures="false"` — cannot tap or type for you
- `canRequestFilterKeyEvents="false"` — cannot see keystrokes
- `onAccessibilityEvent()` processes nothing; the service exists only to answer
  `takeScreenshot()` / `GLOBAL_ACTION_TAKE_SCREENSHOT`

The event, feedback and flag fields are re-asserted at runtime in
`ScreenshotAccessibilityService.onServiceConnected()`, so the *live*
`AccessibilityServiceInfo` that scanners read also reports window-changes-only rather
than `typeAllMask`. (Android's public API has no runtime setter for capabilities —
those come from the XML above.)

## Banking mode (in-app)

The app's first card has a **Turn off for banking / UPI apps** button, shown whenever the
screenshot service is on. It calls `AccessibilityService.disableSelf()`, so SnapTile's
service is switched off without digging through Settings — the warnings stop and the
card switches to an "Accessibility is off — banking-safe" state.

Capture keeps working while the service is off:

- the Quick Settings tile opens the chooser and falls back to Android's MediaProjection
  consent dialog (one tap per capture),
- all three modes (full, region, full + paint) are available on that path,
- the tile never redirects you into Accessibility settings, so you are not trained to
  leave the service on.

Re-enabling always has to be done by you (Android does not allow an app to enable its own
accessibility service) — the same card then shows **Open accessibility settings**.
`disableSelf()` needs Android 7.0+; on older releases the button sends you to Settings
instead.

### What we know so far (2026-10-17)

The warning is **not** about the accessibility declaration, and that is now tested from both
sides rather than inferred. SnapTile `1.1.13` shipped declaring *nothing at all* — capabilities
`0x0`, event types `0x0` — and Super Money flagged it anyway, while **Twilight**, enabled
throughout, declares `CAN_TAKE_SCREENSHOT` and is not flagged. Every field of SnapTile's live
`AccessibilityServiceInfo` is a subset of some unflagged service's:

| | SnapTile 1.1.13 (**flagged**) | Twilight (on, **not** flagged) | Key Mapper (on, not flagged) | Bitwarden (on, not flagged) | Shizuku-Next (on, not flagged) |
|---|---|---|---|---|---|
| install source | sideloaded | Play | **sideloaded** | Play | **sideloaded** |
| capabilities | **0x0 (none)** | 0x80 screenshot | 0x69 window+keys+gestures | 0x1 | 0x1 |
| `accessibilityEventTypes` | **0** | 0 | 0 | 0x20 | 0x820 |
| `feedbackType` / `flags` | 0x10 / 0x1 | 0x10 / 0x0 | 0x0 / 0x0 | 0x10 / 0x50 | 0x10 / 0x41 |

Key Mapper declares window content, gesture injection *and* key-event filtering — the textbook
malicious combination — **and is sideloaded**, and is left alone. That combination of facts is
only possible if the decision does not come from the accessibility info at all.

Ruled out by experiment, not by guesswork:

| Theory | Result |
|---|---|
| "any enabled third-party service is flagged" | **No** — Bitwarden/Key Mapper are enabled and unflagged |
| `typeAllMask`/keylogging event-set heuristic | **No** — Key Mapper declares no event types and is unflagged |
| `isAccessibilityTool="false"` is treated as suspicious | **No** — Bitwarden declares exactly that and is unflagged |
| **provenance** — a trusted store buys a skip | **No** — Key Mapper and Shizuku-Next are sideloaded, enabled and unflagged |
| `accessibilityEventTypes` | **No** — `1.1.12` removed it from the XML *and* the runtime pin; still flagged |
| `canTakeScreenshot` | **No, both ways** — `1.1.13` declared no capability and was flagged; Twilight is enabled *with* it and is not |
| "declares nothing = suspicious" | **No** — same build, same result: `1.1.13` declared nothing and was flagged |
| the service only matters when enabled | **Yes** — disabling it made the warning disappear |

With the accessibility side exhausted, the trigger is app-level. The one thing SnapTile declares
at app level that no enabled, unflagged app on the device declares is **screen capture**:
`FOREGROUND_SERVICE_MEDIA_PROJECTION` plus `android:foregroundServiceType="mediaProjection"`.
`1.1.14` removes exactly that, and nothing else, to test it. Capture moves to
`performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)`, which needs no capability and no
permission.

- **Warning clears** → scanners key on "this app can capture the screen itself". SnapTile then
  ships without MediaProjection, and one tap keeps working via the system screenshot.
- **Warning survives** → nothing configurable is left; the cause is app identity (name,
  installer, install/update dates) and no build can change it. The device already shows the
  precedent: `Key Mapper` and `Shizuku-Next` are the same kind of app done by other people.

Operationally the answer stays: **the warning keys on enabled services**, so Banking mode clears
it in one tap. **Diagnostics: what scanners see** now prints, per enabled service, its declared
capabilities, event types, description text, whether its app declares MediaProjection /
video-output capture / overlay, and its install and update dates — the app-level data any
capture-based rule would have to be reading.

---

## Build

Clone the repository:

```bash
git clone https://github.com/hashierholmes/SnapTile.git
cd SnapTile
```

Open the project in Android Studio or AndroidIDE.

Build the debug APK:

```bash
./gradlew assembleDebug
```

The generated APK should be located at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install it through ADB:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Release Build & Signing

For release builds, use your own local signing configuration rather than committing private signing credentials to the repository.

If the project uses a local `keystore.properties` configuration, keep that file outside version control.

Example:

```properties
STORE_FILE=path/to/your/keystore.jks
STORE_PASSWORD=your_store_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

Add the following to `.gitignore`:

```gitignore
keystore.properties
*.jks
*.keystore
```

Never commit:

- Keystore files
- Keystore passwords
- Key aliases intended to remain private
- Release signing credentials

Official releases should be signed using the maintainer's release key, while developers building their own releases should use their own signing credentials.

---

## Development & Feedback

SnapTile is primarily maintained by its author.

The project does not currently accept direct code contributions or pull requests. This allows the project to maintain a consistent architecture, implementation style, design direction, and dependency philosophy.

Bug reports, suggestions, and feature requests are still welcome through the repository's issue tracker.

Feedback may be reviewed and incorporated at the author's discretion.

Please avoid submitting pull requests unless specifically requested.

---

## Privacy

SnapTile is designed to operate locally on the device.

- No ads
- No analytics
- No tracking
- No account system
- No screenshot uploads
- No backend server
- No API keys
- No `INTERNET` permission
- Screenshots remain on the device unless the user chooses to share them

MediaProjection is used only when the user explicitly starts a capture and grants Android's system screen-capture permission.

---

## License

SnapTile's source code is licensed under the **MIT License**.

You are free to use, copy, modify, distribute, sublicense, and commercially use the source code in accordance with the terms of the MIT License.

The **SnapTile name, logo, branding, visual identity, screenshots, and other project identifiers are not licensed under the MIT License** unless explicitly stated otherwise.

The MIT License applies to the source code only and does not grant permission to use SnapTile's branding or represent a fork or derivative work as the official SnapTile project.

Forks and derivative works are permitted under the MIT License, including commercial distribution, but should use their own name, logo, and branding.

See [`LICENSE`](LICENSE) for the complete license text.

---

## Disclaimer

SnapTile uses Android's official screen-capture mechanisms.

Screen capture is subject to the security and permission restrictions enforced by the Android operating system. SnapTile cannot bypass Android's MediaProjection consent requirements.