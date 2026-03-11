# 📁 Stack de Gestión de Contenido - Alfresco ECM

## 🎯 Descripción General

El **docker-compose-content.yml** implementa una plataforma completa de gestión de contenido empresarial (ECM) usando Alfresco Community. Incluye el stack completo con repositorio, interfaz web, motor de búsqueda, transformaciones y servicios de colaboración.

## 📚 Servicios Incluidos

### 🗄️ **Core Repository**
- **Alfresco Repository** - Motor de contenido y metadatos
- **Alfresco Share** - Interfaz web colaborativa
- **Alfresco Content App** - Interfaz moderna de gestión

### 🔍 **Búsqueda y Indexación**
- **Alfresco Solr** - Motor de búsqueda empresarial

### 🔄 **Transformaciones**
- **Transform Core AIO** - Conversión de documentos
- **Shared File Store** - Almacenamiento temporal

### 💾 **Persistencia**
- **Alfresco PostgreSQL** - Base de datos del repositorio
- **ActiveMQ** - Cola de mensajes para transformaciones

## 🔌 Puertos Expuestos

| Servicio | Puerto | Protocolo | Descripción |
|----------|--------|-----------|-------------|
| **Core Services** | | | |
| Alfresco Repository | 8082 | HTTP | API REST y WebDAV |
| Alfresco Share | 8083 | HTTP | Interfaz web colaborativa |
| Content App | 4201 | HTTP | Interfaz moderna |
| **Supporting Services** | | | |
| Solr Search | 8983 | HTTP | Admin de búsqueda |
| Transform Service | 8091 | HTTP | API de transformaciones |
| Shared File Store | 8099 | HTTP | Almacenamiento temporal |
| ActiveMQ | 8161 | HTTP | Admin de colas |
| ActiveMQ Broker | 61616 | TCP | Broker de mensajes |
| **Database** | | | |
| PostgreSQL | 5434 | TCP | Base de datos |

## 🔑 Accesos por Defecto

### Alfresco Repository
- **URL:** http://localhost:8082/alfresco
- **Admin:** admin / admin
- **API Explorer:** http://localhost:8082/alfresco/api-explorer
- **WebDAV:** http://localhost:8082/alfresco/webdav

### Alfresco Share
- **URL:** http://localhost:8083/share
- **Login:** admin / admin
- **Demo site:** http://localhost:8083/share/page/site/swsdp

### Alfresco Content App
- **URL:** http://localhost:4201
- **Login:** admin / admin
- **Modern UI:** Single Page Application

### Solr Admin
- **URL:** http://localhost:8983/solr
- **Cores:** alfresco, archive
- **Query:** http://localhost:8983/solr/alfresco/select

### ActiveMQ Admin
- **URL:** http://localhost:8161/admin
- **Login:** admin / admin
- **Queues:** Transform requests

## ⚙️ Variables de Entorno Requeridas

```bash
# Database Configuration
ALFRESCO_DB_NAME=alfresco
ALFRESCO_DB_USER=alfresco
ALFRESCO_DB_PASSWORD=alfresco123

# Repository Configuration
ALFRESCO_ADMIN_PASSWORD=admin
ALFRESCO_HOSTNAME=localhost
ALFRESCO_PORT=8082

# Search Configuration
SOLR_ALFRESCO_HOST=alfresco-repository
SOLR_ALFRESCO_PORT=8080

# Transform Configuration
TRANSFORM_SERVICE_ENABLED=true
SHARED_FILE_STORE_URL=http://alfresco-shared-file-store:8099

# Resource Limits
CPU_LIMIT_LARGE=2.0
CPU_LIMIT_MEDIUM=1.0
MEMORY_LIMIT_XLARGE=4g
MEMORY_LIMIT_LARGE=2g
MEMORY_LIMIT_MEDIUM=1g
```

## 🚀 Casos de Uso

### 1. **Gestión Básica de Documentos**

#### Subir documentos via REST API
```bash
# Autenticación
curl -X POST http://localhost:8082/alfresco/api/-default-/public/authentication/versions/1/tickets \
  -H "Content-Type: application/json" \
  -d '{"userId": "admin", "password": "admin"}'

# Subir archivo
curl -X POST http://localhost:8082/alfresco/api/-default-/public/alfresco/versions/1/nodes/-my-/children \
  -H "Authorization: Basic YWRtaW46YWRtaW4=" \
  -F 'filedata=@documento.pdf' \
  -F 'name=mi-documento.pdf' \
  -F 'nodeType=cm:content'
```

