# 📦 Stack Base - Infraestructura Fundamental

## 🎯 Descripción General

El **docker-compose-base.yml** contiene la infraestructura fundamental que sustenta todo el stack tecnológico. Incluye bases de datos principales, sistema de mensajería y servicios de integración core.

## 🏗️ Servicios Incluidos

### 📊 **Bases de Datos**
- **PostgreSQL** - Base de datos relacional principal
- **MongoDB** - Base de datos NoSQL para consultas
- **Kong-DB** - Base de datos para API Gateway

### 📨 **Sistema de Mensajería**
- **Apache Zookeeper** - Coordinación distribuida
- **Apache Kafka** - Streaming de eventos
- **Debezium** - Change Data Capture (CDC)

### 💾 **Servicios de Soporte**
- **PostgreSQL Backup** - Respaldos automáticos

## 🔌 Puertos Expuestos

| Servicio | Puerto | Protocolo | Descripción |
|----------|--------|-----------|-------------|
| PostgreSQL | 5432 | TCP | Base de datos principal |
| MongoDB | 27017 | TCP | Base de datos NoSQL |
| Kong-DB | 5433 | TCP | Base de datos Kong |
| Zookeeper | 2181 | TCP | Coordinación Kafka |
| Kafka | 9092 | TCP | Kafka (Host) |
| Kafka | 29092 | TCP | Kafka (Containers) |
| Kafka JMX | 9999 | TCP | Métricas JMX |
| Debezium | 8083 | TCP | REST API |

## 🔑 Accesos por Defecto

### PostgreSQL Principal
- **Host:** localhost:5432
- **Usuario:** admin
- **Password:** admin123
- **Base de datos:** productos_db

### MongoDB
- **Host:** localhost:27017
- **Usuario:** mongouser
- **Password:** M0ng0_S3cur3_P4ssw0rd_2024!

### Kong Database
- **Host:** localhost:5433
- **Usuario:** kong
- **Password:** kongpass
- **Base de datos:** kong

### Debezium Connect
- **API REST:** http://localhost:8083
- **Health Check:** http://localhost:8083/connectors

## ⚙️ Variables de Entorno Requeridas

```bash
# Base de datos principal
POSTGRES_DB=productos_db
POSTGRES_USER=admin
POSTGRES_PASSWORD=admin123
POSTGRES_PORT=5432

# Kong
KONG_DB_NAME=kong
KONG_DB_USER=kong
KONG_DB_PASSWORD=kongpass

# Kafka
KAFKA_BROKER_ID=1
KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181

# Resource limits
CPU_LIMIT_SMALL=0.25
CPU_LIMIT_MEDIUM=0.5
CPU_LIMIT_LARGE=1.0
CPU_LIMIT_XLARGE=2.0
MEMORY_LIMIT_XSMALL=128m
MEMORY_LIMIT_SMALL=256m
MEMORY_LIMIT_MEDIUM=512m
MEMORY_LIMIT_LARGE=1g
MEMORY_LIMIT_XLARGE=2g

# Backup
BACKUP_ENABLED=true
BACKUP_RETENTION_DAYS=7
```

## 🚀 Casos de Uso

### 1. **Desarrollo de Aplicaciones**
```bash
# Levantar stack base para desarrollo
./manage-stack.sh start base

# Conectar aplicación a PostgreSQL
jdbc:postgresql://localhost:5432/productos_db

# Conectar aplicación a MongoDB
mongodb://mongouser:password@localhost:27017/productos_db
```

### 2. **Integración de Datos con Debezium**
```bash
# Configurar conector PostgreSQL
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "postgres-connector",
    "config": {
      "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
      "database.hostname": "postgres",
      "database.port": "5432",
      "database.user": "admin",
      "database.password": "admin123",
      "database.dbname": "productos_db",
      "database.server.name": "productos"
    }
  }'
```

### 3. **Mensajería con Kafka**
```bash
# Listar topics
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# Crear topic
docker exec kafka kafka-topics --bootstrap-server localhost:9092 \
  --create --topic eventos-productos --partitions 3 --replication-factor 1

# Producir mensajes
docker exec -it kafka kafka-console-producer \
  --bootstrap-server localhost:9092 --topic eventos-productos

# Consumir mensajes
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 --topic eventos-productos --from-beginning
```

## 🔧 Comandos Útiles

### Gestión del Stack
```bash
# Iniciar stack base
./manage-stack.sh start base

# Ver estado
./manage-stack.sh status

# Ver logs
docker-compose -f docker-compose-base.yml logs -f

# Detener stack
./manage-stack.sh stop base
```

