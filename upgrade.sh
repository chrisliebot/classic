#!/bin/sh

# we might be in "config" subdir (if called from loop.sh) so cd back to root
cd "$(dirname "$0")"

# delete old jar files from build directory
rm build/libs/*.jar

# stop on first error, note that the commands above this line need could fail without meaning an actual error
set -e

git pull

./gradlew :shadowJar

# fetch current config repo
git -C "config" pull

# delete old jar and copy new jar
rm -f *.jar
cp build/libs/*.jar ./
