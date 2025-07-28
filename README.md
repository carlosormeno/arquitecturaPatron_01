# Guía Completa para Levantar la Arquitectura Docker

## 🚀 Secuencia de Levantado

### FASE 0: Infraestructura Base (OBLIGATORIO PRIMERO)
```bash
# 1. Levantar la infraestructura base
docker compose -f docker-compose-inicio.yml up -d

# 2. Verificar que las redes se crearon
docker network ls | grep -E "(frontend|backend|messaging|gateway|observability)"

# 3. Verificar volúmenes
docker volume ls | grep -E "(postgres|mongo|kafka|prometheus|grafana)"

# Esperar 30 segundos para que termine el bootstrap
sleep 30
```

### FASE 1: Base de Datos y Mensajería
```bash
# Levantar PostgreSQL, MongoDB, Kafka, ZooKeeper, Debezium
docker compose -f docker-compose-base.yml up -d

# Verificar estado de salud
docker compose -f docker-compose-base.yml ps
```

#### IMPORTANTE: Configurar Kong Database (SOLO UNA VEZ)
```bash
# Ejecutar migraciones de Kong (SOLO LA PRIMERA VEZ)
docker run --rm \
  --network gateway_network \
  -e KONG_DATABASE=postgres \
  -e KONG_PG_HOST=kong-db \
  -e KONG_PG_PASSWORD=kongpass \
  kong:3.6 kong migrations bootstrap
```

### FASE 2: Gateway y Proxy
```bash
# Levantar NGINX y Kong
docker compose -f docker-compose-gateway.yml up -d

# Verificar Kong
curl http://localhost:8001/status
```

### FASE 3: Identidad y Autenticación
```bash
# Levantar Keycloak
docker compose -f docker-compose-identity.yml up -d

# Verificar Keycloak (puede tardar 2-3 minutos)
curl http://localhost:8080/health/ready
```

### FASE 4: Observabilidad
```bash
# Levantar stack completo de monitoreo
docker compose -f docker-compose-observability.yml up -d

# Verificar servicios principales
curl http://localhost:9090/-/healthy  # Prometheus
curl http://localhost:3000/api/health # Grafana
curl http://localhost:9200/_cluster/health # Elasticsearch
```

### FASE 5: Seguridad
```bash
# Levantar herramientas de seguridad
docker compose -f docker-compose-security.yml up -d

# Verificar SonarQube (puede tardar 3-4 minutos)
curl http://localhost:9002/api/system/status
```

### FASE 6: Gestión de Contenido
```bash
# Levantar Alfresco stack
docker compose -f docker-compose-content.yml up -d

# Verificar Alfresco Repository (puede tardar 5-6 minutos)
curl http://localhost:8082/alfresco/api/-default-/public/alfresco/versions/1/probes/-ready-
```

### FASE 7: Plataforma de Datos
```bash
# Levantar Apache NiFi
docker compose -f docker-compose-dataplatform.yml up -d

# Verificar NiFi (puede tardar 3-4 minutos)
curl http://localhost:8084/nifi/
```

### FASE 8: Governance de Datos
```bash
# Levantar DataHub
docker compose -f docker-compose-governance.yml up -d

# Verificar DataHub
curl http://localhost:8085/health
```

### FASE 9: Workflow
```bash
# Levantar Apache Airflow
docker compose -f docker-compose-workflow.yml up -d

# Verificar Airflow
curl http://localhost:8086/health
```

### FASE 10: Reglas de Negocio
```bash
# Levantar GoRules
docker compose -f docker-compose-business.yml up -d

# Verificar GoRules
curl http://localhost:3003/ # Editor
curl http://localhost:8180/health # BRMS
```

### FASE 11: CI/CD
```bash
# Levantar Gitea y Jenkins
docker compose -f docker-compose-cicd.yml up -d

# Verificar servicios
curl http://localhost:3001/ # Gitea
curl http://localhost:8088/ # Jenkins
```

### FASE 12: Registry
```bash
# Levantar Harbor y Nexus
docker compose -f docker-compose-registry.yml up -d

# Verificar servicios
curl http://localhost:8089/api/v2.0/health # Harbor
curl http://localhost:8081/service/rest/v1/status # Nexus
```

### FASE 13: Aplicaciones (FINAL)
```bash
# Levantar tus microservicios y frontend
docker compose -f docker-compose-apps.yml up -d

# Verificar aplicaciones
curl http://localhost:8081/actuator/health # comando-microservicio
curl http://localhost:8082/actuator/health # consulta-microservicio
curl http://localhost:4200/ # frontend
```

## 📊 Verificación Completa del Sistema

