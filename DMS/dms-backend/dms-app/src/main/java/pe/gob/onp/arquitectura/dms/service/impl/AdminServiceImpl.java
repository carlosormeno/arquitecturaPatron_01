
package pe.gob.onp.arquitectura.dms.service.impl;

import pe.gob.onp.arquitectura.dms.api.dto.AdminDtos.*;
import pe.gob.onp.arquitectura.dms.service.AdminService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AdminServiceImpl implements AdminService {
    @Override
    public PageTemplate listTemplates() {
        Template t = new Template("EXP-BASE", 1, "Base", "PUBLISHED", new Scope(null, null),
                "EXP-{yyyy}-{seq}", List.of("00-Entrada", "10-Trámite", "20-Resoluciones", "90-Cierre"),
                List.of("expedienteId", "titulo", "area", "confidencialidad", "fechaApertura", "tenantId"),
                Map.of("maxDepth", 5), "system", OffsetDateTime.now(), OffsetDateTime.now());
        return new PageTemplate(List.of(t), 0, 20, 1);
    }

    @Override
    public Template getTemplate(String id) {
        return listTemplates().content().get(0);
    }

    @Override
    public Template duplicateTemplate(String id) {
        return listTemplates().content().get(0);
    }

    @Override
    public Template resolve(String tenantId, String businessUnit) {
        return listTemplates().content().get(0);
    }

    @Override
    public RetentionPolicy retention() {
        return new RetentionPolicy(Map.of("GENERAL", Map.of("years", 5)), "only_when_retention_expired_and_expediente_closed");
    }

    @Override
    public RetentionPolicy updateRetention(RetentionPolicy p) {
        return p;
    }

    @Override
    public AuditIntegrity integrity() {
        return new AuditIntegrity("hash", "2025-09-08T00:00:00Z", false);
    }
}
