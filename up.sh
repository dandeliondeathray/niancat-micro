#!/usr/bin/env bash

sudo docker create --name tmptaut erikedin/taut
sudo docker cp tmptaut:/var/cert/cert.pem niancat-slack/cert.pem
sudo docker rm tmptaut

sudo docker build -t erikedin/slackrest slackrest
sudo docker build -t erikedin/niancat-slack:ready-for-acceptance -f niancat-slack/Dockerfile.x86_64 niancat-slack

sudo docker-compose -f acceptance/niancat/docker-compose.yml build && \
    sudo docker-compose -f acceptance/niancat/docker-compose.yml run niancatacceptance

echo "Sleeping waiting for Taut to initialize"
sleep 5

