# 📦 Stack de Registry y Artefactos - Harbor y Nexus

## 🎯 Descripción General

El **docker-compose-registry.yml** implementa una plataforma completa de gestión de artefactos usando Harbor como registry de containers y Nexus como repositorio universal de artefactos. Incluye scanning de vulnerabilidades, políticas de seguridad y dashboard de compliance.

## 🏗️ Servicios Incluidos

### 🐋 **Container Registry**
- **Harbor Core** - Servicio principal de registry
- **Harbor Registry** - Almacenamiento de imágenes Docker
- **Harbor JobService** - Procesamiento de tareas asíncronas
- **Harbor Database** - PostgreSQL para metadatos
- **Harbor Redis** - Cache y sesiones

### 📚 **Universal Repository**
- **Nexus Repository** - Repositorio universal de artefactos
- **Nexus Database** - PostgreSQL para metadatos de Nexus

### 📊 **Compliance Dashboard**
- **Compliance Dashboard** - Grafana especializado en compliance

## 🔌 Puertos Expuestos

| Servicio | Puerto | Protocolo | Descripción |
|----------|--------|-----------|-------------|
| **Harbor** | | | |
| Harbor Core | 8089 | HTTP | Interfaz web y API |
| **Nexus** | | | |
| Nexus Repository | 8081 | HTTP | Interfaz web y API |
| **Databases** | | | |
| Harbor Database | - | TCP | PostgreSQL interno |
| Nexus Database | - | TCP | PostgreSQL interno |
| **Compliance** | | | |
| Compliance Dashboard | 3002 | HTTP | Dashboard de compliance |

## 🔑 Accesos por Defecto

### Harbor
- **URL:** http://localhost:8089
- **Usuario:** admin
- **Password:** Harbor12345 (configurado en variables)
- **Docker Registry:** localhost:8089

### Nexus Repository
- **URL:** http://localhost:8081
- **Usuario inicial:** admin
- **Password:** Ver archivo admin.password en container
- **Maven Repository:** http://localhost:8081/repository/maven-public/

### Compliance Dashboard
- **URL:** http://localhost:3002
- **Usuario:** admin
- **Password:** configurado en variables

## ⚙️ Variables de Entorno Requeridas

```bash
# Harbor Configuration
HARBOR_DB_NAME=harbor
HARBOR_DB_USER=harbor
HARBOR_DB_PASSWORD=harbor123
HARBOR_CORE_SECRET=harbor-core-secret-change-in-production
HARBOR_JOBSERVICE_SECRET=harbor-jobservice-secret-change-in-production
HARBOR_ADMIN_PASSWORD=Harbor12345


# Nexus Configuration
POSTGRES_NEXUS_PASSWORD=nexus123
NEXUS_DATA_DIR=/nexus-data

# Compliance Dashboard
COMPLIANCE_DASHBOARD_PASSWORD=compliance123

# Resource Limits
CPU_LIMIT_LARGE=2.0
CPU_LIMIT_MEDIUM=1.0
MEMORY_LIMIT_XLARGE=4g
MEMORY_LIMIT_LARGE=2g
MEMORY_LIMIT_MEDIUM=1g
```

## 馃殌 Casos de Uso

### 1. **Gesti贸n de Im谩genes Docker con Harbor**

#### Configurar Docker para usar Harbor
```bash
# Configurar insecure registry (solo para desarrollo)
# En /etc/docker/daemon.json:
{
  "insecure-registries": ["localhost:8089"]
}

# Restart Docker daemon
sudo systemctl restart docker

# Login a Harbor
docker login localhost:8089
# Username: admin
# Password: Harbor12345

# Crear proyecto en Harbor
curl -X POST "http://localhost:8089/api/v2.0/projects" \
  -H "Authorization: Basic YWRtaW46SGFyYm9yMTIzNDU=" \
  -H "Content-Type: application/json" \
  -d '{
    "project_name": "empresa-microservicios",
    "metadata": {
      "public": "false",
      "enable_content_trust": "true",
      "auto_scan": "true"
    }
  }'
```

