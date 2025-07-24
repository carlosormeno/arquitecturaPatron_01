# 🏗️ Stack Tecnológico Empresarial Modular

Un ecosistema completo de herramientas tecnológicas empresariales, organizado por fases y capacidades específicas. Ideal para demostraciones, desarrollo y arquitecturas de referencia.

## 🎯 Visión General

Este proyecto demuestra una **arquitectura de microservicios completa** con todas las capacidades que una empresa moderna necesita:

- **🔒 Seguridad** (SAST/DAST/IAST)
- **📊 Observabilidad** (Métricas/Logs/Trazas)  
- **🔐 Identidad** (SSO/OAuth2)
- **📁 Gestión de Contenido** (ECM)
- **🔄 CI/CD** (DevSecOps)
- **📈 Gobierno de Datos** (Data Governance)
- **⚡ Procesamiento de Datos** (Data Platform)
- **🌊 Orquestación** (Workflow)
- **📋 Reglas de Negocio** (Business Rules)

## 📋 Stack Completo por Fases

| Fase | Docker Compose | Servicios | Propósito | README |
|------|---------------|-----------|-----------|---------|
| **Principal** | `docker-compose-inicio.yml` | Portainer, Redes | Orquestación | [📖 README-general.md](readme.md) |
| **Base** | `docker-compose-base.yml` | PostgreSQL, MongoDB, Kafka | Infraestructura | [📖 README-base.md](readme_base.md) |
| **Observabilidad** | `docker-compose-observability.yml` | Prometheus, Grafana, ELK, Jaeger | Monitoreo | [📖 README-observability.md](readme_observability.md) |
| **Gateway** | `docker-compose-gateway.yml` | Kong, Nginx | API Gateway | [📖 README-gateway.md](readme_gateway.md) |
| **Identidad** | `docker-compose-identity.yml` | Keycloak | SSO/OAuth2 | [📖 README-identity.md](readme_identity.md) |
| **Seguridad** | `docker-compose-security.yml` | SonarQube, ZAP, Snyk, Falco | DevSecOps | [📖 README-security.md](readme_security.md) |
| **Contenido** | `docker-compose-content.yml` | Alfresco Stack | ECM/DMS | [📖 README-content.md](readme_content.md) |
| **Datos** | `docker-compose-dataplatform.yml` | Apache NiFi | ETL/Pipelines | [📖 README-dataplatform.md](readme_dataplatform.md) |
| **Gobierno** | `docker-compose-governance.yml` | DataHub | Data Catalog | [📖 README-governance.md](readme_governance.md) |
| **Workflow** | `docker-compose-workflow.yml` | Apache Airflow | Orquestación | [📖 README-workflow.md](readme_workflow.md) |
| **Reglas** | `docker-compose-business.yml` | GoRules | Business Rules | [📖 README-business.md](readme_business.md) |
| **CI/CD** | `docker-compose-cicd.yml` | Gitea, Jenkins | DevOps | [📖 README-cicd.md](readme_cicd.md) |
| **Registry** | `docker-compose-registry.yml` | Harbor, Nexus | Artefactos | [📖 README-registry.md](readme_registry.md) |
| **Apps** | `docker-compose-apps.yml` | Microservicios, Frontend | Aplicaciones | [📖 README-apps.md](readme_apps.md) |

# 🐳 Servicios y Versiones por docker-compose

