
# Roadmap de Implementación — Document Management Service (DMS)

## 🗺️ Vista General
Plan dividido en 4 sprints (1–2 semanas cada uno). Al final de Sprint 3 se espera tener un MVP funcional del DMS; Sprint 4 agrega endurecimiento y extras.

---

## 🔹 Sprint 1 — Base técnica (Infraestructura + Esqueleto)
**Duración:** 1 semana  
**Objetivo:** Dejar la capa DMS lista para compilar y desplegar, con seguridad e infraestructura básica.

### Tareas
- Crear proyecto Spring Boot DMS (starter web, security, actuator, prometheus, openapi).  
- Configurar OAuth2 Resource Server → integración con Keycloak vía Kong.  
- Publicar endpoints de health, metrics y Swagger UI.  
- Integrar con Prometheus/Grafana y Jaeger.  
- Dockerizar el servicio y conectarlo a la red `apps`.  
- Configurar NGINX/Kong para enrutar `/api/documents/*` al DMS.  
- Crear roles de DB (`app_role`, `dba_readonly`) y esquema `audit`.

✅ **Entregable:** DMS desplegado en Docker, protegido por JWT, visible en Swagger, con métricas y health.

---

## 🔹 Sprint 2 — Funcionalidades núcleo
**Duración:** 2 semanas  
**Objetivo:** Tener los endpoints clave del MVP operativos.

### Tareas
- Implementar `POST /expedientes` (crear expediente según plantilla).  
- Implementar `POST /expedientes/{id}/documentos` (subida de documento).  
- Implementar `GET /expedientes/{id}` (vista agregada de expediente).  
- Implementar `GET /documentos/{id}` (metadatos + links de preview/descarga).  
- Integrar con Alfresco vía cuenta técnica (REST v1).  
- Validar metadatos obligatorios y nomenclatura en la capa.  
- Auditoría de operaciones (`audit_event` con hash encadenado).  
- Primer tópico Kafka: `document.uploaded`.  

✅ **Entregable:** Flujo básico expediente → documento → búsqueda funcionando. Auditoría registrada y verificada.

---

## 🔹 Sprint 3 — Gobernanza y workflows
**Duración:** 2 semanas  
**Objetivo:** Cerrar el MVP con control de plantillas, políticas y workflow ligero.

### Tareas
- Implementar `GET /busqueda` (consulta paginada por metadatos/estado).  
- Implementar workflow simple: `BORRADOR → EN_REVISION → APROBADO/RECHAZADO`.  
- Implementar `GET /documentos/{id}/auditoria`.  
- Implementar `GET/POST /admin/templates` y `GET /admin/templates/{id}`.  
- Implementar `POST /admin/templates/{id}/duplicate` y `GET /admin/templates/resolve`.  
- Implementar `GET/PUT /admin/policies/retention`.  
- Implementar `GET /admin/audit/integrity`.  
- Segundo y tercer tópico Kafka: `expediente.created`, `document.statusChanged`.  

✅ **Entregable:** MVP del DMS listo: gestión de expedientes/documentos, auditoría, plantillas, políticas y eventos.

---

## 🔹 Sprint 4 — Endurecimiento y extras
**Duración:** 2–3 semanas  
**Objetivo:** Preparar el sistema para producción piloto.

### Tareas
- Integrar renditions on-demand (`pdf`, `docx`, `pptx`, `tiff`).  
- Activar signed links con expiración (descarga externa).  
- Endpoints de administración de backups (`GET/PUT /admin/policies/backups`).  
- Anclaje externo de auditorías (Merkle root → S3/logs inmutables).  
- Alertas en Grafana (errores, auditoría, integridad).  
- Prueba de restore de backups.  
- Documentación para usuarios de front.  

✅ **Entregable:** Sistema robusto, con integridad de auditoría garantizada, seguridad extendida y observabilidad completa.

---

## 📊 Cronograma resumido
- **Sprint 1 (Semana 1):** Infra + esqueleto DMS.  
- **Sprint 2 (Semanas 2–3):** Funcionalidades núcleo (expedientes, docs, auditoría inicial).  
- **Sprint 3 (Semanas 4–5):** Plantillas, políticas, workflow, auditoría extendida.  
- **Sprint 4 (Semanas 6–8):** Renditions, signed links, backups, endurecimiento.

---

**Fin del Roadmap**
