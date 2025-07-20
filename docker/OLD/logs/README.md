# Estructura de la Carpeta de Logs

Esta carpeta contiene los logs centralizados de todos los servicios y herramientas del stack de microservicios, CI/CD y DevSecOps.

## Estructura General

```
logs/
├── airflow/
│   ├── webserver/
│   ├── scheduler/
│   └── postgres/
├── alfresco/
│   ├── repo/
│   ├── share/
│   ├── solr/
│   ├── activemq/
│   ├── postgres/
│   ├── sfs/
│   ├── transform/
│   └── content-app/
├── argocd/
├── command-service/
├── datahub/
│   ├── backend/
│   ├── frontend/
│   └── postgres/
├── falco/
├── frontend/
├── gorules/
│   ├── editor/
│   └── brms/
├── harbor/
│   ├── core/
│   ├── jobservice/
│   ├── registry/
│   ├── db/
│   └── redis/
├── kafka/
├── keycloak/
│   ├── app/
│   └── db/
├── kong/
├── mongodb/
├── nifi/
│   ├── app/
│   └── registry/
├── nginx-edge/
├── owasp-zap/
├── otel-collector/
├── postgres/
├── qwiet/
├── query-service/
├── snyk/
├── sonarqube/
├── trivy/
├── zookeeper/
```

## Otras Carpetas y Archivos de Configuración

### `/filebeat`
- **Utilidad:** Configuración y Dockerfile para el agente Filebeat, que recolecta y envía logs a sistemas como ELK o Loki.
- **Archivos internos:**
  - `filebeat.yml`: Configuración de inputs, outputs y paths de logs.
  - `Dockerfile`: Imagen personalizada para Filebeat.

### `/logging`
- **Utilidad:** Configuración de la capa de logging centralizado (Logstash, Promtail, etc.).
- **Archivos internos:**
  - `logstash.yml`: Configuración general de Logstash.
  - `logstash.conf`: Pipeline de Logstash (inputs, filters, outputs).
  - `promtail-config.yml`: Configuración de Promtail para enviar logs a Loki.

### `/monitoring`
- **Utilidad:** Configuración de monitoreo y observabilidad (Prometheus, Alertmanager, Grafana, Blackbox, etc.).
- **Archivos internos:**
  - `prometheus.yml`: Scrape configs y reglas de Prometheus.
  - `alert-rules.yml`: Reglas de alerta para Prometheus.
  - `alertmanager.yml`: Configuración de Alertmanager.
  - `blackbox.yml`: Configuración de Blackbox Exporter.
  - `kafka-jmx-bitnami.yml`: Configuración de JMX Exporter para Kafka.
  - `/grafana/`: Dashboards y datasources para Grafana.

### `/monitoring/grafana`
- **Utilidad:** Configuración avanzada de Grafana para dashboards, datasources y aprovisionamiento automático.
- **Subcarpetas y archivos internos:**
  - `/dashboards/`: Dashboards JSON listos para importar en Grafana.
    - Ejemplos: `microservices.json`, `logging-comparison.json`, `kafka.json`, `infrastructure.json` (dashboards para microservicios, logging, Kafka, infraestructura, etc.)
  - `/provisioning/`: Configuración de aprovisionamiento automático de Grafana.
    - `/datasources/`: Fuentes de datos preconfiguradas para Grafana.
      - Ejemplos: `prometheus.yml`, `loki.yml`, `elasticsearch.yml` (conexión automática a Prometheus, Loki, Elasticsearch, etc.)
    - `/dashboards/`: Dashboards que se aprovisionan automáticamente al iniciar Grafana.
      - Ejemplo: `dashboard.yml` (define qué dashboards JSON se cargan automáticamente).

- **Resumen:**
  - Permite que Grafana esté listo para usar con dashboards y datasources preconfigurados desde el primer arranque.
  - Facilita la estandarización y el despliegue rápido de entornos de monitoreo.

### `/nginx`
- **Utilidad:** Configuración del reverse proxy Nginx para exponer servicios, balancear carga y aplicar reglas de seguridad.
- **Archivos internos:**
  - `nginx.conf`: Configuración principal de Nginx (incluye logging, headers, rate limiting, etc.).

