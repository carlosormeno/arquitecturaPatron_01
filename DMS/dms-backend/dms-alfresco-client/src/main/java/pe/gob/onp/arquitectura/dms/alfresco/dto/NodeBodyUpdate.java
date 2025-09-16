package pe.gob.onp.arquitectura.dms.alfresco.dto;

import java.util.Map;

public record NodeBodyUpdate(
        String name,
        Map<String, Object> properties
) {}
