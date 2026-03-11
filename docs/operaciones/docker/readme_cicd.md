# 🔄 Stack de CI/CD - Gitea y Jenkins

## 🎯 Descripción General

El **docker-compose-cicd.yml** implementa una plataforma completa de integración y despliegue continuo usando Gitea como repositorio Git y Jenkins como motor de CI/CD. Proporciona control de versiones, pipelines automatizados y deployment de aplicaciones.

## 🏗️ Servicios Incluidos

### 📦 **Source Code Management**
- **Gitea** - Servidor Git autohosteado con interfaz web

### 🚀 **CI/CD Pipeline**
- **Jenkins** - Servidor de automatización para CI/CD

## 🔌 Puertos Expuestos

| Servicio | Puerto | Protocolo | Descripción |
|----------|--------|-----------|-------------|
| **Gitea** | | | |
| Web Interface | 3001 | HTTP | Interfaz web de Gitea |
| SSH Git | 2222 | SSH | Acceso Git por SSH |
| **Jenkins** | | | |
| Web Interface | 8088 | HTTP | Interfaz web de Jenkins |
| JNLP Agent | 50000 | TCP | Puerto para agentes Jenkins |

## 🔑 Accesos por Defecto

### Gitea
- **URL:** http://localhost:3001
- **Setup inicial:** Configurar en primer acceso
- **Git SSH:** ssh://git@localhost:2222/
- **Git HTTPS:** http://localhost:3001/

### Jenkins
- **URL:** http://localhost:8088
- **Setup inicial:** Configurar con unlock key
- **Initial Admin Password:** Ver logs del container

## ⚙️ Variables de Entorno Requeridas

```bash
# Gitea Database Configuration
GITEA_DB_NAME=gitea
GITEA_DB_USER=gitea
GITEA_DB_PASSWORD=gitea123
GITEA_SECRET_KEY=gitea-secret-key-change-this-in-production

# Jenkins Configuration
JENKINS_OPTS=--httpPort=8080
JAVA_OPTS=-Xmx2g -Dhudson.plugins.git.GitSCM.ALLOW_LOCAL_CHECKOUT=true

# Resource Limits
CPU_LIMIT_LARGE=2.0
MEMORY_LIMIT_XLARGE=4g
```

## 🚀 Casos de Uso

### 1. **Configuración Inicial**

#### Setup Gitea
```bash
# Acceder a http://localhost:3001 para configuración inicial
# Configuración recomendada:
# - Database Type: PostgreSQL
# - Host: postgres:5432
# - Database: gitea
# - Username: gitea
# - Password: gitea123

# Crear primer usuario admin via CLI
docker exec gitea gitea admin user create \
  --name admin \
  --password admin123 \
  --email admin@empresa.com \
  --admin

# Crear organización
curl -X POST "http://localhost:3001/api/v1/orgs" \
  -H "Authorization: token YOUR_GITEA_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "empresa-dev",
    "full_name": "Empresa Development",
    "description": "Repositorios de desarrollo de la empresa"
  }'
```

#### Setup Jenkins
```bash
# Obtener initial admin password
docker logs jenkins | grep -A 5 -B 5 "Please use the following password"

# O directamente:
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword

# Instalar plugins recomendados via CLI
docker exec jenkins jenkins-plugin-cli --plugins \
  git \
  pipeline-stage-view \
  docker-workflow \
  kubernetes \
  blueocean \
  slack \
  email-ext
```

### 2. **Repositorio y Configuración de Proyecto**

#### Crear repositorio en Gitea
```bash
# Via API
curl -X POST "http://localhost:3001/api/v1/user/repos" \
  -H "Authorization: token YOUR_GITEA_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "microservicio-productos",
    "description": "Microservicio para gestión de productos",
    "private": false,
    "auto_init": true,
    "gitignores": "Java",
    "license": "MIT",
    "readme": "Default"
  }'

# Clonar y configurar localmente
git clone http://localhost:3001/admin/microservicio-productos.git
cd microservicio-productos

# Configurar hooks pre-commit
cat > .pre-commit-config.yaml << EOF
repos:
  - repo: https://github.com/pre-commit/pre-commit-hooks
    rev: v4.4.0
    hooks:
      - id: trailing-whitespace
      - id: end-of-file-fixer
      - id: check-yaml
      - id: check-json
      - id: check-merge-conflict
EOF
```

#### Estructura de proyecto recomendada
```
microservicio-productos/
├── src/
│   ├── main/java/com/empresa/productos/
│   └── test/java/com/empresa/productos/
├── Dockerfile
├── docker-compose.yml
├── Jenkinsfile
├── sonar-project.properties
├── pom.xml
├── .gitignore
├── README.md
└── deploy/
    ├── k8s/
    │   ├── deployment.yaml
    │   └── service.yaml
    └── docker/
        └── docker-compose.prod.yml
```

