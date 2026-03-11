# 🔒 Stack de Seguridad - SAST/DAST/IAST/Policy

## 🎯 Descripción General

El **docker-compose-security.yml** implementa un stack completo de herramientas de seguridad que cubre análisis estático (SAST), dinámico (DAST), interactivo (IAST) y políticas como código. Ideal para DevSecOps y cumplimiento de estándares de seguridad.

## 🛡️ Servicios Incluidos

### 🔍 **SAST - Static Application Security Testing**
- **SonarQube Community** - Análisis de calidad y seguridad de código
- **Snyk Free** - Análisis de vulnerabilidades en dependencias

### 🕷️ **DAST - Dynamic Application Security Testing**
- **OWASP ZAP** - Proxy de interceptación y análisis dinámico

### ⚡ **IAST/Runtime Security**
- **Falco** - Detección de amenazas en runtime
- **Trivy** - Análisis de vulnerabilidades en containers

### 📋 **Policy as Code**
- **Open Policy Agent (OPA)** - Motor de políticas

### 📊 **Security Dashboard**
- **Grafana Security** - Dashboard centralizado de métricas de seguridad

## 🔌 Puertos Expuestos

| Servicio | Puerto | Protocolo | Descripción |
|----------|--------|-----------|-------------|
| SonarQube | 9002 | HTTP | Interfaz web |
| SonarQube DB | 5435 | TCP | Base de datos PostgreSQL |
| OWASP ZAP | 8092 | HTTP | Proxy/API |
| OWASP ZAP | 8093 | HTTP | Interfaz web |
| Trivy | 8094 | HTTP | API Server |
| Security Dashboard | 3001 | HTTP | Grafana dashboard |
| Open Policy Agent | 8181 | HTTP | API de políticas |

## 🔑 Accesos por Defecto

### SonarQube
- **URL:** http://localhost:9002
- **Usuario:** admin
- **Password:** admin
- **API:** http://localhost:9002/api/system/status

### OWASP ZAP
- **Proxy:** http://localhost:8092
- **Web UI:** http://localhost:8093
- **API Key:** zap-api-key-12345

### Trivy Server
- **API:** http://localhost:8094
- **Health:** http://localhost:8094/healthz

### Security Dashboard
- **URL:** http://localhost:3001
- **Usuario:** admin
- **Password:** admin123

### Open Policy Agent
- **API:** http://localhost:8181
- **Health:** http://localhost:8181/health

## ⚙️ Variables de Entorno Requeridas

```bash
# SonarQube
SONARQUBE_DB_NAME=sonarqube
SONARQUBE_DB_USER=sonarqube
SONARQUBE_DB_PASSWORD=sonarqube123

# Snyk Free (REQUERIDO)
SNYK_FREE_TOKEN=tu-token-snyk-aqui
SNYK_ORG_ID=tu-organization-id

# OWASP ZAP
ZAP_API_KEY=zap-api-key-12345

# Falco
FALCO_GRPC_ENABLED=true
FALCO_GRPC_BIND_ADDRESS=0.0.0.0:5060
```

## 🚀 Casos de Uso

### 1. **Análisis Estático de Código (SAST)**

#### SonarQube - Análisis Local
```bash
# Analizar proyecto Java/Spring Boot
sonar-scanner \
  -Dsonar.projectKey=mi-proyecto \
  -Dsonar.sources=./src \
  -Dsonar.host.url=http://localhost:9002 \
  -Dsonar.login=admin \
  -Dsonar.password=admin

# Analizar proyecto JavaScript/Node.js
sonar-scanner \
  -Dsonar.projectKey=mi-frontend \
  -Dsonar.sources=./src \
  -Dsonar.exclusions=**/node_modules/**
```

#### Snyk - Análisis de Dependencias
```bash
# Los reportes se generan automáticamente en:
ls ./security/snyk/

# Analizar manualmente un proyecto específico
docker exec snyk-cli snyk test /app --json
```

### 2. **Análisis Dinámico (DAST)**

#### OWASP ZAP - Escaneo de Aplicaciones Web
```bash
# Escaneo rápido de una URL
curl "http://localhost:8092/JSON/ascan/action/scan/" \
  -d "url=http://localhost:4200&recurse=true&apikey=zap-api-key-12345"

# Spider de una aplicación
curl "http://localhost:8092/JSON/spider/action/scan/" \
  -d "url=http://localhost:4200&apikey=zap-api-key-12345"

# Obtener alertas de seguridad
curl "http://localhost:8092/JSON/core/view/alerts/" \
  -d "apikey=zap-api-key-12345"
```

### 3. **Análisis de Containers (Trivy)**
```bash
# Escanear imagen Docker
curl -X POST "http://localhost:8094/v1/scans" \
  -H "Content-Type: application/json" \
  -d '{"target": "postgres:15"}'

# Escanear filesystem
docker run --rm -v $(pwd):/app aquasec/trivy fs /app
```

### 4. **Runtime Security (Falco)**
```bash
# Ver eventos de seguridad en tiempo real
docker logs falco -f

# Configurar reglas personalizadas
# Editar: ./security/falco/falco_rules.yaml
```

