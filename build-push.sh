#!/bin/bash

./dns.sh
docker-compose build
./push.sh

./dns-reset.sh
