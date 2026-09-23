<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:FFB703,100:121212&height=200&section=header&text=BingeLock&fontSize=52&fontColor=FFFFFF&animation=fadeIn&fontAlignY=38&desc=Stay%20watching.%20Skip%20the%20%22Are%20you%20still%20watching%3F%22%20interruptions.&descAlignY=58&descSize=16" width="100%" alt="BingeLock Header"/>

<img src="https://readme-typing-svg.demolab.com/?font=Fira+Code&size=22&pause=1000&color=FFB703&center=true&vCenter=true&width=520&lines=%F0%9F%94%92+Lock+it.;%F0%9F%91%80+Keep+watching.;%F0%9F%AB%B3+Flip+it+down.;%F0%9F%94%8B+Protect+your+battery." alt="Typing SVG" />

<br/>
<br/>

<img src="https://img.shields.io/badge/Kotlin-Android-FFB703?style=for-the-badge&logo=kotlin&logoColor=121212" alt="Kotlin Badge"/>
<img src="https://img.shields.io/badge/Android-SDK%2034-FFB703?style=for-the-badge&logo=android&logoColor=121212" alt="Android Badge"/>
<img src="https://img.shields.io/badge/Min%20SDK-30-FFB703?style=for-the-badge&logo=android&logoColor=121212" alt="Min SDK Badge"/>
<img src="https://img.shields.io/badge/Version-1.0-FFB703?style=for-the-badge&logo=git&logoColor=121212" alt="Version Badge"/>

**A focused Android utility for uninterrupted video playback — automating supported playback prompts, keeping the screen awake, providing physical pause control, and protecting battery life.**

</div>

---

## 🎯 What BingeLock is for

**BingeLock** is an Android utility designed to reduce interruptions during supported video playback sessions.

Its core purpose is simple:

> **Keep supported playback moving without requiring repeated “still watching?” confirmation.**

BingeLock monitors the accessibility UI of selected target apps. When a configured playback-pause prompt is detected, it searches for a matching action button and attempts to activate it automatically.

The app runs locally on the device using Android's **Accessibility Service, Foreground Service, WindowManager overlays, Accelerometer, Quick Settings Tile, and SharedPreferences**.

---

## ✨ What it can offer

|                                  |                                                                                                                          |
| -------------------------------- | ------------------------------------------------------------------------------------------------------------------------ |
| 🔒 **Automatic prompt handling** | Detects configured playback-pause phrases and attempts to activate a matching action button automatically.               |
| 🔁 **Media-play fallback**       | Sends an Android media-play key event as a safety-net attempt after detecting a supported prompt.                        |
| 👀 **Screen-awake behavior**     | Uses a transparent `WindowManager` overlay with `FLAG_KEEP_SCREEN_ON` while BingeLock is active.                         |
| 🫳 **Physical pause control**    | Detects a sustained face-down device position and temporarily removes the screen-awake overlay.                          |
| ↩️ **Automatic resume**          | Restores the screen-awake overlay when the device returns upright.                                                       |
| 🔋 **Battery protection**        | Automatically disables BingeLock when the battery falls below the configured threshold while unplugged.                  |
| ⚡ **Quick access**               | Provides an Android Quick Settings tile for rapidly enabling or disabling BingeLock.                                     |
| 📝 **Activity history**          | Records successful automatic dismissals with timestamps and keeps the latest 100 log entries.                            |
| 🔔 **Status feedback**           | Maintains an ongoing foreground notification and sends a separate notification when a prompt is successfully dismissed.  |
| ⚙️ **Configurable behavior**     | Allows target apps, prompt phrases, action buttons, battery threshold, and face-down timing to be changed from Settings. |
| 💾 **Local operation**           | Stores configuration and dismissal history locally using Android `SharedPreferences`.                                    |

The implementation supports configurable defaults rather than hard-coding a single prompt or action.

---

## 🎬 Supported playback targets