#### Crear folder estructura
```bash
# Crear carpeta principal
curl -X POST http://localhost:8082/alfresco/api/-default-/public/alfresco/versions/1/nodes/-my-/children \
  -H "Authorization: Basic YWRtaW46YWRtaW4=" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Documentos Empresa",
    "nodeType": "cm:folder",
    "properties": {
      "cm:title": "Documentos Corporativos",
      "cm:description": "Repositorio central de documentos"
    }
  }'

# Crear subcarpetas
curl -X POST http://localhost:8082/alfresco/api/-default-/public/alfresco/versions/1/nodes/{folder-id}/children \
  -H "Authorization: Basic YWRtaW46YWRtaW4=" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Contratos",
    "nodeType": "cm:folder"
  }'
```

### 2. **Búsqueda Avanzada de Contenido**

#### Búsqueda por metadatos
```bash
# Búsqueda simple por nombre
curl "http://localhost:8082/alfresco/api/-default-/public/search/versions/1/search" \
  -H "Authorization: Basic YWRtaW46YWRtaW4=" \
  -H "Content-Type: application/json" \
  -d '{
    "query": {
      "query": "cm:name:*contrato*"
    }
  }'

# Búsqueda por contenido (full-text)
curl "http://localhost:8082/alfresco/api/-default-/public/search/versions/1/search" \
  -H "Authorization: Basic YWRtaW46YWRtaW4=" \
  -H "Content-Type: application/json" \
  -d '{
    "query": {
      "query": "TEXT:\"acuerdo comercial\""
    }
  }'

# Búsqueda con filtros
curl "http://localhost:8082/alfresco/api/-default-/public/search/versions/1/search" \
  -H "Authorization: Basic YWRtaW46YWRtaW4=" \
  -H "Content-Type: application/json" \
  -d '{
    "query": {
      "query": "*",
      "filterQueries": [
        "cm:creator:admin",
        "cm:modified:[2024-01-01T00:00:00 TO NOW]"
      ]
    }
  }'
```

#### Búsqueda en Solr directamente
```bash
# Query directo a Solr
curl "http://localhost:8983/solr/alfresco/select?q=*:*&fq=TYPE:cm\\:content&rows=10"

# Búsqueda por tipo de archivo
curl "http://localhost:8983/solr/alfresco/select?q=content.mimetype:application/pdf"
```

### 3. **Gestión de Versiones**

#### Control de versiones de documentos
```bash
# Obtener versiones de un documento
curl "http://localhost:8082/alfresco/api/-default-/public/alfresco/versions/1/nodes/{node-id}/versions" \
  -H "Authorization: Basic YWRtaW46YWRtaW4="

# Crear nueva versión
curl -X PUT "http://localhost:8082/alfresco/api/-default-/public/alfresco/versions/1/nodes/{node-id}/content" \
  -H "Authorization: Basic YWRtaW46YWRtaW4=" \
  -F 'filedata=@documento-v2.pdf' \
  -F 'majorVersion=true' \
  -F 'comment=Segunda versión con correcciones'

# Revertir a versión anterior
curl -X POST "http://localhost:8082/alfresco/api/-default-/public/alfresco/versions/1/nodes/{node-id}/versions/{version-id}/revert" \
  -H "Authorization: Basic YWRtaW46YWRtaW4=" \
  -H "Content-Type: application/json" \
  -d '{"majorVersion": true, "comment": "Revertido a versión estable"}'
```

### 4. **Transformaciones de Documentos**

#### Convertir documentos
```bash
# Solicitar transformación PDF a imagen
curl -X POST "http://localhost:8091/transform" \
  -F 'file=@documento.pdf' \
  -F 'targetMimeType=image/png' \
  -F 'options={"page": "1"}'

# Obtener thumbnail
curl "http://localhost:8082/alfresco/api/-default-/public/alfresco/versions/1/nodes/{node-id}/renditions/doclib/content" \
  -H "Authorization: Basic YWRtaW46YWRtaW4=" \
  -o thumbnail.png

# Solicitar preview
curl "http://localhost:8082/alfresco/api/-default-/public/alfresco/versions/1/nodes/{node-id}/renditions/pdf/content" \
  -H "Authorization: Basic YWRtaW46YWRtaW4=" \
  -o preview.pdf
```

### 5. **Colaboración y Sitios**

#### Crear sitio colaborativo
```bash
# Crear sitio via API
curl -X POST "http://localhost:8082/alfresco/api/-default-/public/alfresco/versions/1/sites" \
  -H "Authorization: Basic YWRtaW46YWRtaW4=" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "proyecto-alpha",
    "title": "Proyecto Alpha",
    "description": "Sitio colaborativo para el proyecto Alpha",
    "visibility": "PRIVATE"
  }'

# Invitar usuarios al sitio
curl -X POST "http://localhost:8082/alfresco/api/-default-/public/alfresco/versions/1/sites/proyecto-alpha/members" \
  -H "Authorization: Basic YWRtaW46YWRtaW4=" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "usuario.colaborador",
    "role": "SiteCollaborator"
  }'
```

