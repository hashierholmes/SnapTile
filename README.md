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

Capture is prompt-free for full-screen shots: on API 28+ the accessibility service asks the system to
take the screenshot itself, and the image-processing pipeline stays entirely local to the device.
Region and paint need the bitmap inside the app, so they use `MediaProjection` and its consent
dialog — as does every capture on older releases or while the accessibility service is off.

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
[ ScreenshotChooserActivity ] ──► capture mode
            │
            ▼
[ CapturePrefs.request() ] ──► which capture method?
            │
   ┌────────┴───────────────────────────┐
   ▼                                    ▼
[ Native ]                         [ Legacy ]
[ ScreenshotAccessibilityService ] [ CaptureActivity ]
   │                                   │
   │ full screen:                      │ MediaProjection consent
   │ GLOBAL_ACTION_TAKE_SCREENSHOT     ▼
   │ → the system saves the shot   [ ProjectionService ]
   │                                   │ foreground service
   │ region / paint:                   ▼
   └──────────────► [ VirtualDisplay + ImageReader ]
                        │
                   Single Frame
                        │
                    [ Bitmap ]
                        │
       ┌────────────────┼────────────────┐
       ▼                ▼                ▼
  Full Screen     [ SnipActivity ]   Full + Paint
                  Region / Paint
                        │
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
| `MainActivity` | Provides the launcher/setup screen, the capture-method card, and developer credit. |
| `ScreenshotTileService` | Exposes SnapTile through Android Quick Settings. |
| `ScreenshotChooserActivity` | Allows the user to select the desired capture mode. |
| `CapturePrefs` | Stores the chosen capture method and routes every capture request by it. |
| `ScreenshotAccessibilityService` | The native capture path: system screenshot action for full screen, and the fallback into MediaProjection for region/paint or older releases. |
| `AccessibilitySetupActivity` | The how-to-enable screen, including the restricted-settings step Android hides from side-loaded apps. |
| `CaptureActivity` | Handles MediaProjection authorization, screen capture, bitmap creation, and capture-mode routing. |
| `ProjectionService` | Provides the short-lived foreground service required while MediaProjection is active. |
| `SnipActivity` | Provides region selection and the current screenshot paint editor. |
| `ImageSaver` | Writes captured or edited screenshots to shared storage as PNG files. |
| `PaintToolbar` | Standalone drawing-toolbar helper kept separate from the current `SnipActivity` editor implementation. |
| `DiagnosticsActivity` | Read-only dump of what accessibility scanners can see, for debugging the banking/UPI warning. |

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
- `AccessibilityService` — performs the system screenshot action for the native capture method.
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
2. Open **SnapTile** once. On the **Capture method** card, leave **Native — accessibility** selected
   and tap **Set up the accessibility method**: that screen walks the three steps in the order
   Android enforces them (Accessibility settings → App info + ⋮ → Allow restricted settings →
   Accessibility settings again). One-time; captures are prompt-free afterwards.
3. Open the Android Quick Settings panel.
4. Tap the **Edit** / pencil button.
5. Find **SnapTile** in the available tiles.
6. Drag the tile into the active Quick Settings area.

On Android 9+ full-screen captures use the system screenshot action (no consent dialog, no recording
indicator). Region and paint need the bitmap inside the app, so they go through `MediaProjection`'s
consent dialog — as does any capture on Android 8 and older, or while the accessibility service is
off.

Accessibility access is controlled by Android and cannot be silently granted by SnapTile.

---

## Banking / UPI apps and accessibility