#### Push y pull de im谩genes
```bash
# Tag imagen para Harbor
docker tag microservicio-productos:latest localhost:8089/empresa-microservicios/microservicio-productos:v1.0.0
docker tag microservicio-productos:latest localhost:8089/empresa-microservicios/microservicio-productos:latest

# Push imagen
docker push localhost:8089/empresa-microservicios/microservicio-productos:v1.0.0
docker push localhost:8089/empresa-microservicios/microservicio-productos:latest

# Pull imagen
docker pull localhost:8089/empresa-microservicios/microservicio-productos:v1.0.0

# Listar repositorios
curl "http://localhost:8089/api/v2.0/projects/empresa-microservicios/repositories" \
  -H "Authorization: Basic YWRtaW46SGFyYm9yMTIzNDU="
```

#### Configurar scanning autom谩tico
```bash
# Habilitar auto-scan en proyecto
curl -X PUT "http://localhost:8089/api/v2.0/projects/1" \
  -H "Authorization: Basic YWRtaW46SGFyYm9yMTIzNDU=" \
  -H "Content-Type: application/json" \
  -d '{
    "metadata": {
      "auto_scan": "true",
      "severity": "medium",
      "reuse_sys_cve_allowlist": "true"
    }
  }'

# Ejecutar scan manual
curl -X POST "http://localhost:8089/api/v2.0/projects/empresa-microservicios/repositories/microservicio-productos/artifacts/v1.0.0/scan" \
  -H "Authorization: Basic YWRtaW46SGFyYm9yMTIzNDU="

# Ver resultados de scan
curl "http://localhost:8089/api/v2.0/projects/empresa-microservicios/repositories/microservicio-productos/artifacts/v1.0.0/additions/vulnerabilities" \
  -H "Authorization: Basic YWRtaW46SGFyYm9yMTIzNDU="
```

### 2. **Gesti贸n de Artefactos Maven con Nexus**

#### Configuraci贸n inicial de repositorios
```bash
# Obtener password inicial de admin
docker exec nexus cat /nexus-data/admin.password

# Login via API
curl -X POST "http://localhost:8081/service/rest/v1/security/users/admin/change-password" \
  -H "Content-Type: text/plain" \
  -u "admin:INITIAL_PASSWORD" \
  -d "newAdminPassword123"

# Crear repositorio Maven hosted
curl -X POST "http://localhost:8081/service/rest/v1/repositories/maven/hosted" \
  -H "Content-Type: application/json" \
  -u "admin:newAdminPassword123" \
  -d '{
    "name": "empresa-maven-releases",
    "online": true,
    "storage": {
      "blobStoreName": "default",
      "strictContentTypeValidation": true
    },
    "maven": {
      "versionPolicy": "RELEASE",
      "layoutPolicy": "STRICT"
    }
  }'

# Crear repositorio Maven snapshots
curl -X POST "http://localhost:8081/service/rest/v1/repositories/maven/hosted" \
  -H "Content-Type: application/json" \
  -u "admin:newAdminPassword123" \
  -d '{
    "name": "empresa-maven-snapshots",
    "online": true,
    "storage": {
      "blobStoreName": "default",
      "strictContentTypeValidation": true
    },
    "maven": {
      "versionPolicy": "SNAPSHOT",
      "layoutPolicy": "STRICT"
    }
  }'
```

#### Configurar Maven para usar Nexus
```xml
<!-- settings.xml -->
<settings>
  <servers>
    <server>
      <id>empresa-nexus</id>
      <username>admin</username>
      <password>newAdminPassword123</password>
    </server>
  </servers>
  
  <mirrors>
    <mirror>
      <id>empresa-nexus</id>
      <mirrorOf>central</mirrorOf>
      <url>http://localhost:8081/repository/maven-public/</url>
    </mirror>
  </mirrors>
  
  <profiles>
    <profile>
      <id>empresa-nexus</id>
      <repositories>
        <repository>
          <id>central</id>
          <url>http://localhost:8081/repository/maven-public/</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>true</enabled></snapshots>
        </repository>
      </repositories>
      <pluginRepositories>
        <pluginRepository>
          <id>central</id>
          <url>http://localhost:8081/repository/maven-public/</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>true</enabled></snapshots>
        </pluginRepository>
      </pluginRepositories>
    </profile>
  </profiles>
  
  <activeProfiles>
    <activeProfile>empresa-nexus</activeProfile>
  </activeProfiles>
</settings>
```