BingeLock ships with these target packages enabled by default:

```text
com.google.android.youtube
app.morphe.android.youtube
```

They can be individually enabled or disabled from the **Target Apps** section in Settings, with at least one target app required.

---

## 🧠 How automatic dismissal works

BingeLock uses Android's Accessibility framework to inspect the active accessibility tree of supported target applications.

The process is:

```text
Supported target app
        │
        ▼
Accessibility event received
        │
        ▼
Configured prompt phrase detected
        │
        ▼
Search configured action buttons
        │
        ▼
Clickable target found
        │
        ▼
ACTION_CLICK attempted
        │
        ├── Success ──► Log + notification
        │
        └── Failure
              │
              ▼
        Continue fallback logic
```

When a configured prompt is detected, BingeLock:

1. Searches for each configured action button.
2. Looks for a clickable node associated with the button.
3. Checks the node itself, its parent, and its grandparent for a clickable target.
4. Attempts `ACTION_CLICK`.
5. Sends a media-play key event as a safety-net action.
6. Records successful dismissals in local history.
7. Sends a dismissal notification.

If no configured action button can be clicked, BingeLock also attempts to use the detected prompt's own clickable parent hierarchy as a fallback.

---

## 🛡️ Defensive prompt handling

BingeLock does not attempt to interact with every application on the device.

The accessibility service first checks whether the current package belongs to the configured target package set:

```text
Accessibility Event
       │
       ▼
Is package configured?
       │
   ┌───┴───┐
   │       │
  No      Yes
   │       │
 Ignore    ▼
       Inspect UI
```

## It also applies a **2-second dismissal debounce** to the media-play fallback so repeated accessibility events do not continuously dispatch media-play commands.

## 👀 Screen-awake behavior

When BingeLock is active, the foreground service creates a transparent `1 × 1` application overlay using:

```text
TYPE_APPLICATION_OVERLAY
FLAG_NOT_TOUCHABLE
FLAG_NOT_FOCUSABLE
FLAG_KEEP_SCREEN_ON
```

This provides the screen-awake behavior without placing a visible interface over the playback application.

The overlay can be temporarily removed through face-down detection.

---

## 🫳 Face-down detection

BingeLock can use the device's accelerometer to detect a sustained face-down position.

Default behavior:

```text
Face-down detection: ON
Pause duration:       10 seconds
```

The configurable duration ranges from:

```text
5 seconds ─────────────── 60 seconds
```

When the device remains face-down longer than the configured duration:

```text
Face-down detected
        │
        ▼
Duration threshold reached
        │
        ▼
Remove screen-awake overlay
        │
        ▼
BingeLock enters paused state
```

## When the device returns upright, the overlay is restored and the paused state is cleared.

## 🔋 Battery protection

BingeLock includes a configurable battery floor.

The default threshold is:

```text
15%
```

The Settings screen allows the threshold to be configured between:

```text
5% ─────────────── 50%
```

When the battery falls **below** the configured threshold while the device is **not plugged in**, BingeLock:

```text
Battery below configured floor
          │
          ▼
Disable master state
          │
          ▼
Clear face-down pause state
          │
          ▼
Stop BingeLock service
```

## The service receives the current battery state through Android's battery broadcast system and evaluates both battery percentage and charging state.

## ⚡ Quick Settings control

BingeLock exposes an Android Quick Settings tile.

The tile:

* Shows the current BingeLock state.
* Toggles the master enabled state.
* Starts the foreground service when enabled.
* Stops the service when disabled.
* Clears the face-down paused state when toggled.

This provides system-level access without opening the main application.

---

## ⚙️ Settings

BingeLock includes a dedicated Settings screen with four main configuration areas.

### Battery Floor

Configure when BingeLock should automatically disable itself:

```text
5% → 50%
```

### Face-down Detection

Enable or disable physical pause control and configure the delay:

```text
5 → 60 seconds
```

### Target Apps

