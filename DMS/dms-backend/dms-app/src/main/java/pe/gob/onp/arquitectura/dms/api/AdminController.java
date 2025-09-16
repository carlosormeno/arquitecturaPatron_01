
package pe.gob.onp.arquitectura.dms.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.gob.onp.arquitectura.dms.api.dto.AdminDtos.*;
import pe.gob.onp.arquitectura.dms.service.AdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final AdminService svc;
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    public AdminController(AdminService s) {
        this.svc = s;
    }

    @GetMapping("/templates")
    @PreAuthorize("hasRole('DMS_ADMIN')")
    public PageTemplate listTemplates() {
        log.info("GET /admin/templates - Listando plantillas");

        try {
            PageTemplate result = svc.listTemplates();
            log.info("Plantillas listadas exitosamente: {} plantillas encontradas",
                    result.totalElements());
            return result;
        } catch (Exception e) {
            log.error("Error en controller al listar plantillas: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/templates/{id}")
    @PreAuthorize("hasRole('DMS_ADMIN')")
    public Template getTemplate(@PathVariable("id") String id) {
        log.info("GET /admin/templates/{} - Consultando plantilla", id);

        try {
            Template result = svc.getTemplate(id);
            log.info("Plantilla consultada exitosamente: ID='{}', nombre='{}'",
                    result.id(), result.title());
            return result;
        } catch (Exception e) {
            log.error("Error en controller al consultar plantilla '{}': {}",
                    id, e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/templates/{id}/duplicate")
    @PreAuthorize("hasRole('DMS_ADMIN')")
    public Template duplicate(@PathVariable("id") String id) {
        log.info("POST /admin/templates/{}/duplicate - Duplicando plantilla", id);

        try {
            Template result = svc.duplicateTemplate(id);
            log.info("Plantilla duplicada exitosamente: original='{}', nueva='{}'",
                    id, result.id());
            return result;
        } catch (Exception e) {
            log.error("Error en controller al duplicar plantilla '{}': {}",
                    id, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/templates/resolve")
    @PreAuthorize("hasAnyRole('DMS_ADMIN','EXPEDIENTE_ADMIN')")
    public Template resolve(String tenantId, String businessUnit) {
        log.info("GET /admin/templates/resolve - Resolviendo plantilla: tenantId='{}', businessUnit='{}'",
                tenantId, businessUnit);

        try {
            Template result = svc.resolve(tenantId, businessUnit);
            log.info("Plantilla resuelta exitosamente: ID='{}', título='{}'",
                    result.id(), result.title());
            return result;
        } catch (Exception e) {
            log.error("Error en controller al resolver plantilla para tenant '{}', businessUnit '{}': {}",
                    tenantId, businessUnit, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/policies/retention")
    @PreAuthorize("hasRole('DMS_ADMIN')")
    public RetentionPolicy retention() {
        log.info("GET /admin/policies/retention - Consultando política de retención");

        try {
            RetentionPolicy result = svc.retention();
            log.info("Política de retención consultada exitosamente");
            log.debug("Política de retención: {}", result);
            return result;
        } catch (Exception e) {
            log.error("Error en controller al consultar política de retención: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PutMapping("/policies/retention")
    @PreAuthorize("hasRole('DMS_ADMIN')")
    public RetentionPolicy updateRetention(@RequestBody RetentionPolicy p) {
        log.info("PUT /admin/policies/retention - Actualizando política de retención");
        log.debug("Nueva política: {}", p);

        try {
            RetentionPolicy result = svc.updateRetention(p);
            log.info("Política de retención actualizada exitosamente");
            return result;
        } catch (Exception e) {
            log.error("Error en controller al actualizar política de retención: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/audit/integrity")
    @PreAuthorize("hasRole('DMS_ADMIN')")
    public AuditIntegrity integrity() {
        log.info("GET /admin/audit/integrity - Consultando auditoría de integridad");

        try {
            AuditIntegrity result = svc.integrity();
            log.info("Auditoría de integridad consultada exitosamente: mismatchesDetected={}",
                    result.mismatchesDetected());
            return result;
        } catch (Exception e) {
            log.error("Error en controller al consultar auditoría de integridad: {}", e.getMessage(), e);
            throw e;
        }
    }
}
