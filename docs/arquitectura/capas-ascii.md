# Arquitectura del Proyecto por Capas - Vista ASCII

Esta vista resume la arquitectura objetivo del proyecto por capas (7 capas de flujo operativo y 5 capas transversales), mostrando las herramientas principales y los protocolos/flujos que las conectan.

```text
========================================================================================
                         CAPAS DE FLUJO OPERATIVO Y DATOS (Verticales)
========================================================================================

┌──────────────────────────────────────────────────────────────────────────────────────┐
│ CAPA 01: PRESENTACIÓN Y CANALES DE NEGOCIO                                           │
│ Angular Frontend (Web App) | Flutter App (Mobile iOS/Android & Desktop)              │
└──────────────────────────────────────────────────────────────────────────────────────┘
                                         │
                                         │  [ FLUJO 01-02 ]
                                         │  HTTP/2, HTTPS / TLS v1.3
                                         │  Peticiones de usuario final (Web & Mobile)
                                         ▼
┌──────────────────────────────────────────────────────────────────────────────────────┐
│ CAPA 02: BORDES Y ENRUTAMIENTO (EDGE)                                                │
│ Nginx Edge                                                                           │
│ Reverse proxy | TLS termination | routing publico | headers | exposicion web         │
└──────────────────────────────────────────────────────────────────────────────────────┘
                                         │
                                         │  [ FLUJO 02-03 ]
                                         │  mTLS / Reverse Proxy Pass
                                         │  Redirección interna al API Gateway
                                         ▼
┌──────────────────────────────────────────────────────────────────────────────────────┐
│ CAPA 03: GESTIÓN Y GOBIERNO DE APIS (API MANAGEMENT)                                 │
│ WSO2 API Manager                                                                     │
│ publicacion de APIs | autenticacion OAuth2/JWT | policies | throttling | gobierno    │
└──────────────────────────────────────────────────────────────────────────────────────┘
                                         │
          ┌──────────────────────────────┼──────────────────────────────┐
          │ [ FLUJO 03-04A ]             │ [ FLUJO 03-04B ]             │ [ FLUJO 03-04C ]
          │ REST / JSON (OAuth2 JWT)     │ REST / JSON (OAuth2 JWT)     │ REST / CMIS / JSON
          │ Peticiones de Escritura      │ Peticiones de Lectura        │ Operaciones Documentales
          ▼                              ▼                              ▼
┌──────────────────────┐       ┌──────────────────────┐       ┌──────────────────────────┐
│ CAPA 04: BACKEND     │       │ CAPA 04: BACKEND     │       │ CAPA 04: GESTIÓN         │
│ CQRS (ESCRITURA)     │       │ CQRS (LECTURA)       │       │ DOCUMENTAL (DMS)         │
│ Command Service      │       │ Query Service        │       │ DMS Service              │
│ Java 21 + Spring     │       │ Java 21 + Spring     │       │ Java 21 + Spring         │
└──────────┬───────────┘       └──────────▲───────────┘       └────────────┬─────────────┘
           │                              │                                │
           │ [ FLUJO 04A-05A ]            │                                │ [ FLUJO 04C-05C ]
           │ JDBC / TCP                   │                                │ CMIS / REST API
           │ Transacciones Escritura      │                                │ Almacenamiento ECM
           ▼                              │                                ▼
┌──────────────────────┐                  │                   ┌──────────────────────────┐
│ CAPA 05: PERSISTENCIA│                  │                   │ CAPA 05: REPOSITORIO ECM │
│ TRANSACCIONAL        │                  │                   │ Alfresco Stack           │
│ PostgreSQL           │                  │                   │ ECM Engine, Solr, MQ,    │
│ (Base de Escritura)  │                  │                   │ Share / Content App UIs  │
└──────────┬───────────┘                  │                   └──────────────────────────┘
           │                              │                                
           │ [ FLUJO 05A-06 ]             │ [ FLUJO 06-04B ]
           │ Binary Replication / WAL     │ Native Kafka Protocol (TCP)
           │ Lectura de Cambios (CDC)     │ Consumo de Eventos Desnormalizados
           ▼                              │                                
┌─────────────────────────────────────────┴────────────────────────────────────────────┐
│ CAPA 06: INTEGRACIÓN, EVENT STREAMING Y CDC                                          │
│ Debezium (CDC Connect) | Apache Kafka (KRaft) | Schema Registry (Avro/JSON)          │
│ Kafdrop / AKHQ (UI Inspección de Tópicos)                                            │
└─────────────────────────────┬────────────────────────────────────────────────────────┘
                              │
                              │ [ FLUJO 06-07 ]
                              │ Kafka Connect / Spark Streaming (TCP)
                              │ Ingesta Streaming de Eventos al Lakehouse
                              ▼
┌──────────────────────────────────────────────────────────────────────────────────────┐
│ CAPA 07: PLATAFORMA DE DATOS, LAKEHOUSE Y BI                                         │
│ MinIO (S3 Parquet/Iceberg) | Project Nessie (Catalog) | Apache Spark (ETL)           │
│ Trino (Motor SQL Analítico) | Power BI (Dashboards/Reportes)                         │
│ Apache Airflow (Orquestación) | Great Expectations (Calidad)                         │
│ OpenMetadata (Catálogo/Linaje) | Jupyter (Exploración / Data Science)                │
└──────────────────────────────────────────────────────────────────────────────────────┘

========================================================================================
                   CAPAS TRANSVERSALES Y DE PLATAFORMA (Cross-Cutting)
========================================================================================

┌──────────────────────────────────────────────────────────────────────────────────────┐
│ CAPA 08: IDENTIDAD Y ACCESO (IAM)                                                    │
│ Keycloak (IAM / OIDC / SAML / Single Sign-On / User Management)                      │
└──────────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────────────┐
│ CAPA 09: GESTIÓN DE SECRETOS Y CRIPTOGRAFÍA                                          │
│ HashiCorp Vault (KV Store, Dynamic Database Credentials, Auto-rotation, PKI Engine)  │
└──────────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────────────┐
│ CAPA 10: SEGURIDAD DE APLICACIONES, POLÍTICAS Y DEVSECOPS                            │
│ OPA (Open Policy Agent - ABAC/RBAC) | SonarQube (SAST) | OWASP ZAP (DAST)            │
│ Trivy (Container & Vuln Scanner) | Falco (Runtime Security) | Snyk Broker            │
└──────────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────────────┐
│ CAPA 11: OBSERVABILIDAD Y TELEMETRÍA CENTRALIZADA                                    │
│ OpenTelemetry Collector | Jaeger (Trazas) | Prometheus + Alertmanager (Métricas)     │
│ ELK Stack (Logs) | Grafana (Dashboards) | Node Exporter | cAdvisor                   │
└──────────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────────────┐
│ CAPA 12: DEVOPS, CI/CD E INFRAESTRUCTURA DE PLATAFORMA                               │
│ GitLab Ultimate (Git, Pipelines, Registry, Security) | Sonatype Nexus (Artefactos)   │
│ Docker / Docker Compose | Portainer (UI Operación)                                   │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

## Lectura Rápida y Resumen de Principales Cambios

1. Flujo de Negocio y CQRS (Capas 01 a 06)
- Canales Omnicanal (Capa 01): Angular (Web) y Flutter (Mobile/Desktop) consumen las APIs públicas a través de Nginx Edge (Capa 02).
- API Management (Capa 03): WSO2 API Manager unifica el enrutamiento, limita tráfico (throttling) y valida tokens OAuth2/JWT antes de delegar a los microservicios.
- Estrategia CQRS (Capa 04):
    - Command Service procesa transacciones de escritura directamente en PostgreSQL (Capa 05).
    - Debezium captura los cambios desde el registro de transacciones (WAL) de PostgreSQL y los envía hacia Apache Kafka en la Capa 06 (Integración, Event Streaming y CDC).
    - Query Service consume dichos eventos desde Kafka y actualiza de forma desnormalizada el modelo de lectura en MongoDB.
- Gestión Documental: DMS Service abstrae las operaciones sobre Alfresco Stack.

2. Modern Data Platform & Lakehouse (Capa 07)
- Reemplaza herramientas legacy e integra un stack de Data Lakehouse / Iceberg:
    - Almacenamiento S3 en MinIO estructurado en tablas Iceberg/Parquet.
    - Catálogo versionado tipo Git con Project Nessie.
    - Procesamiento pesado con Apache Spark y consultas analíticas SQL hiperrápidas con Trino conectadas directamente a Power BI.
    - Gobierno, linaje y calidad de datos centralizados con OpenMetadata, Great Expectations (GX), Apache Airflow y Jupyter Notebooks.

3. Capas Transversales (Capas 08 a 12)
- IAM (Capa 08): Keycloak administra identidades, credenciales de usuario y Single Sign-On.
- Secretos (Capa 09): HashiCorp Vault inyecta credenciales dinámicas de BD y llaves TLS en runtime.
- Seguridad (Capa 10): OPA resuelve autorización fina (ABAC), respaldado por SonarQube, OWASP ZAP, Trivy, Falco y Snyk Broker para seguridad en pipeline y runtime.
- Observabilidad (Capa 11): Estandarizada mediante OpenTelemetry Collector, distribuyendo trazas a Jaeger, métricas a Prometheus/Grafana y logs al ELK Stack.
- DevOps (Capa 12): Consolidación total en GitLab Ultimate (Git, Pipelines, Registry interno y SAST/DAST) + Sonatype Nexus sobre Docker Compose / Portainer.
