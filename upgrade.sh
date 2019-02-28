#!/bin/bash

# we might be in "config" subdir (if called from loop.sh) so cd back to root
cd "$(dirname "$0")"

# stop on first error, dont do earlier or change to script dir could fail
set -e

git pull
git submodule update
gradle :shadowJar

# delete old jar and copy new jar
rm *.jar
cp build/libs/*.jar ./