#### Deploy de artefactos Maven
```xml
<!-- pom.xml -->
<distributionManagement>
  <repository>
    <id>empresa-nexus</id>
    <name>Empresa Releases</name>
    <url>http://localhost:8081/repository/empresa-maven-releases/</url>
  </repository>
  <snapshotRepository>
    <id>empresa-nexus</id>
    <name>Empresa Snapshots</name>
    <url>http://localhost:8081/repository/empresa-maven-snapshots/</url>
  </snapshotRepository>
</distributionManagement>
```

```bash
# Deploy artefacto
mvn clean deploy

# Upload manual de artefacto
curl -X POST "http://localhost:8081/service/rest/v1/components?repository=empresa-maven-releases" \
  -H "Content-Type: multipart/form-data" \
  -u "admin:newAdminPassword123" \
  -F "maven2.groupId=com.empresa" \
  -F "maven2.artifactId=microservicio-common" \
  -F "maven2.version=1.0.0" \
  -F "maven2.asset1=@target/microservicio-common-1.0.0.jar" \
  -F "maven2.asset1.extension=jar"
```

### 3. **Repositorios para NPM y Docker**

#### Configurar repositorio NPM
```bash
# Crear repositorio NPM hosted
curl -X POST "http://localhost:8081/service/rest/v1/repositories/npm/hosted" \
  -H "Content-Type: application/json" \
  -u "admin:newAdminPassword123" \
  -d '{
    "name": "empresa-npm-internal",
    "online": true,
    "storage": {
      "blobStoreName": "default",
      "strictContentTypeValidation": true
    }
  }'

# Configurar npm para usar Nexus
npm config set registry http://localhost:8081/repository/npm-public/

# Autenticaci贸n NPM
npm login --registry=http://localhost:8081/repository/empresa-npm-internal/

# Publish package
npm publish --registry=http://localhost:8081/repository/empresa-npm-internal/
```

#### Configurar Docker Registry en Nexus
```bash
# Crear Docker hosted repository
curl -X POST "http://localhost:8081/service/rest/v1/repositories/docker/hosted" \
  -H "Content-Type: application/json" \
  -u "admin:newAdminPassword123" \
  -d '{
    "name": "empresa-docker-internal",
    "online": true,
    "storage": {
      "blobStoreName": "default",
      "strictContentTypeValidation": true
    },
    "docker": {
      "v1Enabled": false,
      "forceBasicAuth": true,
      "httpPort": 8082
    }
  }'

# Configurar Docker para usar Nexus
# En /etc/docker/daemon.json:
{
  "insecure-registries": ["localhost:8082"]
}

# Login y push
docker login localhost:8082
docker tag mi-imagen:latest localhost:8082/mi-imagen:v1.0.0
docker push localhost:8082/mi-imagen:v1.0.0
```

### 4. **Pol铆ticas de Seguridad y Compliance**

#### Configurar policies en Harbor
```bash
# Crear policy de retenci贸n
curl -X POST "http://localhost:8089/api/v2.0/projects/empresa-microservicios/retentions" \
  -H "Authorization: Basic YWRtaW46SGFyYm9yMTIzNDU=" \
  -H "Content-Type: application/json" \
  -d '{
    "algorithm": "or",
    "rules": [
      {
        "disabled": false,
        "action": "retain",
        "template": "recentXDays",
        "params": {
          "recentXDays": 30
        },
        "tag_selectors": [
          {
            "kind": "doublestar",
            "decoration": "matches",
            "pattern": "latest"
          }
        ],
        "scope_selectors": {
          "repository": [
            {
              "kind": "doublestar",
              "decoration": "repoMatches",
              "pattern": "**"
            }
          ]
        }
      }
    ],
    "trigger": {
      "kind": "Schedule",
      "settings": {
        "cron": "0 2 * * *"
      }
    },
    "scope": {
      "level": "project",
      "ref": 1
    }
  }'

# Configurar webhook para notificaciones
curl -X POST "http://localhost:8089/api/v2.0/projects/empresa-microservicios/webhook/policies" \
  -H "Authorization: Basic YWRtaW46SGFyYm9yMTIzNDU=" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "vulnerability-alert",
    "description": "Notificar vulnerabilidades cr铆ticas",
    "project_id": 1,
    "targets": [
      {
        "type": "slack",
        "address": "https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK",
        "skip_cert_verify": true
      }
    ],
    "event_types": [
      "SCANNING_COMPLETED"
    ],
    "enabled": true
  }'
```

