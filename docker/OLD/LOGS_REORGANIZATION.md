# 📁 Reorganización de Logs - Estructura Modular

## ✅ Cambios Realizados

Se ha reorganizado la estructura de logs para agrupar por solución/módulo, facilitando la administración y el acceso a los logs de cada componente.

## 🏗️ Nueva Estructura de Logs

```
logs/
├── alfresco/                    # Gestión Documental
│   ├── postgres/               # Base de datos Alfresco
│   ├── activemq/               # Mensajería
│   ├── sfs/                    # Shared File Store
│   ├── transform/              # Servicio de transformación
│   ├── solr/                   # Motor de búsqueda
│   ├── repo/                   # Repositorio principal
│   ├── share/                  # Interfaz web tradicional
│   └── content-app/            # Interfaz moderna
├── datahub/                    # Gobernanza de Datos
│   ├── postgres/               # Base de datos DataHub
│   ├── backend/                # Backend de DataHub
│   └── frontend/               # Frontend de DataHub
├── airflow/                    # Orquestación de Workflows
│   ├── postgres/               # Base de datos Airflow
│   ├── webserver/              # Servidor web Airflow
│   └── scheduler/              # Programador de tareas
├── gorules/                    # Reglas de Negocio
│   ├── editor/                 # Editor de reglas
│   └── brms/                   # Motor de reglas
├── keycloak/                   # Gestión de Identidad
│   ├── db/                     # Base de datos Keycloak
│   └── app/                    # Aplicación Keycloak
├── nifi/                       # Plataforma de Datos
│   ├── registry/               # Registro de flujos
│   └── app/                    # Aplicación NiFi
├── sonarqube/                  # Análisis de código
├── owasp-zap/                  # Testing de seguridad
├── falco/                      # Runtime security
├── trivy/                      # Análisis de vulnerabilidades
├── nginx-edge/                 # Proxy reverso
├── postgres/                   # Base de datos principal
├── mongodb/                    # Base de datos NoSQL
├── kafka/                      # Mensajería
├── zookeeper/                  # Coordinación
├── kong/                       # API Gateway
├── otel-collector/             # Telemetría
├── command-service/            # Microservicio de comandos
├── query-service/              # Microservicio de consultas
└── frontend/                   # Aplicación web
```

## 🔧 Cambios en docker-compose.yml

### **Keycloak**
- `./logs/keycloak-db` → `./logs/keycloak/db`
- `./logs/keycloak` → `./logs/keycloak/app`

### **Alfresco**
- `./logs/alfresco-postgres` → `./logs/alfresco/postgres`
- `./logs/alfresco-activemq` → `./logs/alfresco/activemq`
- `./logs/alfresco-sfs` → `./logs/alfresco/sfs`
- `./logs/alfresco-transform` → `./logs/alfresco/transform`
- `./logs/alfresco-solr` → `./logs/alfresco/solr`
- `./logs/alfresco-repo` → `./logs/alfresco/repo`
- `./logs/alfresco-share` → `./logs/alfresco/share`
- `./logs/alfresco-content-app` → `./logs/alfresco/content-app`

### **DataHub**
- `./logs/datahub-postgres` → `./logs/datahub/postgres`
- `./logs/datahub-backend` → `./logs/datahub/backend`
- `./logs/datahub-frontend` → `./logs/datahub/frontend`

### **Airflow**
- `./logs/airflow-postgres` → `./logs/airflow/postgres`
- `./logs/airflow` → `./logs/airflow/webserver`

### **GoRules**
- `./logs/gorules-editor` → `./logs/gorules/editor`
- `./logs/gorules-brms` → `./logs/gorules/brms`

### **NiFi**
- `./logs/nifi-registry` → `./logs/nifi/registry`
- `./logs/nifi` → `./logs/nifi/app`

## 🎯 Beneficios de la Reorganización

1. **Mejor Organización**: Logs agrupados por solución/módulo
2. **Facilidad de Administración**: Acceso más intuitivo a los logs
3. **Escalabilidad**: Estructura preparada para nuevos módulos
4. **Mantenimiento**: Más fácil localizar y gestionar logs específicos
5. **Backup Selectivo**: Posibilidad de hacer backup por módulo

## 📊 Servicios por Módulo

### **Alfresco (8 servicios)**
- postgres, activemq, sfs, transform, solr, repo, share, content-app

### **DataHub (3 servicios)**
- postgres, backend, frontend

### **Airflow (3 servicios)**
- postgres, webserver, scheduler

### **GoRules (2 servicios)**
- editor, brms

### **Keycloak (2 servicios)**
- db, app

### **NiFi (2 servicios)**
- registry, app

### **Servicios Independientes (11 servicios)**
- sonarqube, owasp-zap, falco, trivy, nginx-edge, postgres, mongodb, kafka, zookeeper, kong, otel-collector, command-service, query-service, frontend

## 🚀 Próximos Pasos

1. **Verificar funcionamiento**: Probar que todos los servicios escriban logs correctamente
2. **Configurar rotación**: Implementar rotación de logs por módulo
3. **Backup automático**: Configurar backup automático de logs por módulo
4. **Monitoreo**: Implementar alertas de logs por módulo
5. **Documentación**: Crear guías de troubleshooting por módulo 