Select the applications BingeLock should monitor:

```text
☑ YouTube
☑ YouTube Morphe
```

At least one target application must remain selected.

### Advanced

Customize:

```text
Prompt phrases
Action buttons
```

Each entry is entered **one item per line**.

Changes to prompt phrases and action buttons are saved with a short debounce delay, allowing the user to edit the fields without writing every keystroke immediately.

---

## 📝 Default prompt configuration

BingeLock ships with the following prompt phrases:

```text
Video paused. Continue watching?
Are you still watching?
Still there?
Video paused
Video Paused
Still watching? Video will pause soon.
Are you still there?
Click to resume playback.
Paused due to inactivity.
Resume video?
```

Default action buttons:

```text
Yes
Continue
Continue watching
CONTINUE
```

These values can be edited from **Settings → Advanced**.

---

## 🔔 Notifications

BingeLock uses two types of notifications.

### Persistent service notification

While active, the foreground service displays:

```text
BingeLock active
```

Its status reflects whether the service is currently keeping the screen awake or has been paused by face-down detection.

### Dismissal notification

After a successful automatic dismissal, BingeLock sends a separate notification indicating:

```text
BingeLock Triggered
Automatically dismissed "[prompt]" prompt in [app].
```

This notification uses a separate notification ID so it does not overwrite the persistent service notification.

---

## 📝 Activity history

Successful automatic dismissals are stored locally with:

```text
Timestamp
Detected prompt/action
Target package
```

Example:

```text
[2026-09-23 08:30:15 PM] Auto-dismissed "Continue" on com.google.android.youtube
```

## BingeLock retains the latest **100 log lines**. The history can also be cleared directly from the main screen.

## 🔐 Required Android capabilities

BingeLock uses several Android system capabilities to provide its functionality.

The application declares:

```text
SYSTEM_ALERT_WINDOW
FOREGROUND_SERVICE
FOREGROUND_SERVICE_SPECIAL_USE
POST_NOTIFICATIONS
```

It also registers:

```text
Accessibility Service
Foreground Service
Quick Settings Tile Service
Main Activity
Settings Activity
```

The Accessibility Service is protected by Android's `BIND_ACCESSIBILITY_SERVICE` permission, while the Quick Settings component uses `BIND_QUICK_SETTINGS_TILE`.

> **Note:** Android system permissions and accessibility access must be granted/configured on the device for the corresponding BingeLock functionality to operate.

---

## 🧩 What it's made of

BingeLock is intentionally built around native Android components.

| Component                 | Role                                                            |
| ------------------------- | --------------------------------------------------------------- |
| **Kotlin**                | Application language                                            |
| **Android SDK 34**        | Compile and target platform                                     |
| **Android SDK 30+**       | Minimum supported platform                                      |
| **AndroidX Core KTX**     | Android Kotlin support                                          |
| **AndroidX AppCompat**    | Application compatibility layer                                 |
| **Accessibility Service** | Detects configured playback prompts and interacts with their UI |
| **Foreground Service**    | Maintains the active BingeLock runtime                          |
| **WindowManager Overlay** | Keeps the screen awake                                          |
| **Accelerometer**         | Detects sustained face-down orientation                         |
| **SharedPreferences**     | Stores configuration and history locally                        |
| **Quick Settings Tile**   | Provides system-level enable/disable control                    |
| **NotificationManager**   | Provides service and dismissal notifications                    |

The Gradle configuration currently uses Java 11 compatibility and the AndroidX Core KTX/AppCompat dependencies.

---

## 🏗️ Architecture