### 3. **Pipeline de CI/CD con Jenkins**

#### Jenkinsfile para microservicio Spring Boot
```groovy
// Jenkinsfile
pipeline {
    agent any
    
    environment {
        DOCKER_REGISTRY = 'harbor:8089'
        DOCKER_IMAGE = 'empresa/microservicio-productos'
        GITEA_CREDENTIALS = 'gitea-credentials'
        SONAR_TOKEN = credentials('sonar-token')
        SLACK_CHANNEL = '#ci-cd'
    }
    
    stages {
        stage('Checkout') {
            steps {
                git branch: 'main',
                    credentialsId: "${GITEA_CREDENTIALS}",
                    url: 'http://gitea:3000/admin/microservicio-productos.git'
            }
        }
        
        stage('Build & Test') {
            parallel {
                stage('Maven Build') {
                    steps {
                        sh '''
                            ./mvnw clean compile
                            ./mvnw test
                            ./mvnw package -DskipTests
                        '''
                    }
                    post {
                        always {
                            publishTestResults testResultsPattern: 'target/surefire-reports/*.xml'
                            publishCoverage adapters: [
                                jacoco(path: 'target/site/jacoco/jacoco.xml')
                            ]
                        }
                    }
                }
                
                stage('Code Quality') {
                    steps {
                        withSonarQubeEnv('SonarQube') {
                            sh '''
                                ./mvnw sonar:sonar \
                                  -Dsonar.projectKey=microservicio-productos \
                                  -Dsonar.host.url=http://sonarqube:9000 \
                                  -Dsonar.login=${SONAR_TOKEN}
                            '''
                        }
                    }
                }
            }
        }
        
        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
        
        stage('Security Scan') {
            parallel {
                stage('Dependency Check') {
                    steps {
                        sh '''
                            ./mvnw org.owasp:dependency-check-maven:check
                        '''
                        publishHTML([
                            allowMissing: false,
                            alwaysLinkToLastBuild: true,
                            keepAll: true,
                            reportDir: 'target/reports',
                            reportFiles: 'dependency-check-report.html',
                            reportName: 'OWASP Dependency Check'
                        ])
                    }
                }
                
                stage('Container Scan') {
                    steps {
                        script {
                            sh '''
                                docker build -t ${DOCKER_IMAGE}:${BUILD_NUMBER} .
                                trivy image --format json --output trivy-report.json ${DOCKER_IMAGE}:${BUILD_NUMBER}
                            '''
                        }
                    }
                }
            }
        }
        
        stage('Docker Build & Push') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                script {
                    def dockerImage = docker.build("${DOCKER_IMAGE}:${BUILD_NUMBER}")
                    docker.withRegistry("https://${DOCKER_REGISTRY}", 'harbor-credentials') {
                        dockerImage.push()
                        dockerImage.push('latest')
                    }
                }
            }
        }
        
        stage('Deploy to Staging') {
            when {
                branch 'develop'
            }
            steps {
                sh '''
                    envsubst < deploy/k8s/deployment.yaml | kubectl apply -f -
                    kubectl set image deployment/microservicio-productos \
                      microservicio-productos=${DOCKER_REGISTRY}/${DOCKER_IMAGE}:${BUILD_NUMBER} \
                      -n staging
                    kubectl rollout status deployment/microservicio-productos -n staging
                '''
            }
        }
        
        stage('Integration Tests') {
            when {
                branch 'develop'
            }
            steps {
                sh '''
                    # Esperar que el deployment esté listo
                    kubectl wait --for=condition=available --timeout=300s deployment/microservicio-productos -n staging
                    
                    # Ejecutar tests de integración
                    ./mvnw test -Dtest=IntegrationTest \
                      -Dspring.profiles.active=integration \
                      -Dapp.base-url=http://staging.empresa.com
                '''
            }
        }
        
        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                input message: 'Deploy to Production?', ok: 'Deploy'
                
                sh '''
                    kubectl set image deployment/microservicio-productos \
                      microservicio-productos=${DOCKER_REGISTRY}/${DOCKER_IMAGE}:${BUILD_NUMBER} \
                      -n production
                    kubectl rollout status deployment/microservicio-productos -n production
                '''
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        
        success {
            slackSend(
                channel: "${SLACK_CHANNEL}",
                color: 'good',
                message: "✅ Pipeline SUCCESS: ${env.JOB_NAME} - ${env.BUILD_NUMBER}"
            )
        }
        
        failure {
            slackSend(
                channel: "${SLACK_CHANNEL}",
                color: 'danger',
                message: "❌ Pipeline FAILED: ${env.JOB_NAME} - ${env.BUILD_NUMBER}"
            )
        }
    }
}
```

