# 📋 Stack de Gobierno de Datos - DataHub

## 🎯 Descripción General

El **docker-compose-governance.yml** implementa una plataforma completa de gobierno de datos usando DataHub de LinkedIn. Proporciona catálogo de datos, lineage, descoberta, calidad de datos y políticas de gobernanza para toda la organización.

## 🏗️ Servicios Incluidos

### 📊 **Data Catalog Platform**
- **DataHub Backend (GMS)** - Metadata management service
- **DataHub Frontend** - Interfaz web de usuario
- **DataHub PostgreSQL** - Base de datos de metadatos
- **DataHub Elasticsearch** - Motor de búsqueda de metadatos

## 🔌 Puertos Expuestos

| Servicio | Puerto | Protocolo | Descripción |
|----------|--------|-----------|-------------|
| DataHub Frontend | 9003 | HTTP | Interfaz web principal |
| DataHub Backend | 8085 | HTTP | API REST de metadatos |
| DataHub PostgreSQL | 5437 | TCP | Base de datos |
| DataHub Elasticsearch | 9201 | HTTP | Motor de búsqueda |

## 🔑 Accesos por Defecto

### DataHub Frontend
- **URL:** http://localhost:9003
- **Demo login:** datahub / datahub
- **Admin login:** admin / admin

### DataHub Backend (GMS)
- **API:** http://localhost:8085
- **Health:** http://localhost:8085/health
- **OpenAPI:** http://localhost:8085/openapi/swagger-ui/index.html

### DataHub Elasticsearch
- **API:** http://localhost:9201
- **Health:** http://localhost:9201/_cluster/health
- **Indices:** http://localhost:9201/_cat/indices

### DataHub Database
- **Host:** localhost:5437
- **Usuario:** datahub
- **Password:** datahub123
- **Base de datos:** datahub

## ⚙️ Variables de Entorno Requeridas

```bash
# DataHub Database
DATAHUB_DB_NAME=datahub
DATAHUB_DB_USER=datahub
DATAHUB_DB_PASSWORD=datahub123

# DataHub Configuration
DATAHUB_GMS_HOST=datahub-backend
DATAHUB_GMS_PORT=8080
DATAHUB_SECRET=datahub123456789012345678901234567890

# Elasticsearch
ELASTICSEARCH_HOST=datahub-elasticsearch
ELASTICSEARCH_PORT=9200

# Analytics
DATAHUB_ANALYTICS_ENABLED=true

# Resource Limits
CPU_LIMIT_LARGE=2.0
CPU_LIMIT_MEDIUM=1.0
MEMORY_LIMIT_LARGE=2g
MEMORY_LIMIT_MEDIUM=1g
```

## 🚀 Casos de Uso

### 1. **Registro de Assets de Datos**

#### Registrar dataset via API
```bash
# Crear dataset
curl -X POST "http://localhost:8085/entities?action=ingest" \
  -H "Content-Type: application/json" \
  -d '{
    "entity": {
      "entityType": "dataset",
      "entityUrn": "urn:li:dataset:(urn:li:dataPlatform:postgres,productos.productos,PROD)",
      "changeType": "UPSERT",
      "aspectName": "datasetProperties",
      "aspect": {
        "customProperties": {},
        "externalUrl": null,
        "name": "productos",
        "qualifiedName": "productos.productos",
        "description": "Tabla principal de productos del sistema"
      }
    }
  }'

# Agregar schema
curl -X POST "http://localhost:8085/entities?action=ingest" \
  -H "Content-Type: application/json" \
  -d '{
    "entity": {
      "entityType": "dataset",
      "entityUrn": "urn:li:dataset:(urn:li:dataPlatform:postgres,productos.productos,PROD)",
      "changeType": "UPSERT",
      "aspectName": "schemaMetadata",
      "aspect": {
        "schemaName": "productos",
        "platform": "urn:li:dataPlatform:postgres",
        "version": 1,
        "fields": [
          {
            "fieldPath": "id",
            "type": {
              "type": {
                "com.linkedin.schema.NumberType": {}
              }
            },
            "nativeDataType": "bigint",
            "description": "Identificador único del producto"
          },
          {
            "fieldPath": "nombre",
            "type": {
              "type": {
                "com.linkedin.schema.StringType": {}
              }
            },
            "nativeDataType": "varchar(255)",
            "description": "Nombre del producto"
          }
        ]
      }
    }
  }'
```

