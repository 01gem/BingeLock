# ◼ BingeLock

> **Stay in the moment. Let the screen keep going.**
>
> A lightweight Android utility that quietly keeps video playback
> sessions alive by dismissing recurring **"Are you still watching?"**
> prompts --- without interrupting the experience.

::: {align="center"}
![Android](https://img.shields.io/badge/Android-30%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Android-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![SDK](https://img.shields.io/badge/Target%20SDK-34-111111?style=for-the-badge)
![Version](https://img.shields.io/badge/Version-1.0-111111?style=for-the-badge)
:::

------------------------------------------------------------------------

## ✦ What is BingeLock?

**BingeLock** is a small, purpose-built Android app designed around one
idea:

> **You started watching. You shouldn't have to keep proving you're
> still there.**

It runs as a foreground service and uses Android's Accessibility
framework to detect familiar playback interruption prompts in supported
YouTube environments. When a matching prompt appears, BingeLock attempts
to activate its clickable control automatically.

The app also includes a small overlay service to help maintain an active
viewing state, plus a **face-down gesture** that temporarily pauses the
overlay.

No accounts.\
No cloud service.\
No backend.\
No unnecessary complexity.

Just a focused local utility.

------------------------------------------------------------------------

## ◇ Features

### `AUTO-DISMISS`

Detects common continuation prompts such as:

-   `Video paused. Continue watching?`
-   `Are you still watching?`
-   `Still there?`

When a matching accessibility node is found, BingeLock attempts to click
the associated control automatically.

### `KEEP AWAKE`

A foreground service creates a transparent application overlay
configured with `FLAG_KEEP_SCREEN_ON`, helping prevent the screen from
sleeping while BingeLock is active.

### `FACE-DOWN PAUSE`

Place the device face-down for roughly **10 seconds** and BingeLock
pauses its overlay.

Turn the device upright again and the overlay resumes.

This gives you a simple physical "pause" gesture without opening the
app.

### `LOW-BATTERY SAFETY`

When battery drops below the configured floor of **15%** while the
device is not plugged in, BingeLock automatically disables itself and
stops the service.

### `QUICK SETTINGS TILE`

Toggle BingeLock directly from Android's Quick Settings panel.

The tile reflects the current enabled/disabled state.

### `DISMISSAL HISTORY`

Successful automatic dismissals are timestamped and stored locally.

The app keeps the latest **100 log entries**.

### `LOCAL-FIRST`

Preferences and history are stored using Android `SharedPreferences`.

There is no network layer in the current implementation.

------------------------------------------------------------------------

## ◎ How it works

``` text
                    ┌───────────────────┐
                    │   BingeLock ON    │
                    └─────────┬─────────┘
                              │
                    ┌─────────▼─────────┐
                    │ Foreground Service │
                    └───────┬─────┬──────┘
                            │     │
              ┌─────────────┘     └──────────────┐
              ▼                                  ▼
     ┌────────────────┐                 ┌─────────────────┐
     │ Accessibility  │                 │ Accelerometer   │
     │    Service     │                 │   Monitoring    │
     └───────┬────────┘                 └────────┬────────┘
             │                                   │
             ▼                                   ▼
    Find continuation prompt             Face-down ~10 sec?
             │                                   │
             ▼                              ┌────┴────┐
      Click matching node                 YES        NO
             │                              │         │
             ▼                              ▼         ▼
       Save local log                Pause overlay   Resume
             │
             ▼
      Optional notification
```

------------------------------------------------------------------------

## ⌁ Supported targets

The current accessibility implementation watches for events from:

  Package                        Target
  ------------------------------ ----------------
  `com.google.android.youtube`   YouTube
  `app.morphe.android.youtube`   YouTube Morphe

The matching logic is intentionally based on visible accessibility text
rather than a single hard-coded view hierarchy.

That makes the implementation relatively small, while still allowing it
to locate a clickable parent when the matched text node itself isn't
clickable.

------------------------------------------------------------------------

## ◌ Architecture

BingeLock is intentionally compact.

``` text
app/
└── src/main/
    ├── java/com/gem/bingelock/
    │   ├── MainActivity.kt
    │   ├── BingeService.kt
    │   ├── BingeAccessibilityService.kt
    │   ├── BingeTileService.kt
    │   └── PrefsHelper.kt
    │
    ├── res/
    │   ├── layout/
    │   │   └── activity_main.xml
    │   └── values/
    │       └── themes.xml
    │
    └── AndroidManifest.xml
```

### Core components

**`MainActivity`**\
The primary control surface. Handles the master switch, service
startup/shutdown, permission routing, status text, and dismissal
history.

**`BingeService`**\
The persistent foreground service. Owns the transparent overlay, battery
monitoring, accelerometer listener, notification, and face-down
pause/resume behavior.

**`BingeAccessibilityService`**\
Monitors accessibility events from supported target applications,
searches for known continuation prompts, performs clicks, records
successful dismissals, and posts a notification.

**`BingeTileService`**\
Provides the Android Quick Settings toggle.

**`PrefsHelper`**\
A tiny persistence layer around `SharedPreferences`.

------------------------------------------------------------------------

## ◐ Permissions

BingeLock uses Android capabilities that require explicit user
permission.

  -----------------------------------------------------------------------
  Permission / capability             Purpose
  ----------------------------------- -----------------------------------
  `SYSTEM_ALERT_WINDOW`               Create the transparent overlay

  `FOREGROUND_SERVICE`                Keep the service running

  `FOREGROUND_SERVICE_SPECIAL_USE`    Declare the foreground service type

  `POST_NOTIFICATIONS`                Surface BingeLock notifications

  Accessibility Service               Detect and interact with supported
                                      playback prompts
  -----------------------------------------------------------------------

### Accessibility matters

The accessibility service is central to BingeLock's automatic dismissal
behavior.

Android's Accessibility framework exposes the visible UI hierarchy to
the service. BingeLock searches that hierarchy for its configured prompt
strings and then attempts to trigger the nearest clickable node.

Because accessibility behavior can vary between applications and
versions, the implementation is intentionally defensive and logs
failures rather than assuming every prompt has the same structure.

------------------------------------------------------------------------

## ◒ Design language

The UI follows a deliberately restrained **dark utility** aesthetic.

### Visual direction

``` text
BACKGROUND       #121212
SECONDARY PANEL  #1A1A1A
DIVIDER          #333333
PRIMARY TEXT     #FFFFFF
SECONDARY TEXT   #BBBBBB
MUTED TEXT       #888888
```

The interface avoids decorative UI in favor of:

-   high contrast
-   generous spacing
-   simple controls
-   monospace activity logs
-   minimal status feedback
-   a dark, distraction-free surface

The goal is for BingeLock to feel more like a **quiet system utility**
than another content-heavy app.

------------------------------------------------------------------------

## ⚡ Runtime behavior

### Turning BingeLock on

1.  Enable the master switch.
2.  Grant overlay permission when prompted.
3.  Start the foreground service.
4.  BingeLock creates its transparent overlay.
5.  Battery and accelerometer monitoring begin.

### When a prompt appears

1.  An accessibility event arrives.
2.  BingeLock checks whether it came from a supported package.
3.  The active accessibility tree is searched for known prompt text.
4.  BingeLock looks for a clickable node or clickable parent.
5.  The action is performed.
6.  A timestamped entry is saved locally.
7.  A notification can report the dismissal.

### When the phone is face-down

After approximately **10 seconds** in the detected face-down
orientation:

``` text
Overlay → removed
State   → paused
```

When the phone returns upright:

``` text
Overlay → restored
State   → active
```

### Battery protection

Below **15% battery**, when unplugged:

``` text
BingeLock → disabled
Service   → stopped
Pause     → cleared
```

------------------------------------------------------------------------

## 🧰 Tech stack

-   **Kotlin**
-   **Android SDK 34**
-   **Minimum SDK 30**
-   **AndroidX Core KTX**
-   **AndroidX AppCompat**
-   **Foreground Service**
-   **Accessibility Service**
-   **WindowManager overlay**
-   **Accelerometer**
-   **SharedPreferences**
-   **Android Quick Settings Tile**

The project currently targets **Java 11 compatibility** for compilation.

------------------------------------------------------------------------

## 🚀 Getting started

### Requirements

-   Android Studio
-   Android SDK 34
-   Android device/emulator running Android 11+ (`minSdk 30`)
-   A device with an accelerometer for face-down detection

### Build

Clone the repository and open it in Android Studio.

Then build the debug APK through Android Studio or Gradle.

``` bash
./gradlew assembleDebug
```

Install to a connected device:

``` bash
./gradlew installDebug
```

> The exact Gradle wrapper configuration is not included in the supplied
> source snapshot, so the commands above assume the repository contains
> the standard Android Gradle wrapper files.

------------------------------------------------------------------------

## 🔐 First-run setup

After installation:

1.  Open **BingeLock**.
2.  Enable the master switch.
3.  Allow **Display over other apps** permission.
4.  Enable the BingeLock accessibility service in Android Settings.
5.  Optionally add the BingeLock Quick Settings tile.
6.  Return to the app and start watching.

Once active, BingeLock operates in the background through its foreground
service.

------------------------------------------------------------------------

## 📝 Current state

**Version:** `1.0`\
**Status:** Production-stable source snapshot

The current implementation is deliberately focused rather than
feature-heavy.

### Included

-   Automatic prompt detection
-   Automatic prompt dismissal
-   Foreground service
-   Screen-awake overlay
-   Face-down pause/resume
-   Battery floor protection
-   Quick Settings tile
-   Local dismissal history
-   Persistent preferences
-   Foreground notifications

### Not yet implemented

The UI currently contains a **"Settings (coming soon)"** control.
Although the application stores a battery floor and target package
preferences, the current main-screen implementation does not expose a
full settings editor.

This is a useful foundation for future configuration without changing
the core service architecture.

------------------------------------------------------------------------

## 🛣️ Possible future directions

Ideas that naturally fit the existing architecture:

-   configurable battery threshold
-   editable target applications
-   customizable prompt strings
-   accessibility-service diagnostics
-   log clearing/export
-   richer Quick Settings state
-   configurable face-down timeout
-   per-app rules
-   improved settings screen
-   Material 3 visual refresh
-   optional boot/startup behavior

These are **future possibilities**, not features currently implemented.

------------------------------------------------------------------------

## ⚠️ Notes & limitations

BingeLock depends on Android system behavior and the accessibility
hierarchy exposed by target applications.

A UI change in YouTube or another supported application may change:

-   prompt wording
-   accessibility node structure
-   clickability
-   event timing

As a result, automatic dismissal is not guaranteed across every future
application version.

The face-down detector also depends on the device exposing a usable
accelerometer.

------------------------------------------------------------------------

## 🧪 Logging & debugging

The project uses the Android log tag:

``` text
BingeLock
```

Useful runtime events include:

``` text
Service onCreate
Service onStartCommand
Overlay added
Overlay removed
Accelerometer listener registered
Battery floor reached
Face-down sustained
Device is upright. Resuming.
MATCH for "..."
CLICK result=true
```

This makes the project relatively easy to diagnose through Android
Studio's Logcat.

------------------------------------------------------------------------

## 📄 License

No license is specified in the supplied project source.

If this repository will be published publicly, add an appropriate
`LICENSE` file before distributing the project.

------------------------------------------------------------------------

::: {align="center"}
### ◼ BingeLock

**Less interruption. More watching.**

Built as a focused Android utility.
:::
