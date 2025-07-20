# 📊 Stack de Observabilidad - Métricas, Logs y Trazas

## 🎯 Descripción General

El **docker-compose-observability.yml** implementa una plataforma completa de observabilidad que combina múltiples stacks de monitoreo: Prometheus/Grafana para métricas, ELK + Loki para logging, y Jaeger para distributed tracing. Proporciona visibilidad total del ecosistema.

## 🔭 Servicios Incluidos

### 📈 **Stack de Métricas (Prometheus)**
- **Prometheus** - Recolección y almacenamiento de métricas
- **Grafana** - Visualización y dashboards
- **AlertManager** - Gestión de alertas
- **Node Exporter** - Métricas del sistema host
- **cAdvisor** - Métricas de containers
- **Blackbox Exporter** - Monitoreo de endpoints

### 📝 **Stack de Logging (Dual: ELK + Loki)**
- **Elasticsearch** - Motor de búsqueda para logs
- **Logstash** - Procesamiento de logs
- **Kibana** - Interfaz web para logs
- **Loki** - Sistema de logging ligero
- **Promtail** - Agente de recolección para Loki
- **Filebeat** - Recolector de logs

### 🔍 **Stack de Tracing**
- **Jaeger** - Distributed tracing
- **OpenTelemetry Collector** - Recolección de telemetría

### 🎛️ **Exporters Especializados**
- **Kafka JMX Exporter** - Métricas JMX de Kafka
- **Kafka Exporter** - Métricas de negocio de Kafka
- **Elasticsearch Exporter** - Métricas de Elasticsearch

## 🔌 Puertos Expuestos

| Servicio | Puerto | Protocolo | Descripción |
|----------|--------|-----------|-------------|
| **Métricas** | | | |
| Prometheus | 9090 | HTTP | Interface web y API |
| Grafana | 3000 | HTTP | Dashboards |
| AlertManager | 9093 | HTTP | Gestión de alertas |
| Node Exporter | 9100 | HTTP | Métricas del host |
| cAdvisor | 8089 | HTTP | Métricas containers |
| Blackbox Exporter | 9115 | HTTP | Health checks |
| **Logging** | | | |
| Elasticsearch | 9200 | HTTP | API REST |
| Elasticsearch | 9300 | TCP | Transport layer |
| Kibana | 5601 | HTTP | Interface web |
| Logstash | 5044 | TCP | Beats input |
| Logstash | 9600 | HTTP | API de métricas |
| Loki | 3100 | HTTP | API de logs |
| **Tracing** | | | |
| Jaeger UI | 16686 | HTTP | Interface web |
| Jaeger Collector | 4317 | gRPC | OTLP collector |
| Jaeger Collector | 14250 | gRPC | Jaeger gRPC |
| OTel Collector | 4318 | HTTP | OTLP HTTP |
| OTel Collector | 9464 | HTTP | Métricas Prometheus |
| **Kafka Monitoring** | | | |
| Kafka JMX Exporter | 9309 | HTTP | Métricas JMX |
| Kafka Exporter | 9308 | HTTP | Métricas Kafka |
| **Elasticsearch** | | | |
| ES Exporter | 9114 | HTTP | Métricas ES |

## 🔑 Accesos por Defecto

### Grafana (Principal Dashboard)
- **URL:** http://localhost:3000
- **Usuario:** admin
- **Password:** admin123
- **Datasources:** Prometheus, Loki, Elasticsearch

### Prometheus
- **URL:** http://localhost:9090
- **Targets:** http://localhost:9090/targets
- **Config:** http://localhost:9090/config

### Kibana (ELK)
- **URL:** http://localhost:5601
- **No requiere autenticación**

### Jaeger (Tracing)
- **URL:** http://localhost:16686

### AlertManager
- **URL:** http://localhost:9093

## ⚙️ Variables de Entorno Requeridas