#### Registrar dashboard y métricas
```bash
# Crear dashboard
curl -X POST "http://localhost:8085/entities?action=ingest" \
  -H "Content-Type: application/json" \
  -d '{
    "entity": {
      "entityType": "dashboard",
      "entityUrn": "urn:li:dashboard:(grafana,productos-dashboard)",
      "changeType": "UPSERT",
      "aspectName": "dashboardInfo",
      "aspect": {
        "title": "Dashboard de Productos",
        "description": "Métricas principales del catálogo de productos",
        "charts": [
          "urn:li:chart:(grafana,productos-count)",
          "urn:li:chart:(grafana,ventas-por-categoria)"
        ],
        "datasets": [
          "urn:li:dataset:(urn:li:dataPlatform:postgres,productos.productos,PROD)"
        ]
      }
    }
  }'
```

### 2. **Data Lineage y Dependency Tracking**

#### Establecer lineage entre datasets
```bash
# Crear relación de lineage
curl -X POST "http://localhost:8085/entities?action=ingest" \
  -H "Content-Type: application/json" \
  -d '{
    "entity": {
      "entityType": "dataJob",
      "entityUrn": "urn:li:dataJob:(urn:li:dataFlow:(nifi,productos-sync,PROD),sync-postgres-mongo)",
      "changeType": "UPSERT",
      "aspectName": "dataJobInfo",
      "aspect": {
        "name": "sync-postgres-mongo",
        "description": "Sincronización de productos de PostgreSQL a MongoDB",
        "type": "ETL"
      }
    }
  }'

# Establecer inputs y outputs
curl -X POST "http://localhost:8085/entities?action=ingest" \
  -H "Content-Type: application/json" \
  -d '{
    "entity": {
      "entityType": "dataJob",
      "entityUrn": "urn:li:dataJob:(urn:li:dataFlow:(nifi,productos-sync,PROD),sync-postgres-mongo)",
      "changeType": "UPSERT",
      "aspectName": "dataJobInputOutput",
      "aspect": {
        "inputDatasets": [
          "urn:li:dataset:(urn:li:dataPlatform:postgres,productos.productos,PROD)"
        ],
        "outputDatasets": [
          "urn:li:dataset:(urn:li:dataPlatform:mongodb,productos.productos_sync,PROD)"
        ]
      }
    }
  }'
```

### 3. **Búsqueda y Descubrimiento**

#### Búsqueda avanzada de assets
```bash
# Búsqueda por texto
curl -X POST "http://localhost:8085/entities?action=search" \
  -H "Content-Type: application/json" \
  -d '{
    "input": "productos",
    "entity": "dataset",
    "start": 0,
    "count": 10
  }'

# Búsqueda con filtros
curl -X POST "http://localhost:8085/entities?action=search" \
  -H "Content-Type: application/json" \
  -d '{
    "input": "*",
    "entity": "dataset",
    "filter": {
      "criteria": [
        {
          "field": "platform",
          "value": "postgres",
          "condition": "EQUAL"
        }
      ]
    },
    "start": 0,
    "count": 20
  }'

# Autocompletado
curl -X GET "http://localhost:8085/entities?action=autocomplete&entity=dataset&query=prod"
```

### 4. **Gestión de Tags y Glosarios**

#### Crear glosario de términos de negocio
```bash
# Crear glosario
curl -X POST "http://localhost:8085/entities?action=ingest" \
  -H "Content-Type: application/json" \
  -d '{
    "entity": {
      "entityType": "glossaryNode",
      "entityUrn": "urn:li:glossaryNode:productos-glosario",
      "changeType": "UPSERT",
      "aspectName": "glossaryNodeInfo",
      "aspect": {
        "definition": "Términos relacionados con el catálogo de productos",
        "name": "Productos - Glosario de Términos"
      }
    }
  }'

# Crear término específico
curl -X POST "http://localhost:8085/entities?action=ingest" \
  -H "Content-Type: application/json" \
  -d '{
    "entity": {
      "entityType": "glossaryTerm",
      "entityUrn": "urn:li:glossaryTerm:SKU",
      "changeType": "UPSERT",
      "aspectName": "glossaryTermInfo",
      "aspect": {
        "definition": "Stock Keeping Unit - Identificador único de producto para inventario",
        "name": "SKU",
        "termSource": "Retail Operations",
        "sourceRef": "https://wiki.empresa.com/sku",
        "sourceUrl": "https://wiki.empresa.com/sku"
      }
    }
  }'

# Aplicar término a dataset
curl -X POST "http://localhost:8085/entities?action=ingest" \
  -H "Content-Type: application/json" \
  -d '{
    "entity": {
      "entityType": "dataset",
      "entityUrn": "urn:li:dataset:(urn:li:dataPlatform:postgres,productos.productos,PROD)",
      "changeType": "UPSERT",
      "aspectName": "glossaryTerms",
      "aspect": {
        "terms": [
          {
            "urn": "urn:li:glossaryTerm:SKU"
          }
        ]
      }
    }
  }'
```

