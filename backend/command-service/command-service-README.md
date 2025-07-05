# ✳️ Microservicio: command-service

Servicio de escritura que expone una API REST para insertar productos. Los productos se almacenan en PostgreSQL, y los eventos de inserción son capturados por Debezium para ser enviados a Kafka.

## 📦 Estructura del Proyecto

```
command-service/
├── application/
├── config/
├── domain/
├── infrastructure/
└── resources/
```

- Arquitectura Hexagonal (puertos y adaptadores)
- CQRS (solo comandos)
- Sin lectura de datos, solo escritura

## 🧭 Flujo general

1. Cliente Angular envía `POST /api/productos`
2. `ProductoController` recibe el `CrearProductoCommand`
3. Se transforma a objeto de dominio `Producto`
4. `CrearProductoUseCase` ejecuta el caso de uso
5. `GuardarProductoPort` persiste vía `ProductoJpaAdapter`
6. Se guarda en PostgreSQL
7. Debezium detecta el cambio y lo envía a Kafka

## 📲 Endpoint de prueba

```http
POST http://localhost:8081/api/productos
Content-Type: application/json

{
  "id": "P001",
  "nombre": "Laptop Dell",
  "precio": 3500.00
}
```

## ✅ Respuesta esperada:

```json
{
  "id": "P001",
  "nombre": "Laptop Dell",
  "precio": 3500.00
}
```

## 🔍 Visualización en Kafka (Kafdrop)

- Navega a `http://localhost:9000`
- Verifica el topic: `dbserver1.public.producto`

## 📊 Observabilidad

- Prometheus: [http://localhost:9090](http://localhost:9090)
- Grafana: [http://localhost:3000](http://localhost:3000)
- Jaeger: [http://localhost:16686](http://localhost:16686)

## 🧼 Limpieza del entorno

```bash
docker compose down -v
```

Esto eliminará contenedores y volúmenes persistentes (incluida la base de datos).
