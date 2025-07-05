FROM docker.elastic.co/beats/filebeat:8.18.2

COPY filebeat.yml /usr/share/filebeat/filebeat.yml

USER root
RUN chmod go-w /usr/share/filebeat/filebeat.yml