#### Gestión de tags
```bash
# Crear tag
curl -X POST "http://localhost:8085/entities?action=ingest" \
  -H "Content-Type: application/json" \
  -d '{
    "entity": {
      "entityType": "tag",
      "entityUrn": "urn:li:tag:PII",
      "changeType": "UPSERT",
      "aspectName": "tagProperties",
      "aspect": {
        "name": "PII",
        "description": "Información Personal Identificable - Requiere tratamiento especial"
      }
    }
  }'

# Aplicar tag a campo
curl -X POST "http://localhost:8085/entities?action=ingest" \
  -H "Content-Type: application/json" \
  -d '{
    "entity": {
      "entityType": "dataset",
      "entityUrn": "urn:li:dataset:(urn:li:dataPlatform:postgres,usuarios.usuarios,PROD)",
      "changeType": "UPSERT",
      "aspectName": "editableSchemaFieldInfo",
      "aspect": {
        "fieldPath": "email",
        "globalTags": {
          "tags": [
            {
              "tag": "urn:li:tag:PII"
            }
          ]
        }
      }
    }
  }'
```

### 5. **Data Quality y Profiling**

#### Registrar métricas de calidad
```bash
# Agregar data profiling
curl -X POST "http://localhost:8085/entities?action=ingest" \
  -H "Content-Type: application/json" \
  -d '{
    "entity": {
      "entityType": "dataset",
      "entityUrn": "urn:li:dataset:(urn:li:dataPlatform:postgres,productos.productos,PROD)",
      "changeType": "UPSERT",
      "aspectName": "datasetProfile",
      "aspect": {
        "timestampMillis": '$(date +%s)000',
        "rowCount": 150000,
        "columnCount": 8,
        "fieldProfiles": [
          {
            "fieldPath": "precio",
            "uniqueCount": 45000,
            "uniqueProportion": 0.3,
            "nullCount": 12,
            "nullProportion": 0.00008,
            "min": "0.99",
            "max": "9999.99",
            "mean": "234.50",
            "median": "89.99",
            "stdev": "456.78"
          }
        ]
      }
    }
  }'

# Registrar assertion de calidad
curl -X POST "http://localhost:8085/entities?action=ingest" \
  -H "Content-Type: application/json" \
  -d '{
    "entity": {
      "entityType": "assertion",
      "entityUrn": "urn:li:assertion:productos-precio-positivo",
      "changeType": "UPSERT",
      "aspectName": "assertionInfo",
      "aspect": {
        "type": "FIELD",
        "description": "El precio del producto debe ser mayor que 0",
        "statement": "precio > 0",
        "operator": "GREATER_THAN",
        "parameters": [
          {
            "value": "0",
            "type": "NUMBER"
          }
        ]
      }
    }
  }'
```

## 🔧 Comandos Útiles

### Gestión del Stack
```bash
# Iniciar governance stack
./manage-stack.sh start governance

# Ver logs de DataHub
docker logs datahub-backend -f
docker logs datahub-frontend -f

# Restart servicios
docker restart datahub-backend
docker restart datahub-frontend
```

### Administración de DataHub
```bash
# Health check completo
curl "http://localhost:8085/health" | jq '.'

# Ver estadísticas del sistema
curl "http://localhost:8085/config" | jq '.'

# Limpiar cache
curl -X POST "http://localhost:8085/operations?action=restoreIndices"

# Backup de metadatos
curl -X GET "http://localhost:8085/entities?action=list&entity=dataset" > datasets-backup.json
```

### Elasticsearch Operations
```bash
# Ver índices de DataHub
curl "http://localhost:9201/_cat/indices?v"

# Ver mapping de metadata
curl "http://localhost:9201/datahub_usage_event_datahubevent_v1/_mapping" | jq '.'

# Buscar en índice directo
curl "http://localhost:9201/datasetindex_v2/_search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": {
      "match": {
        "name": "productos"
      }
    }
  }' | jq '.'

# Estadísticas de búsqueda
curl "http://localhost:9201/_cluster/stats" | jq '.indices'
```

