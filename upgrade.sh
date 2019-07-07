#!/bin/bash

# we might be in "config" subdir (if called from loop.sh) so cd back to root
cd "$(dirname "$0")"

# stop on first error, dont do earlier or change to script dir could fail
set -e

git pull
gradle :shadowJar

# fetch current config repo
git -C "config" pull

# delete old jar and copy new jar
rm -f *.jar
cp build/libs/*.jar ./
