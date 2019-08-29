#!/bin/bash

cd "$(dirname "$0")"
screen -dmS chrisliebot \
	./loop.sh
