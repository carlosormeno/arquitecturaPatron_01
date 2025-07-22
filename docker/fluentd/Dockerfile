FROM fluent/fluentd:v1.17-debian-1

USER root

# Instalar plugins necesarios
RUN fluent-gem install \
    fluent-plugin-opensearch \
    fluent-plugin-prometheus \
    fluent-plugin-multi-format-parser \
    fluent-plugin-record-modifier \
    fluent-plugin-rewrite-tag-filter

# Configurar permisos
RUN mkdir -p /var/log/fluentd && \
    chown -R fluent:fluent /var/log/fluentd

USER fluent
