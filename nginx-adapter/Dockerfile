FROM nginx
RUN rm /etc/nginx/nginx.conf
ADD conf /etc/nginx/
ADD start.sh /

ENTRYPOINT ["/start.sh"]