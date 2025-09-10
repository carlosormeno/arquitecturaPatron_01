
package pe.gob.onp.arquitectura.dms.service;
import pe.gob.onp.arquitectura.dms.api.dto.DocumentoDtos.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
public interface DocumentosService {
  Documento get(String id);
  UploadResponse upload(MultipartFile file, String tipoDoc, Map<String,String> metadatos);
  Map<String,String> transition(String id, String accion, String comentario);
}
