package pe.gob.onp.arquitectura.dms.alfresco.dto;

import java.util.List;

/**
 * Mapea:
 * GET /nodes/{parentId}/children?where=(name='...')&include=properties
 * <p>
 * {
 * "list": {
 * "entries": [ { "entry": { "id": "...", "name": "...", "properties": { ... } } } ]
 * }
 * }
 */
public class NodeChildrenList {
    private ListPart list;

    public ListPart getList() {
        return list;
    }

    public void setList(ListPart list) {
        this.list = list;
    }

    public static class ListPart {
        private List<EntryWrapper> entries;

        public List<EntryWrapper> getEntries() {
            return entries;
        }

        public void setEntries(List<EntryWrapper> entries) {
            this.entries = entries;
        }
    }

    public static class EntryWrapper {
        // OJO: usamos directamente el "inner record" NodeEntry.Node
        private NodeEntry.Node entry;

        public NodeEntry.Node getEntry() {
            return entry;
        }

        public void setEntry(NodeEntry.Node entry) {
            this.entry = entry;
        }
    }
}
