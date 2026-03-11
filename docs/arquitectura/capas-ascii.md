# Arquitectura del Proyecto por Capas - Vista ASCII

Esta vista resume la arquitectura objetivo del proyecto por capas, mostrando las herramientas principales y sus relaciones.

```text
┌──────────────────────────────────────────────────────────────────────────────────────┐
│ CAPA DE PRESENTACION Y CANALES                                                      │
│ Angular Frontend | Alfresco Share | Alfresco Content App | Grafana | Kibana        │
│ Kafdrop | Portainer | Gitea | Jenkins | Harbor | Nexus | DataHub UI | GoRules UI   │
└──────────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌──────────────────────────────────────────────────────────────────────────────────────┐
│ CAPA EDGE                                                                           │
│ Nginx Edge                                                                          │
│ Reverse proxy | TLS termination | routing publico | headers | exposicion web        │
└──────────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌──────────────────────────────────────────────────────────────────────────────────────┐
│ CAPA DE API MANAGEMENT                                                              │
│ WSO2 API Manager                                                                    │
│ publicacion de APIs | autenticacion API | policies | throttling | gobierno          │
└──────────────────────────────────────────────────────────────────────────────────────┘
                                        │
          ┌─────────────────────────────┼─────────────────────────────┐
          │                             │                             │
          ▼                             ▼                             ▼
┌──────────────────────┐    ┌──────────────────────┐    ┌──────────────────────────┐
│ CAPA BACKEND CQRS    │    │ CAPA BACKEND CQRS    │    │ CAPA DE GESTION          │
│ Command Service      │    │ Query Service        │    │ DOCUMENTAL               │
│ Java 21 + Spring     │    │ Java 21 + Spring     │    │ DMS Service              │
│ Escritura            │    │ Lectura              │    │ Java 21 + Spring         │
└──────────┬───────────┘    └──────────┬───────────┘    └────────────┬─────────────┘
           │                           │                              │
           ▼                           ▼                              ▼
┌──────────────────────┐    ┌──────────────────────┐    ┌──────────────────────────┐
│ PostgreSQL           │    │ MongoDB              │    │ Alfresco Repository      │
│ datos transaccionales│    │ modelo de lectura    │    │ servicio ECM principal   │
└──────────┬───────────┘    └──────────▲───────────┘    └────────────┬─────────────┘
           │                           │                              │
           ▼                           │                              ▼
┌──────────────────────┐               │                 ┌──────────────────────────┐
│ Debezium             │───────────────┼───────────────►│ Kafka                    │
│ CDC desde PostgreSQL │               │                │ eventos e integracion    │
└──────────────────────┘               │                └──────────────────────────┘
                                       │
                                       └──── Query Service consume eventos

                         ┌────────────────────────────────────────────────┐
                         │ ALFRESCO STACK                                │
                         │ Solr | Shared File Store | Transform | MQ     │
                         └────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────────────┐
│ CAPA DE IDENTIDAD Y SEGURIDAD                                                       │
│ Keycloak | SonarQube | OWASP ZAP | Trivy | Falco | Snyk Broker | OPA               │
└──────────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────────────┐
│ CAPA DE OBSERVABILIDAD                                                               │
│ Prometheus | Alertmanager | Jaeger | OpenTelemetry Collector | Loki | Promtail      │
│ Elasticsearch | Logstash | Filebeat | Grafana | Node Exporter | cAdvisor            │
└──────────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────────────┐
│ CAPA DATA PLATFORM, GOBIERNO Y WORKFLOW                                              │
│ Apache NiFi | NiFi Registry | DataHub Backend | Apache Airflow | GoRules BRMS       │
└──────────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────────────┐
│ CAPA DEVOPS Y OPERACION DE PLATAFORMA                                                │
│ Docker Compose | Portainer | Gitea | Jenkins | Harbor | Nexus                       │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

## Lectura Rápida

- `Angular` consume APIs a través de `Nginx Edge` y `WSO2 API Manager`.
- `Command Service` escribe en `PostgreSQL`.
- `Debezium` captura cambios de `PostgreSQL` y los publica en `Kafka`.
- `Query Service` consume esos eventos y mantiene el modelo de lectura en `MongoDB`.
- `DMS Service` abstrae la integración documental y se apoya en `Alfresco`.
- `Keycloak` resuelve autenticación e identidad.
- `Prometheus`, `Grafana`, `Jaeger`, `Loki` y `ELK` cubren observabilidad.
- `NiFi`, `DataHub`, `Airflow` y `GoRules` amplían la plataforma en integración, gobierno, workflow y reglas.
