#!/usr/bin/env bash

sudo docker-compose -f acceptance/niancat/docker-compose.yml build && \
    sudo docker-compose -f acceptance/niancat/docker-compose.yml run niancatacceptance

sudo docker-compose -f acceptance/niancat/docker-compose.yml down