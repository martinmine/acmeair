#!/bin/bash

./dns.sh
docker-compose build
./push.sh

cd nginx-adapter
docker build -t martinmine/nginx-adapter .
docker push martinmine/nginx-adapter
cd ..
./dns-reset.sh
