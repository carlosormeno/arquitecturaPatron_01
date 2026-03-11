# 🔄 Stack de Plataforma de Datos - Apache NiFi

## 🎯 Descripción General

El **docker-compose-dataplatform.yml** implementa una plataforma completa de procesamiento y orquestación de datos usando Apache NiFi. Proporciona capacidades ETL/ELT, integración de datos en tiempo real, y gestión visual de flujos de datos.

## 🏗️ Servicios Incluidos

### 🌊 **Data Flow Management**
- **Apache NiFi** - Orquestador visual de flujos de datos
- **NiFi Registry** - Versionado y gestión de flujos

## 🔌 Puertos Expuestos

| Servicio | Puerto | Protocolo | Descripción |
|----------|--------|-----------|-------------|
| NiFi Web UI | 8084 | HTTP | Interfaz web principal |
| NiFi HTTPS | 8443 | HTTPS | Interfaz web segura |
| NiFi Registry | 18080 | HTTP | Registry web UI |

## 🔑 Accesos por Defecto

### Apache NiFi
- **URL HTTP:** http://localhost:8084/nifi
- **URL HTTPS:** https://localhost:8443/nifi
- **Usuario:** admin
- **Password:** admin123456789
- **Docs:** http://localhost:8084/nifi-docs

### NiFi Registry
- **URL:** http://localhost:18080/nifi-registry
- **API:** http://localhost:18080/nifi-registry-api
- **Swagger:** http://localhost:18080/nifi-registry-api/swagger/ui.html

## ⚙️ Variables de Entorno Requeridas

```bash
# NiFi Security
NIFI_WEB_HTTP_PORT=8080
NIFI_WEB_HTTPS_PORT=8443
SINGLE_USER_CREDENTIALS_USERNAME=admin
SINGLE_USER_CREDENTIALS_PASSWORD=admin123456789

# NiFi Configuration
NIFI_SENSITIVE_PROPS_KEY=nifi123456789012
NIFI_CLUSTER_IS_NODE=false
NIFI_ELECTION_MAX_WAIT=30 sec

# NiFi Registry
NIFI_REGISTRY_FLOW_PROVIDER=file
NIFI_REGISTRY_DB_DIR=/opt/nifi-registry/nifi-registry-current/database

# Resource Limits
CPU_LIMIT_XLARGE=2.0
CPU_LIMIT_LARGE=1.0
MEMORY_LIMIT_XLARGE=8g
MEMORY_LIMIT_LARGE=4g
```

## 🚀 Casos de Uso

### 1. **ETL Básico - Base de Datos a Base de Datos**

#### Configurar flujo PostgreSQL → MongoDB
```bash
# Processors necesarios:
# 1. ExecuteSQL (PostgreSQL)
# 2. ConvertAvroToJSON
# 3. PutMongo (MongoDB)

# Crear Connection Pool para PostgreSQL
# Controller Services → DBCPConnectionPool
# Database Connection URL: jdbc:postgresql://postgres:5432/productos_db
# Database Driver Class: org.postgresql.Driver
# Database User: admin
# Database Password: admin123

# Configurar ExecuteSQL
# SQL select query: SELECT * FROM productos WHERE updated_at > ?
# SQL select query arguments: ${last_update_time}
# Max Rows Per Flow File: 1000

# Configurar PutMongo
# Mongo URI: mongodb://mongo:27017
# Mongo Database Name: productos_db
# Mongo Collection Name: productos_sync
```

#### Template de sincronización automática
```xml
<!-- template_sync_databases.xml -->
<template>
  <description>Sincronización automática entre PostgreSQL y MongoDB</description>
  <processors>
    <processor>
      <name>Query PostgreSQL</name>
      <type>org.apache.nifi.processors.standard.ExecuteSQL</type>
      <schedulingPeriod>5 min</schedulingPeriod>
      <properties>
        <property name="SQL select query">
          SELECT id, nombre, precio, categoria, updated_at 
          FROM productos 
          WHERE updated_at > '${last_sync_time:now():minus(5):toNumber():divide(60000):toDate()}'
        </property>
      </properties>
    </processor>
  </processors>
</template>
```

### 2. **Integración con APIs REST**

#### Consumir API externa y procesar datos
```bash
# Processors:
# 1. InvokeHTTP → 2. EvaluateJsonPath → 3. RouteOnAttribute → 4. PutDatabaseRecord

# Configurar InvokeHTTP
# HTTP Method: GET
# Remote URL: https://api.ejemplo.com/productos
# HTTP Headers: 
#   - Authorization: Bearer ${api_token}
#   - Accept: application/json

# Configurar EvaluateJsonPath
# Destination: flowfile-attribute
# Return Type: auto-detect
# $.data[*].id → product_id
# $.data[*].name → product_name
# $.data[*].price → product_price

# Configurar RouteOnAttribute
# valid_product: ${product_price:gt(0)}
# invalid_product: ${product_price:le(0)}
```

