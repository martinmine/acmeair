FROM websphere-liberty:common

ADD Dockerfile /root/Dockerfile

RUN rm -rf /opt/ibm/wlp/usr/servers/defaultServer/server.xml
ADD server.xml /opt/ibm/wlp/usr/servers/defaultServer/server.xml

RUN installUtility install --acceptLicense defaultServer
RUN rm -rf /opt/ibm/wlp/usr/servers/defaultServer/workarea

ADD ./build/libs/*.war /opt/ibm/wlp/usr/servers/defaultServer/apps

RUN apt-get update && apt-get install -y nginx-full
RUN rm /etc/nginx/nginx.conf
ADD conf /etc/nginx/

EXPOSE 80

CMD ["/bin/bash", "-c", "nginx & /opt/ibm/wlp/bin/server run defaultServer"]