### Script de Verificación
```bash
#!/bin/bash
echo "=== VERIFICACIÓN COMPLETA DEL SISTEMA ==="

# Servicios principales
services=(
  "http://localhost:9001/api/status:Portainer"
  "http://localhost:5432:PostgreSQL"
  "http://localhost:27017:MongoDB"
  "http://localhost:9092:Kafka"
  "http://localhost:8000:Kong Gateway"
  "http://localhost:8080/health/ready:Keycloak"
  "http://localhost:9090/-/healthy:Prometheus"
  "http://localhost:3000/api/health:Grafana"
  "http://localhost:9200/_cluster/health:Elasticsearch"
  "http://localhost:5601/api/status:Kibana"
  "http://localhost:9002/api/system/status:SonarQube"
  "http://localhost:8084/nifi/:NiFi"
  "http://localhost:8086/health:Airflow"
  "http://localhost:8081/actuator/health:Comando Microservice"
  "http://localhost:8082/actuator/health:Consulta Microservice"
  "http://localhost:4200/:Frontend"
)

for service in "${services[@]}"; do
  url=$(echo $service | cut -d: -f1-2)
  name=$(echo $service | cut -d: -f3-)
  
  if curl -s -f "$url" > /dev/null 2>&1; then
    echo "✅ $name: OK"
  else
    echo "❌ $name: FAIL"
  fi
done
```

## 🔧 Comandos de Gestión Útiles

### Monitoreo General
```bash
# Ver estado de todos los servicios
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# Ver logs de un servicio específico
docker logs -f [nombre-contenedor]

# Ver uso de recursos
docker stats

# Ver redes
docker network ls

# Ver volúmenes
docker volume ls
```

### Parar Servicios por Fases
```bash
# Parar aplicaciones primero
docker compose -f docker-compose-apps.yml down

# Parar por orden inverso
docker compose -f docker-compose-registry.yml down
docker compose -f docker-compose-cicd.yml down
docker compose -f docker-compose-business.yml down
docker compose -f docker-compose-workflow.yml down
docker compose -f docker-compose-governance.yml down
docker compose -f docker-compose-dataplatform.yml down
docker compose -f docker-compose-content.yml down
docker compose -f docker-compose-security.yml down
docker compose -f docker-compose-observability.yml down
docker compose -f docker-compose-identity.yml down
docker compose -f docker-compose-gateway.yml down
docker compose -f docker-compose-base.yml down
docker compose -f docker-compose-inicio.yml down
```

### Limpieza Completa (¡CUIDADO! Borra todos los datos)
```bash
# Parar todo
docker stop $(docker ps -aq)

# Eliminar contenedores
docker rm $(docker ps -aq)

# Eliminar volúmenes (BORRA DATOS)
docker volume prune -f

# Eliminar redes
docker network prune -f

# Eliminar imágenes no utilizadas
docker image prune -a -f
```

## 🌐 URLs de Acceso

| Servicio | URL | Credenciales |
|----------|-----|--------------|
| Portainer | http://localhost:9001 | Admin setup inicial |
| Kong Manager | http://localhost:8002 | - |
| Keycloak | http://localhost:8080 | admin/admin123 |
| Prometheus | http://localhost:9090 | - |
| Grafana | http://localhost:3000 | admin/admin123 |
| Elasticsearch | http://localhost:9200 | - |
| Kibana | http://localhost:5601 | - |
| SonarQube | http://localhost:9002 | admin/admin |
| OWASP ZAP | http://localhost:8092 | - |
| NiFi | http://localhost:8084/nifi | admin/admin123456789 |
| Airflow | http://localhost:8086 | admin/admin123 |
| DataHub | http://localhost:9003 | - |
| Alfresco Share | http://localhost:8083/share | admin/admin |
| Alfresco Content App | http://localhost:4201 | admin/admin |
| GoRules Editor | http://localhost:3003 | - |
| Gitea | http://localhost:3001 | Setup inicial |
| Jenkins | http://localhost:8088 | Setup inicial |
| Harbor | http://localhost:8089 | admin/Harbor12345 |
| Nexus | http://localhost:8081 | admin/admin123 |
| Frontend App | http://localhost:4200 | - |
| Comando API | http://localhost:8081 | - |
| Consulta API | http://localhost:8082 | - |

## ⚠️ Notas Importantes

1. **Orden de levantado**: Es crítico seguir el orden indicado
2. **Tiempos de espera**: Algunos servicios pueden tardar varios minutos en estar listos
3. **Recursos**: Esta arquitectura requiere al menos 16GB RAM y 8 CPU cores
4. **Configuración inicial**: Algunos servicios requieren configuración manual tras el primer arranque
5. **Kong migrations**: Solo ejecutar una vez la migración de Kong
6. **Logs**: Monitorear logs durante el levantado para detectar problemas temprano

## 🐛 Solución de Problemas Comunes

### Si un servicio no arranca:
```bash
# Ver logs detallados
docker logs -f [nombre-contenedor]

# Reiniciar servicio específico
docker compose -f [archivo].yml restart [servicio]

# Verificar dependencias
docker compose -f [archivo].yml ps
```

### Si hay problemas de red:
```bash
# Recrear redes
docker network prune -f
docker compose -f docker-compose-inicio.yml up -d
```

### Si hay problemas de volúmenes:
```bash
# Verificar permisos
sudo chown -R $USER:$USER ./logs
sudo chmod -R 755 ./logs
```