Some banking and UPI apps warn when *any* third-party accessibility service is enabled. The checks
are **presence-based** in the only sense that matters: nothing about the service's declaration,
capability, class name, description text or provenance changes the outcome. Every one of those
variables was tested — see [What we know so far](#what-we-know-so-far-2026-10-17) — which is why
minimising SnapTile's declared capabilities cannot, on its own, silence the warning.

SnapTile's service is still deliberately screenshot-only, and since 1.2.1 it declares **exactly what
ScreenshotTile declares**: the smallest config an accessibility service can have and still take a
screenshot.

```xml
<accessibility-service
    android:accessibilityEventTypes="typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:description="@string/accessibility_desc" />
```

No capability at all — no window content, no gestures, no keystroke filtering, not even the
screenshot capability. There is nothing declared that the app does not use: `onAccessibilityEvent()`
processes nothing, and the service exists only to answer `GLOBAL_ACTION_TAKE_SCREENSHOT`.

The event, feedback and flag fields are re-asserted at runtime in
`ScreenshotAccessibilityService.onServiceConnected()`, with event types pinned to `0` because the
service consumes none, so the *live* `AccessibilityServiceInfo` a scanner reads is a subset of the
file. (Android's public API has no runtime setter for capabilities: those come from the XML.)

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
| **the screen-capture declaration** (`FOREGROUND_SERVICE_MEDIA_PROJECTION` + `foregroundServiceType`) | **No** — `1.1.14` removed both, still flagged with `mediaProjection=no` |
| **install/update dates** ("recently installed" rule) | **No** — every enabled service arrived 2026-10-14…17; Key Mapper and Shizuku-Next are as new and unflagged |
| the service only matters when enabled | **Yes** — disabling it made the warning disappear |

With the accessibility side exhausted, the trigger is app-level. The one thing SnapTile declared
at app level that no enabled, unflagged app on the device declares is **screen capture**:
`FOREGROUND_SERVICE_MEDIA_PROJECTION` plus `android:foregroundServiceType="mediaProjection"`.
`1.1.14` removed exactly that, and nothing else, to test it. Capture moved to
`performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)`, which needs no capability and no
permission.

**Result: the warning survived.** `1.1.14` is flagged with `mediaProjection=no` on its own
Diagnostics screen, so the manifest is exhausted too. Three further facts came out of the same
dump:

- **install/update dates do not decide it** — every enabled service on the device was installed
  between 2026-10-14 and 2026-10-17, SnapTile included; Key Mapper (10-15) and Shizuku-Next
  (10-16) are the same age and side-loaded, and neither is flagged.
- **Power Menu, Lock Screen and Switchly are Play apps**, not side-loaded ones — they were
  invisible to the earlier dump only because package visibility hid them.
- **Lock Screen (`com.olalab.lockscreen`) publishes byte-identical accessibility info to
  SnapTile 1.1.14** — capabilities `0x0`, event types `0x0`, `feedbackType 0x10`, `flags 0x1` —
  and is not flagged. The only differences left are `isAccessibilityTool` (true there, false
  here), its install source, and what the two apps say about themselves.

So `1.1.15` tested the two remaining explanations, and both are readable on the Diagnostics
screen:

1. **The scanner reads what an app says about itself** — the bound class name
   (`ScreenshotAccessibilityService`) and the description text ("...only to take screenshots...")
   are both readable out of the accessibility list with no permission at all. 1.1.15 declared a
   **twin service** (`TileAssistService`, "SnapTile tile action") with identical behaviour and
   an identical config, but a neutral class name and wording. Enable exactly one of the two
   SnapTile entries and compare. **Result: still flagged.** The class name and the description are
   not read either, and the twin was deleted in 1.2.1.
2. **"Unknown app + enabled accessibility service" is enough on its own** — the `a11yprobe` apps
   are side-loaded packages with no history. Enable one alongside SnapTile: if the warning names it
   too, the rule is generic and no build can fix it for a side-loaded app. **Not needed** — the test
   below answered it first.

**The control that should have been run first.** The 1.2.0 screenshot of the Super Money dialog
named exactly one app: **"SnapTile Debug"** — the debug package (`com.snaptile.app.debug`,
`application-debuggable`, label "SnapTile Debug"), while Key Mapper (window content, gesture
injection, key filtering, side-loaded) sat enabled in the same list and was *not* named. That
suggested the rule might simply be "debuggable app with an enabled accessibility service", which
would have explained every negative result above — all of them were run on debug builds. The
hypothesis was tested directly: **the signed release build is flagged too.**

The conclusion, and what 1.2.1 ships with: **nothing an app can declare changes this.** The warning
follows what the app *is* — a screenshot tool with an accessibility service enabled — which a
side-loaded app cannot argue with, and which an app cannot rewrite its way out of. The durable
options are the two the app already offers: **Legacy** (no accessibility service, no warning, one
consent dialog per capture) while using banking and UPI apps, **Native** when the capture itself
matters more than the warning.

Operationally the answer stays: **the warning keys on enabled services**, so Banking mode clears
it in one tap. **Diagnostics: what scanners see** now prints, per enabled service, its component
class name, declared capabilities, event types, description text, the app's install source
(API 34 `PACKAGE_SOURCE_*`), whether its app declares MediaProjection / video-output capture /
overlay, its install and update dates, **every activity and service class it declares**, and
**its full permission list** — so SnapTile's app-level surface can be diffed line by line
against the enabled apps that are not flagged.

### `isAccessibilityTool="true"` — tested, answered, removed (1.1.16)

1.1.16 set `android:isAccessibilityTool="true"` at the owner's request, against the expectation
written down beforehand that it would change nothing. **It changed nothing**, so the line is gone
in 1.2.0. It was also false: the flag asserts that the service exists to assist users with
disabilities, which is not what SnapTile is, and Google's guidance says *falsely* declaring it is
what triggers a Play Protect warning naming the app. A claim that is both untrue and ineffective
does not belong in the tree.

Compile-time trap worth knowing, because it cost an hour: **the newer attributes only exist in the
`-v22` resource variant.** aapt2 strips attributes newer than `minSdk` (21) out of the base
`res/xml/` file and writes them into `res/xml-v22/`, so dumping the base variant shows seven
attributes and looks exactly like the capability and flags were silently dropped. Read
`res/xml-v22/accessibility_service_config.xml` (or the `v22` file in an optimised release APK) to
see what the device actually gets.

### The external check: ScreenshotTile is flagged too

[cvzi/ScreenshotTile](https://github.com/cvzi/ScreenshotTile) — a mature, widely used screenshot
tile app — is flagged by Super Money exactly as SnapTile is. Its accessibility config declares:

```xml
<accessibility-service
    android:accessibilityEventTypes="typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:description="@string/accessibility_service_description" />
```

That is *less* than any SnapTile build has ever shipped: one event type, a description, no
capability, no screenshot permission, no MediaProjection-flavoured declaration. It is flagged
anyway. Two consequences:

1. **The trigger is not a field in the accessibility config.** SnapTile's matrix already said so
   (capability, event types, feedback, flags, `isAccessibilityTool`, the mediaProjection
   declaration, install provenance and dates were each tested and each changed nothing); an
   independent app with an emptier config confirms it from outside.
2. **It is a property of the category, not of SnapTile.** An app that presents itself as a
   screenshot tool and has an accessibility service enabled gets flagged. Key Mapper, Shizuku,
   Bitwarden and Twilight are all enabled on the reference device and none is flagged — none of
   them is a screenshot tool.

### Capture methods (1.2.0, table revised in 1.2.1)

1.2.0 stopped hunting for a setting that clears the warning and gave the user the choice instead.
The main screen has a **Capture method** card:

| Method | How it captures | Cost |
| --- | --- | --- |
| **Native — accessibility** (default) | `ScreenshotAccessibilityService` + `GLOBAL_ACTION_TAKE_SCREENSHOT` — the system takes and saves the full-screen shot. Region and paint go through MediaProjection | needs an enabled accessibility service, and banks/UPI apps warn about it |
| **Legacy — screen recording** | `CaptureActivity` + MediaProjection, the way any screen recorder works | Android asks for consent before **every** capture |

Choosing Legacy also disables SnapTile's accessibility service, because a legacy method that
leaves the service enabled pays the warning for nothing. Choosing Native never tries to enable
anything — Android requires the user for that, so the card offers a setup button instead.

When a capture is requested with the native method selected but nothing enabled, SnapTile opens
the **setup screen** rather than silently switching methods: it would be answering a different
question than the one asked, particularly from the Quick Settings tile.

The capability question was closed in 1.2.1 rather than 1.2.0. `canTakeScreenshot="true"` came back
in 1.2.0 on the grounds that the matrix had tested it in both directions and found it free; 1.2.1
drops it anyway, along with every other optional attribute, because "free" was still one declared
capability more than the reference app needs and one more than SnapTile uses. The consequences are
deliberate and visible: full-screen captures are taken and saved by the system (no consent, no
bitmap in the app), and region/paint captures need MediaProjection's consent dialog.

### The setup screen

`AccessibilitySetupActivity` explains the step Android hides from side-loaded apps, in the order
Android actually enforces it. The order is the whole point, and getting it wrong leaves the user
stuck: **"Allow restricted settings" only appears in the App info overflow menu after the user has
tried to enable the service once and been refused.** So:

1. **Accessibility settings** → tap SnapTile. The switch is greyed out and Android says a restricted
   setting is blocking it. That refusal is what unlocks step 2.
2. **App info** → ⋮ → **Allow restricted settings**.
3. **Accessibility settings** again → tap SnapTile and turn the switch on. It stays on.

The two buttons are numbered to match (1 opens Accessibility settings, 2 opens App info), and the
drawn schematic illustrates step 2 — drawn, not screenshotted, so it cannot go stale across Android
versions and OEM skins. Reached from the main screen, and automatically when the native method is
chosen but the service is off.

**Both open questions closed in 1.2.1, so the twin and the probe are no longer needed.** Whether the
scanner reads what the app says about itself: no — the twin service (1.1.15–1.2.0) was flagged
identically and has been deleted. Whether "unknown app + enabled accessibility service" is enough on
its own: yes, in effect — the release build is flagged and it has nothing left to strip. The
`a11yprobe` module stays in the tree as a standalone diagnostic app; it is not part of the SnapTile
build.

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