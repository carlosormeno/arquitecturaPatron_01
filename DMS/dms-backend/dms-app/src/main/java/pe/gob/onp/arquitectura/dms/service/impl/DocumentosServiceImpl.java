
package pe.gob.onp.arquitectura.dms.service.impl;

import pe.gob.onp.arquitectura.dms.api.dto.DocumentoDtos.*;
import pe.gob.onp.arquitectura.dms.service.DocumentosService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class DocumentosServiceImpl implements DocumentosService {
    @Override
    public Documento get(String id) {
        // TODO: obtener metadatos + links de preview/descarga
        return new Documento(id, "demo.pdf", "PDF", "APROBADO", false, new Links("/preview", "/download"));
    }

    @Override
    public UploadResponse upload(MultipartFile file, String tipoDoc, Map<String, String> metadatos) {
        // TODO: subir a Alfresco, setear metadatos, calcular hash
        return new UploadResponse("NODE-123", "1.0");
    }

    @Override
    public Map<String, String> transition(String id, String accion, String comentario) {
        // TODO: cambiar estado de workflow
        return Map.of("id", id, "status", "OK", "accion", accion);
    }
}