### 4. **Pipeline para Frontend (React/Angular)**

#### Jenkinsfile para aplicación frontend
```groovy
// Jenkinsfile para frontend
pipeline {
    agent any
    
    environment {
        NODE_VERSION = '18'
        DOCKER_REGISTRY = 'harbor:8089'
        DOCKER_IMAGE = 'empresa/frontend-app'
    }
    
    stages {
        stage('Setup') {
            steps {
                sh '''
                    nvm use ${NODE_VERSION}
                    npm ci
                '''
            }
        }
        
        stage('Lint & Test') {
            parallel {
                stage('ESLint') {
                    steps {
                        sh 'npm run lint'
                        recordIssues enabledForFailure: true, tools: [esLint()]
                    }
                }
                
                stage('Unit Tests') {
                    steps {
                        sh 'npm run test:ci'
                    }
                    post {
                        always {
                            publishTestResults testResultsPattern: 'coverage/junit.xml'
                            publishCoverage adapters: [
                                istanbulCoberturaAdapter('coverage/cobertura-coverage.xml')
                            ]
                        }
                    }
                }
                
                stage('Type Check') {
                    steps {
                        sh 'npm run type-check'
                    }
                }
            }
        }
        
        stage('Build') {
            parallel {
                stage('Development Build') {
                    when {
                        branch 'develop'
                    }
                    steps {
                        sh '''
                            npm run build:dev
                            tar -czf dist-dev.tar.gz dist/
                        '''
                        archiveArtifacts artifacts: 'dist-dev.tar.gz', fingerprint: true
                    }
                }
                
                stage('Production Build') {
                    when {
                        branch 'main'
                    }
                    steps {
                        sh '''
                            npm run build:prod
                            tar -czf dist-prod.tar.gz dist/
                        '''
                        archiveArtifacts artifacts: 'dist-prod.tar.gz', fingerprint: true
                    }
                }
            }
        }
        
        stage('E2E Tests') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                sh '''
                    # Iniciar aplicación para testing
                    npm run start:test &
                    APP_PID=$!
                    
                    # Esperar que la app esté lista
                    while ! curl -f http://localhost:4200/health; do
                        sleep 5
                    done
                    
                    # Ejecutar tests E2E
                    npm run e2e
                    
                    # Limpiar
                    kill $APP_PID
                '''
            }
            post {
                always {
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: false,
                        keepAll: true,
                        reportDir: 'e2e/reports',
                        reportFiles: 'index.html',
                        reportName: 'E2E Test Report'
                    ])
                }
            }
        }
        
        stage('Docker Build & Deploy') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                script {
                    def environment = env.BRANCH_NAME == 'main' ? 'prod' : 'staging'
                    
                    sh '''
                        docker build \
                          --build-arg ENVIRONMENT=${environment} \
                          -t ${DOCKER_IMAGE}:${BUILD_NUMBER} \
                          -t ${DOCKER_IMAGE}:latest-${environment} \
                          .
                    '''
                    
                    docker.withRegistry("https://${DOCKER_REGISTRY}", 'harbor-credentials') {
                        sh '''
                            docker push ${DOCKER_IMAGE}:${BUILD_NUMBER}
                            docker push ${DOCKER_IMAGE}:latest-${environment}
                        '''
                    }
                }
            }
        }
    }
}
```

### 5. **GitOps y Deployment Automatizado**

#### Configuración de webhooks Gitea → Jenkins
```bash
# Configurar webhook en Gitea
curl -X POST "http://localhost:3001/api/v1/repos/admin/microservicio-productos/hooks" \
  -H "Authorization: token YOUR_GITEA_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "gitea",
    "config": {
      "url": "http://jenkins:8080/gitea-webhook/post",
      "content_type": "json",
      "secret": "webhook-secret"
    },
    "events": ["push", "pull_request"],
    "active": true
  }'

# Configurar Jenkins para recibir webhooks
# 1. Instalar Gitea Plugin en Jenkins
# 2. Configurar webhook endpoint en job configuration
# 3. Trigger builds on: Gitea webhook
```