#### Workflow y aprobaciones
```bash
# Iniciar workflow de aprobación
curl -X POST "http://localhost:8082/alfresco/api/-default-/public/workflow/versions/1/processes" \
  -H "Authorization: Basic YWRtaW46YWRtaW4=" \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionId": "activiti$reviewpoolprocess:1:4",
    "variables": {
      "bpm_workflowDescription": "Revisión de documento",
      "bpm_assignees": ["admin"],
      "wf_notifyMe": true
    },
    "items": ["{node-id}"]
  }'
```

## 🔧 Comandos Útiles

### Gestión del Stack
```bash
# Iniciar content stack
./manage-stack.sh start content

# Ver logs específicos
docker logs alfresco-repository -f
docker logs alfresco-share -f
docker logs alfresco-solr -f

# Verificar estado de servicios
curl http://localhost:8082/alfresco/api/-default-/public/alfresco/versions/1/probes/-ready-
curl http://localhost:8083/share/page/
curl http://localhost:8983/solr/admin/cores?action=STATUS
```

### Administración de Alfresco
```bash
# Limpiar cache del repositorio
curl -X POST "http://localhost:8082/alfresco/service/cache/content/clear" \
  -H "Authorization: Basic YWRtaW46YWRtaW4="

# Reindexar Solr
curl "http://localhost:8983/solr/admin/cores?action=RELOAD&core=alfresco"

# Backup del repositorio
docker exec alfresco-repository \
  java -cp "webapps/alfresco/WEB-INF/lib/*" \
  org.alfresco.tools.Export \
  -user admin -pwd admin \
  -s /opt/alfresco/alf_data/contentstore \
  -d /backup/export-$(date +%Y%m%d).acp

# Estadísticas del sistema
curl "http://localhost:8082/alfresco/service/api/admin/restrictions" \
  -H "Authorization: Basic YWRtaW46YWRtaW4="
```

### Solr Administration
```bash
# Status de cores
curl "http://localhost:8983/solr/admin/cores?action=STATUS"

# Optimizar índices
curl "http://localhost:8983/solr/alfresco/update?optimize=true"

# Ver estadísticas de búsqueda
curl "http://localhost:8983/solr/alfresco/admin/luke"

# Monitoring de queries
curl "http://localhost:8983/solr/alfresco/admin/stats.jsp"
```

## 🔍 Troubleshooting

### Problemas Comunes

1. **Alfresco Repository no inicia**
   ```bash
   # Verificar memoria asignada
   docker stats alfresco-repository
   
   # Verificar conectividad a PostgreSQL
   docker exec alfresco-repository nc -zv alfresco-postgres 5432
   
   # Verificar logs
   docker logs alfresco-repository --tail 100
   ```

2. **Problemas de indexación en Solr**
   ```bash
   # Verificar conectividad Solr → Alfresco
   docker exec alfresco-solr curl -f http://alfresco-repository:8080/alfresco
   
   # Reindexar core
   curl "http://localhost:8983/solr/admin/cores?action=RELOAD&core=alfresco"
   
   # Ver logs de Solr
   docker logs alfresco-solr --tail 100
   ```

3. **Transformaciones fallan**
   ```bash
   # Verificar Transform Service
   curl http://localhost:8091/ready
   
   # Ver colas en ActiveMQ
   curl http://localhost:8161/admin/queues.jsp
   
   # Test manual de transformación
   curl -X POST http://localhost:8091/transform \
     -F 'file=@test.pdf' \
     -F 'targetMimeType=text/plain'
   ```

4. **Performance issues**
   ```bash
   # Verificar uso de memoria
   docker stats alfresco-repository alfresco-solr
   
   # Limpiar caches
   curl -X POST "http://localhost:8082/alfresco/service/cache/clear" \
     -H "Authorization: Basic YWRtaW46YWRtaW4="
   
   # Optimizar base de datos
   docker exec alfresco-postgres psql -U alfresco -d alfresco \
     -c "VACUUM ANALYZE;"
   ```

## 📊 Monitoreo y Métricas

