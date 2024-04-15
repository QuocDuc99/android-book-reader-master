#!/bin/bash

set -e

uuu() {
  local FROM="$1"
  local TO="$2"
  local COUNT=${FROM%/}
  local COUNT=`echo "${COUNT//\//$'\n'}" | wc -l`
  if [[ "$FROM" == */ ]]; then
    local COUNT=$((COUNT))
  else
    local COUNT=$((COUNT-1))
  fi
  echo "$FROM --> $TO"
  mkdir -p "$TO"
  git archive a/master -- "$FROM" | tar x --strip-components=$COUNT -C "$TO"
}

mmm() {
  local FROM="$1"
  local TO="$2"
  mkdir -p "$TO"
  rsync -av --remove-source-files "$FROM" "$TO"
}

git remote get-url a 2> /dev/null || git remote add a https://github.com/geometer/FBReaderJ
git fetch a

uuu proguard.cfg ./
uuu assets src/main/
uuu jni src/main/
uuu res src/main/
uuu fbreader/app/src/main/java src/main/

(
  cd src/main/java/
  for F in $( find . -name *.aidl ); do
    TO="../aidl/$F"
    echo "$F --> $TO"
    mkdir -p $(dirname $TO)
    mv "$F" "$TO"
  done
)

mmm src/main/java/org/fbreader/util util/src/main/java/org/fbreader/

uuu third-party/AmbilWarna/AndroidManifest.xml ambilWarna/src/main/
uuu third-party/AmbilWarna/res ambilWarna/src/main/
uuu third-party/AmbilWarna/src/ ambilWarna/src/main/java

uuu third-party/SuperToasts/AndroidManifest.xml superToasts/src/main/
uuu third-party/SuperToasts/res superToasts/src/main/
uuu third-party/SuperToasts/src/ superToasts/src/main/java/

uuu third-party/android-filechooser/code/AndroidManifest.xml androidFileChooser/src/main/
uuu third-party/android-filechooser/LICENSE androidFileChooser/
uuu third-party/android-filechooser/NOTICE androidFileChooser/
uuu third-party/android-filechooser/code/proguard.cfg androidFileChooser
uuu third-party/android-filechooser/code/res androidFileChooser/src/main/
uuu third-party/android-filechooser/code/src/ androidFileChooser/src/main/java/

uuu third-party/drag-sort-listview/library/AndroidManifest.xml dragSortListview/src/main/
uuu third-party/drag-sort-listview/README.md dragSortListview/
uuu third-party/drag-sort-listview/library/res  dragSortListview/src/main/
uuu third-party/drag-sort-listview/library/src/  dragSortListview/src/main/java/
