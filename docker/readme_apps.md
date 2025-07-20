# 🚀 Stack de Aplicaciones - Microservicios y Frontend

## 🎯 Descripción General

El **docker-compose-apps.yml** contiene las aplicaciones principales del sistema: microservicios basados en Spring Boot (patrón CQRS) y frontend Angular, junto con herramientas de utilidad para desarrollo y monitoreo.

## 🏗️ Servicios Incluidos

### 🔄 **Microservicios (CQRS Pattern)**
- **Comando Microservicio** - Gestión de escrituras y comandos
- **Consulta Microservicio** - Gestión de lecturas y queries
- **Frontend Angular** - Interfaz de usuario web

### 🛠️ **Herramientas de Desarrollo**
- **Kafdrop** - Interfaz web para monitoreo de Kafka

## 🔌 Puertos Expuestos

| Servicio | Puerto | Protocolo | Descripción |
|----------|--------|-----------|-------------|
| Comando Microservicio | 8081 | HTTP | API REST de comandos |
| Consulta Microservicio | 8082 | HTTP | API REST de consultas |
| Frontend | 4200 | HTTP | Aplicación web Angular |
| Kafdrop | 9000 | HTTP | UI de Kafka |

## 🔑 Accesos por Defecto

### Comando Microservicio
- **URL:** http://localhost:8081
- **API Docs:** http://localhost:8081/swagger-ui.html
- **Health:** http://localhost:8081/actuator/health
- **Metrics:** http://localhost:8081/actuator/prometheus

### Consulta Microservicio
- **URL:** http://localhost:8082
- **API Docs:** http://localhost:8082/swagger-ui.html
- **Health:** http://localhost:8082/actuator/health
- **Metrics:** http://localhost:8082/actuator/prometheus

### Frontend Angular
- **URL:** http://localhost:4200
- **Environment:** Development mode
- **Hot Reload:** Habilitado

### Kafdrop (Kafka UI)
- **URL:** http://localhost:9000
- **Funciones:** Topics, Messages, Consumers, Brokers

## ⚙️ Variables de Entorno Requeridas

```bash
# Spring Boot Configuration
SPRING_PROFILES_ACTIVE=dev
JWT_SECRET=Sup3r_S3cur3_JWT_K3y_F0r_Pr0duct10n_2024_M1n1mum_32_Ch4r4ct3rs!
JWT_EXPIRATION=3600000

# Database Connections
POSTGRES_DB=productos_db
POSTGRES_USER=admin
POSTGRES_PASSWORD=admin123

# Logging
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=DEBUG

# JVM Settings
JAVA_OPTS_COMMAND=-Xms256m -Xmx512m -XX:+UseG1GC -Dfile.encoding=UTF-8
JAVA_OPTS_QUERY=-Xms256m -Xmx512m -XX:+UseG1GC -Dfile.encoding=UTF-8

# Resource Limits
CPU_LIMIT_LARGE=1.0
CPU_LIMIT_MEDIUM=0.5
MEMORY_LIMIT_LARGE=1g
MEMORY_LIMIT_MEDIUM=512m
```

## 🚀 Casos de Uso

### 1. **Desarrollo Local**

#### Levantar solo las aplicaciones
```bash
# Asegurar que base stack esté corriendo
./manage-stack.sh start base

# Iniciar aplicaciones
./manage-stack.sh start apps

# Verificar salud de servicios
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

#### Hot Reload para desarrollo
```bash
# Spring Boot DevTools está habilitado
# Los cambios en Java se reflejan automáticamente

# Para rebuild de imágenes:
docker-compose -f docker-compose-apps.yml build comando-microservicio
docker-compose -f docker-compose-apps.yml up -d comando-microservicio
```

### 2. **Patrón CQRS en Acción**

#### Comando (Escritura)
```bash
# Crear producto
curl -X POST http://localhost:8081/api/productos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "nombre": "Producto Test",
    "precio": 99.99,
    "categoria": "Electronics"
  }'