#### Webhook endpoint para recibir datos
```bash
# Configurar HandleHttpRequest processor
# Listening Port: 9999
# HTTP Context Map: http-context-map
# Allowed Paths: /webhook/productos

# Configurar HandleHttpResponse processor
# HTTP Status Code: 200
# HTTP Context Map: http-context-map

# URL webhook: http://localhost:9999/webhook/productos
```

### 3. **Procesamiento de Archivos**

#### Monitoreo de directorio y procesamiento CSV
```bash
# Processors:
# 1. GetFile → 2. ConvertRecord → 3. RouteOnContent → 4. PutDatabaseRecord

# Configurar GetFile
# Input Directory: /opt/nifi/input
# File Filter: .*\.csv
# Keep Source File: false
# Minimum File Age: 5 sec

# Configurar ConvertRecord (CSV → JSON)
# Record Reader: CSVReader
# Record Writer: JsonRecordSetWriter
# Schema Access Strategy: Use String Fields From Header

# Configurar Record Reader (CSV)
# Schema Access Strategy: Use String Fields From Header
# Treat First Line as Header: true
# CSV Format: RFC4180
```

#### Procesamiento de archivos grandes (streaming)
```bash
# Para archivos > 1GB usar SplitRecord
# SplitRecord processor:
# Records Per Split: 10000
# Record Reader: CSVReader
# Record Writer: CSVRecordSetWriter

# Seguido por processors paralelos:
# Concurrent Tasks: 5
# Run Schedule: 0 sec (máxima velocidad)
```

### 4. **Integración con Kafka**

#### Productor Kafka
```bash
# Configurar PublishKafka processor
# Kafka Brokers: kafka:29092
# Topic Name: productos-eventos
# Message Key Field: product_id
# Delivery Guarantee: Best Effort

# Security Protocol: PLAINTEXT
# Batch Size: 16384
# Compression Type: snappy
```

#### Consumidor Kafka
```bash
# Configurar ConsumeKafka processor
# Kafka Brokers: kafka:29092
# Topic Name(s): productos-eventos
# Group ID: nifi-consumer-group
# Offset Reset: earliest

# Auto Commit Offsets: true
# Commit Offsets Strategy: Async Commit
# Max Poll Records: 10000
```

### 5. **Data Quality y Validación**

#### Validación de datos con esquemas
```bash
# Configurar ValidateRecord processor
# Record Reader: JsonTreeReader
# Schema Access Strategy: Use Schema Name Property
# Schema Registry: AvroSchemaRegistry

# Schema ejemplo para productos:
{
  "type": "record",
  "name": "Producto",
  "fields": [
    {"name": "id", "type": "long"},
    {"name": "nombre", "type": "string"},
    {"name": "precio", "type": "double"},
    {"name": "categoria", "type": "string"}
  ]
}

# Routing:
# valid → Continuar procesamiento
# invalid → Log errores y enviar a DLQ
```

#### Data profiling y limpieza
```bash
# Processors para limpieza:
# 1. UpdateAttribute → Agregar metadatos
# 2. ReplaceText → Limpiar caracteres especiales
# 3. RouteOnAttribute → Filtrar registros válidos
# 4. UpdateRecord → Transformar campos

# UpdateRecord para normalización:
# /precio → ${field.value:toNumber()}
# /nombre → ${field.value:trim():toUpper()}
# /fecha → ${field.value:toDate('yyyy-MM-dd')}
```

## 🔧 Comandos Útiles

### Gestión del Stack
```bash
# Iniciar data platform stack
./manage-stack.sh start dataplatform

# Ver logs de NiFi
docker logs nifi -f

# Ver logs de Registry
docker logs nifi-registry -f

# Restart servicios
docker restart nifi
docker restart nifi-registry
```

### Administración de NiFi
```bash
# Backup de flujos
curl -X GET "http://localhost:8084/nifi-api/flow/templates" \
  -H "Accept: application/json" > nifi-templates-backup.json

# Exportar configuración
curl -X GET "http://localhost:8084/nifi-api/flow/current-user" \
  -H "Accept: application/json" > nifi-flow-backup.json

# Importar template
curl -X POST "http://localhost:8084/nifi-api/process-groups/root/templates/upload" \
  -F "template=@my-template.xml"

# Limpiar repositorio de contenido
curl -X DELETE "http://localhost:8084/nifi-api/system-diagnostics/content" \
  -H "Content-Type: application/json"
```

