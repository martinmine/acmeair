FROM websphere-liberty:common

ADD Dockerfile /root/Dockerfile

RUN rm -rf /opt/ibm/wlp/usr/servers/defaultServer/server.xml
ADD server.xml /opt/ibm/wlp/usr/servers/defaultServer/server.xml

RUN installUtility install --acceptLicense defaultServer
RUN rm -rf /opt/ibm/wlp/usr/servers/defaultServer/workarea

ADD ./build/libs/*.war /opt/ibm/wlp/usr/servers/defaultServer/apps

EXPOSE 9091 9491
#ENTRYPOINT ["/bin/bash"]

ENV CUSTOMER_SERVICE=192.168.99.100/acmeair
ENV AUTH_SERVICE=192.168.99.100/acmeair
ENV MONGO_HOST=192.168.99.100

CMD ["/opt/ibm/wlp/bin/server", "run", "defaultServer"]