### Data Ingestion CLI
```bash
# Instalar DataHub CLI (en container)
docker exec datahub-backend pip install acryl-datahub

# Ingest desde archivo
docker exec datahub-backend datahub ingest -c /tmp/postgres-ingestion.yml

# Ingest desde PostgreSQL
docker exec datahub-backend datahub ingest \
  --platform postgres \
  --config '{
    "username": "admin",
    "password": "admin123",
    "host_port": "postgres:5432",
    "database": "productos_db"
  }'

# Test de conectividad
docker exec datahub-backend datahub check postgres-connectivity
```

## 🔍 Troubleshooting

### Problemas Comunes

1. **DataHub Backend no inicia**
   ```bash
   # Verificar conectividad a dependencias
   docker exec datahub-backend nc -zv datahub-postgres 5432
   docker exec datahub-backend nc -zv datahub-elasticsearch 9200
   
   # Ver logs detallados
   docker logs datahub-backend --tail 100
   
   # Verificar variables de entorno
   docker exec datahub-backend env | grep DATAHUB
   ```

2. **Frontend no carga**
   ```bash
   # Verificar conectividad a backend
   docker exec datahub-frontend curl -f http://datahub-backend:8080/health
   
   # Ver logs del frontend
   docker logs datahub-frontend --tail 50
   
   # Verificar configuración
   docker exec datahub-frontend cat /etc/datahub/frontend/conf/application.conf
   ```

3. **Búsqueda no funciona**
   ```bash
   # Verificar estado de Elasticsearch
   curl "http://localhost:9201/_cluster/health"
   
   # Ver índices
   curl "http://localhost:9201/_cat/indices?v"
   
   # Reindexar si es necesario
   curl -X POST "http://localhost:8085/operations?action=restoreIndices"
   ```

4. **Ingestion fails**
   ```bash
   # Ver logs de ingestion
   docker logs datahub-backend | grep "ingest"
   
   # Test conectividad a source
   docker exec datahub-backend nc -zv postgres 5432
   
   # Verificar permisos de usuario
   docker exec postgres psql -U admin -d productos_db \
     -c "SELECT * FROM information_schema.tables LIMIT 5;"
   ```

## 📊 Monitoreo y Métricas

### Health Checks
```bash
# DataHub Backend health
curl "http://localhost:8085/health" | jq '.status'

# Elasticsearch cluster health
curl "http://localhost:9201/_cluster/health" | jq '.status'

# Database connectivity
curl "http://localhost:8085/config" | jq '.managedIngestion'

# Frontend health
curl "http://localhost:9003/admin" -I
```

### Usage Analytics
```bash
# Ver estadísticas de uso
curl "http://localhost:8085/analytics?action=getUsageStats" | jq '.'

# Top datasets más accedidos
curl "http://localhost:8085/analytics?action=getTopDatasets&start=0&count=10" | jq '.'

# Actividad de usuarios
curl "http://localhost:8085/analytics?action=getUserActivity" | jq '.'

# Tendencias de búsqueda
curl "http://localhost:8085/analytics?action=getSearchTrends" | jq '.'
```

### Performance Metrics
```json
{
  "datahub_metrics": {
    "ingestion_rate": "entities_per_minute",
    "search_latency": "average_response_time_ms",
    "api_requests": "requests_per_second",
    "elasticsearch_docs": "total_documents_indexed",
    "lineage_depth": "average_lineage_hops"
  }
}
```

## 🔐 Seguridad y Políticas

### Configuración de Autenticación
```bash
# Configurar OIDC con Keycloak
# application.yml para frontend:
auth:
  oidc:
    enabled: true
    clientId: datahub
    clientSecret: ${KEYCLOAK_CLIENT_SECRET}
    discoveryUri: http://keycloak:8080/realms/empresa-realm/.well-known/openid-configuration

# Configurar roles y permisos
curl -X POST "http://localhost:8085/entities?action=ingest" \
  -H "Content-Type: application/json" \
  -d '{
    "entity": {
      "entityType": "corpuser",
      "entityUrn": "urn:li:corpuser:data.steward",
      "changeType": "UPSERT",
      "aspectName": "corpUserInfo",
      "aspect": {
        "email": "data.steward@empresa.com",
        "title": "Data Steward",
        "fullName": "Data Steward"
      }
    }
  }'
```

