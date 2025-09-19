
package pe.gob.onp.arquitectura.dms.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.gob.onp.arquitectura.dms.alfresco.AlfrescoClient;
import pe.gob.onp.arquitectura.dms.api.dto.DocumentoDtos;
import pe.gob.onp.arquitectura.dms.api.dto.ExpedienteDtos.*;
import pe.gob.onp.arquitectura.dms.service.ExpedientesService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/expedientes")
public class ExpedientesController {

    private final ExpedientesService svc;
    private final AlfrescoClient alfrescoClient;
    private static final Logger log = LoggerFactory.getLogger(ExpedientesController.class);

    public ExpedientesController(ExpedientesService svc, AlfrescoClient alfrescoClient) {
        this.svc = svc;
        this.alfrescoClient = alfrescoClient;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DMS_ADMIN','EXPEDIENTE_ADMIN')")
    public Expediente create(@RequestBody CreateRequest req) {
        log.info("POST /expedientes - Creando expediente: {}", req.titulo());
        try {
            log.info("Entramos al try", req.titulo());
            Expediente result = svc.create(req);
            log.info("Expediente creado exitosamente: {}", result.expedienteId());
            return result;
        } catch (Exception e) {
            log.error("Error en controller al crear expediente: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DMS_ADMIN','EXPEDIENTE_ADMIN','EDITOR','REVISOR','LECTOR','EXTERNO')")
    public View get(@PathVariable("id") String id) {
        log.info("GET /expedientes/{} - Consultando expediente", id);

        try {
            View result = svc.view(id);
            log.info("Expediente consultado exitosamente: ID='{}', título='{}'",
                    result.expediente().expedienteId(), result.expediente().titulo());
            log.debug("Documentos en expediente: {}", result.documentos().totalElements());
            return result;
        } catch (Exception e) {
            log.error("Error en controller al consultar expediente '{}': {}",
                    id, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/carpetas-base")
    @PreAuthorize("hasAnyRole('DMS_ADMIN','EXPEDIENTE_ADMIN','EDITOR')")
    public List<CarpetaBase> getCarpetasBase() {
        log.info("GET /expedientes/carpetas-base - Listando carpetas base disponibles");

        try {
            List<CarpetaBase> carpetas = svc.getCarpetasBase();
            log.info("Carpetas base obtenidas: {} carpetas", carpetas.size());
            log.debug("Carpetas disponibles: {}",
                    carpetas.stream().map(CarpetaBase::nombre).toList());
            return carpetas;
        } catch (Exception e) {
            log.error("Error obteniendo carpetas base: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/debug/root")
    @PreAuthorize("hasRole('DMS_ADMIN')")
    public Map<String, String> debugRoot() {
        log.info("DEBUG: Verificando contenido de Alfresco root");

        try {
            alfrescoClient.debugRootContent();  // Necesitas inyectar AlfrescoClient
            return Map.of("status", "success", "message", "Check logs for results");
        } catch (Exception e) {
            log.error("Error en debug: {}", e.getMessage());
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    @PostMapping("/{expedienteId}/documentos")
    @PreAuthorize("hasAnyRole('DMS_ADMIN','EXPEDIENTE_ADMIN','EDITOR')")
    public UploadResponseCompleto uploadDocumento(
            @PathVariable("expedienteId") String expedienteId,
            @RequestPart("file") MultipartFile file,
            @RequestPart("subcarpeta") String subcarpeta,
            @RequestPart(value = "tipoDocumento", required = false) String tipoDocumento,
            @RequestPart(value = "descripcion", required = false) String descripcion,
            @RequestPart(value = "metadatos", required = false) Map<String, String> metadatos) {

        log.info("POST /expedientes/{}/documentos - Subiendo documento: archivo='{}', subcarpeta='{}', tipo='{}'",
                expedienteId, file.getOriginalFilename(), subcarpeta, tipoDocumento);

        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("El archivo no puede estar vacío");
            }

            byte[] contenido = file.getBytes();
            String mimeType = file.getContentType();
            if (mimeType == null || mimeType.isBlank()) {
                mimeType = "application/octet-stream";
            }

            UploadDocumentoRequest request = new UploadDocumentoRequest(
                    contenido,
                    file.getOriginalFilename(),
                    mimeType,
                    subcarpeta,
                    tipoDocumento != null ? tipoDocumento : "DOCUMENTO",
                    descripcion,
                    metadatos
            );

            UploadResponseCompleto result = svc.uploadDocumento(expedienteId, request);

            log.info("Documento subido exitosamente: nodeId='{}', expediente='{}', subcarpeta='{}'",
                    result.nodeId(), expedienteId, result.subcarpeta());

            return result;

        } catch (Exception e) {
            log.error("Error subiendo documento a expediente '{}': {}", expedienteId, e.getMessage(), e);
            throw new RuntimeException("Error al subir documento: " + e.getMessage(), e);
        }
    }

    @GetMapping("/{expedienteId}/documentos")
    @PreAuthorize("hasAnyRole('DMS_ADMIN','EXPEDIENTE_ADMIN','EDITOR','REVISOR','LECTOR','EXTERNO')")
    public PageDocumento<DocumentoInfo> getDocumentos(
            @PathVariable("expedienteId") String expedienteId,
            @RequestParam(value = "subcarpeta", required = false) String subcarpeta,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        log.info("GET /expedientes/{}/documentos - Listando documentos: subcarpeta='{}', page={}, size={}",
                expedienteId, subcarpeta, page, size);

        try {
            PageDocumento<DocumentoInfo> result = svc.getDocumentos(expedienteId, subcarpeta, page, size);

            log.info("Documentos listados: expediente='{}', total={}, página={}/{}",
                    expedienteId, result.totalElements(), page, result.totalElements() / size);

            return result;

        } catch (Exception e) {
            log.error("Error listando documentos de expediente '{}': {}", expedienteId, e.getMessage(), e);
            throw new RuntimeException("Error al listar documentos: " + e.getMessage(), e);
        }
    }

    @GetMapping("/{expedienteId}/subcarpetas")
    @PreAuthorize("hasAnyRole('DMS_ADMIN','EXPEDIENTE_ADMIN','EDITOR','REVISOR','LECTOR','EXTERNO')")
    public List<SubcarpetaInfo> getSubcarpetas(@PathVariable("expedienteId") String expedienteId) {

        log.info("GET /expedientes/{}/subcarpetas - Obteniendo información de subcarpetas", expedienteId);

        try {
            List<DocumentoDtos.SubcarpetaInfo> result = svc.getSubcarpetas(expedienteId);

            log.info("Subcarpetas obtenidas: expediente='{}', cantidad={}",
                    expedienteId, result.size());

            return result;

        } catch (Exception e) {
            log.error("Error obteniendo subcarpetas de expediente '{}': {}", expedienteId, e.getMessage(), e);
            throw new RuntimeException("Error al obtener subcarpetas: " + e.getMessage(), e);
        }
    }

}