#### Pipeline de deployment con GitOps
```yaml
# deploy/gitops/application.yaml
apiVersion: argoprocd.argoproj.io/v1alpha1
kind: Application
metadata:
  name: microservicio-productos
  namespace: argocd
spec:
  project: default
  source:
    repoURL: http://gitea:3000/admin/microservicio-productos.git
    targetRevision: HEAD
    path: deploy/k8s
  destination:
    server: https://kubernetes.default.svc
    namespace: production
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
    - CreateNamespace=true
```

## 🔧 Comandos Útiles

### Gestión del Stack
```bash
# Iniciar CI/CD stack
./manage-stack.sh start cicd

# Ver logs de Jenkins
docker logs jenkins -f

# Ver logs de Gitea
docker logs gitea -f

# Acceder a Jenkins CLI
docker exec jenkins java -jar /var/jenkins_home/war/WEB-INF/jenkins-cli.jar -s http://localhost:8080/ help
```

### Administración de Jenkins
```bash
# Instalar plugins via CLI
docker exec jenkins jenkins-plugin-cli --plugins git pipeline-stage-view

# Crear job via CLI
cat > job-config.xml << EOF
<project>
  <scm class="hudson.plugins.git.GitSCM">
    <configVersion>2</configVersion>
    <userRemoteConfigs>
      <hudson.plugins.git.UserRemoteConfig>
        <url>http://gitea:3000/admin/microservicio-productos.git</url>
      </hudson.plugins.git.UserRemoteConfig>
    </userRemoteConfigs>
  </scm>
</project>
EOF

docker exec jenkins java -jar /var/jenkins_home/war/WEB-INF/jenkins-cli.jar \
  -s http://localhost:8080/ create-job microservicio-productos < job-config.xml

# Trigger build
docker exec jenkins java -jar /var/jenkins_home/war/WEB-INF/jenkins-cli.jar \
  -s http://localhost:8080/ build microservicio-productos

# Ver logs de build
docker exec jenkins java -jar /var/jenkins_home/war/WEB-INF/jenkins-cli.jar \
  -s http://localhost:8080/ console microservicio-productos 1
```

### Administración de Gitea
```bash
# Crear usuario via CLI
docker exec gitea gitea admin user create \
  --name developer1 \
  --password dev123 \
  --email dev1@empresa.com

# Crear repositorio via CLI
docker exec gitea gitea admin repo create \
  --owner admin \
  --name nuevo-proyecto \
  --private=false

# Backup de Gitea
docker exec gitea gitea dump -c /data/gitea/conf/app.ini

# Ver estadísticas
docker exec gitea gitea admin stats
```

### Git Operations
```bash
# Configurar Git para usar Gitea local
git config --global user.name "Developer"
git config --global user.email "dev@empresa.com"

# Clonar repositorio
git clone http://localhost:3001/admin/microservicio-productos.git

# Configurar remote para SSH
git remote set-url origin ssh://git@localhost:2222/admin/microservicio-productos.git

# Push con tags
git push origin main --tags

# Crear y push feature branch
git checkout -b feature/nueva-funcionalidad
git push -u origin feature/nueva-funcionalidad
```

## 🔍 Troubleshooting

### Problemas Comunes

1. **Jenkins no puede clonar de Gitea**
   ```bash
   # Verificar conectividad
   docker exec jenkins curl -f http://gitea:3000/
   
   # Verificar credenciales en Jenkins
   # Manage Jenkins → Credentials → Add credentials
   
   # Test manual de clone
   docker exec jenkins git clone http://gitea:3000/admin/test-repo.git /tmp/test
   ```

2. **Webhooks no funcionan**
   ```bash
   # Verificar webhook en Gitea
   curl "http://localhost:3001/api/v1/repos/admin/microservicio-productos/hooks" \
     -H "Authorization: token YOUR_TOKEN"
   
   # Test webhook manualmente
   curl -X POST "http://jenkins:8080/gitea-webhook/post" \
     -H "Content-Type: application/json" \
     -d '{"repository": {"clone_url": "http://gitea:3000/admin/test.git"}}'
   
   # Ver logs de Jenkins para webhooks
   docker logs jenkins | grep webhook
   ```

3. **Build failures**
   ```bash
   # Ver workspace del job
   docker exec jenkins ls -la /var/jenkins_home/workspace/
   
   # Ver logs detallados
   docker exec jenkins cat /var/jenkins_home/jobs/microservicio-productos/builds/lastBuild/log
   
   # Verificar herramientas disponibles
   docker exec jenkins which docker
   docker exec jenkins which kubectl
   ```

4. **Performance issues**
   ```bash
   # Ver uso de recursos
   docker stats jenkins gitea
   
   # Limpiar workspaces viejos
   docker exec jenkins find /var/jenkins_home/workspace -type d -mtime +7 -exec rm -rf {} +
   
   # Ver jobs con más consumo
   docker exec jenkins du -sh /var/jenkins_home/jobs/*
   ```

