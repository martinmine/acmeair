#!/bin/bash

# Resolves the service names to their corresponding IP

while IFS= read -r serverEntry
do
    serverName=$(echo $serverEntry | awk '{print $1}')
    serverIp=$(echo $serverEntry | awk '{print $2}')
    serviceName=$(echo $serverEntry | awk '{print $3}')
    sed -i "s/${serviceName}/${serverIp}/g" ./acmeair-*/Dockerfile
    sed -i "s/${serviceName}/${serverIp}/g" ./nginx/conf/nginx.conf
done < ~/Documents/acmeair-openstack/serverlist
