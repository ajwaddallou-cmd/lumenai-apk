#!/bin/bash
set -ux
mkdir -p shots
adb wait-for-device
adb install -r build/Nuskha-preview.apk
adb shell monkey -p de.nuskha.keyboard -c android.intent.category.LAUNCHER 1
sleep 8
adb shell screencap -p /sdcard/nuskha_main.png
adb pull /sdcard/nuskha_main.png shots/main.png
adb logcat -d -s AndroidRuntime:E | tail -20 || true
adb shell ime list -s | grep nuskha && echo "IME registered"
adb shell ime enable de.nuskha.keyboard/.NuskhaIME && adb shell ime set de.nuskha.keyboard/.NuskhaIME && echo "IME set"
adb logcat -d | grep -i "de.nuskha" | grep -i "fatal\|exception" | head -5 || true
echo EMULATOR_TEST_DONE
