# 🌐 Stack de Gateway - Kong API Gateway y Nginx Edge

## 🎯 Descripción General

El **docker-compose-gateway.yml** implementa la capa de gateway y proxy reverso del ecosistema. Incluye Kong como API Gateway empresarial y Nginx como edge proxy, proporcionando enrutamiento, autenticación, rate limiting y balanceo de carga.

## 🚪 Servicios Incluidos

### 🔗 **API Gateway**
- **Kong** - API Gateway empresarial con plugins
- **Kong Manager** - Interfaz de administración web (OSS)

### 🌍 **Edge Layer**
- **Nginx Edge** - Proxy reverso y load balancer

## 🔌 Puertos Expuestos

| Servicio | Puerto | Protocolo | Descripción |
|----------|--------|-----------|-------------|
| **Nginx Edge** | | | |
| HTTP | 80 | HTTP | Tráfico web general |
| HTTPS | 443 | HTTPS | Tráfico web seguro |
| **Kong API Gateway** | | | |
| Proxy HTTP | 8000 | HTTP | API Gateway público |
| Proxy HTTPS | 8443 | HTTPS | API Gateway seguro |
| Admin API | 8001 | HTTP | REST API administrativa |
| Admin HTTPS | 8444 | HTTPS | Admin API segura |
| Kong Manager | 8002 | HTTP | Interfaz web OSS |

## 🔑 Accesos por Defecto

### Kong API Gateway
- **Proxy:** http://localhost:8000
- **Admin API:** http://localhost:8001
- **Kong Manager:** http://localhost:8002
- **Health Check:** http://localhost:8001/status

### Nginx Edge
- **HTTP:** http://localhost:80
- **HTTPS:** https://localhost:443
- **Status:** http://localhost/nginx_status

## ⚙️ Variables de Entorno Requeridas

```bash
# Kong Database (debe existir en base stack)
KONG_DB_PASSWORD=kongpass

# Resource Limits
CPU_LIMIT_LARGE=1.0
MEMORY_LIMIT_LARGE=1g

# SSL/TLS (Opcional)
SSL_CERT_PATH=./certs/server.crt
SSL_KEY_PATH=./certs/server.key
```

## 🚀 Casos de Uso

### 1. **Configuración Básica de Kong**

#### Configurar un servicio y ruta
```bash
# Crear servicio backend
curl -X POST http://localhost:8001/services \
  -H "Content-Type: application/json" \
  -d '{
    "name": "comando-service",
    "url": "http://comando-microservicio:8081"
  }'

# Crear ruta para el servicio
curl -X POST http://localhost:8001/services/comando-service/routes \
  -H "Content-Type: application/json" \
  -d '{
    "name": "comando-route",
    "paths": ["/api/v1/comandos"]
  }'

# Probar la ruta
curl http://localhost:8000/api/v1/comandos/health
```

#### Configurar rate limiting
```bash
# Aplicar rate limiting global
curl -X POST http://localhost:8001/plugins \
  -H "Content-Type: application/json" \
  -d '{
    "name": "rate-limiting",
    "config": {
      "minute": 100,
      "hour": 1000
    }
  }'

# Rate limiting por servicio
curl -X POST http://localhost:8001/services/comando-service/plugins \
  -H "Content-Type: application/json" \
  -d '{
    "name": "rate-limiting",
    "config": {
      "minute": 50,
      "hour": 500
    }
  }'
```

### 2. **Autenticación y Autorización**

#### Configurar JWT Authentication
```bash
# Habilitar plugin JWT
curl -X POST http://localhost:8001/plugins \
  -H "Content-Type: application/json" \
  -d '{
    "name": "jwt",
    "config": {
      "uri_param_names": ["token"],
      "header_names": ["Authorization"]
    }
  }'

# Crear consumer
curl -X POST http://localhost:8001/consumers \
  -H "Content-Type: application/json" \
  -d '{"username": "demo-user"}'

# Crear credential JWT para consumer
curl -X POST http://localhost:8001/consumers/demo-user/jwt \
  -H "Content-Type: application/json" \
  -d '{
    "algorithm": "HS256",
    "key": "demo-key",
    "secret": "demo-secret"
  }'
```

