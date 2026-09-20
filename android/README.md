# Quiz TV app

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

APK output:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

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