# Actualizar producto
curl -X PUT http://localhost:8081/api/productos/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "nombre": "Producto Actualizado",
    "precio": 89.99
  }'

# Eliminar producto
curl -X DELETE http://localhost:8081/api/productos/1 \
  -H "Authorization: Bearer $JWT_TOKEN"
```

#### Consulta (Lectura)
```bash
# Obtener todos los productos
curl http://localhost:8082/api/productos

# Buscar por ID
curl http://localhost:8082/api/productos/1

# Filtrar por categoría
curl "http://localhost:8082/api/productos?categoria=Electronics"

# Búsqueda paginada
curl "http://localhost:8082/api/productos?page=0&size=10&sort=precio,desc"
```

### 3. **Monitoreo con Kafdrop**

#### Verificar eventos en Kafka
1. **Acceder a Kafdrop:** http://localhost:9000
2. **Ver topics:** Deberías ver topics como `producto-eventos`
3. **Examinar mensajes:** Click en topic → Messages
4. **Monitorear consumers:** Ver consumer groups y lag

#### Topics esperados:
- `producto-eventos` - Eventos de dominio
- `producto-commands` - Comandos enviados
- `producto-snapshots` - Snapshots de estado

### 4. **Testing de APIs**

#### Postman Collection
```json
{
  "info": { "name": "Microservicios API" },
  "item": [
    {
      "name": "Health Check Command",
      "request": {
        "method": "GET",
        "url": "http://localhost:8081/actuator/health"
      }
    },
    {
      "name": "Create Product",
      "request": {
        "method": "POST",
        "url": "http://localhost:8081/api/productos",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "raw": "{\n  \"nombre\": \"Test Product\",\n  \"precio\": 99.99\n}"
        }
      }
    }
  ]
}
```

## 🔧 Comandos Útiles

### Gestión del Stack
```bash
# Iniciar aplicaciones
./manage-stack.sh start apps

# Ver logs en tiempo real
docker-compose -f docker-compose-apps.yml logs -f

# Restart específico
docker-compose -f docker-compose-apps.yml restart comando-microservicio

# Escalar servicios
docker-compose -f docker-compose-apps.yml up -d --scale consulta-microservicio=3
```

### Debugging de Aplicaciones
```bash
# Ver logs específicos
docker logs comando-microservicio -f
docker logs consulta-microservicio -f

# Acceder al container
docker exec -it comando-microservicio bash

# Ver métricas de JVM
curl http://localhost:8081/actuator/metrics/jvm.memory.used

# Ver configuración
curl http://localhost:8081/actuator/configprops
```

### Base de Datos
```bash
# Verificar conexión a PostgreSQL (Comando)
docker exec comando-microservicio \
  curl -f "jdbc:postgresql://postgres:5432/productos_db"

# Verificar conexión a MongoDB (Consulta) 
docker exec consulta-microservicio \
  mongosh mongodb://mongo:27017/productos_db --eval "db.adminCommand('ping')"
```

### Kafka Integration
```bash
# Producir evento manual
docker exec kafka kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic producto-eventos

# Consumir eventos
docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic producto-eventos \
  --from-beginning
```

## 🔍 Troubleshooting

### Problemas Comunes

1. **Microservicio no inicia**
   ```bash
   # Verificar logs
   docker logs comando-microservicio
   
   # Verificar conexión a BD
   docker exec comando-microservicio nc -zv postgres 5432
   
   # Verificar variables de entorno
   docker exec comando-microservicio env | grep SPRING
   ```

2. **Error de conexión a base de datos**
   ```bash
   # Verificar que base stack esté corriendo
   docker ps | grep postgres
   docker ps | grep mongo
   
   # Test conectividad
   docker exec postgres pg_isready -U admin -d productos_db
   ```

3. **Frontend no carga**
   ```bash
   # Verificar logs de nginx
   docker logs frontend
   
   # Verificar archivos estáticos
   docker exec frontend ls -la /usr/share/nginx/html
   ```

4. **JWT Token inválido**
   ```bash
   # Generar nuevo token para testing
   curl -X POST http://localhost:8081/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username": "admin", "password": "admin"}'
   ```

5. **Alto uso de memoria**
   ```bash
   # Monitorear recursos
   docker stats comando-microservicio consulta-microservicio
   
   # Ajustar heap size
   # En .env: JAVA_OPTS_COMMAND=-Xms128m -Xmx256m
   ```

## 📊 Monitoreo y Métricas

### Health Checks Disponibles
```bash
# Health general
curl http://localhost:8081/actuator/health