### Administración de Bases de Datos
```bash
# Conectar a PostgreSQL
docker exec -it postgres psql -U admin -d productos_db

# Conectar a MongoDB
docker exec -it mongodb mongosh -u mongouser -p

# Backup manual de PostgreSQL
docker exec postgres pg_dump -U admin -d productos_db > backup.sql

# Restaurar PostgreSQL
docker exec -i postgres psql -U admin -d productos_db < backup.sql
```

### Monitoreo de Kafka
```bash
# Estado del cluster
docker exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092

# Información de topics
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --describe

# Consumer groups
docker exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list
```

## ⚠️ CONFIGURACIÓN INICIAL DE KONG

### 🔑 **Migraciones de Kong (OBLIGATORIO)**

Kong requiere inicializar su base de datos PostgreSQL ejecutando las migraciones **SOLO UNA VEZ** antes de levantar el stack de gateway.

```bash
# ⚠️ EJECUTAR ESTE COMANDO MANUALMENTE CUANDO SE CREE LA BASE DE DATOS POR PRIMERA VEZ:
docker run --rm \
  --network backend_network \
  -e KONG_DATABASE=postgres \
  -e KONG_PG_HOST=kong-db \
  -e KONG_PG_PASSWORD=kongpass \
  kong:3.6 kong migrations bootstrap
```

### 📋 **Notas Importantes:**

1. **Solo ejecutar UNA VEZ** por instalación nueva
2. **Ejecutar antes** de levantar `docker-compose-gateway.yml`
3. **NO incluir** en docker-compose.yml como servicio persistente
4. **Para actualizaciones** de versión mayor (ej: 3.x → 4.x), usar:
   ```bash
   docker run --rm \
     --network backend_network \
     -e KONG_DATABASE=postgres \
     -e KONG_PG_HOST=kong-db \
     -e KONG_PG_PASSWORD=kongpass \
     kong:4.x kong migrations up
   ```

### 🔄 **Orden de Ejecución Recomendado:**

```bash
# 1. Levantar infraestructura base
docker compose -f docker-compose-base.yml up -d

# 2. Esperar que kong-db esté saludable
docker compose -f docker-compose-base.yml ps kong-db

# 3. Ejecutar migraciones de Kong (SOLO UNA VEZ)
docker run --rm \
  --network backend_network \
  -e KONG_DATABASE=postgres \
  -e KONG_PG_HOST=kong-db \
  -e KONG_PG_PASSWORD=kongpass \
  kong:3.6 kong migrations bootstrap

# 4. Ahora sí levantar el gateway
docker compose -f docker-compose-gateway.yml up -d
```

## 🔍 Troubleshooting

### Problemas Comunes

1. **PostgreSQL no inicia**
   ```bash
   # Verificar logs
   docker logs postgres
   
   # Verificar permisos de volúmenes
   sudo chown -R 999:999 ./data/postgres
   ```

2. **Kafka no se conecta a Zookeeper**
   ```bash
   # Verificar que Zookeeper esté corriendo
   docker exec zookeeper echo stat | nc localhost 2181
   
   # Restart Kafka si es necesario
   docker-compose -f docker-compose-base.yml restart kafka
   ```

3. **Debezium no puede conectar a PostgreSQL**
   ```bash
   # Verificar configuración de replicación
   docker exec postgres psql -U admin -d productos_db \
     -c "SHOW wal_level;"
   ```

## 📊 Monitoreo y Métricas

### Health Checks Disponibles
- **PostgreSQL:** `pg_isready -U admin -d productos_db`
- **MongoDB:** `mongosh --eval "db.adminCommand('ping')"`
- **Kafka:** `kafka-broker-api-versions --bootstrap-server localhost:9092`
- **Debezium:** `curl http://localhost:8083/connectors`

### Métricas JMX (Kafka)
- **URL:** `service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi`
- **Métricas disponibles:** Throughput, latencia, consumer lag, etc.

## 🔐 Consideraciones de Seguridad

1. **Cambiar passwords por defecto** en producción
2. **Configurar SSL/TLS** para conexiones externas
3. **Restringir acceso a puertos** usando firewall
4. **Habilitar autenticación** en Kafka
5. **Configurar backups cifrados**

## 📈 Escalabilidad

### Para mayor carga:
1. **Aumentar particiones** en Kafka
2. **Configurar cluster** de MongoDB
3. **Pool de conexiones** en PostgreSQL
4. **Ajustar resource limits** según hardware

## 🔗 Dependencias

Este stack es **prerequisito** para:
- `docker-compose-apps.yml` (requiere bases de datos)
- `docker-compose-observability.yml` (métricas de Kafka)
- `docker-compose-gateway.yml` (requiere Kong-DB)

## 📚 Enlaces Útiles

- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [MongoDB Manual](https://docs.mongodb.com/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Debezium Documentation](https://debezium.io/documentation/)
- [Apache Zookeeper Guide](https://zookeeper.apache.org/doc/)