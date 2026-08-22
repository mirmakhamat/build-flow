#!/bin/bash
set -e

# Java 21 va Android SDK muhit o'zgaruvchilari
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export ANDROID_HOME=/home/mirmaxamat/Android/Sdk
export PATH=$JAVA_HOME/bin:$PATH

echo "=========================================="
echo "🚀 BuildFlow Production Build Boshlandi..."
echo "=========================================="

# 1. Oldingi build qoldiqlarini tozalash
./gradlew clean

# 2. Release APK va Google Play Bundle (AAB) ni yig'ish
./gradlew assembleRelease bundleRelease --no-daemon

echo "=========================================="
echo "✅ Build Muvaffaqiyatli Yakunlandi!"
echo "=========================================="
echo "📁 Release APK: app/build/outputs/apk/release/app-release.apk"
echo "📁 Google Play AAB: app/build/outputs/bundle/release/app-release.aab"
echo "=========================================="
ls -lh app/build/outputs/apk/release/app-release.apk app/build/outputs/bundle/release/app-release.aab