#### Configurar cleanup policies en Nexus
```bash
# Crear cleanup policy
curl -X POST "http://localhost:8081/service/rest/v1/cleanup-policies" \
  -H "Content-Type: application/json" \
  -u "admin:newAdminPassword123" \
  -d '{
    "name": "cleanup-old-snapshots",
    "notes": "Eliminar snapshots antiguos",
    "format": "maven2",
    "criteria": {
      "lastBlobUpdated": 30,
      "isPrerelease": true
    }
  }'

# Aplicar policy a repositorio
curl -X PUT "http://localhost:8081/service/rest/v1/repositories/maven/hosted/empresa-maven-snapshots" \
  -H "Content-Type: application/json" \
  -u "admin:newAdminPassword123" \
  -d '{
    "name": "empresa-maven-snapshots",
    "online": true,
    "storage": {
      "blobStoreName": "default",
      "strictContentTypeValidation": true
    },
    "cleanup": {
      "policyNames": ["cleanup-old-snapshots"]
    },
    "maven": {
      "versionPolicy": "SNAPSHOT",
      "layoutPolicy": "STRICT"
    }
  }'
```

### 5. **Integraci贸n con CI/CD**

#### Jenkins pipeline con Harbor y Nexus
```groovy
// Jenkinsfile con registry integration
pipeline {
    agent any
    
    environment {
        HARBOR_REGISTRY = 'localhost:8089'
        HARBOR_PROJECT = 'empresa-microservicios'
        NEXUS_URL = 'http://localhost:8081'
        HARBOR_CREDENTIALS = 'harbor-credentials'
        NEXUS_CREDENTIALS = 'nexus-credentials'
    }
    
    stages {
        stage('Build & Test') {
            steps {
                sh '''
                    # Configurar Maven para usar Nexus
                    cp /var/jenkins_home/settings.xml ~/.m2/settings.xml
                    
                    # Build y test
                    ./mvnw clean test package
                '''
            }
        }
        
        stage('Publish Artifacts') {
            parallel {
                stage('Maven Artifacts') {
                    steps {
                        withCredentials([usernamePassword(
                            credentialsId: "${NEXUS_CREDENTIALS}",
                            usernameVariable: 'NEXUS_USER',
                            passwordVariable: 'NEXUS_PASSWORD'
                        )]) {
                            sh '''
                                # Deploy a Nexus
                                ./mvnw deploy \
                                  -DaltDeploymentRepository=nexus::default::${NEXUS_URL}/repository/empresa-maven-releases/ \
                                  -Dusername=${NEXUS_USER} \
                                  -Dpassword=${NEXUS_PASSWORD}
                            '''
                        }
                    }
                }
                
                stage('Docker Image') {
                    steps {
                        script {
                            def imageName = "${HARBOR_REGISTRY}/${HARBOR_PROJECT}/${env.JOB_NAME}:${env.BUILD_NUMBER}"
                            def latestImage = "${HARBOR_REGISTRY}/${HARBOR_PROJECT}/${env.JOB_NAME}:latest"
                            
                            // Build imagen
                            sh "docker build -t ${imageName} -t ${latestImage} ."
                            
                            // Push a Harbor
                            withCredentials([usernamePassword(
                                credentialsId: "${HARBOR_CREDENTIALS}",
                                usernameVariable: 'HARBOR_USER',
                                passwordVariable: 'HARBOR_PASSWORD'
                            )]) {
                                sh '''
                                    echo ${HARBOR_PASSWORD} | docker login ${HARBOR_REGISTRY} -u ${HARBOR_USER} --password-stdin
                                    docker push ${imageName}
                                    docker push ${latestImage}
                                '''
                            }
                        }
                    }
                }
            }
        }
        
        stage('Security Scan') {
            steps {
                script {
                    // Trigger scan en Harbor
                    def imageName = "${env.JOB_NAME}"
                    def imageTag = "${env.BUILD_NUMBER}"
                    
                    withCredentials([usernamePassword(
                        credentialsId: "${HARBOR_CREDENTIALS}",
                        usernameVariable: 'HARBOR_USER',
                        passwordVariable: 'HARBOR_PASSWORD'
                    )]) {
                        sh '''
                            # Trigger scan
                            curl -X POST "${HARBOR_REGISTRY}/api/v2.0/projects/${HARBOR_PROJECT}/repositories/${imageName}/artifacts/${imageTag}/scan" \
                              -u "${HARBOR_USER}:${HARBOR_PASSWORD}"
                            
                            # Esperar resultados
                            sleep 30
                            
                            # Obtener resultados
                            curl "${HARBOR_REGISTRY}/api/v2.0/projects/${HARBOR_PROJECT}/repositories/${imageName}/artifacts/${imageTag}/additions/vulnerabilities" \
                              -u "${HARBOR_USER}:${HARBOR_PASSWORD}" \
                              -o vulnerability-report.json
                        '''
                    }
                    
                    // Parse resultados y fallar si hay vulnerabilidades cr铆ticas
                    def vulnerabilities = readJSON file: 'vulnerability-report.json'
                    def criticalCount = vulnerabilities.findAll { it.severity == 'Critical' }.size()
                    
                    if (criticalCount > 0) {
                        error("Found ${criticalCount} critical vulnerabilities!")
                    }
                }
            }
        }
    }
    
    post {
        always {
            // Limpiar im谩genes locales
            sh '''
                docker image prune -f
                docker system prune -f --volumes
            '''
        }
    }
}
```