| Docker Compose                   | Nombre de Servicio | Imagen utilizada | Versión |
|----------------------------------|--------------------|------------------|---------|
| docker-compose-inicio.yml        | portainer | portainer/portainer-ce | 2.31.3-alpine |
| docker-compose-inicio.yml        | wait-portainer | busybox | latest |
| docker-compose-base.yml          | postgres | debezium/postgres | 17-alpine |
| docker-compose-base.yml          | postgres-backup | postgres | 15 |
| docker-compose-base.yml          | mongo | mongo | 8.0.11 |
| docker-compose-base.yml          | kong-db | postgres | 15 |
| docker-compose-base.yml          | zookeeper | confluentinc/cp-zookeeper | 7.9.2 |
| docker-compose-base.yml          | kafka | confluentinc/cp-kafka | 7.9.2 |
| docker-compose-base.yml          | debezium | quay.io/debezium/connect | 3.2 |
| docker-compose-observability.yml | prometheus | prom/prometheus | v3.5.0 |
| docker-compose-observability.yml | node-exporter | prom/node-exporter | v1.7.0 |
| docker-compose-observability.yml | cadvisor | gcr.io/cadvisor/cadvisor | v0.52.0 |
| docker-compose-observability.yml | blackbox-exporter | prom/blackbox-exporter | v0.24.0 |
| docker-compose-observability.yml | alertmanager | prom/alertmanager | v0.26.0 |
| docker-compose-observability.yml | jaeger | jaegertracing/all-in-one | 1.55 |
| docker-compose-observability.yml | otel-collector | otel/opentelemetry-collector-contrib | 0.126.0 |
| docker-compose-observability.yml | loki | grafana/loki | 2.9.15 |
| docker-compose-observability.yml | promtail | grafana/promtail | 2.9.15 |
| docker-compose-observability.yml | elasticsearch | docker.elastic.co/elasticsearch/elasticsearch | 8.18.2 |
| docker-compose-observability.yml | elasticsearch-exporter | quay.io/prometheuscommunity/elasticsearch-exporter | latest |
| docker-compose-observability.yml | logstash | docker.elastic.co/logstash/logstash | 8.18.2 |
| docker-compose-observability.yml | kibana | docker.elastic.co/kibana/kibana | 8.18.2 |
| docker-compose-observability.yml | filebeat | build: context: ./filebeat | custom |
| docker-compose-observability.yml | kafka-jmx-exporter | bitnami/jmx-exporter | 1.3.0 |
| docker-compose-observability.yml | kafka-exporter | danielqsj/kafka-exporter | v1.9.0 |
| docker-compose-observability.yml | grafana | grafana/grafana | 12.0.2 |
| docker-compose-gateway.yml       | nginx-edge | nginx | 1.29.0-alpine |
| docker-compose-gateway.yml       | kong | kong | 3.6 |
| docker-compose-identity.yml      | keycloak-db | postgres | 15 |
| docker-compose-identity.yml      | keycloak | bitnami/keycloak | 26.3.1 |
| docker-compose-security.yml      | sonarqube-db | postgres | 15 |
| docker-compose-security.yml      | sonarqube | sonarqube | 25.6.0.109173-community |
| docker-compose-security.yml      | owasp-zap | zaproxy/zap-stable | 2.16.1 |
| docker-compose-security.yml      | falco | falcosecurity/falco-no-driver | 0.39.1 |
| docker-compose-security.yml      | trivy | aquasec/trivy | 0.52.0 |
| docker-compose-security.yml      | security-dashboard | grafana/grafana | 12.0.2 |
| docker-compose-security.yml      | snyk-broker | snyk/broker | github-com |
| docker-compose-security.yml      | shiftleft | shiftleft/core | latest |
| docker-compose-security.yml      | open-policy-agent | openpolicyagent/opa | latest |
| docker-compose-content.yml       | alfresco-postgres | postgres | 15 |
| docker-compose-content.yml       | alfresco-activemq | alfresco/alfresco-activemq | 5.17-jre17-rockylinux8 |
| docker-compose-content.yml       | alfresco-shared-file-store | alfresco/alfresco-shared-file-store | latest |
| docker-compose-content.yml       | alfresco-transform-core-aio | alfresco/alfresco-transform-core-aio | 5.1.7 |
| docker-compose-content.yml       | alfresco-solr | alfresco/alfresco-search-services | 2.0.15 |
| docker-compose-content.yml       | alfresco-repository | alfresco/alfresco-content-repository-community | 25.2.0-A.16 |
| docker-compose-content.yml       | alfresco-share | alfresco/alfresco-share | 23.2.4 |
| docker-compose-content.yml       | alfresco-content-app | alfresco/alfresco-content-app | 6.0.0 |
| docker-compose-dataplatform.yml  | nifi-registry | apache/nifi-registry | 2.0.0 |
| docker-compose-dataplatform.yml  | nifi | apache/nifi | 2.0.0 |
| docker-compose-governance.yml    | datahub-postgres | postgres | 15 |
| docker-compose-governance.yml    | datahub-elasticsearch | elasticsearch | 8.15.0 |
| docker-compose-governance.yml    | datahub-backend | acryldata/datahub-gms | v0.14.1 |
| docker-compose-governance.yml    | datahub-frontend | acryldata/datahub-frontend-react | v0.14.1 |
| docker-compose-workflow.yml      | airflow-postgres | postgres | 15 |
| docker-compose-workflow.yml      | redis | redis | 7.4-alpine |
| docker-compose-workflow.yml      | airflow-init | apache/airflow | 2.10.3 |
| docker-compose-workflow.yml      | airflow-webserver | apache/airflow | 2.10.3 |
| docker-compose-workflow.yml      | airflow-scheduler | apache/airflow | 2.10.3 |
| docker-compose-business.yml      | gorules-editor | gorules/editor | 1.15.0 |
| docker-compose-business.yml      | gorules-brms | gorules/brms | latest |
| docker-compose-cicd.yml          | gitea | gitea/gitea | 1.23.1 |
| docker-compose-cicd.yml          | jenkins | jenkins/jenkins | lts-alpine |
| docker-compose-registry.yml      | harbor-db | postgres | 15 |
| docker-compose-registry.yml      | harbor-redis | redis | 7.4-alpine |
| docker-compose-registry.yml      | harbor-core | goharbor/harbor-core | v2.13.1 |
| docker-compose-registry.yml      | harbor-jobservice | goharbor/harbor-jobservice | v2.13.1 |
| docker-compose-registry.yml      | harbor-registry | goharbor/registry-photon | v2.13.1 |
| docker-compose-registry.yml      | postgres-nexus | postgres | 15-alpine |
| docker-compose-registry.yml      | nexus | sonatype/nexus3 | latest |
| docker-compose-registry.yml      | compliance-dashboard | grafana/grafana | 12.0.2 |
| docker-compose-apps.yml          | comando-microservicio | comando-microservicio | latest |
| docker-compose-apps.yml          | consulta-microservicio | consulta-microservicio | latest |
| docker-compose-apps.yml          | frontend | frontend | latest |
| docker-compose-apps.yml          | kafdrop | obsidiandynamics/kafdrop | 4.1.0 |

