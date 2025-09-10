
package pe.gob.onp.arquitectura.dms.alfresco.dto;

import java.util.Map;

public record NodeBodyCreate(
        String name,
        String nodeType,
        Map<String, Object> properties
) {
}