## 馃敡 Comandos 脷tiles

### Gesti贸n del Stack
```bash
# Iniciar registry stack
./manage-stack.sh start registry

# Ver logs de Harbor
docker logs harbor-core -f
docker logs harbor-registry -f

# Ver logs de Nexus
docker logs nexus -f

# Restart servicios
docker restart harbor-core
docker restart nexus
```

### Administraci贸n de Harbor
```bash
# Health check
curl "http://localhost:8089/api/v2.0/health"

# Listar proyectos
curl "http://localhost:8089/api/v2.0/projects" \
  -H "Authorization: Basic YWRtaW46SGFyYm9yMTIzNDU="

# Estad铆sticas del sistema
curl "http://localhost:8089/api/v2.0/statistics" \
  -H "Authorization: Basic YWRtaW46SGFyYm9yMTIzNDU="

# Configurar garbage collection
curl -X POST "http://localhost:8089/api/v2.0/system/gc/schedule" \
  -H "Authorization: Basic YWRtaW46SGFyYm9yMTIzNDU=" \
  -H "Content-Type: application/json" \
  -d '{
    "schedule": {
      "type": "Weekly",
      "weekday": 1,
      "offtime": 10800
    }
  }'
```

### Administraci贸n de Nexus
```bash
# Health check
curl "http://localhost:8081/service/rest/v1/status"

# Listar repositorios
curl "http://localhost:8081/service/rest/v1/repositories" \
  -u "admin:newAdminPassword123"

# Ver estad铆sticas de storage
curl "http://localhost:8081/service/rest/v1/status/writable" \
  -u "admin:newAdminPassword123"

# Ejecutar cleanup task
curl -X POST "http://localhost:8081/service/rest/v1/tasks/run" \
  -H "Content-Type: application/json" \
  -u "admin:newAdminPassword123" \
  -d '{"name": "cleanup-old-snapshots"}'

# Backup de configuraci贸n
curl "http://localhost:8081/service/rest/v1/script" \
  -u "admin:newAdminPassword123" > nexus-config-backup.json
```

### Docker Registry Operations
```bash
# Listar im谩genes en Harbor
curl "http://localhost:8089/api/v2.0/projects/empresa-microservicios/repositories" \
  -H "Authorization: Basic YWRtaW46SGFyYm9yMTIzNDU="

# Ver tags de una imagen
curl "http://localhost:8089/api/v2.0/projects/empresa-microservicios/repositories/microservicio-productos/artifacts" \
  -H "Authorization: Basic YWRtaW46SGFyYm9yMTIzNDU="

# Eliminar imagen espec铆fica
curl -X DELETE "http://localhost:8089/api/v2.0/projects/empresa-microservicios/repositories/microservicio-productos/artifacts/v1.0.0" \
  -H "Authorization: Basic YWRtaW46SGFyYm9yMTIzNDU="

# Verificar digest de imagen
docker manifest inspect localhost:8089/empresa-microservicios/microservicio-productos:latest
```