## 🚀 Inicio Rápido

### 1. **Prerequisitos**
```bash
# Verificar Docker y Docker Compose
docker --version && docker-compose --version

# Clonar repositorio
git clone <tu-repo>
cd stack-tecnologico

# Configurar variables de entorno
cp .env.example .env
# Editar .env con tus valores
```

```bash
#Crear carpetas de logs
mkdir -p logs/{airflow/{webserver,scheduler,postgres},alfresco/{repo,share,solr,activemq,postgres,sfs,transform,content-app},argocd,command-service,datahub/{backend,frontend,postgres},falco,frontend,gorules/{editor,brms},harbor/{core,jobservice,registry,db,redis},kafka,keycloak/{app,db},kong,mongodb,nifi/{app,registry},nginx-edge,owasp-zap,otel-collector,postgres,qwiet,query-service,snyk,sonarqube,trivy,zookeeper}
```
```bash
#Darles persmisos
chmod -R 777 logs/
```

### 2. **Iniciar Stack Básico** (Recomendado para empezar)

```bash
# Comandos para iniciar el stack basico
docker compose -f docker-compose-inicio.yml up -d
docker compose -f docker-compose-base.yml up -d
docker compose -f docker-compose-observability.yml up -d
docker compose -f docker-compose-gateway.yml up -d
docker compose -f docker-compose-apps.yml up -d
```

### 3. **Verificar Instalación**
```bash
# Ver estado general
docker ps

# Acceder a interfaces principales
echo "🎛️  Portainer: http://localhost:9001"