### `/security`
- **Utilidad:** Configuración y reglas para herramientas de seguridad, escaneo, análisis de código y dashboards de seguridad.
- **Subcarpetas y archivos internos:**
  - `/provisioning/`: Configuración de dashboards y datasources de seguridad para Grafana.
    - `/datasources/`: Ejemplo: `security-datasources.yml` (fuentes de datos de seguridad preconfiguradas para Grafana).
    - `/dashboards/`: Dashboards de seguridad para aprovisionamiento automático (puede estar vacío si no hay dashboards custom).
  - `/zap/`: Políticas y scripts para OWASP ZAP.
    - `/policies/`: Políticas de escaneo personalizadas (puede estar vacío).
    - `/scripts/`: Scripts de automatización para ZAP (puede estar vacío).
  - `/sonarqube/`: Plugins y configuración personalizada de SonarQube.
    - `/conf/`: Configuración avanzada de SonarQube (puede estar vacío).
    - `/plugins/`: Plugins adicionales para SonarQube (puede estar vacío).
  - `/dashboards/`: Dashboards de seguridad adicionales (puede estar vacío).
  - `/trivy/`: Configuración y reportes de Trivy (puede estar vacío).
  - `/falco/`: Reglas y configuración de Falco (puede estar vacío).

- **Resumen:**
  - Centraliza la configuración y reglas de todas las herramientas de seguridad y análisis del stack.
  - Permite aprovisionar dashboards de seguridad en Grafana y mantener scripts/políticas custom para ZAP, SonarQube, Trivy y Falco.

# Carpeta `/security` - Seguridad Centralizada en tu Stack

Esta carpeta centraliza toda la configuración, reglas, scripts y dashboards relacionados con la **seguridad** de la plataforma de microservicios y DevSecOps.

## ¿Por qué existe esta carpeta?
- **Centralización:** Agrupa en un solo lugar todo lo relacionado con la seguridad, facilitando el mantenimiento, la auditoría y el onboarding.
- **Modularidad:** Cada herramienta de seguridad tiene su propio espacio para configuraciones, reglas y reportes.
- **Automatización:** Permite aprovisionar dashboards, datasources y políticas de seguridad automáticamente en herramientas como Grafana, ZAP, SonarQube, etc.
- **Compliance:** Facilita cumplir con normativas y auditorías al tener toda la evidencia y configuración de seguridad centralizada.
- **DevSecOps:** Integra la seguridad en todo el ciclo de vida del software, desde el desarrollo hasta la operación.

## Subcarpetas y su utilidad
- **provisioning/**: Dashboards y datasources de seguridad para Grafana (visualización de métricas y alertas de seguridad).
- **zap/**: Políticas y scripts personalizados para OWASP ZAP (escaneo DAST automatizado).
- **sonarqube/**: Plugins y configuración avanzada para SonarQube (análisis SAST).
- **dashboards/**: Dashboards de seguridad adicionales (visualización centralizada).
- **trivy/**: Configuración y reportes de Trivy (escaneo de vulnerabilidades en imágenes y dependencias).
- **falco/**: Reglas y configuración de Falco (detección de amenazas en tiempo real a nivel de sistema/host).

## Buenas prácticas
- Si agregas nuevas herramientas de seguridad, crea una subcarpeta bajo `/security`.
- Mantén scripts, políticas y dashboards versionados aquí para trazabilidad y colaboración.
- Usa esta carpeta como referencia para auditorías y revisiones de seguridad.

---

**¡La seguridad es parte integral de tu plataforma, no un agregado posterior!** 


### `otel-collector-config.yaml`
- **Utilidad:** Configuración del OpenTelemetry Collector para recolectar, procesar y exportar métricas y trazas a Prometheus, Jaeger, etc.
- **Ejemplo de uso:**
  - Define receivers (OTLP), exporters (Prometheus, Jaeger), y pipelines de traces y metrics.

---

## Estándar de Organización
- **Cada servicio tiene su propia subcarpeta** para facilitar la administración, backup y auditoría de logs.
- **Servicios complejos** (como Alfresco, DataHub, Airflow, Keycloak, Nifi, Gorules, Harbor) tienen subcarpetas internas para cada componente.
- **Todos los servicios del docker-compose** deben apuntar a su subcarpeta correspondiente para logs.
- **Si agregas nuevos servicios**, crea una subcarpeta bajo `logs/` para ellos.
- **Para servicios con muchos logs**, puedes agregar subcarpetas internas por año/mes/día si lo necesitas para rotación o archivado.

## Ejemplo de configuración en docker-compose.yml
```yaml
  comando-microservicio:
    # ...
    volumes:
      - ./logs/command-service:/app/logs/command-service

  alfresco-repository:
    # ...
    volumes:
      - ./logs/alfresco/repo:/usr/local/tomcat/logs

  harbor-core:
    # ...
    volumes:
      - ./logs/harbor/core:/var/log/harbor
```

## Buenas Prácticas
- Centraliza todos los logs aquí para facilitar el monitoreo y troubleshooting.
- Realiza backups periódicos de esta carpeta.
- Implementa rotación de logs si algún servicio genera grandes volúmenes de datos.
- Si usas herramientas de monitoreo/logging (ELK, Loki, etc.), apunta sus agentes a estas rutas.

---

**¡Sigue este estándar para mantener tu stack profesional, ordenado y fácil de mantener!** 