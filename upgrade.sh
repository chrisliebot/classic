#!/bin/bash

# we might be in "config" subdir (if called from loop.sh) so cd back to root
cd "$(dirname "$0")"


git pull
git submodule update
# TODO backup old .jar in case build fails, otherwise trash it
gradle :shadowJar