### 5. **Policy as Code (OPA)**
```bash
# Evaluar una política
curl -X POST http://localhost:8181/v1/data/example/allow \
  -H "Content-Type: application/json" \
  -d '{"input": {"user": "admin", "action": "read"}}'

# Subir nueva política
curl -X PUT http://localhost:8181/v1/policies/mi-politica \
  -H "Content-Type: text/plain" \
  -d @./policies/mi-politica.rego
```

## 🔧 Comandos Útiles

### Gestión del Stack
```bash
# Iniciar stack de seguridad
./manage-stack.sh start security

# Ver logs de todos los servicios
docker-compose -f docker-compose-security.yml logs -f

# Reiniciar servicio específico
docker-compose -f docker-compose-security.yml restart sonarqube
```

### SonarQube - Administración
```bash
# Crear proyecto
curl -X POST "http://localhost:9002/api/projects/create" \
  -u admin:admin \
  -d "project=mi-proyecto&name=Mi Proyecto"

# Ver métricas
curl "http://localhost:9002/api/measures/component" \
  -u admin:admin \
  -d "component=mi-proyecto&metricKeys=bugs,vulnerabilities,code_smells"
```

### Snyk - Comandos Manuales
```bash
# Test específico
docker exec snyk-cli snyk test --severity-threshold=high

# Monitor proyecto
docker exec snyk-cli snyk monitor --project-name=demo-security

# Container test
docker exec snyk-cli snyk container test postgres:15
```

### ZAP - Automatización
```bash
# Baseline scan
docker run -t owasp/zap2docker-stable zap-baseline.py \
  -t http://localhost:4200

# Full scan
docker run -t owasp/zap2docker-stable zap-full-scan.py \
  -t http://localhost:4200
```

## 🔍 Troubleshooting

### Problemas Comunes

1. **SonarQube no inicia**
   ```bash
   # Verificar límites de memoria
   docker stats sonarqube
   
   # Aumentar memory si es necesario
   # En .env: MEMORY_LIMIT_XLARGE=4g
   ```

2. **Snyk token inválido**
   ```bash
   # Verificar token
   docker logs snyk-cli
   
   # Obtener nuevo token en: https://app.snyk.io/account
   ```

3. **Falco no detecta eventos**
   ```bash
   # Verificar permisos
   docker logs falco
   
   # Asegurar que tiene acceso a /proc, /dev
   ```

4. **ZAP no puede acceder a aplicación**
   ```bash
   # Verificar conectividad de red
   docker exec owasp-zap curl http://host.docker.internal:4200
   ```

## 📊 Métricas y Reportes

### SonarQube Quality Gates
- **Bugs:** 0 nuevos bugs
- **Vulnerabilidades:** 0 nuevas vulnerabilidades
- **Code Smells:** Rating A
- **Cobertura:** > 80%
- **Duplicación:** < 3%

### Dashboards Disponibles
1. **Security Overview** - Resumen ejecutivo
2. **SAST Metrics** - Métricas de análisis estático
3. **DAST Results** - Resultados de análisis dinámico
4. **Container Security** - Vulnerabilidades en imágenes
5. **Policy Violations** - Violaciones de políticas

## 🔐 Mejores Prácticas

### Integración en CI/CD
```yaml
# Ejemplo Jenkins pipeline
stages:
  - name: "SAST Analysis"
    script: "sonar-scanner"
  - name: "Dependency Check"
    script: "snyk test"
  - name: "Container Scan"
    script: "trivy image myapp:latest"
  - name: "DAST Scan"
    script: "zap-baseline.py -t $TARGET_URL"
```

### Configuración de Alertas
1. **Critical vulnerabilities** → Slack/Email inmediato
2. **High severity** → Daily report
3. **Medium/Low** → Weekly summary

### Políticas Recomendadas
1. **No secrets en código**
2. **Dependencies actualizadas**
3. **Container images escaneadas**
4. **Code coverage > 80%**

## 🔗 Dependencias

### Prerequisitos:
- `docker-compose-base.yml` (para redes y volúmenes)

### Integra con:
- `docker-compose-apps.yml` (análisis de aplicaciones)
- `docker-compose-cicd.yml` (integración en pipelines)
- `docker-compose-observability.yml` (métricas de seguridad)

## 📚 Enlaces Útiles

- [SonarQube Documentation](https://docs.sonarqube.org/)
- [Snyk Documentation](https://docs.snyk.io/)
- [OWASP ZAP User Guide](https://www.zaproxy.org/docs/)
- [Falco Documentation](https://falco.org/docs/)
- [Trivy Documentation](https://aquasecurity.github.io/trivy/)
- [OPA Documentation](https://www.openpolicyagent.org/docs/)

## 🎖️ Compliance

Este stack ayuda a cumplir con:
- **OWASP Top 10**
- **NIST Cybersecurity Framework**
- **ISO 27001**
- **SOC 2**
- **PCI DSS** (análisis de código)
- **GDPR** (data protection)