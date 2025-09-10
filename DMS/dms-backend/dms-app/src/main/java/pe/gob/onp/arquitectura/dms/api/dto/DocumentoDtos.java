package pe.gob.onp.arquitectura.dms.api.dto;
import java.util.Map;
public class DocumentoDtos {
  public record Documento(String nodeId, String nombre, String tipoDoc, String estadoWf, boolean cifrado, Links links){}
  public record Links(String preview, String download){}
  public record UploadResponse(String nodeId, String version){}
}
