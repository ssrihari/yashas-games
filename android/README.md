# Quiz TV wrapper

This is a small Android TV WebView app that loads the live quiz page and forwards the remote D-pad to the page.

## Build

```bash
export ANDROID_SDK_ROOT=/usr/local/share/android-commandlinetools
gradle -p android assembleDebug
```

APK output:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

## Install over Wi-Fi

Enable **Developer options → Wireless debugging** on the TV, then pair/connect with `adb`:

```bash
adb pair TV_IP:PAIRING_PORT
adb connect TV_IP:DEBUG_PORT
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

The app loads the current hosted quiz from:

```text
https://ssrihari.github.io/yashas-games/quiz.html
```

So quiz/question/audio updates do not require rebuilding the APK.
