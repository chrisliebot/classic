#!/bin/bash

cd "$(dirname "$0")"
screen -dmS chrisliebot-irc \
	./loop.sh