#### Configurar OAuth2
```bash
# Habilitar OAuth2
curl -X POST http://localhost:8001/plugins \
  -H "Content-Type: application/json" \
  -d '{
    "name": "oauth2",
    "config": {
      "scopes": ["read", "write"],
      "mandatory_scope": true,
      "enable_authorization_code": true
    }
  }'
```

### 3. **Balanceo de Carga y Service Discovery**

#### Configurar upstream con múltiples targets
```bash
# Crear upstream
curl -X POST http://localhost:8001/upstreams \
  -H "Content-Type: application/json" \
  -d '{
    "name": "consulta-upstream",
    "algorithm": "round-robin",
    "healthchecks": {
      "active": {
        "http_path": "/actuator/health",
        "healthy": {
          "interval": 10,
          "successes": 2
        }
      }
    }
  }'

# Agregar targets al upstream
curl -X POST http://localhost:8001/upstreams/consulta-upstream/targets \
  -d "target=consulta-microservicio:8082&weight=100"

# Crear servicio usando upstream
curl -X POST http://localhost:8001/services \
  -H "Content-Type: application/json" \
  -d '{
    "name": "consulta-lb-service",
    "host": "consulta-upstream"
  }'
```

### 4. **Nginx - Configuración Avanzada**

#### Configurar SSL Termination
```nginx
# nginx.conf
server {
    listen 443 ssl http2;
    server_name localhost;
    
    ssl_certificate /etc/nginx/certs/server.crt;
    ssl_certificate_key /etc/nginx/certs/server.key;
    
    location / {
        proxy_pass http://kong:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

#### Load Balancing con Nginx
```nginx
upstream backend_apps {
    least_conn;
    server comando-microservicio:8081 weight=3;
    server consulta-microservicio:8082 weight=2;
    keepalive 32;
}

server {
    location /api/ {
        proxy_pass http://backend_apps;
    }
}
```

### 5. **Monitoreo y Observabilidad**

#### Habilitar métricas Prometheus en Kong
```bash
# Instalar plugin prometheus
curl -X POST http://localhost:8001/plugins \
  -H "Content-Type: application/json" \
  -d '{
    "name": "prometheus",
    "config": {
      "per_consumer": true
    }
  }'

# Acceder a métricas
curl http://localhost:8001/metrics
```

#### Configurar logging
```bash
# HTTP Log plugin
curl -X POST http://localhost:8001/plugins \
  -H "Content-Type: application/json" \
  -d '{
    "name": "http-log",
    "config": {
      "http_endpoint": "http://logstash:5044",
      "method": "POST"
    }
  }'
```

## 🔧 Comandos Útiles

### Gestión del Stack
```bash
# Iniciar gateway stack
./manage-stack.sh start gateway

# Ver estado de Kong
curl http://localhost:8001/status

# Reload configuración Nginx
docker exec nginx-edge nginx -s reload

# Ver configuración actual de Kong
curl http://localhost:8001/services
curl http://localhost:8001/routes
curl http://localhost:8001/plugins
```

### Administración de Kong
```bash
# Backup configuración
kong config db_export kong-backup.yaml

# Restaurar configuración
kong config db_import kong-backup.yaml

# Ver logs de Kong
docker logs kong -f

# Test de conectividad
curl -i http://localhost:8000/
```

### Debugging de Rutas
```bash
# Verificar qué ruta coincide
curl -H "Host: api.ejemplo.com" \
     -H "X-Debug: 1" \
     http://localhost:8000/api/test

# Ver headers de respuesta
curl -I http://localhost:8000/api/v1/comandos

