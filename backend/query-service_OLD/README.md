### Query Service - Spring Boot + Kafka + MongoDB

## 🌱 Paso 1: Crear proyecto base

Puedes usar [Spring Initializr](https://start.spring.io/) con las siguientes configuraciones:

- **Project**: Maven
- **Language**: Java
- **Spring Boot**: 3.2.x o 3.5.x
- **Packaging**: Jar
- **Java**: 21

### 📦 Dependencias:
- Spring Web
- Spring for Apache Kafka
- Spring Data MongoDB
- Spring Boot DevTools (opcional)
- Lombok
- Actuator (para observabilidad)

---

## 📁 Estructura de paquetes sugerida

```
src/main/java/com/example/query
├── application
│   └── service
├── domain
│   └── model
├── infrastructure
│   ├── config
│   ├── kafka
│   └── persistence
├── controller
└── QueryServiceApplication.java
```