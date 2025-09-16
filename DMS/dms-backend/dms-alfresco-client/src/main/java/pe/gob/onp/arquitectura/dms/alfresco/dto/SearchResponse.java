
package pe.gob.onp.arquitectura.dms.alfresco.dto;

import java.util.List;
import java.util.Map;

public record SearchResponse(SearchList list) {
    public record SearchList(List<Entry> entries) {
    }

    public record Entry(Node entry) {
    }

    public record Node(String id, String name, String nodeType, Map<String, Object> properties) {
    }
}