## 馃攳 Troubleshooting

### Problemas Comunes

1. **Harbor no inicia correctamente**
   ```bash
   # Verificar todos los servicios de Harbor
   docker ps | grep harbor
   
   # Ver logs de core service
   docker logs harbor-core --tail 100
   
   # Verificar conectividad a base de datos
   docker exec harbor-core nc -zv harbor-db 5432
   
   # Reiniciar todos los servicios Harbor
   docker restart harbor-db harbor-redis harbor-core harbor-registry harbor-jobservice
   ```

2. **Nexus out of memory**
   ```bash
   # Ver uso de memoria
   docker stats nexus
   
   # Aumentar heap size
   # En docker-compose: JAVA_MAX_MEM=4g
   
   # Ver logs de memoria
   docker logs nexus | grep -i "outofmemory\|heap"
   
   # Limpiar cache si es necesario
   curl -X POST "http://localhost:8081/service/rest/v1/tasks/run" \
     -u "admin:password" \
     -d '{"name": "rebuild-maven-metadata"}'
   ```

3. **Docker push/pull failures**
   ```bash
   # Verificar insecure registries
   cat /etc/docker/daemon.json
   
   # Test conectividad
   curl -v http://localhost:8089/v2/
   
   # Verificar autenticaci贸n
   docker login localhost:8089 -u admin
   
   # Ver logs detallados
   docker push localhost:8089/test:latest --debug
   ```

4. **Disk space issues**
   ```bash
   # Ver uso de almacenamiento
   docker exec harbor-core df -h
   docker exec nexus df -h
   
   # Ejecutar garbage collection en Harbor
   curl -X POST "http://localhost:8089/api/v2.0/system/gc" \
     -H "Authorization: Basic YWRtaW46SGFyYm9yMTIzNDU="
   
   # Cleanup en Nexus
   curl -X POST "http://localhost:8081/service/rest/v1/tasks/run" \
     -u "admin:password" \
     -d '{"name": "repository.docker.gc"}'
   ```

## 馃搳 Monitoreo y M茅tricas

### Harbor Metrics
```bash
# Estad铆sticas generales
curl "http://localhost:8089/api/v2.0/statistics" \
  -H "Authorization: Basic YWRtaW46SGFyYm9yMTIzNDU="

# M茅tricas de proyectos
curl "http://localhost:8089/api/v2.0/projects?page_size=100" \
  -H "Authorization: Basic YWRtaW46SGFyYm9yMTIzNDU=" | \
  jq '.[] | {name: .name, repo_count: .repo_count, chart_count: .chart_count}'

# Top im谩genes m谩s descargadas
curl "http://localhost:8089/api/v2.0/projects/empresa-microservicios/repositories?page_size=50" \
  -H "Authorization: Basic YWRtaW46SGFyYm9yMTIzNDU=" | \
  jq 'sort_by(.pull_count) | reverse | .[:10]'
```

### Nexus Metrics
```bash
# M茅tricas de repositorios
curl "http://localhost:8081/service/rest/v1/repositories" \
  -u "admin:password" | \
  jq '.[] | {name: .name, format: .format, type: .type, online: .online}'

# Estad铆sticas de storage
curl "http://localhost:8081/service/rest/v1/read-only" \
  -u "admin:password"

# Tasks status
curl "http://localhost:8081/service/rest/v1/tasks" \
  -u "admin:password" | \
  jq '.items[] | {name: .name, type: .type, status: .status}'
```

### Compliance Dashboard
```json
{
  "registry_dashboard": {
    "panels": [
      {
        "title": "Vulnerability Summary",
        "query": "harbor_vulnerabilities_total by severity"
      },
      {
        "title": "Image Pull Rate",
        "query": "rate(harbor_image_pulls_total[5m])"
      },
      {
        "title": "Storage Usage",
        "query": "harbor_storage_used_bytes / harbor_storage_total_bytes"
      },
      {
        "title": "Failed Scans",
        "query": "rate(harbor_scan_failures_total[1h])"
      }
    ]
  }
}
```

## 馃攼 Seguridad y Mejores Pr谩cticas