```text
┌──────────────────────────────┐
│         MainActivity         │
│  Master switch + history     │
└──────────────┬───────────────┘
               │
               ▼
┌──────────────────────────────┐
│      SettingsActivity        │
│ Battery / Face-down / Apps   │
│ Prompt phrases / Actions     │
└──────────────┬───────────────┘
               │
               ▼
┌──────────────────────────────┐
│        PrefsHelper           │
│   Local SharedPreferences    │
└───────┬───────────┬──────────┘
        │           │
        ▼           ▼
┌─────────────┐  ┌─────────────────────┐
│BingeService │  │AccessibilityService │
│             │  │                     │
│ Screen      │  │ Prompt detection    │
│ Battery     │  │ UI interaction      │
│ Accelerometer│ │ Media fallback      │
│ Notification│  │ History + alerts    │
└─────────────┘  └─────────────────────┘
        ▲
        │
┌───────┴───────────┐
│ BingeTileService  │
│ Quick Settings    │
└───────────────────┘
```

---

## 📂 Project structure

```text
app/
├── build.gradle.kts
└── src/main/
    ├── AndroidManifest.xml
    │
    ├── java/com/gem/bingelock/
    │   ├── MainActivity.kt
    │   ├── SettingsActivity.kt
    │   ├── BingeService.kt
    │   ├── BingeAccessibilityService.kt
    │   ├── BingeTileService.kt
    │   └── PrefsHelper.kt
    │
    └── res/
        ├── layout/
        │   ├── activity_main.xml
        │   └── activity_settings.xml
        │
        └── values/
            └── themes.xml
```

The current production source contains separate activities for the main interface and settings, dedicated services for foreground/background behavior, accessibility handling and Quick Settings integration, plus a shared preferences helper.

---

## 📦 Build configuration

```text
Compile SDK:      34
Target SDK:       34
Minimum SDK:      30
Version Code:     1
Version Name:     1.0
Java Compatibility: 11
```

The release build currently has minification disabled.

---

## 🚀 Build

Build a debug APK:

```bash
./gradlew assembleDebug
```

Install directly onto a connected Android device:

```bash
./gradlew installDebug
```

For the installable APK, see the repository's **Releases** section.

---

## 🔄 Runtime behavior

When BingeLock is enabled:

```text
                    ┌─────────────────────┐
                    │      BingeLock      │
                    │       ACTIVE        │
                    └──────────┬──────────┘
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                    │
          ▼                    ▼                    ▼
   Accessibility         Screen Awake          Battery Watch
     Service                Overlay                 │
          │                    │                    │
          ▼                    │                    ▼
   Detect Prompt              │             Below Floor?
          │                    │                    │
          ▼                    │              ┌─────┴─────┐
   Find Action                │             Yes           No
          │                    │              │             │
          ▼                    │              ▼             │
   Click Target               │        Stop Service       │
          │                    │                            │
          ▼                    │                            │
   Media-Play Fallback        │                            │
          │                    │                            │
          ▼                    ▼                            ▼
      Log + Alert       Face-down Detection          Continue
```

---

## 💡 Design principles

BingeLock is designed around a few simple principles:

* **Local-first** — no external backend is required for its core functionality.
* **Configurable** — prompts, actions, apps, battery behavior, and face-down timing can be changed.
* **Defensive interaction** — accessibility actions are limited to configured target packages and known UI patterns.
* **Low visual footprint** — the screen-awake mechanism uses a transparent overlay rather than a visible playback control.
* **Battery-aware** — the service can automatically shut itself down when the device reaches the configured battery floor.
* **System-integrated** — foreground service status, Android notifications, accessibility services, sensors, and Quick Settings are used where appropriate.

---

## 📱 Current scope

BingeLock currently focuses on:

```text
Android
   │
   ├── Supported video applications
   ├── Automatic playback-prompt handling
   ├── Screen-awake behavior
   ├── Face-down physical pause
   ├── Battery protection
   ├── Quick Settings control
   └── Local activity history
```

Its prompt detection and interaction behavior is driven by configurable accessibility text rather than a network API or direct integration with a video platform.

---

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:121212,100:FFB703&height=120&section=footer" width="100%" alt="Footer animation"/>

**Less interruption. More watching.**

</div>
