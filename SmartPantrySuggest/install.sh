#!/bin/bash
# Run from your Android Studio project root (the folder that contains "app"):
#   bash SmartPantrySuggest/install.sh
set -e
HERE="$(cd "$(dirname "$0")" && pwd)"
GRADLE=app/build.gradle.kts
[ -f "$GRADLE" ] || GRADLE=app/build.gradle
if [ ! -f "$GRADLE" ]; then echo "ERROR: run this from the project root (the folder that contains 'app')."; exit 1; fi
PKG=$(grep -E 'namespace' "$GRADLE" | head -1 | sed -E "s/.*[\"']([^\"']+)[\"'].*/\1/")
if [ -z "$PKG" ]; then echo "ERROR: could not find the namespace in $GRADLE"; exit 1; fi
echo "Package name found: $PKG"
DEST="app/src/main/java/$(echo "$PKG" | tr . /)"
mkdir -p "$DEST"
for f in "$HERE"/java/*.java; do
  cp "$f" "$DEST"/
  sed -i "s/^package com\.example\.smartpantry;/package $PKG;/" "$DEST/$(basename "$f")"
done
mkdir -p app/src/main/res
cp -r "$HERE"/res/* app/src/main/res/
cp "$HERE/AndroidManifest.xml" app/src/main/AndroidManifest.xml
echo "Suggestions feature installed. In Android Studio: File > Reload All from Disk, then Run."
