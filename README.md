# Screenshot Tile

A lightweight Android Quick Settings tile for capturing screenshots — with a built-in Snipping Tool and paint annotation editor. No ads, no tracking, no internet permission.

---

## Features

| Mode | Description |
|------|-------------|
| **Full Screen** | Captures the entire display instantly and saves as lossless PNG |
| **Snip Region** | Freeze the screen, draw a rectangle to crop, then annotate and save |
| **Full Screen + Paint** | Capture full screen and jump straight into the paint editor |

### Snip Region overlay
- Drag to draw a selection rectangle
- Drag **inside** the rect to move it
- Drag a **corner handle** (blue dots) to resize
- Drag **outside** the rect to start a new selection
- **Save button** (top-left, appears after selection) → confirms the crop and enters the paint editor
- **Back button** → cancel without saving

### Paint editor
- **Save** — flattens all strokes onto the image and writes a lossless PNG
- **Undo / Redo** — per-stroke history
- **Brush size** — tap the brush icon to cycle Small / Medium / Large
- **Color palette** — 12 colors in a scrollable row; selected color is highlighted with a white ring
- All painting is done at native bitmap resolution — no quality loss

---

## Screenshots saved to
`Pictures/Screenshots/` — visible in your Gallery / Photos app immediately.

Format: `Screenshot_YYYYMMDD_HHmmss.png` (PNG, lossless)

---

## Requirements

| Item | Value |
|------|-------|
| Min Android | **9.0 (API 28)** |
| Target Android | 14 (API 34) |
| Permissions | `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PROJECTION`, `POST_NOTIFICATIONS` |
| No AndroidX | ✓ Pure Android SDK |
| Internet | ✗ Not requested |

> Android requires a one-time "Start now" system consent dialog before any screen capture. This is an OS-level security requirement and cannot be bypassed by any sideloaded app.

---

## Building

### Requirements
- Android Studio Hedgehog or newer
- JDK 8+
- Android SDK with Build Tools 34

### Steps

```bash
git clone https://github.com/YOUR_USERNAME/ScreenshotTile.git
cd ScreenshotTile
```

Open in Android Studio → **Build → Generate Signed APK** (or just hit **Run** for a debug build).

Or build from the command line:

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### Install via ADB

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Setup on device

1. Install the APK
2. Open the **Screenshot Tile** app — it shows setup instructions
3. Swipe down **twice** to open the full Quick Settings panel
4. Tap the **pencil / edit** icon to enter edit mode
5. Find the **Screenshot** tile and drag it into your active tiles
6. Done — tap the tile anytime to take a screenshot

---

## How it works (architecture)

```
ScreenshotTileService    QS tile click
        │
        ▼
ScreenshotChooserActivity   Modal dialog: Full / Snip / Full+Paint
        │
        ▼
CaptureActivity          Requests MediaProjection permission (one-time dialog)
        │                Creates MediaProjection immediately (avoids token expiry)
        ▼
ProjectionService        Foreground service captures ONE frame via VirtualDisplay
        │                Stops itself immediately after — no persistent screen cast
        │
        ├─ Full Screen ──► ImageSaver → PNG saved
        │
        ├─ Snip Region ──► SnipActivity (freeze + draw rect → paint editor)
        │
        └─ Full+Paint ───► SnipActivity (paint editor directly)
```

**Key design decision:** `MediaProjection` is created in `CaptureActivity` (not the service) because the token becomes invalid when re-parcelled across process boundaries on Android 10+. It is passed via a static field to avoid this.

The service calls `stopSelf()` immediately after capturing one frame, so the screen cast indicator disappears right away.

---

## Package name

`my.example.application`

Change it in `app/build.gradle` (`applicationId`) and `AndroidManifest.xml` (`package`) if you want to publish to the Play Store.

---

## License

MIT — do whatever you want with it.
