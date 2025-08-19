# Estructura del Proyecto
```
command-service/
├── src/
│   ├── main/
│   │   ├── java/com/ejemplo/producto/
│   │   │
│   │   ├── CommandServiceApplication.java       # Clase principal Spring Boot
│   │   │
│   │   ├── config/
│   │   │   └── BeansConfig.java                 # Configuración de dependencias
│   │   │
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   └── Producto.java                # Entidad del dominio (pura)
│   │   │   └── ports/
│   │   │       ├── in/
│   │   │       │   └── CrearProductoUseCase.java     # Puerto de entrada
│   │   │       └── out/
│   │   │           └── GuardarProductoPort.java      # Puerto de salida
│   │   │
│   │   ├── application/
│   │   │   ├── service/
│   │   │   │   └── CrearProductoService.java         # Implementación del caso de uso
│   │   │   └── mapper/
│   │   │       └── ProductoMapper.java               # Mapea entre dominio y entidad JPA
│   │   │
│   │   ├── infrastructure/
│   │   │   ├── controller/
│   │   │   │   └── ProductoController.java           # Exposición vía REST
│   │   │   ├── dto/
│   │   │   │   ├── CrearProductoCommand.java         # DTO de entrada
│   │   │   │   └── ProductoResponse.java             # DTO de salida
│   │   │   └── persistence/
│   │   │       ├── ProductoJpaEntity.java           # Entidad JPA
│   │   │       ├── ProductoRepository.java          # Interfaz Spring Data
│   │   │       └── ProductoJpaAdapter.java          # Adaptador de salida
│   │   │
│   │   └── resources/
│   │       └── application.yml                      # Configuración (DB, Actuator, etc.)
│
├── pom.xml                                           # Declaración Maven
```
# Microservicio: command-service
Responsabilidad:
    Exponer una API REST para crear productos
    Guardar esos productos en PostgreSQL
    No se encarga de leer ni consultar productos
    Los cambios hechos se propagan automáticamente vía Debezium → Kafka


## Flujo entre las capas

1. **Cliente Angular** realiza una solicitud `POST /api/productos` con datos JSON.
2. `ProductoController` recibe el `CrearProductoCommand` (DTO).
3. Se construye un objeto de dominio `Producto`.
4. Se invoca el puerto de entrada `CrearProductoUseCase`.
5. `CrearProductoService` ejecuta la lógica del caso de uso.
6. Se llama al puerto de salida `GuardarProductoPort`.
7. `ProductoJpaAdapter` convierte el dominio a entidad (`ProductoJpaEntity`) y guarda en PostgreSQL.
8. Spring ejecuta el SQL de `schema.sql` si no existen las tablas.
9. Si está configurado Debezium, capturará el `INSERT` y lo enviará a Kafka.

---

## Cómo ejecutar el proyecto

1. **Levanta los contenedores**:
   ```bash
   docker-compose up -d
   ```

2. **Ejecuta el microservicio** (`command-service`) desde IntelliJ:
   Ejecuta `CommandServiceApplication.java`

3. **Verifica exposición de métricas**:
   Abre en navegador:
   [http://localhost:8081/actuator/prometheus](http://localhost:8081/actuator/prometheus)

4. **Accede a Prometheus**: [http://localhost:9090](http://localhost:9090)

5. **Accede a Grafana**: [http://localhost:3000](http://localhost:3000)
    - Usuario: `admin`, Contraseña: `admin`
    - Agrega Prometheus como Data Source (`http://prometheus:9090`)
6. Verificar servicios disponibles:
- PostgreSQL → `localhost:5432`
- Kafka → `localhost:9092`
- Debezium → `localhost:8083`
- Prometheus → `localhost:9090`
- Grafana → `localhost:3000`
- Jaeger → `localhost:16686`
- Kafdrop → `localhost:9000`

---

### Crear el conector Debezium para PostgreSQL

#### Opción 1: Usando `curl`

1. Crea el archivo `register-postgres.json` en la raíz del proyecto con el siguiente contenido:

```json
{
  "name": "postgres-productos-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "database.hostname": "postgres",
    "database.port": "5432",
    "database.user": "admin",
    "database.password": "admin123",
    "database.dbname": "productos_db",
    "topic.prefix": "dbserver1",
    "plugin.name": "pgoutput",
    "slot.name": "productos_slot",
    "publication.name": "productos_publication",
    "table.include.list": "public.producto",
    "key.converter": "org.apache.kafka.connect.json.JsonConverter",
    "value.converter": "org.apache.kafka.connect.json.JsonConverter",
    "key.converter.schemas.enable": "false",
    "value.converter.schemas.enable": "false"
  }
}
```

2. Ejecuta el siguiente comando desde Git Bash o consola compatible con `curl`:

```bash
curl -i -X POST http://localhost:8083/connectors \
  -H "Accept: application/json" \
  -H "Content-Type: application/json" \
  -d @register-postgres.json
```

---

#### Opción 2: Usando Postman

1. Método: `POST`
2. URL: `http://localhost:8083/connectors`
3. Headers:
    - `Accept: application/json`
    - `Content-Type: application/json`
4. Body:
    - Tipo: `raw`
    - Formato: `JSON`
    - Contenido: (el mismo que el archivo anterior)

## Ejemplo de uso del API

```http
POST http://localhost:8081/api/productos
Content-Type: application/json

{
  "id": "P001",
  "nombre": "Laptop Dell",
  "precio": 3500.00
}
```

### ✅ Respuesta esperada:
```json
{
  "id": "P001",
  "nombre": "Laptop Dell",
  "precio": 3500.00
}
```

---

### 🔍 Visualizar mensajes Kafka con Kafdrop

1. Asegúrate de tener este bloque en `docker-compose.yml`:

```yaml
  kafdrop:
    image: obsidiandynamics/kafdrop:4.0.0
    container_name: kafdrop
    ports:
      - "9000:9000"
    depends_on:
      - kafka
    environment:
      KAFKA_BROKER_CONNECT: kafka:9092
      JVM_OPTS: "-Xms32M -Xmx64M"
```

2. Levanta los contenedores:

```bash
docker compose up -d
```

3. Abre el navegador en:

```
http://localhost:9000
```

Allí podrás explorar el topic `dbserver1.public.producto` generado por Debezium.

---
2. ¿El conector Debezium está realmente activo?

Abre en el navegador:

http://localhost:8083/connectors

Y confirma que veas:

["postgres-productos-connector"]

Si ves el nombre, accede a:

http://localhost:8083/connectors/postgres-productos-connector/status

Y confirma que diga "state": "RUNNING"
---

### 🧪 Probar microservicio

Puedes hacer un `POST` a la API de productos:

```
POST http://localhost:8081/api/productos
Content-Type: application/json

{
  "nombre": "Tablet",
  "precio": 999.90
}
```

Esto insertará un producto en PostgreSQL y Debezium lo enviará a Kafka automáticamente.

---

### 📊 Observabilidad

- Prometheus en `http://localhost:9090`
- Grafana en `http://localhost:3000` (admin/admin)
- Jaeger en `http://localhost:16686`

---

### 🧼 Limpieza del entorno

```bash
docker compose down -v
```

Esto elimina los contenedores y volúmenes persistentes.

---