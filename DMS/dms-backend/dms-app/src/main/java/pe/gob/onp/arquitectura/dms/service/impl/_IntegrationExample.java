
package pe.gob.onp.arquitectura.dms.service.impl;

import org.springframework.stereotype.Service;
import pe.gob.onp.arquitectura.dms.alfresco.AlfrescoClient;
import pe.gob.onp.arquitectura.dms.api.dto.ExpedienteDtos.*;

import java.util.Map;

@Service
public class _IntegrationExample {
    private final AlfrescoClient alfresco;

    public _IntegrationExample(AlfrescoClient alfresco) {
        this.alfresco = alfresco;
    }

    public View demoCreateExpediente(String parentId) {
        var folder = alfresco.createFolder(parentId, "EXP-2025-0001", Map.of("cm:title", "Demo expediente"));
        var node = folder.entry();
        var ex = new Expediente(node.id(), node.name(), "Demo expediente", "VIGENTE", Map.of());
        var page = new PageDocumento(java.util.List.of(), 0, 20, 0);
        return new View(ex, page);
    }
}
