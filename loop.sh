#!/bin/bash

# switch to config directory
cd "config"

while true; do
	java \
		-Dfile.encoding=UTF-8 \
		-Dlog4j.configurationFile=log4j2.xml \
		-Dchrisliebot.core=prod-core.json \
		-Xmx200m \
		-XX:+UseStringDeduplication \
		--enable-preview \
		-jar ../chrisliebot-*.jar
	code=$?

	case $code in
	0) # proper shutdown
		echo "Bot performed proper shutdown, exiting restart loop"
		exit 0
		;;
	10) # restart request
		echo "Bot requested to restart"
		continue
		;;
	20) # upgrade requested
		echo "Bot requested to upgrade"
		../upgrade.sh
		continue
		;;
	*)
		echo "Unknown exit code, attempting recovery restart in a few seconds"
		sleep 10
		continue
		;;
	esac
done
