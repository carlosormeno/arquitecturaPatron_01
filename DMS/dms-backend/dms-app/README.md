
# DMS Backend Skeleton (Java 21 / Spring Boot 3.5)
Este esqueleto incluye controladores, DTOs y servicios *stub* para arrancar el desarrollo del backend DMS.
## Ejecutar
mvn spring-boot:run
## Endpoints base
- POST /expedientes
- GET /expedientes/{id}
- POST /documentos/upload
- GET /documentos/{id}
- POST /documentos/{id}/workflow
- GET /admin/templates
- GET /admin/templates/{id}
- POST /admin/templates/{id}/duplicate
- GET /admin/templates/resolve
- GET /admin/policies/retention
- PUT /admin/policies/retention
- GET /admin/audit/integrity
