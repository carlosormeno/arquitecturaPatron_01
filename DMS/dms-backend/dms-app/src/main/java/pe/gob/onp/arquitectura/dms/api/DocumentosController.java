
package pe.gob.onp.arquitectura.dms.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(DocumentosController.class);

    public DocumentosController(DocumentosService svc) {
        this.svc = svc;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DMS_ADMIN','EXPEDIENTE_ADMIN','EDITOR','REVISOR','LECTOR','EXTERNO')")
    public Documento get(@PathVariable("id") String id) {
        log.info("GET /documentos/{} - Consultando documento", id);

        try {
            Documento result = svc.get(id);
            log.info("Documento consultado exitosamente: ID='{}', nombre='{}', tipo='{}'",
                    result.nodeId(), result.nombre(), result.tipoDoc());
            return result;
        } catch (Exception e) {
            log.error("Error en controller al consultar documento '{}': {}",
                    id, e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('DMS_ADMIN','EXPEDIENTE_ADMIN','EDITOR')")
    public UploadResponse upload(@RequestPart("file") MultipartFile file,
                                 @RequestPart(value = "tipoDoc", required = false) String tipoDoc,
                                 @RequestPart(value = "metadatos", required = false) Map<String, String> metadatos) {
        log.info("POST /documentos/upload - Subiendo archivo: nombre='{}', tamaño={} bytes, tipo='{}'",
                file.getOriginalFilename(), file.getSize(), tipoDoc);
        log.debug("Metadatos del archivo: {}", metadatos);

        try {
            UploadResponse result = svc.upload(file, tipoDoc, metadatos);
            log.info("Archivo subido exitosamente: nodeId='{}', versión='{}'",
                    result.nodeId(), result.version());
            return result;
        } catch (Exception e) {
            log.error("Error en controller al subir archivo '{}': {}",
                    file.getOriginalFilename(), e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/{id}/workflow")
    @PreAuthorize("hasAnyRole('DMS_ADMIN','EXPEDIENTE_ADMIN','REVISOR')")
    public Map<String, String> transition(@PathVariable("id") String id, @RequestBody Map<String, String> body) {
        String accion = body.getOrDefault("accion", "");
        String comentario = body.getOrDefault("comentario", "");

        log.info("POST /documentos/{}/workflow - Transición de workflow: acción='{}', comentario='{}'",
                id, accion, comentario);

        try {
            Map<String, String> result = svc.transition(id, accion, comentario);
            log.info("Transición de workflow exitosa para documento '{}': {}", id, result);
            return result;
        } catch (Exception e) {
            log.error("Error en controller al realizar transición de workflow para documento '{}': {}",
                    id, e.getMessage(), e);
            throw e;
        }
    }
}
