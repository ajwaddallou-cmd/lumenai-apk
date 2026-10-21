set -eu
export LD_LIBRARY_PATH=/usr/x86_64-linux-gnu/lib${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}
cd ~/nuskha
bt=$HOME/android-sdk/build-tools/35.0.0
platform=$HOME/android-sdk/platforms/android-35/android.jar
mkdir -p build/classes build/gen build/dex
"$bt/aapt" package -f -M AndroidManifest.xml -S res -A assets -I "$platform" -J build/gen -F build/unsigned.apk
javac -source 8 -target 8 -bootclasspath "$platform" -classpath "$bt/core-lambda-stubs.jar" -d build/classes src/de/nuskha/keyboard/*.java
java -cp "$bt/lib/d8.jar" com.android.tools.r8.D8 --lib "$platform" --min-api 31 --output build/dex build/classes/de/nuskha/keyboard/*.class
python3 - <<'PY'
import zipfile
with zipfile.ZipFile('build/unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:z.write('build/dex/classes.dex','classes.dex')
PY
"$bt/zipalign" -f -p 4 build/unsigned.apk build/aligned.apk
[ -f build/debug.jks ] || keytool -genkeypair -keystore build/debug.jks -storepass android -keypass android -alias debug -dname CN=NuskhaPreview -keyalg RSA -validity 3650
java -jar "$bt/lib/apksigner.jar" sign --ks build/debug.jks --ks-pass pass:android --out build/Nuskha-preview.apk build/aligned.apk
java -jar "$bt/lib/apksigner.jar" verify --verbose build/Nuskha-preview.apk
sha256sum build/Nuskha-preview.apk
