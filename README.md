<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:FFB703,100:121212&height=200&section=header&text=BingeLock&fontSize=52&fontColor=FFFFFF&animation=fadeIn&fontAlignY=38&desc=Stay%20watching.%20Skip%20the%20%22Are%20you%20still%20watching%3F%22%20interruptions.&descAlignY=58&descSize=16" width="100%" alt="BingeLock Header"/>

<img src="https://readme-typing-svg.demolab.com/?font=Fira+Code&size=22&pause=1000&color=FFB703&center=true&vCenter=true&width=520&lines=%F0%9F%94%92+Lock+it.;%F0%9F%91%80+Keep+watching.;%F0%9F%AB%B3+Flip+it+down.;%F0%9F%94%8B+Protect+your+battery." alt="Typing SVG" />

<br/>
<br/>

<img src="https://img.shields.io/badge/Kotlin-Android-FFB703?style=for-the-badge&logo=kotlin&logoColor=121212" alt="Kotlin Badge"/>
<img src="https://img.shields.io/badge/Android-SDK%2034-FFB703?style=for-the-badge&logo=android&logoColor=121212" alt="Android Badge"/>
<img src="https://img.shields.io/badge/Min%20SDK-30-FFB703?style=for-the-badge&logo=android&logoColor=121212" alt="Min SDK Badge"/>
<img src="https://img.shields.io/badge/Version-1.0-FFB703?style=for-the-badge&logo=git&logoColor=121212" alt="Version Badge"/>

**A focused Android utility for uninterrupted video playback — combining prompt automation, screen-awake behavior, physical pause control, and battery protection.**

</div>

---

## 🎯 What BingeLock is for

BingeLock is built to reduce interruptions during supported video playback sessions.

Its core purpose is simple:

> **Keep playback moving without requiring repeated “still watching?” confirmation.**

It does this locally on the device through Android's Accessibility framework, foreground-service capabilities, window overlays, sensors, and persistent local preferences.

---

## ✨ What it can offer

| | |
|---|---|
| 🔒 **Automatic prompt handling** | Detects supported continuation prompts and attempts to activate the matching clickable control automatically. |
| 👀 **Screen-awake behavior** | Maintains a transparent overlay configured to keep the screen on while BingeLock is active. |
| 🫳 **Physical pause control** | A sustained face-down position pauses the overlay; returning upright restores it. |
| 🔋 **Battery protection** | Drops into an automatic shutdown state below **15%** battery when the device is unplugged. |
| ⚡ **Quick access** | Provides a dedicated Android Quick Settings tile for fast enable/disable control. |
| 📝 **Activity history** | Records successful automatic dismissals with timestamps and retains the latest **100 entries**. |
| 🔔 **Status feedback** | Uses an ongoing foreground notification plus dismissal alerts to expose current activity. |
| 💾 **Local operation** | Stores preferences and logs locally with Android `SharedPreferences`. |

### Supported playback targets

```text
com.google.android.youtube
app.morphe.android.youtube
```

---

## 🧩 What it's made of

BingeLock is intentionally built from native Android components rather than external services.

| Component | Role |
|---|---|
| **Kotlin** | Application language |
| **Android SDK 34** | Compile + target platform |
| **Android SDK 30+** | Minimum supported platform |
| **Accessibility Service** | Detects supported playback prompts and interacts with their UI |
| **Foreground Service** | Keeps the core runtime active |
| **WindowManager Overlay** | Provides the transparent screen-awake layer |
| **Accelerometer** | Detects sustained face-down orientation |
| **SharedPreferences** | Stores app state and dismissal history |
| **Quick Settings Tile** | Provides a system-level toggle |
| **AndroidX AppCompat / Core KTX** | Application support libraries |

---

## ⚙️ What it can ensure

Within the behavior implemented by the current source:

```text
Supported prompt detected
        │
        ▼
Accessibility tree searched
        │
        ▼
Clickable target located
        │
        ▼
Automatic click attempted
        │
        ├── Success → log + notification
        └── Failure → no forced action
```

And while active:

```text
BingeLock
   ├── Keeps the screen awake
   ├── Watches battery level
   ├── Watches device orientation
   └── Maintains visible service status
```

The implementation is defensive: it checks the target package, searches for known prompt text, walks up to two parent levels for a clickable target, and logs the result.

---

## 📂 Project structure

```text
app/src/main/java/com/gem/bingelock/
├── MainActivity.kt
├── BingeService.kt
├── BingeAccessibilityService.kt
├── BingeTileService.kt
└── PrefsHelper.kt
```

---

## 🚀 Build

```bash
./gradlew assembleDebug
```

Install on a connected device:

```bash
./gradlew installDebug
```

For the installable APK, see the repository's **Releases** section.

---

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:121212,100:FFB703&height=120&section=footer" width="100%" alt="Footer animation"/>

**Less interruption. More watching.**

</div>
