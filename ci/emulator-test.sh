#!/bin/bash
set -ux
mkdir -p shots
adb wait-for-device
adb shell settings put global hide_error_dialogs 1 || true
adb install -r build/Nuskha-preview.apk
adb shell am start -n de.nuskha.keyboard/.MainActivity
sleep 35
# dismiss any ANR/error dialog that still appeared
UI=$(adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb shell cat /sdcard/ui.xml 2>/dev/null | tr -d "\r")
echo "$UI" | grep -o "isn.t responding" | head -1 || true
WAITB=$(echo "$UI" | python3 -c "
import sys,re
s=sys.stdin.read()
m=re.search(r\"text=.(Wait|Warten).[^b]*bounds=.\[(\d+),(\d+)\]\[(\d+),(\d+)\]\",s)
print(f\"{(int(m.group(2))+int(m.group(4)))//2} {(int(m.group(3))+int(m.group(5)))//2}\" if m else \"\")")
if [ -n "$WAITB" ]; then adb shell input tap $WAITB; sleep 12; fi
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
