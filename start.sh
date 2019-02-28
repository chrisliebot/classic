#!/bin/bash

cd "$(dirname "$0")/config"
screen -dmS chrisliebot-irc ./loop.sh
