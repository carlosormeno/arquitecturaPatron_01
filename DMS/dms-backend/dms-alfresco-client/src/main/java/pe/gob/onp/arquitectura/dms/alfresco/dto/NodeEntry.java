
package pe.gob.onp.arquitectura.dms.alfresco.dto;

import java.util.Map;

public record NodeEntry(Node entry) {
    public record Node(String id, String name, String nodeType,
                       Map<String, Object> properties,
                       Content content // <- nuevo, opcional
    ) {
    }

    public record Content(String mimeType, Long size, String encoding) {
    }
}

