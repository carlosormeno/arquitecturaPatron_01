
package pe.gob.onp.arquitectura.dms.api.dto;
import java.time.OffsetDateTime;
import java.util.List; import java.util.Map;
public class AdminDtos {
  public record Template(String id, int version, String title, String state,
                         Scope scope, String idPattern, List<String> folders,
                         List<String> metadataRequired, Map<String,Object> constraints,
                         String owner, OffsetDateTime createdAt, OffsetDateTime updatedAt){}
  public record Scope(String tenantId, String businessUnit){}
  public record PageTemplate(List<Template> content, int page, int size, long totalElements){}
  public record RetentionPolicy(Map<String,Object> categories, String deletePolicy){}
  public record AuditIntegrity(String lastRootHash, String lastAnchoredAt, boolean mismatchesDetected){}
}
