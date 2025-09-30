
package pe.gob.onp.arquitectura.dms.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.DigestUtils;
import pe.gob.onp.arquitectura.dms.alfresco.AlfrescoClient;
import pe.gob.onp.arquitectura.dms.api.dto.ExpedienteDtos.*;
import pe.gob.onp.arquitectura.dms.service.ExpedientesService;
import pe.gob.onp.arquitectura.dms.service.impl.HybridDownloadService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import pe.gob.onp.arquitectura.dms.api.dto.DocumentoDtos.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import pe.gob.onp.arquitectura.dms.alfresco.dto.NodeEntry;
import pe.gob.onp.arquitectura.dms.service.impl.HybridDownloadService;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@RestController
@RequestMapping("/expedientes")
public class ExpedientesController {

    private final ExpedientesService svc;
    private final AlfrescoClient alfrescoClient;
    private static final Logger log = LoggerFactory.getLogger(ExpedientesController.class);
    private final HybridDownloadService hybridDownloadService;

    public ExpedientesController(ExpedientesService svc, AlfrescoClient alfrescoClient, HybridDownloadService hybridDownloadService) {
        this.svc = svc;
        this.alfrescoClient = alfrescoClient;
        this.hybridDownloadService = hybridDownloadService;
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

        log.info("POST /expedientes/{}/documentos - Subiendo documento: archivo='{}', subcarpeta='{}'",
                expedienteId, file.getOriginalFilename(), subcarpeta);

        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("El archivo no puede estar vacío");
            }

            UploadDocumentoRequest request = new UploadDocumentoRequest(
                    file.getBytes(),
                    file.getOriginalFilename(),
                    file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                    subcarpeta,
                    tipoDocumento != null ? tipoDocumento : "DOCUMENTO",
                    descripcion,
                    metadatos
            );

            return svc.uploadDocumento(expedienteId, request);

        } catch (Exception e) {
            log.error("Error subiendo documento: {}", e.getMessage(), e);
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

        log.info("GET /expedientes/{}/documentos - Listando documentos", expedienteId);
        return svc.getDocumentos(expedienteId, subcarpeta, page, size);
    }

    @GetMapping("/{expedienteId}/documentos/{documentoId}/download")
    @PreAuthorize("hasAnyRole('DMS_ADMIN','EXPEDIENTE_ADMIN','EDITOR','REVISOR','LECTOR','EXTERNO')")
    public ResponseEntity<Resource> downloadDocumento(
            @PathVariable("expedienteId") String expedienteId,
            @PathVariable("documentoId") String documentoId) {

        log.info("GET /expedientes/{}/documentos/{}/download", expedienteId, documentoId);

        /*try {
            NodeEntry documentoNode = alfrescoClient.getNode(documentoId);
            Map<String, Object> props = documentoNode.entry().properties();

            // NUEVA LÓGICA HÍBRIDA
            String tipoAlmacenamiento = getStringProperty(props, "dms:tipoAlmacenamiento", "ALFRESCO");

            if ("S3".equals(tipoAlmacenamiento)) {
                return downloadFromS3(documentoNode, props);
            } else {
                // Descarga original de Alfresco (documentos existentes)
                return downloadFromAlfresco(documentoId, documentoNode);
            }

        } catch (Exception e) {
            log.error("Error descargando documento {}: {}", documentoId, e.getMessage(), e);
            throw new RuntimeException("Error al descargar documento: " + e.getMessage(), e);
        }*/
        return hybridDownloadService.downloadDocument(documentoId);
    }

    @GetMapping("/{expedienteId}/subcarpetas")
    @PreAuthorize("hasAnyRole('DMS_ADMIN','EXPEDIENTE_ADMIN','EDITOR','REVISOR','LECTOR','EXTERNO')")
    public List<SubcarpetaInfo> getSubcarpetas(@PathVariable("expedienteId") String expedienteId) {
        log.info("GET /expedientes/{}/subcarpetas", expedienteId);
        return svc.getSubcarpetas(expedienteId);
    }

    /*private ResponseEntity<Resource> downloadFromAlfresco(String documentoId, NodeEntry documentoNode) {
        byte[] contenido = alfrescoClient.downloadFile(documentoId);
        ByteArrayResource resource = new ByteArrayResource(contenido);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                documentoNode.entry().content() != null ?
                        documentoNode.entry().content().mimeType() : "application/octet-stream"));
        headers.setContentLength(contenido.length);
        headers.setContentDispositionFormData("attachment", documentoNode.entry().name());

        return ResponseEntity.ok().headers(headers).body(resource);
    }*/

    /*private ResponseEntity<Resource> downloadFromS3(NodeEntry documentoNode, Map<String, Object> props) {
        String s3Key = getStringProperty(props, "dms:s3Key", null);
        String bucket = getStringProperty(props, "dms:s3Bucket", bucketName);

        if (s3Key == null) {
            throw new RuntimeException("Documento sin referencia S3 válida");
        }

        try {
            // Descargar de S3
            ResponseBytes<GetObjectResponse> s3Object = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(s3Key).build()
            );

            // Verificar integridad
            String expectedHash = getStringProperty(props, "dms:contentHash", null);
            if (expectedHash != null) {
                String actualHash = DigestUtils.sha256Hex(s3Object.asByteArray());
                if (!expectedHash.equals(actualHash)) {
                    log.error("Hash mismatch para documento {}: expected={}, actual={}",
                            documentoNode.entry().id(), expectedHash, actualHash);
                    throw new RuntimeException("Error de integridad del archivo");
                }
            }

            ByteArrayResource resource = new ByteArrayResource(s3Object.asByteArray());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    s3Object.response().contentType() != null ?
                            s3Object.response().contentType() : "application/octet-stream"));
            headers.setContentLength(s3Object.asByteArray().length);
            headers.setContentDispositionFormData("attachment", documentoNode.entry().name());

            return ResponseEntity.ok().headers(headers).body(resource);

        } catch (Exception e) {
            log.error("Error descargando de S3: key={}, bucket={}", s3Key, bucket, e);
            throw new RuntimeException("Error accediendo a S3: " + e.getMessage(), e);
        }
    }*/

    // Método auxiliar que ya tienes en ExpedientesServiceImpl
    private String getStringProperty(Map<String, Object> properties, String key, String defaultValue) {
        Object value = properties.get(key);
        return value != null ? value.toString() : defaultValue;
    }

}