# Health detallado
curl http://localhost:8081/actuator/health/db
curl http://localhost:8081/actuator/health/kafka

# Info de aplicación
curl http://localhost:8081/actuator/info
```

### Métricas Prometheus
```bash
# Métricas HTTP
curl http://localhost:8081/actuator/prometheus | grep http_server_requests

# Métricas JVM
curl http://localhost:8081/actuator/prometheus | grep jvm_memory

# Métricas custom
curl http://localhost:8081/actuator/prometheus | grep productos_created_total
```

### Logging Structure
```json
{
  "@timestamp": "2024-01-15T10:30:00.000Z",
  "level": "INFO",
  "logger_name": "com.ejemplo.ProductoController",
  "message": "Producto creado: ID=123",
  "mdc": {
    "traceId": "abc123",
    "spanId": "def456",
    "userId": "admin"
  }
}
```

## 🏗️ Arquitectura del Sistema

### 📐 **Patrón CQRS Implementado**
```
┌─────────────────┐    ┌─────────────────┐
│    Frontend     │    │   API Gateway   │
│   (Angular)     │◄──►│     (Kong)      │
└─────────────────┘    └─────────────────┘
                                │
                ┌───────────────┼───────────────┐
                │               │               │
        ┌───────▼──────┐       │       ┌──────▼───────┐
        │   Command    │       │       │    Query     │
        │ Microservice │       │       │ Microservice │
        │ (Write Side) │       │       │ (Read Side)  │
        └──────┬───────┘       │       └──────┬───────┘
               │               │              │
        ┌──────▼───────┐       │       ┌──────▼───────┐
        │ PostgreSQL   │       │       │  MongoDB     │
        │ (ACID Writes)│       │       │ (Fast Reads) │
        └──────┬───────┘       │       └──────────────┘
               │               │              ▲
        ┌──────▼───────┐    ┌──▼──────┐      │
        │   Debezium   │───►│  Kafka  │──────┘
        │    (CDC)     │    │ Events  │
        └──────────────┘    └─────────┘
```

### 🔄 **Flujo de Datos**
1. **Frontend** envía comando → **Kong Gateway**
2. **Kong** rutea → **Command Microservice**
3. **Command** valida y persiste → **PostgreSQL**
4. **Debezium** captura cambios → **Kafka**
5. **Query Microservice** consume eventos → actualiza **MongoDB**
6. **Frontend** consulta datos → **Query Microservice**

## 🔗 Dependencias

### Prerequisitos:
- `docker-compose-base.yml` (PostgreSQL, MongoDB, Kafka)
- `docker-compose-gateway.yml` (Kong API Gateway)

### Opcional pero recomendado:
- `docker-compose-observability.yml` (métricas y logs)
- `docker-compose-identity.yml` (autenticación con Keycloak)

## 🔐 Seguridad

### JWT Configuration
```yaml
# application.yml
jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION}
  
security:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS}
    allowed-methods: ${CORS_ALLOWED_METHODS}
```

### API Security Headers
```bash
# Verificar headers de seguridad
curl -I http://localhost:8081/api/productos

# Headers esperados:
# X-Content-Type-Options: nosniff
# X-Frame-Options: DENY
# X-XSS-Protection: 1; mode=block
```

## 📚 Enlaces Útiles

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Angular Documentation](https://angular.io/docs)
- [CQRS Pattern](https://martinfowler.com/bliki/CQRS.html)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/actuator-api/htmlsingle/)
- [Kafka Integration](https://spring.io/projects/spring-kafka)