### Repository Health Checks
```bash
# Repository readiness
curl http://localhost:8082/alfresco/api/-default-/public/alfresco/versions/1/probes/-ready-

# Repository liveness
curl http://localhost:8082/alfresco/api/-default-/public/alfresco/versions/1/probes/-live-

# System info
curl "http://localhost:8082/alfresco/service/api/admin/admin-systemsummary" \
  -H "Authorization: Basic YWRtaW46YWRtaW4="
```

### Performance Metrics
```bash
# JVM metrics
curl "http://localhost:8082/alfresco/service/api/admin/admin-jmx" \
  -H "Authorization: Basic YWRtaW46YWRtaW4="

# Database connections
curl "http://localhost:8082/alfresco/service/api/admin/admin-dbconnpoolstats" \
  -H "Authorization: Basic YWRtaW46YWRtaW4="

# Content store stats
curl "http://localhost:8082/alfresco/service/api/admin/admin-contentstores" \
  -H "Authorization: Basic YWRtaW46YWRtaW4="
```

### Solr Metrics
```bash
# Core statistics
curl "http://localhost:8983/solr/alfresco/admin/stats.jsp"

# Index size
curl "http://localhost:8983/solr/admin/cores?action=STATUS&core=alfresco&wt=json" | \
  jq '.status.alfresco.index'

# Query performance
curl "http://localhost:8983/solr/alfresco/admin/plugins?stats=true"
```

## 🔐 Seguridad y Mejores Prácticas

### Configuración de Seguridad
```bash
# Cambiar password de admin
curl -X PUT "http://localhost:8082/alfresco/api/-default-/public/alfresco/versions/1/people/admin" \
  -H "Authorization: Basic YWRtaW46YWRtaW4=" \
  -H "Content-Type: application/json" \
  -d '{
    "properties": {
      "cm:password": "NewSecurePassword123!"
    }
  }'

# Configurar permisos por defecto
curl -X PUT "http://localhost:8082/alfresco/api/-default-/public/alfresco/versions/1/nodes/{node-id}/permissions" \
  -H "Authorization: Basic YWRtaW46YWRtaW4=" \
  -H "Content-Type: application/json" \
  -d '{
    "permissions": {
      "locallySet": [{
        "authorityId": "GROUP_EVERYONE",
        "name": "Read",
        "accessStatus": "DENIED"
      }]
    }
  }'
```

### Backup Strategy
```bash
# Backup completo automático
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backup/alfresco_${DATE}"

# 1. Backup base de datos
docker exec alfresco-postgres pg_dump -U alfresco alfresco > ${BACKUP_DIR}/database.sql

# 2. Backup contentstore
docker cp alfresco-repository:/usr/local/tomcat/alf_data/contentstore ${BACKUP_DIR}/

# 3. Backup solr indexes
docker cp alfresco-solr:/opt/alfresco-search-services/data ${BACKUP_DIR}/solr-data

# 4. Comprimir
tar -czf ${BACKUP_DIR}.tar.gz ${BACKUP_DIR}/
```

## 🏗️ Arquitectura del Sistema

### Flujo de Documentos
```
Upload → Repository → Transform Service → Solr Indexing
   ↓         ↓             ↓              ↓
Content   Metadata    Renditions     Search Index
 Store   Database    (thumbnails)   (full-text)
```

### Integración con Otros Servicios
```bash
# Webhook para notificaciones
curl -X POST "http://localhost:8082/alfresco/service/api/admin/admin-webhooks" \
  -H "Authorization: Basic YWRtaW46YWRtaW4=" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "http://notification-service:8080/webhook",
    "events": ["repo.nodes.created", "repo.nodes.updated"]
  }'
```

## 🔗 Dependencias

### Prerequisitos:
- `docker-compose-base.yml` (redes)

### Integra con:
- `docker-compose-apps.yml` (integración con microservicios)
- `docker-compose-identity.yml` (SSO con Keycloak)
- `docker-compose-observability.yml` (métricas y logs)
- `docker-compose-workflow.yml` (procesos de negocio)

## 📚 Enlaces Útiles

- [Alfresco Documentation](https://docs.alfresco.com/)
- [Alfresco REST API](https://api-explorer.alfresco.com/)
- [Alfresco Content Services SDK](https://docs.alfresco.com/content-services/latest/develop/)
- [Apache Solr Documentation](https://solr.apache.org/guide/)
- [Alfresco Transform Service](https://docs.alfresco.com/transform-service/latest/)
- [ActiveMQ Documentation](https://activemq.apache.org/documentation.html)

## 🎯 Próximos Pasos

1. **Configurar SSL/TLS** para acceso seguro
2. **Integrar con LDAP** para autenticación
3. **Configurar clustering** para alta disponibilidad
4. **Implementar custom content models**
5. **Configurar Smart Folders** y reglas automáticas