### Harbor Security Hardening
```bash
# Configurar HTTPS (producci贸n)
# Generar certificados SSL
openssl req -newkey rsa:4096 -nodes -sha256 -keyout harbor.key -x509 -days 365 -out harbor.crt

# Configurar RBAC granular
curl -X POST "http://localhost:8089/api/v2.0/projects/empresa-microservicios/members" \
  -H "Authorization: Basic YWRtaW46SGFyYm9yMTIzNDU=" \
  -H "Content-Type: application/json" \
  -d '{
    "role_id": 2,
    "member_user": {
      "username": "developer1"
    }
  }'

# Configurar content trust
curl -X PUT "http://localhost:8089/api/v2.0/projects/empresa-microservicios" \
  -H "Authorization: Basic YWRtaW46SGFyYm9yMTIzNDU=" \
  -H "Content-Type: application/json" \
  -d '{
    "metadata": {
      "enable_content_trust": "true",
      "enable_content_trust_cosign": "true"
    }
  }'
```

### Nexus Security Configuration
```bash
# Configurar LDAP authentication
curl -X POST "http://localhost:8081/service/rest/v1/security/ldap" \
  -H "Content-Type: application/json" \
  -u "admin:password" \
  -d '{
    "name": "company-ldap",
    "protocol": "ldap",
    "host": "ldap.empresa.com",
    "port": 389,
    "searchBase": "dc=empresa,dc=com",
    "authScheme": "simple",
    "authUsername": "cn=nexus,ou=services,dc=empresa,dc=com"
  }'

# Configurar repository firewall
curl -X POST "http://localhost:8081/service/rest/v1/security/content-selectors" \
  -H "Content-Type: application/json" \
  -u "admin:password" \
  -d '{
    "name": "allow-empresa-packages",
    "description": "Solo permitir packages de la empresa",
    "expression": "coordinate.groupId =~ \"^com\\.empresa\\..*\""
  }'
```

### Backup y Disaster Recovery
```bash
#!/bin/bash
# Script de backup completo

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backup/registry_${DATE}"

# 1. Backup Harbor database
docker exec harbor-db pg_dump -U harbor harbor > ${BACKUP_DIR}/harbor-db.sql

# 2. Backup Harbor data
docker cp harbor-registry:/storage ${BACKUP_DIR}/harbor-storage

# 3. Backup Nexus data
docker cp nexus:/nexus-data ${BACKUP_DIR}/nexus-data

# 4. Backup configuration
curl "http://localhost:8089/api/v2.0/configurations" \
  -H "Authorization: Basic YWRtaW46SGFyYm9yMTIzNDU=" > ${BACKUP_DIR}/harbor-config.json

curl "http://localhost:8081/service/rest/v1/script" \
  -u "admin:password" > ${BACKUP_DIR}/nexus-config.json

# 5. Comprimir
tar -czf ${BACKUP_DIR}.tar.gz ${BACKUP_DIR}/

echo "Backup completed: ${BACKUP_DIR}.tar.gz"
```

## 馃敆 Dependencias

### Prerequisitos:
- `docker-compose-base.yml` (redes y PostgreSQL)

### Integra con:
- `docker-compose-cicd.yml` (push/pull en pipelines)
- `docker-compose-security.yml` (scanning de vulnerabilidades)
- `docker-compose-observability.yml` (m茅tricas de registry)
- `docker-compose-apps.yml` (deployment de im谩genes)

## 馃摎 Enlaces 脷tiles

- [Harbor Documentation](https://goharbor.io/docs/)
- [Harbor REST API](https://goharbor.io/docs/latest/build-customize-contribute/configure-swagger/)
- [Nexus Repository Documentation](https://help.sonatype.com/repomanager3)
- [Nexus REST API](https://help.sonatype.com/repomanager3/integrations/rest-and-integration-api)
- [Docker Registry API](https://docs.docker.com/registry/spec/api/)
- [Maven Deploy Plugin](https://maven.apache.org/plugins/maven-deploy-plugin/)

## 馃幆 Pr贸ximos Pasos

1. **Configurar HTTPS** con certificados v谩lidos
2. **Implementar backup autom谩tico** de artefactos
3.# 馃摝 Stack de Registry y Artefactos - Harbor y Nexus