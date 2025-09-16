
package pe.gob.onp.arquitectura.dms.alfresco.dto;

/*public record SearchRequest(
        Query query,
        Integer maxItems,
        Integer skipCount
) {
    public record Query(String query) {
    }
}*/

public record SearchRequest(Query query, Paging paging) {
    public record Query(String query) {
    }

    public record Paging(int skipCount, int maxItems) {
    }
}
