package pe.gob.onp.arquitectura.dms.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import pe.gob.onp.arquitectura.dms.alfresco.AlfrescoClient;
import pe.gob.onp.arquitectura.dms.alfresco.dto.NodeChildrenList;
import pe.gob.onp.arquitectura.dms.alfresco.dto.NodeEntry;
import pe.gob.onp.arquitectura.dms.api.dto.DocumentoDtos.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class HybridStorageService {

    private static final Logger log = LoggerFactory.getLogger(HybridStorageService.class);

    private final S3Client s3Client;
    private final AlfrescoClient alfrescoClient;

    @Value("${s3.bucket.name:dms-storage}")
    private String bucketName;

    @Value("${storage.mode:hybrid}")
    private String storageMode;

    // Constantes actualizadas según tu modelo
    private static final String DMS_NAMESPACE = "dms:";
    public static final String PROP_TIPO_ALMACENAMIENTO = DMS_NAMESPACE + "tipoAlmacenamiento";
    public static final String PROP_S3_KEY = DMS_NAMESPACE + "s3Key";
    public static final String PROP_S3_BUCKET = DMS_NAMESPACE + "s3Bucket";
    public static final String PROP_S3_ETAG = DMS_NAMESPACE + "s3ETag";
    public static final String PROP_CONTENT_HASH = DMS_NAMESPACE + "contentHash";
    public static final String PROP_TAMANO_BYTES = DMS_NAMESPACE + "tamanoBytes";
    public static final String PROP_MIME_TYPE = DMS_NAMESPACE + "mimeType";

    public HybridStorageService(S3Client s3Client, AlfrescoClient alfrescoClient) {
        this.s3Client = s3Client;
        this.alfrescoClient = alfrescoClient;
    }

    public UploadResponseCompleto storeDocument(String expedienteId, UploadDocumentoRequest request) {
        if ("alfresco".equals(storageMode)) {
            return storeInAlfrescoOnly(expedienteId, request);
        } else {
            return storeInS3WithAlfrescoMetadata(expedienteId, request);
        }
    }

    private UploadResponseCompleto storeInS3WithAlfrescoMetadata(String expedienteId, UploadDocumentoRequest request) {
        String s3Key = generateS3Key(expedienteId, request.subcarpeta(), request.nombreArchivo());

        try {
            // 1. Validar que el expediente existe
            NodeEntry expedienteNode = alfrescoClient.getNode(expedienteId);
            validateExpedienteNode(expedienteNode);

            // 2. Upload a S3
            String contentHash = DigestUtils.sha256Hex(request.contenido());
            PutObjectResponse s3Response = uploadToS3(s3Key, request, expedienteId, contentHash);

            // 3. Crear nodo de referencia en Alfresco
            NodeEntry alfrescoNode = createReferenceNodeInAlfresco(
                    expedienteId, request, s3Key, s3Response.eTag(), contentHash
            );

            return construirUploadResponse(alfrescoNode, request, s3Key);

        } catch (Exception e) {
            // Rollback S3 si falló la creación en Alfresco
            rollbackS3Upload(s3Key);
            throw new RuntimeException("Error en almacenamiento híbrido: " + e.getMessage(), e);
        }
    }

    private UploadResponseCompleto storeInAlfrescoOnly(String expedienteId, UploadDocumentoRequest request) {
        // Usar el método original de Alfresco
        try {
            NodeEntry expedienteNode = alfrescoClient.getNode(expedienteId);
            String subcarpetaId = findOrCreateSubcarpeta(expedienteNode.entry().id(), request.subcarpeta());

            Map<String, Object> propiedades = construirPropiedadesDocumento(request, expedienteId);

            NodeEntry documentoNode = alfrescoClient.uploadFile(
                    subcarpetaId,
                    request.nombreArchivo(),
                    request.contenido(),
                    request.mimeType(),
                    propiedades
            );

            return construirUploadResponse(documentoNode, request, null);

        } catch (Exception e) {
            throw new RuntimeException("Error almacenando en Alfresco: " + e.getMessage(), e);
        }
    }

    private void validateExpedienteNode(NodeEntry expedienteNode) {
        if (expedienteNode == null || expedienteNode.entry() == null) {
            throw new IllegalArgumentException("Expediente no encontrado");
        }

        String nodeType = expedienteNode.entry().nodeType();
        if (!"dms:expediente".equals(nodeType) && !"cm:folder".equals(nodeType)) {
            throw new IllegalArgumentException("El nodo no es un expediente válido: " + nodeType);
        }
    }

    private PutObjectResponse uploadToS3(String s3Key, UploadDocumentoRequest request,
                                         String expedienteId, String contentHash) {

        Map<String, String> metadata = Map.of(
                "expediente-id", expedienteId,
                "subcarpeta", request.subcarpeta(),
                "tipo-documento", request.tipoDocumento(),
                "content-hash", contentHash,
                "usuario-subida", "system"
        );

        return s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .contentType(request.mimeType())
                        .metadata(metadata)
                        .build(),
                RequestBody.fromBytes(request.contenido())
        );
    }

    private NodeEntry createReferenceNodeInAlfresco(String expedienteId, UploadDocumentoRequest request,
                                                    String s3Key, String eTag, String contentHash) {

        // Buscar subcarpeta
        String subcarpetaId = findOrCreateSubcarpeta(expedienteId, request.subcarpeta());

        // Propiedades del documento híbrido
        Map<String, Object> properties = construirPropiedadesDocumento(request, expedienteId);

        // Propiedades específicas de referencia S3
        properties.put(PROP_TIPO_ALMACENAMIENTO, "S3");
        properties.put(PROP_S3_KEY, s3Key);
        properties.put(PROP_S3_BUCKET, bucketName);
        properties.put(PROP_S3_ETAG, eTag);
        properties.put(PROP_CONTENT_HASH, contentHash);
        properties.put(PROP_TAMANO_BYTES, (long) request.contenido().length);
        properties.put(PROP_MIME_TYPE, request.mimeType());
        properties.put("dms:fechaSincronizacion", OffsetDateTime.now().toString());

        // Crear nodo sin contenido binario, solo metadatos
        return alfrescoClient.createDocumentMetadata(subcarpetaId, request.nombreArchivo(), properties);
    }

    private String findOrCreateSubcarpeta(String expedienteId, String nombreSubcarpeta) {
        try {
            // Buscar subcarpeta existente
            NodeChildrenList children = alfrescoClient.getNodeChildren(
                    expedienteId,
                    String.format("(name='%s')", nombreSubcarpeta.replace("'", "\\'"))
            );

            if (children != null && children.getList() != null &&
                    children.getList().getEntries() != null && !children.getList().getEntries().isEmpty()) {
                return children.getList().getEntries().get(0).getEntry().id();
            }

            // Crear subcarpeta si no existe
            Map<String, Object> props = Map.of(
                    "cm:title", nombreSubcarpeta,
                    "cm:description", "Subcarpeta para " + nombreSubcarpeta.toLowerCase()
            );

            NodeEntry subcarpeta = alfrescoClient.createFolder(expedienteId, nombreSubcarpeta, props);
            return subcarpeta.entry().id();

        } catch (Exception e) {
            log.error("Error buscando/creando subcarpeta {}: {}", nombreSubcarpeta, e.getMessage());
            throw new RuntimeException("Error con subcarpeta: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> construirPropiedadesDocumento(UploadDocumentoRequest request, String expedienteId) {
        Map<String, Object> properties = new HashMap<>();

        // Propiedades estándar
        properties.put("cm:title", request.nombreArchivo());
        properties.put("cm:description", request.descripcion() != null ? request.descripcion() : "");

        // Propiedades DMS obligatorias según tu modelo
        properties.put("dms:tipoDocumento", request.tipoDocumento());
        properties.put("dms:fechaDocumento", LocalDate.now().toString());

        // Propiedades DMS opcionales
        properties.put("dms:subcarpeta", request.subcarpeta());
        properties.put("dms:expedienteId", expedienteId);
        properties.put("dms:fechaSubida", OffsetDateTime.now().toString());
        properties.put("dms:usuarioSubida", "system");

        // Metadatos adicionales
        if (request.metadatos() != null) {
            request.metadatos().forEach((key, value) -> {
                if (value != null && !value.trim().isEmpty()) {
                    properties.put("dms:" + key, value);
                }
            });
        }

        return properties;
    }

    private void rollbackS3Upload(String s3Key) {
        try {
            log.warn("Realizando rollback de S3 para key: {}", s3Key);
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build());
            log.info("Rollback S3 completado para key: {}", s3Key);
        } catch (Exception e) {
            log.error("Error en rollback S3 para key {}: {}", s3Key, e.getMessage());
        }
    }

    private UploadResponseCompleto construirUploadResponse(NodeEntry documentoNode, UploadDocumentoRequest request, String s3Key) {
        if (documentoNode == null || documentoNode.entry() == null) {
            throw new RuntimeException("Error: Respuesta inválida de Alfresco");
        }

        NodeEntry.Node node = documentoNode.entry();

        return new UploadResponseCompleto(
                node.id(),
                "1.0",
                node.name() != null ? node.name() : request.nombreArchivo(),
                request.subcarpeta(),
                request.tipoDocumento(),
                request.contenido().length,
                request.mimeType(),
                OffsetDateTime.now(),
                "system"
        );
    }

    private String generateS3Key(String expedienteId, String subcarpeta, String nombreArchivo) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String sanitizedFilename = sanitizeFilename(nombreArchivo);
        return String.format("expedientes/%s/%s/%s_%s",
                expedienteId, subcarpeta, timestamp, sanitizedFilename);
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}