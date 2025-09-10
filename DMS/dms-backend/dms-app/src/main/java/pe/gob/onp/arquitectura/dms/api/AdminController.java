
package pe.gob.onp.arquitectura.dms.api;

import pe.gob.onp.arquitectura.dms.api.dto.AdminDtos.*;
import pe.gob.onp.arquitectura.dms.service.AdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final AdminService svc;

    public AdminController(AdminService s) {
        this.svc = s;
    }

    @GetMapping("/templates")
    @PreAuthorize("hasRole('DMS_ADMIN')")
    public PageTemplate listTemplates() {
        return svc.listTemplates();
    }

    @GetMapping("/templates/{id}")
    @PreAuthorize("hasRole('DMS_ADMIN')")
    public Template getTemplate(@PathVariable("id") String id) {
        return svc.getTemplate(id);
    }

    @PostMapping("/templates/{id}/duplicate")
    @PreAuthorize("hasRole('DMS_ADMIN')")
    public Template duplicate(@PathVariable("id") String id) {
        return svc.duplicateTemplate(id);
    }

    @GetMapping("/templates/resolve")
    @PreAuthorize("hasAnyRole('DMS_ADMIN','EXPEDIENTE_ADMIN')")
    public Template resolve(String tenantId, String businessUnit) {
        return svc.resolve(tenantId, businessUnit);
    }

    @GetMapping("/policies/retention")
    @PreAuthorize("hasRole('DMS_ADMIN')")
    public RetentionPolicy retention() {
        return svc.retention();
    }

    @PutMapping("/policies/retention")
    @PreAuthorize("hasRole('DMS_ADMIN')")
    public RetentionPolicy updateRetention(@RequestBody RetentionPolicy p) {
        return svc.updateRetention(p);
    }

    @GetMapping("/audit/integrity")
    @PreAuthorize("hasRole('DMS_ADMIN')")
    public AuditIntegrity integrity() {
        return svc.integrity();
    }
}