## 📊 Monitoreo y Métricas

### Jenkins Metrics
```bash
# Métricas via API
curl "http://localhost:8088/metrics/currentUser/metrics?pretty=true"

# Información del sistema
curl "http://localhost:8088/systemInfo/api/json?pretty=true"

# Jobs status
curl "http://localhost:8088/api/json?tree=jobs[name,color,lastBuild[number,result,timestamp]]&pretty=true"
```

### Gitea Metrics
```bash
# Estadísticas de repositorios
curl "http://localhost:3001/api/v1/admin/stats" \
  -H "Authorization: token YOUR_TOKEN"

# Información de usuario
curl "http://localhost:3001/api/v1/user" \
  -H "Authorization: token YOUR_TOKEN"
```

### CI/CD Pipeline Metrics
```json
{
  "cicd_dashboard": {
    "panels": [
      {
        "title": "Build Success Rate",
        "query": "jenkins_builds_success_total / jenkins_builds_total"
      },
      {
        "title": "Average Build Duration",
        "query": "avg(jenkins_build_duration_seconds)"
      },
      {
        "title": "Queue Size",
        "query": "jenkins_queue_size"
      },
      {
        "title": "Failed Builds",
        "query": "rate(jenkins_builds_failed_total[5m])"
      }
    ]
  }
}
```

## 🔐 Seguridad y Mejores Prácticas

### Jenkins Security
```bash
# Configurar seguridad basada en matriz
# Manage Jenkins → Configure Global Security
# Authorization: Matrix-based security

# Crear usuario de servicio
docker exec jenkins java -jar /var/jenkins_home/war/WEB-INF/jenkins-cli.jar \
  -s http://localhost:8080/ create-user service-account --password service123

# Configurar API tokens
# User → Configure → API Token → Add new token
```

### Gitea Security
```bash
# Configurar autenticación 2FA
# User Settings → Security → Two-Factor Authentication

# Configurar deploy keys
curl -X POST "http://localhost:3001/api/v1/repos/admin/microservicio-productos/keys" \
  -H "Authorization: token YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Jenkins Deploy Key",
    "key": "ssh-rsa AAAAB3NzaC1yc2E...",
    "read_only": true
  }'

# Configurar branch protection
curl -X POST "http://localhost:3001/api/v1/repos/admin/microservicio-productos/branch_protections" \
  -H "Authorization: token YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "branch_name": "main",
    "enable_push": false,
    "enable_push_whitelist": true,
    "push_whitelist_usernames": ["admin"],
    "require_signed_off_by": true,
    "enable_status_check": true
  }'
```

### Secrets Management
```groovy
// En Jenkinsfile - usar credentials de forma segura
pipeline {
    environment {
        DATABASE_PASSWORD = credentials('database-password')
        API_KEY = credentials('external-api-key')
    }
    
    stages {
        stage('Deploy') {
            steps {
                withCredentials([
                    string(credentialsId: 'deploy-token', variable: 'DEPLOY_TOKEN')
                ]) {
                    sh '''
                        echo "Using secure credentials"
                        # Never echo actual credentials
                        kubectl create secret generic app-secrets \
                          --from-literal=database-password="${DATABASE_PASSWORD}" \
                          --from-literal=api-key="${API_KEY}"
                    '''
                }
            }
        }
    }
}
```

## 🔗 Dependencias

### Prerequisitos:
- `docker-compose-base.yml` (PostgreSQL para Gitea)

### Integra con:
- `docker-compose-security.yml` (análisis de código)
- `docker-compose-registry.yml` (push de imágenes)
- `docker-compose-observability.yml` (métricas de CI/CD)
- `docker-compose-apps.yml` (deployment de microservicios)

## 📚 Enlaces Útiles

- [Gitea Documentation](https://docs.gitea.io/)
- [Jenkins Documentation](https://www.jenkins.io/doc/)
- [Jenkins Pipeline Syntax](https://www.jenkins.io/doc/book/pipeline/syntax/)
- [Gitea API Documentation](https://docs.gitea.io/en-us/api-usage/)
- [Jenkins REST API](https://www.jenkins.io/doc/book/using/remote-access-api/)
- [GitOps with ArgoCD](https://argo-cd.readthedocs.io/)

## 🎯 Próximos Pasos

1. **Configurar agentes distribuidos** de Jenkins
2. **Implementar pipeline templates** reutilizables
3. **Configurar disaster recovery** para repositorios
4. **Integrar con herramientas** de security scanning
5. **Implementar automated testing** strategies