```bash
# Grafana
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=admin123

# Prometheus
PROMETHEUS_RETENTION=30d

# Resource Limits
CPU_LIMIT_SMALL=0.25
CPU_LIMIT_MEDIUM=0.5
CPU_LIMIT_LARGE=1.0
CPU_LIMIT_XLARGE=2.0
MEMORY_LIMIT_SMALL=256m
MEMORY_LIMIT_MEDIUM=512m
MEMORY_LIMIT_LARGE=1g
MEMORY_LIMIT_XLARGE=2g
```

## 🚀 Casos de Uso

### 1. **Monitoreo de Infraestructura**

#### Ver métricas del sistema
```bash
# CPU, Memoria, Disco del host
curl "http://localhost:9090/api/v1/query?query=node_cpu_seconds_total"

# Métricas de containers
curl "http://localhost:9090/api/v1/query?query=container_cpu_usage_seconds_total"
```

#### Dashboards recomendados en Grafana:
- **Node Exporter Full** - Vista completa del host
- **Docker Container Metrics** - Métricas de containers
- **Kafka Overview** - Monitoreo de Kafka

### 2. **Monitoreo de Aplicaciones**

#### Configurar métricas custom en Spring Boot
```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: prometheus,health,metrics
  metrics:
    export:
      prometheus:
        enabled: true
```

#### Query examples en Prometheus:
```promql
# Request rate
rate(http_requests_total[5m])

# Response time percentiles
histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))

# Error rate
rate(http_requests_total{status=~"5.."}[5m]) / rate(http_requests_total[5m])
```

### 3. **Análisis de Logs**

#### Kibana - Búsquedas avanzadas
```
# Errores en los últimos 15 minutos
level:ERROR AND @timestamp:[now-15m TO now]

# Logs de un servicio específico
container.name:"comando-microservicio" AND level:ERROR

# Request con alta latencia
fields.duration:>2000 AND message:"HTTP"
```

#### Loki - LogQL queries
```logql
# Logs de error por servicio
{container_name="comando-microservicio"} |= "ERROR"

# Rate de logs de error
rate({container_name=~".*microservicio.*"} |= "ERROR"[5m])

# Logs filtered por pattern
{container_name="kafka"} | json | latency > 100
```

### 4. **Distributed Tracing**

#### Configurar OpenTelemetry en Spring Boot
```yaml
# application.yml
management:
  tracing:
    sampling:
      probability: 0.1
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces
```

#### Analizar trazas en Jaeger:
1. **Service Map** - Visualizar dependencias
2. **Trace Timeline** - Analizar latencia
3. **Error Analysis** - Identificar fallos

### 5. **Alerting y Notificaciones**

#### Configurar alertas en Prometheus
```yaml
# alert-rules.yml
groups:
  - name: application.rules
    rules:
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.1
        for: 5m
        annotations:
          summary: "High error rate detected"
```

#### Configurar notificaciones en AlertManager
```yaml
# alertmanager.yml
route:
  group_by: ['alertname']
  receiver: 'web.hook'
receivers:
  - name: 'web.hook'
    slack_configs:
      - api_url: 'YOUR_SLACK_WEBHOOK'
        channel: '#alerts'
```

## 🔧 Comandos Útiles

### Gestión del Stack
```bash
# Iniciar observabilidad
./manage-stack.sh start observability

# Ver estado de todos los servicios
docker-compose -f docker-compose-observability.yml ps

# Verificar health de Prometheus
curl http://localhost:9090/-/healthy
```

### Prometheus - Administración
```bash
# Reload configuration
curl -X POST http://localhost:9090/-/reload

# Ver targets activos
curl http://localhost:9090/api/v1/targets

# Query API
curl "http://localhost:9090/api/v1/query?query=up"
```

### Elasticsearch - Administración
```bash
# Health del cluster
curl http://localhost:9200/_cluster/health

# Ver índices
curl http://localhost:9200/_cat/indices?v

# Crear index template
curl -X PUT "http://localhost:9200/_index_template/logs" \
  -H "Content-Type: application/json" \
  -d @./logging/index-template.json
```

### Grafana -