### Monitoreo de Performance
```bash
# Ver estadísticas del sistema
curl "http://localhost:8084/nifi-api/system-diagnostics" | jq '.systemDiagnostics'

# Ver estado de processors
curl "http://localhost:8084/nifi-api/flow/status" | jq '.controllerStatus'

# Ver historial de provenance
curl "http://localhost:8084/nifi-api/provenance" | jq '.provenance'

# Métricas de throughput
curl "http://localhost:8084/nifi-api/flow/status/history" | jq '.statusHistory'
```

### NiFi Registry Operations
```bash
# Listar buckets
curl "http://localhost:18080/nifi-registry-api/buckets" | jq '.'

# Crear bucket
curl -X POST "http://localhost:18080/nifi-registry-api/buckets" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Production Flows",
    "description": "Flujos de producción"
  }'

# Listar flows en bucket
curl "http://localhost:18080/nifi-registry-api/buckets/{bucket-id}/flows" | jq '.'

# Ver versiones de flow
curl "http://localhost:18080/nifi-registry-api/buckets/{bucket-id}/flows/{flow-id}/versions" | jq '.'
```

## 🔍 Troubleshooting

### Problemas Comunes

1. **NiFi no inicia o va lento**
   ```bash
   # Verificar memoria asignada
   docker stats nifi
   
   # Aumentar heap size si es necesario
   # En docker-compose: -Xmx8g
   
   # Ver logs de startup
   docker logs nifi --tail 200
   
   # Verificar espacio en disco
   docker exec nifi df -h
   ```

2. **Processors en estado "Invalid"**
   ```bash
   # Ver detalles del error
   curl "http://localhost:8084/nifi-api/processors/{processor-id}" | jq '.component.validationErrors'
   
   # Verificar Controller Services
   curl "http://localhost:8084/nifi-api/flow/controller/controller-services" | jq '.'
   
   # Verificar conectividad a servicios externos
   docker exec nifi nc -zv postgres 5432
   docker exec nifi nc -zv kafka 29092
   ```

3. **Performance degradado**
   ```bash
   # Ver queue sizes
   curl "http://localhost:8084/nifi-api/flow/status" | jq '.controllerStatus.queued'
   
   # Verificar CPU y memoria
   curl "http://localhost:8084/nifi-api/system-diagnostics" | jq '.systemDiagnostics.aggregateSnapshot'
   
   # Limpiar repositorios
   curl -X DELETE "http://localhost:8084/nifi-api/system-diagnostics/content"
   curl -X DELETE "http://localhost:8084/nifi-api/system-diagnostics/provenance"
   ```

4. **Registry connectivity issues**
   ```bash
   # Verificar conectividad NiFi → Registry
   docker exec nifi curl -f http://nifi-registry:18080/nifi-registry-api/buckets
   
   # Ver configuración de registry en NiFi
   curl "http://localhost:8084/nifi-api/controller/registry-clients" | jq '.'
   
   # Test manual de conexión
   curl "http://localhost:18080/nifi-registry-api/about" | jq '.'
   ```

## 📊 Monitoreo y Métricas

### Health Checks
```bash
# NiFi health
curl "http://localhost:8084/nifi-api/system-diagnostics" | jq '.systemDiagnostics.aggregateSnapshot.totalMemory'

# Registry health
curl "http://localhost:18080/nifi-registry-api/about" | jq '.buildInfo'

# Controller status
curl "http://localhost:8084/nifi-api/flow/status" | jq '.controllerStatus.activeThreadCount'
```

### Performance Metrics
```bash
# Throughput por processor
curl "http://localhost:8084/nifi-api/flow/status/history?componentIds={processor-id}" | jq '.statusHistory'

# Memory usage
curl "http://localhost:8084/nifi-api/system-diagnostics" | jq '.systemDiagnostics.aggregateSnapshot.usedMemory'

# Queue statistics
curl "http://localhost:8084/nifi-api/flow/status" | jq '.controllerStatus.queued'

# Active threads
curl "http://localhost:8084/nifi-api/flow/status" | jq '.controllerStatus.activeThreadCount'
```

