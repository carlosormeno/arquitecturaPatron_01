
package pe.gob.onp.arquitectura.dms.api.dto;
import jakarta.validation.constraints.NotBlank;
import java.util.List; import java.util.Map;
public class ExpedienteDtos {
  public record CreateRequest(
      String plantillaId,
      String businessUnit,
      @NotBlank String titulo,
      @NotBlank String area,
      @NotBlank String confidencialidad,
      Map<String,String> metadatos){}
  public record Expediente(String expedienteId, String titulo, String estado, Map<String,String> metadatos){}
  public record View(Expediente expediente, PageDocumento documentos){}
  public record PageDocumento(List<Documento> content, int page, int size, long totalElements){}
  public record Documento(String nodeId, String nombre, String tipoDoc, String estadoWf, boolean cifrado){}
}
