#!/bin/bash
set -ux
mkdir -p shots
adb wait-for-device
for i in $(seq 1 60); do B=$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d "\r"); [ "$B" = "1" ] && break; sleep 5; done
echo "boot_completed=$B"
adb shell settings put global hide_error_dialogs 1 || true
adb install -r build/Nuskha-preview.apk
adb shell am start -n de.nuskha.keyboard/.MainActivity
sleep 30
adb shell input tap 110 430 || true
sleep 8
FOCUS=$(adb shell dumpsys window 2>/dev/null | grep -i mCurrentFocus | head -1)
echo "CURRENT_FOCUS: $FOCUS"
adb exec-out screencap -p > shots/main.png
SIZE=$(stat -c %s shots/main.png)
echo "main.png bytes: $SIZE"
if [ "$SIZE" -lt 10000 ]; then echo "SCREENSHOT_EMPTY"; exit 1; fi
adb logcat -d -s AndroidRuntime:E | tail -20 || true
adb shell ime list -s | grep nuskha && echo "IME registered"
adb shell ime enable de.nuskha.keyboard/.NuskhaIME && adb shell ime set de.nuskha.keyboard/.NuskhaIME && echo "IME set"
adb logcat -d | grep -i "de.nuskha" | grep -i "fatal\|exception" | head -5 || true
echo EMULATOR_TEST_DONE
