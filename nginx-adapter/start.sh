#!/bin/bash

sed -i "s/__replace_host__/$2/g" /etc/nginx/nginx.conf
sed -i "s/__replace_listening_port__/$1/g" /etc/nginx/nginx.conf
nginx