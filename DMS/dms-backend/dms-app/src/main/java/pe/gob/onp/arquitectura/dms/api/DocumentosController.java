
package pe.gob.onp.arquitectura.dms.api;

import pe.gob.onp.arquitectura.dms.api.dto.DocumentoDtos.*;
import pe.gob.onp.arquitectura.dms.service.DocumentosService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/documentos")
public class DocumentosController {
    private final DocumentosService svc;

    public DocumentosController(DocumentosService svc) {
        this.svc = svc;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DMS_ADMIN','EXPEDIENTE_ADMIN','EDITOR','REVISOR','LECTOR','EXTERNO')")
    public Documento get(@PathVariable("id") String id) {
        return svc.get(id);
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('DMS_ADMIN','EXPEDIENTE_ADMIN','EDITOR')")
    public UploadResponse upload(@RequestPart("file") MultipartFile file,
                                 @RequestPart(value = "tipoDoc", required = false) String tipoDoc,
                                 @RequestPart(value = "metadatos", required = false) Map<String, String> metadatos) {
        return svc.upload(file, tipoDoc, metadatos);
    }

    @PostMapping("/{id}/workflow")
    @PreAuthorize("hasAnyRole('DMS_ADMIN','EXPEDIENTE_ADMIN','REVISOR')")
    public Map<String, String> transition(@PathVariable("id") String id, @RequestBody Map<String, String> body) {
        return svc.transition(id, body.getOrDefault("accion", ""), body.getOrDefault("comentario", ""));
    }
}