### Custom Metrics Dashboard
```json
{
  "dashboard": "NiFi Data Platform",
  "panels": [
    {
      "title": "Throughput Rate",
      "query": "rate(nifi_amount_flowfiles_transferred[5m])"
    },
    {
      "title": "Queue Size",
      "query": "nifi_amount_flowfiles_queued"
    },
    {
      "title": "Processing Time",
      "query": "nifi_average_lineage_duration_seconds"
    }
  ]
}
```

## 🔐 Seguridad y Mejores Prácticas

### Configuración de Seguridad
```bash
# Cambiar credenciales por defecto
# En nifi.properties:
nifi.web.http.host=0.0.0.0
nifi.web.http.port=8080
nifi.security.user.login.identity.provider=single-user-provider
nifi.security.user.authorizer=single-user-authorizer

# Para HTTPS en producción:
nifi.web.https.host=0.0.0.0
nifi.web.https.port=8443
nifi.security.keystore=/opt/nifi/conf/keystore.jks
nifi.security.keystoreType=JKS
nifi.security.keystorePasswd=keystorepassword
```

### Data Governance
```bash
# Lineage tracking automático
# NiFi rastrea automáticamente:
# - Origen de datos
# - Transformaciones aplicadas
# - Destino final
# - Tiempo de procesamiento

# Configurar retención de provenance
# Admin → Controller Settings → Data Provenance
# Max Storage Time: 30 days
# Max Storage Size: 10 GB
```

### Backup Strategy
```bash
#!/bin/bash
# Script de backup completo

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backup/nifi_${DATE}"

# 1. Export templates
curl "http://localhost:8084/nifi-api/flow/templates" > ${BACKUP_DIR}/templates.json

# 2. Export flow configuration
curl "http://localhost:8084/nifi-api/flow/current-user" > ${BACKUP_DIR}/flow.json

# 3. Backup registry flows
curl "http://localhost:18080/nifi-registry-api/buckets" > ${BACKUP_DIR}/registry-buckets.json

# 4. Backup file repositories
docker cp nifi:/opt/nifi/nifi-current/flowfile_repository ${BACKUP_DIR}/
docker cp nifi:/opt/nifi/nifi-current/content_repository ${BACKUP_DIR}/

# 5. Compress
tar -czf ${BACKUP_DIR}.tar.gz ${BACKUP_DIR}/
```

## 🏗️ Arquitectura de Integración

### Flujo de Datos Típico
```
Data Sources → NiFi Ingestion → Processing → Data Destinations
     ↓              ↓             ↓            ↓
   APIs         GetFile       Transform    PutDatabase
   Files        InvokeHTTP    ValidateRecord  PublishKafka
   Databases    ConsumeKafka  ConvertRecord   PutFile
   Streams      GetSQL        RouteOnAttribute PutElastic
```

### Conectores Disponibles
```bash
# Databases
- ExecuteSQL, PutSQL
- PutDatabaseRecord
- QueryDatabaseTable

# Big Data
- PutHDFS, GetHDFS
- PutHBase, FetchHBase
- PutElasticsearch

# Cloud
- PutS3Object, FetchS3Object
- PutGCSObject
- PutAzureBlobStorage

# Message Queues
- PublishKafka, ConsumeKafka
- PublishAMQP, ConsumeAMQP
- PublishMQTT, ConsumeMQTT

# APIs
- InvokeHTTP
- PostHTTP
- HandleHttpRequest
```

## 🔗 Dependencias

### Prerequisitos:
- `docker-compose-base.yml` (redes y Kafka)

### Integra con:
- `docker-compose-observability.yml` (métricas y logs)
- `docker-compose-governance.yml` (catálogo de datos)
- `docker-compose-apps.yml` (APIs de microservicios)

## 📚 Enlaces Útiles

- [Apache NiFi Documentation](https://nifi.apache.org/docs.html)
- [NiFi User Guide](https://nifi.apache.org/docs/nifi-docs/html/user-guide.html)
- [NiFi Registry Guide](https://nifi.apache.org/docs/nifi-registry-docs/html/user-guide.html)
- [NiFi Expression Language](https://nifi.apache.org/docs/nifi-docs/html/expression-language-guide.html)
- [NiFi REST API](https://nifi.apache.org/docs/nifi-docs/rest-api/index.html)
- [NiFi Best Practices](https://community.cloudera.com/t5/Community-Articles/HDF-Best-Practices-for-Setting-up-a-High-Performance-NiFi/ta-p/244999)

## 🎯 Próximos Pasos

1. **Configurar clustering** para alta disponibilidad
2. **Implementar custom processors** específicos del negocio
3. **Configurar SSL/TLS** para comunicaciones seguras
4. **Integrar con data catalog** (DataHub)
5. **Configurar alertas** para fallos de flujos