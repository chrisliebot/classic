#!/bin/bash


while true; do
	java \
			-Dfile.encoding=UTF-8 \
			-Dlog4j.configurationFile=log4j2.xml \
			-Dconfig.dir=prod
			-Xmx100m \
			-XX:+UseStringDeduplication \
			-jar ../build/libs/chrisliebot-irc-*-all.jar
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
		*)
			echo "Unknown exit code, attempting recovery restart in a few seconds"
			sleep 10
			continue
		;;
	esac
done
