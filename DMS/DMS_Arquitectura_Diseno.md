
# Diseño Arquitectónico — Document Management Service (DMS)

## Índice
1. Contexto General
2. ADR-001 — Capa Intermedia
3. ADR-002 — Estrategia de Permisos
4. ADR-003 — Metadatos & Plantillas
5. ADR-004 — Políticas, Configuración Dinámica & Anti-Tampering
6. OpenAPI v1 (resumen)
7. Checklist de Despliegue
8. Métricas de Éxito

---

# 1. Contexto General
Proyecto de arquitectura TI con Docker en Linux, que integra Alfresco Community 25.2, Keycloak 26.0.5, Kong, NGINX Edge, microservicios en Spring Boot, PostgreSQL, MongoDB, Kafka/Zookeeper y observabilidad con Prometheus, Grafana, Jaeger y cAdvisor.

Decisión central: introducir un **Document Management Service (DMS)** como capa intermedia de dominio frente a Alfresco, detrás de Kong+Keycloak, con contratos propios, auditoría robusta y soporte de multi-plantillas.

---

# 2. ADR-001 — Capa Intermedia
**Estado:** Aceptado (v1)  
**Contexto:** Necesitamos exponer capacidades documentales sin acoplar frontends a APIs verbosas de Alfresco.  
**Decisión:** Crear el DMS como fachada de dominio; Alfresco queda como backend puro.  
**Consecuencias:** + Control, APIs limpias, observabilidad. – Operar un servicio adicional.

---

# 3. ADR-002 — Estrategia de Permisos
**Estado:** Aceptado (v1)  
**Decisión:** Estrategia híbrida: enforcement en DMS, reflejo opcional en Alfresco (carpeta raíz de expediente con herencia).  
**Consecuencias:** + Coherencia en frontends; – Gobernar ACLs reflejadas.

---

# 4. ADR-003 — Metadatos & Plantillas
**Estado:** Aceptado (v1)  

### Metadatos de Expediente
- expedienteId, titulo, area, estado, fechaApertura, fechaCierre, confidencialidad, owner, tenantId, plantillaId.

### Metadatos de Documento
- expedienteRef, tipoDoc, hash, cifrado, retencionCategoria, fechaRetencion, estadoWf.

### Plantillas
- Default global y overrides por tenantId/businessUnit.  
- Versionadas, con estados (DRAFT/PUBLISHED/DEPRECATED).

---

# 5. ADR-004 — Políticas, Configuración Dinámica & Anti-Tampering
**Estado:** Aceptado (v2)  

### Controles Anti-DBA
- Roles mínimos (`app_role`, `dba_readonly`).  
- Auditoría append-only con triggers y hash encadenado.  
- Anclaje periódico en storage inmutable.  
- Observabilidad (`pgaudit`, métricas).  
- Cambios en políticas/backups vía UI/API con versionado.

### Políticas
- Retención editable (por UI).  
- Renditions: thumbnails + pdf preview (on-demand).  
- Backups editables por UI con dual-approval.  
- Signed links (expiran en 24h por defecto).

---

# 6. OpenAPI v1 (resumen)
### Endpoints principales
- `/expedientes` (POST/GET)  
- `/expedientes/{id}/documentos` (POST)  
- `/documentos/{id}` (GET)  
- `/busqueda` (GET)  
- `/documentos/{id}/workflow` (POST)  
- `/documentos/{id}/auditoria` (GET)  
- `/admin/templates` (GET/POST)  
- `/admin/templates/{id}` (GET)  
- `/admin/templates/{id}/duplicate` (POST)  
- `/admin/templates/resolve` (GET)  
- `/admin/policies/retention` (GET/PUT)  
- `/admin/policies/backups` (GET/PUT)  
- `/admin/audit/integrity` (GET)

Swagger UI disponible con `bearerAuth` y (opcional) `oauth2`.

---

# 7. Checklist de Despliegue
1. Crear roles de DB (`app_role`, `dba_readonly`).  
2. REVOKE UPDATE/DELETE en tablas `audit.*`.  
3. Triggers anti-update/delete en `audit.*`.  
4. Implementar hash encadenado + Merkle root.  
5. Configurar anclaje externo de auditoría.  
6. Activar `pgaudit` y scraping Prometheus.  
7. Parametrizar jobs de backup según políticas.  
8. Configurar rutas NGINX/Kong para `/api/documents/*`.

---

# 8. Métricas de Éxito
- % de expedientes conformes a plantilla.  
- Tiempo de subida/búsqueda p90 dentro de SLOs.  
- 0 cambios en políticas sin rastro en auditoría.  
- 0 mismatches en verificación de integridad.  
- Restore tests exitosos trimestralmente.

---

**Fin del Documento**