# Test de SSL
openssl s_client -connect localhost:443
```

## 🔍 Troubleshooting

### Problemas Comunes

1. **Kong no se conecta a la base de datos**
   ```bash
   # Verificar conectividad a Kong-DB
   docker exec kong nc -zv kong-db 5432
   
   # Ver logs detallados
   docker logs kong --tail 100
   
   # Verificar variables de entorno
   docker exec kong env | grep KONG
   ```

2. **502 Bad Gateway desde Kong**
   ```bash
   # Verificar que el servicio backend esté corriendo
   curl http://comando-microservicio:8081/actuator/health
   
   # Verificar configuración del servicio en Kong
   curl http://localhost:8001/services/comando-service
   
   # Test directo al upstream
   curl -H "Host: localhost" http://localhost:8000/api/v1/test
   ```

3. **SSL/TLS issues en Nginx**
   ```bash
   # Verificar certificados
   docker exec nginx-edge nginx -t
   
   # Verificar configuración SSL
   openssl x509 -in ./certs/server.crt -text -noout
   ```

4. **Rate limiting muy restrictivo**
   ```bash
   # Ver configuración actual
   curl http://localhost:8001/plugins | jq '.data[] | select(.name=="rate-limiting")'
   
   # Deshabilitar temporalmente
   curl -X PATCH http://localhost:8001/plugins/{plugin-id} \
     -d "enabled=false"
   ```

## 📊 Métricas y Monitoreo

### Kong Métricas (Prometheus)
```promql
# Request rate por servicio
rate(kong_http_requests_total[5m])

# Latencia promedio
kong_request_latency_seconds

# Error rate
rate(kong_http_requests_total{code=~"5.."}[5m]) / rate(kong_http_requests_total[5m])
```

### Nginx Métricas
```bash
# Status page
curl http://localhost/nginx_status

# Access logs analysis
docker exec nginx-edge tail -f /var/log/nginx/access.log | grep "5\d\d"
```

### Health Checks Automáticos
```bash
# Script de monitoreo
#!/bin/bash
curl -f http://localhost:8001/status && \
curl -f http://localhost/nginx_status && \
echo "Gateway stack healthy"
```

## 🔐 Seguridad y Mejores Prácticas

### Configuración Segura de Kong
```bash
# Ocultar headers del servidor
curl -X POST http://localhost:8001/plugins \
  -d "name=response-transformer" \
  -d "config.remove.headers=Server,X-Powered-By"

# CORS configuration
curl -X POST http://localhost:8001/plugins \
  -d "name=cors" \
  -d "config.origins=https://app.midominio.com" \
  -d "config.methods=GET,POST,PUT,DELETE" \
  -d "config.headers=Accept,Authorization,Content-Type"

# Request size limiting
curl -X POST http://localhost:8001/plugins \
  -d "name=request-size-limiting" \
  -d "config.allowed_payload_size=10"
```

### Nginx Security Headers
```nginx
add_header X-Frame-Options DENY;
add_header X-Content-Type-Options nosniff;
add_header X-XSS-Protection "1; mode=block";
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains";
```

## 🏗️ Arquitectura de Red

### Flujo de Tráfico
```
Internet/Clients
        ↓
[Nginx Edge :80/443]
        ↓
[Kong Gateway :8000]
        ↓
[Microservicios Backend]
```

### Configuración de Redes
- **frontend** - Nginx, Kong
- **backend** - Kong, Microservicios
- **gateway** - Kong internals

## 🔗 Dependencias

### Prerequisitos:
- `docker-compose-base.yml` (Kong-DB)

### Integra con:
- `docker-compose-apps.yml` (routing a microservicios)
- `docker-compose-identity.yml` (integración con Keycloak)
- `docker-compose-observability.yml` (métricas y logs)

## 📚 Enlaces Útiles

- [Kong Documentation](https://docs.konghq.com/)
- [Kong Plugin Hub](https://docs.konghq.com/hub/)
- [Nginx Documentation](https://nginx.org/en/docs/)
- [Kong Manager OSS](https://docs.konghq.com/gateway/latest/kong-manager-oss/)
- [Kong Admin API](https://docs.konghq.com/gateway/latest/admin-api/)
- [Kong Rate Limiting](https://docs.konghq.com/hub/kong-inc/rate-limiting/)

## 🎯 Próximos Pasos

1. **Configurar SSL/TLS** con certificados válidos
2. **Integrar con Keycloak** para SSO
3. **Configurar métricas** en Grafana
4. **Implementar circuit breaker** patterns
5. **Configurar API versioning** strategies