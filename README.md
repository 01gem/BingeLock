<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:FFB703,100:121212&height=200&section=header&text=BingeLock&fontSize=52&fontColor=FFFFFF&animation=fadeIn&fontAlignY=38&desc=Stay%20watching.%20Skip%20the%20%22Are%20you%20still%20watching%3F%22%20interruptions.&descAlignY=58&descSize=16" width="100%" alt="BingeLock Header"/>

<img src="https://readme-typing-svg.demolab.com/?font=Fira+Code&size=22&pause=1000&color=FFB703&center=true&vCenter=true&width=500&lines=%F0%9F%94%92+Lock+it.;%F0%9F%91%80+Keep+watching.;%F0%9F%AB%B3+Flip+it+down.;%F0%9F%94%8B+Save+your+battery." alt="Typing SVG" />

<br/>
<br/>

<img src="https://img.shields.io/badge/Kotlin-Android-FFB703?style=for-the-badge&logo=kotlin&logoColor=121212" alt="Kotlin Badge"/>
<img src="https://img.shields.io/badge/Android-SDK%2034-FFB703?style=for-the-badge&logo=android&logoColor=121212" alt="Android Badge"/>
<img src="https://img.shields.io/badge/Min%20SDK-30-FFB703?style=for-the-badge&logo=android&logoColor=121212" alt="Min SDK Badge"/>
<img src="https://img.shields.io/badge/Version-1.0-FFB703?style=for-the-badge&logo=git&logoColor=121212" alt="Version Badge"/>

**A lightweight Android utility that automatically dismisses supported video continuation prompts, keeps the screen awake, and can pause when the phone is face-down.**

</div>

---

## ✨ Features

| | |
|---|---|
| 🔒 **Auto-dismiss** | Detects `Video paused. Continue watching?`, `Are you still watching?`, and `Still there?` in supported accessibility targets and attempts to click the matching control. |
| 👀 **Keep awake** | Uses a transparent application overlay with `FLAG_KEEP_SCREEN_ON` while BingeLock is active. |
| 🫳 **Face-down pause** | Detects a sustained face-down position for roughly **10 seconds**, removes the overlay, then restores it when the phone is upright again. |
| 🔋 **Battery floor** | Automatically disables BingeLock below **15%** battery while unplugged. |
| ⚡ **Quick Settings** | Toggle BingeLock directly from an Android Quick Settings tile. |
| 📝 **Dismissal logs** | Successful automatic dismissals are timestamped and stored locally, with the latest **100 entries** retained. |
| 🌑 **Dark utility UI** | Minimal dark interface with high-contrast status text and a monospace activity log. |
| 💾 **Local storage** | Preferences and logs are stored with Android `SharedPreferences`. |

<div align="center">
<img src="https://media.giphy.com/media/xT9IgG50Fb7Mi0prBC/giphy.gif" width="220" alt="Watching animation"/>
</div>

---

## 🛠 Tech Stack

- **Language:** Kotlin
- **Platform:** Android
- **Compile / Target SDK:** 34
- **Min SDK:** 30
- **UI:** XML + AndroidX AppCompat
- **Background:** Foreground Service
- **Automation:** Accessibility Service
- **Sensors:** Accelerometer
- **Overlay:** `WindowManager`
- **Persistence:** `SharedPreferences`
- **Quick toggle:** Android `TileService`

---

## 📂 Project structure

```text
app/src/main/java/com/gem/bingelock/
├── MainActivity.kt                    # Main UI + master switch + history
├── BingeService.kt                    # Foreground service, overlay, battery + sensor logic
├── BingeAccessibilityService.kt       # Prompt detection + auto-click handling
├── BingeTileService.kt                # Quick Settings tile
└── PrefsHelper.kt                     # SharedPreferences wrapper
```

### Supported targets

```text
com.google.android.youtube
app.morphe.android.youtube
```

---

## 🚀 Getting started

### Requirements

- Android Studio
- Android SDK 34
- Android 11+ device/emulator
- Accelerometer for face-down detection

### Build

```bash
./gradlew assembleDebug
```

Install on a connected device:

```bash
./gradlew installDebug
```

### First-run setup

1. Open **BingeLock**
2. Enable the master switch
3. Grant **Display over other apps** permission
4. Enable the **BingeLock Accessibility Service**
5. Add the Quick Settings tile, if desired

> BingeLock relies on Android's Accessibility framework for automatic prompt detection and interaction.

---

## ⚙️ How it works

```text
BingeLock ON
    │
    ├── Foreground Service
    │      ├── Transparent overlay → keep screen awake
    │      ├── Battery monitor     → stop below 15% unplugged
    │      └── Accelerometer       → face-down pause/resume
    │
    └── Accessibility Service
           ├── Watch supported packages
           ├── Find known prompt text
           ├── Click matching node
           ├── Save timestamped log
           └── Send dismissal notification
```

---

## ⚠️ Notes

BingeLock depends on the accessibility hierarchy and visible prompt text exposed by the target app. Changes to YouTube or other supported apps may therefore affect automatic dismissal.

The current UI includes a **“Settings (coming soon)”** button; the underlying implementation already stores settings such as the battery floor and target packages, but the full settings editor is not currently exposed in the UI.

---

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:121212,100:FFB703&height=120&section=footer" width="100%" alt="Footer animation"/>

**Less interruption. More watching.**

Made with ☕ and a stubborn dislike of unnecessary prompts.

</div>
