#!/bin/bash

trap exit INT

export NIANCAT_CHAT_BASE_URL=http://cayce:8082/v1
export NOTIFICATION_CHANNEL=dandeliondeathray
export SLACK_BOT_TOKEN=$(cat niancat.token)
export SLACK_APP_TOKEN=$(cat connections-write.token)

while [ 1 ]
do
	TODAY=$(date +"%Y%m%d-%H%M%S")
	LOGFILE="log-niancat-${TODAY}.txt"

	#python3 niancat-slack/niancatslack.py 2>&1 | tee ${LOGFILE}
	python3 niancat-slack/niancatslack.py

	echo "The service stopped for some reason. Sleeping 30 seconds..."
	sleep 30
done