### Data Policies
```bash
# Crear política de acceso a datos
curl -X POST "http://localhost:8085/entities?action=ingest" \
  -H "Content-Type: application/json" \
  -d '{
    "entity": {
      "entityType": "dataHubPolicy",
      "entityUrn": "urn:li:dataHubPolicy:pii-access-policy",
      "changeType": "UPSERT",
      "aspectName": "dataHubPolicyInfo",
      "aspect": {
        "displayName": "PII Access Policy",
        "description": "Restringir acceso a datos PII a usuarios autorizados",
        "type": "METADATA",
        "state": "ACTIVE",
        "resources": {
          "filter": {
            "criteria": [
              {
                "field": "tags",
                "value": "urn:li:tag:PII",
                "condition": "EQUAL"
              }
            ]
          }
        },
        "privileges": ["VIEW_ENTITY_PAGE", "EDIT_ENTITY"],
        "actors": {
          "users": ["urn:li:corpuser:data.steward"],
          "groups": ["urn:li:corpGroup:data-governance-team"]
        }
      }
    }
  }'
```

### Audit Logging
```bash
# Ver logs de auditoría
curl "http://localhost:8085/analytics?action=getAuditLogs&start=0&count=100" | jq '.'

# Configurar retención de auditoría
# En application.yml:
analytics:
  enabled: true
  retentionPolicy:
    auditLogs: 365  # días
    usageEvents: 90 # días
```

## 🏗️ Arquitectura de Integración

### Data Ingestion Pipeline
```
Data Sources → DataHub Ingestion → Metadata Store → Search Index
     ↓              ↓                 ↓              ↓
  Postgres       Python CLI       PostgreSQL    Elasticsearch
  MongoDB        REST API         (metadata)     (search)
  NiFi           Kafka Sink
  Kafka          Custom Hooks
```

### Ingestion Sources Configurados
```yaml
# postgres-ingestion.yml
source:
  type: postgres
  config:
    username: admin
    password: admin123
    host_port: postgres:5432
    database: productos_db
    schema_pattern:
      allow: ["public"]
    table_pattern:
      allow: ["productos", "categorias", "usuarios"]

# nifi-ingestion.yml
source:
  type: nifi
  config:
    site_url: http://nifi:8084/nifi
    auth:
      username: admin
      password: admin123456789
    process_group_pattern:
      allow: [".*"]

# kafka-ingestion.yml
source:
  type: kafka
  config:
    connection:
      bootstrap: kafka:29092
    topic_patterns:
      allow: ["productos.*", "eventos.*"]
```

### Custom Metadata Integration
```python
# Script para ingestion custom
from datahub.emitter.mce_builder import make_data_platform_urn, make_dataset_urn
from datahub.emitter.mcp import MetadataChangeProposalWrapper
from datahub.emitter.rest_emitter import DatahubRestEmitter
from datahub.metadata.schema_classes import DatasetPropertiesClass

emitter = DatahubRestEmitter("http://localhost:8085")

dataset_urn = make_dataset_urn(
    platform="custom",
    name="mi_dataset_especial",
    env="PROD"
)

metadata_change_proposal = MetadataChangeProposalWrapper(
    entityType="dataset",
    changeType="UPSERT",
    entityUrn=dataset_urn,
    aspectName="datasetProperties",
    aspect=DatasetPropertiesClass(
        name="Mi Dataset Especial",
        description="Dataset creado por proceso custom"
    )
)

emitter.emit_mcp(metadata_change_proposal)
```

## 🔗 Dependencias

### Prerequisitos:
- `docker-compose-base.yml` (redes)

### Integra con:
- `docker-compose-dataplatform.yml` (NiFi metadata)
- `docker-compose-apps.yml` (API datasets)
- `docker-compose-observability.yml` (métricas de uso)
- `docker-compose-identity.yml` (autenticación OIDC)

## 📚 Enlaces Útiles

- [DataHub Documentation](https://datahubproject.io/docs/)
- [DataHub REST API](https://datahubproject.io/docs/api/restli/restli-overview)
- [DataHub CLI Guide](https://datahubproject.io/docs/cli)
- [DataHub Ingestion Sources](https://datahubproject.io/docs/metadata-ingestion/source-overview)
- [DataHub GraphQL API](https://datahubproject.io/docs/api/graphql/getting-started)
- [DataHub Python SDK](https://datahubproject.io/docs/metadata-ingestion/as-a-library)

## 🎯 Próximos Pasos

1. **Configurar ingestion automática** desde todas las fuentes
2. **Implementar data quality rules** y monitoring
3. **Configurar alertas** para cambios en esquemas
4. **Integrar con workflow approval** para cambios críticos
5. **Configurar lineage automático** desde ETL tools