# Quiz and Garden TV apps

This Gradle project builds two separate Android TV apps:

| Module | App | Package | Hosted page |
| --- | --- | --- | --- |
| `app` | **Quiz** | `com.yashas.quiztv` | `quiz.html` |
| `garden` | **Garden** | `com.yashas.gardentv` | `garden.html` |

They are installed, launched, and updated independently; each has its own launcher tile and its own saved data. Prerequisites, building, and Wi-Fi pairing below apply to both. The install, launch, and test sections are for Quiz; the Garden app has its own section [at the end](#garden-app).

## Quiz

A native Android TV WebView app for the quiz. The Android wrapper handles the TV remote and loads the live quiz page:

```text
https://ssrihari.github.io/yashas-games/quiz.html
```

The quiz remains HTML/CSS/JavaScript. Question content, layout, and audio updates are delivered from GitHub Pages without rebuilding the APK.

## Prerequisites

- Android TV with **Developer options → Wireless debugging** enabled
- A computer on the same Wi-Fi network as the TV
- Android SDK platform tools (`adb`)
- Gradle

On this development machine, the SDK and `adb` are at:

```bash
export ANDROID_SDK_ROOT=/usr/local/share/android-commandlinetools
export ADB="$ANDROID_SDK_ROOT/platform-tools/adb"
```

## Build the APK

From the repository root:

```bash
export ANDROID_SDK_ROOT=/usr/local/share/android-commandlinetools
gradle -p android assembleDebug
```

This builds both apps. APK output:

```text
android/app/build/outputs/apk/debug/app-debug.apk         # Quiz
android/garden/build/outputs/apk/debug/garden-debug.apk   # Garden
```

To build only one: `gradle -p android :app:assembleDebug` or `gradle -p android :garden:assembleDebug`.

## First-time Wi-Fi pairing

On the TV, open **Settings → Device Preferences → Developer options → Wireless debugging → Pair device with pairing code**.

Use the IP address and pairing port shown by the TV:

```bash
$ADB pair TV_IP:PAIRING_PORT
```

Enter the pairing code shown on the TV. Then return to the Wireless debugging screen and connect using the debug port:

```bash
$ADB connect TV_IP:DEBUG_PORT
$ADB devices
```

Use the device serial printed by `adb devices` for the remaining commands. The serial may be an mDNS value such as `adb-..._adb-tls-connect._tcp`.

## Install or update

```bash
SERIAL='DEVICE_SERIAL_FROM_ADB_DEVICES'
$ADB -s "$SERIAL" install -r android/app/build/outputs/apk/debug/app-debug.apk
```

`-r` updates the wrapper without clearing the quiz's locally stored progress. Do **not** use `pm clear` unless you intentionally want to erase saved progress and cached app data.

The app appears on the TV under **Apps → Quiz** and remains installed after a TV restart.

## Launch

From the TV, open **Home → Apps → Quiz**. Or launch it from the computer:

```bash
$ADB -s "$SERIAL" shell am start -n com.yashas.quiztv/.MainActivity
```

The app briefly shows the Quiz splash screen. Press **OK** on **Start** to begin.

## Remote navigation test

The app uses spatial TV navigation:

- **Left / Right:** move within the current row
- **Up / Down:** move between the header, answer, and bottom navigation rows
- **OK:** activate the focused item
- **Back:** close an open picker/report, or leave the app

The rows are:

1. Header: **Done**, **Category**, **Level**
2. Answers: answer choices
3. Bottom navigation: **Previous**, **Hear**, **Next**

Suggested smoke test:

1. Open Quiz and verify the splash screen appears.
2. Press OK on Start; verify question audio plays.
3. Use Left/Right to move between answers and OK to select one.
4. Use Down, then Left/Right, to focus Previous, Hear, or Next.
5. Use Up to reach Category, then Left/Right and OK to open a picker.
6. Complete or reveal enough questions to open Today’s progress.
7. Verify progress buttons are navigable.
8. Open Parent details and use Up/Down to scroll the full report.
9. Close and reopen the app; verify saved progress remains.

## Useful test commands

Capture a TV screenshot:

```bash
$ADB -s "$SERIAL" exec-out screencap -p > /tmp/quiz-tv.png
```

View wrapper remote logs:

```bash
$ADB -s "$SERIAL" logcat -s QuizTV:D '*:S'
```

Force-stop and relaunch without clearing saved progress:

```bash
$ADB -s "$SERIAL" shell am force-stop com.yashas.quiztv
$ADB -s "$SERIAL" shell am start -n com.yashas.quiztv/.MainActivity
```

Clear all app data only when deliberately resetting the TV:

```bash
$ADB -s "$SERIAL" shell pm clear com.yashas.quiztv
```

## Updating the quiz

Changes to `quiz.html`, question data, CSS, JavaScript, or `audio/*.mp3` only need to be committed and pushed to GitHub Pages. Rebuild the APK only when changing the native Android wrapper, launcher icon, or manifest.

The wrapper disables WebView HTTP caching so the TV fetches the current hosted quiz when it launches. If a page update is not visible, force-stop and reopen Quiz.

Saved answer records are stored locally in the app's WebView storage. They survive normal app restarts and APK updates, but are erased by clearing app data or uninstalling the app.

## Garden app

GARDEN GUARD (game 9 of `keys.html`) as its own TV app. Like Quiz, the wrapper loads a hosted page and forwards remote buttons to it:

```text
https://ssrihari.github.io/yashas-games/garden.html
```

`garden.html` is a standalone copy of the game with a remote control scheme; `keys.html` is unchanged. **Push `garden.html` and `audio/keys/gg-tv-*.mp3` to GitHub Pages before installing** — until then the app has nothing to load.

### Install and launch

```bash
$ADB -s "$SERIAL" install -r android/garden/build/outputs/apk/debug/garden-debug.apk
$ADB -s "$SERIAL" shell am start -n com.yashas.gardentv/.MainActivity
```

The app appears on the TV under **Apps → Garden**.

Debug builds can load a page served from the computer instead of GitHub Pages, to try changes before pushing:

```bash
python3 -m http.server 8000            # from the repository root
$ADB -s "$SERIAL" reverse tcp:8000 tcp:8000
$ADB -s "$SERIAL" shell am start -n com.yashas.gardentv/.MainActivity \
    --es url http://localhost:8000/garden.html
```

(On the Android emulator, `http://10.0.2.2:8000/garden.html` reaches the computer without `adb reverse`.)

### Remote controls

| Where | Button | Does |
| --- | --- | --- |
| Level pick | Left / Right, OK | choose Easy / Medium / Hard, start |
| Level pick | Back | exit the app |
| Board | Arrows | move the yellow cursor |
| Board | OK on an empty tile | open the helper picker: Left / Right to choose, OK to plant, Back to cancel |
| Board | OK on a helper | open Dig up / Keep: Left / Right, OK; Back keeps it |
| Board | Down from the bottom row | focus the **START** button (Up returns to the board) |
| Anywhere in a game | Play/Pause, or OK on START | start the wave; during a wave, pause / go on |
| Game | Back | first press pauses and asks; second press goes to the level pick |
| Game over | Left / Right, OK | **Play again** or **Levels** (Play/Pause also plays again) |

The game holds still while the picker or dig-up pop-up is open, and pauses if the app goes to the background. Holding a direction repeats.

Keyboard, for testing in a desktop browser: arrows, Enter = OK, Escape or Backspace = Back, Space = Play/Pause, plus the original shortcuts `1`–`5` (plant), `X` (dig up), and `E` / `M` / `H` (level).

### Smoke test

1. Open Garden; the level pick shows with Easy focused and the intro plays.
2. Right/Left move between levels; OK on Easy starts the board.
3. OK on a tile, Right to a helper, OK plants it; water drops by its cost.
4. OK on that helper, OK on **Dig up**; the water comes back.
5. Down past the bottom row to **START**, OK; bugs walk in. Play/Pause pauses and resumes.
6. Back once (prompt), Back again (level pick), Back again (app exits).

Logs and force-stop:

```bash
$ADB -s "$SERIAL" logcat -s GardenTV:D '*:S'
$ADB -s "$SERIAL" shell am force-stop com.yashas.gardentv
```

### Updating the garden

Changes to `garden.html` or its audio only need to be pushed to GitHub Pages. Voice clips for new lines in its `voice-lines` block are generated with:

```bash
python3 scripts/generate_keys_audio.py --html garden.html
```

Rebuild the APK only when changing `android/garden` (wrapper, icon, banner, or manifest).
