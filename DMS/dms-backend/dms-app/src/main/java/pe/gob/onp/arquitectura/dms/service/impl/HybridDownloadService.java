package pe.gob.onp.arquitectura.dms.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import pe.gob.onp.arquitectura.dms.alfresco.AlfrescoClient;
import pe.gob.onp.arquitectura.dms.alfresco.dto.NodeEntry;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.util.Map;

import static pe.gob.onp.arquitectura.dms.service.impl.HybridStorageService.*;

@Service
public class HybridDownloadService {

    private static final Logger log = LoggerFactory.getLogger(HybridDownloadService.class);

    private final S3Client s3Client;
    private final AlfrescoClient alfrescoClient;

    @Value("${s3.bucket.name:dms-storage}")
    private String bucketName;

    public HybridDownloadService(S3Client s3Client, AlfrescoClient alfrescoClient) {
        this.s3Client = s3Client;
        this.alfrescoClient = alfrescoClient;
    }

    public ResponseEntity<Resource> downloadDocument(String documentoId) {
        NodeEntry documentoNode = alfrescoClient.getNode(documentoId);
        Map<String, Object> props = documentoNode.entry().properties();

        String tipoAlmacenamiento = getStringProperty(props, PROP_TIPO_ALMACENAMIENTO, "ALFRESCO");

        switch (tipoAlmacenamiento) {
            case "S3":
                return downloadFromS3(documentoNode, props);
            case "ALFRESCO":
                return downloadFromAlfresco(documentoId, documentoNode);
            default:
                throw new IllegalStateException("Tipo de almacenamiento no soportado: " + tipoAlmacenamiento);
        }
    }

    private ResponseEntity<Resource> downloadFromS3(NodeEntry documentoNode, Map<String, Object> props) {
        String s3Key = getStringProperty(props, PROP_S3_KEY, null);
        String bucket = getStringProperty(props, PROP_S3_BUCKET, bucketName);

        if (s3Key == null || bucket == null) {
            throw new RuntimeException("Documento S3 sin referencia válida");
        }

        try {
            ResponseBytes<GetObjectResponse> s3Object = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(s3Key)
                            .build()
            );

            // Verificar integridad si existe hash
            String expectedHash = getStringProperty(props, PROP_CONTENT_HASH, null);
            if (expectedHash != null) {
                String actualHash = DigestUtils.sha256Hex(s3Object.asByteArray());
                if (!expectedHash.equals(actualHash)) {
                    throw new RuntimeException("Error de integridad del archivo");
                }
            }

            ByteArrayResource resource = new ByteArrayResource(s3Object.asByteArray());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    getStringProperty(props, PROP_MIME_TYPE, "application/octet-stream")));
            headers.setContentLength(s3Object.asByteArray().length);
            headers.setContentDispositionFormData("attachment", documentoNode.entry().name());

            return ResponseEntity.ok().headers(headers).body(resource);

        } catch (Exception e) {
            log.error("Error descargando de S3: key={}, bucket={}", s3Key, bucket, e);
            throw new RuntimeException("Error accediendo a S3: " + e.getMessage(), e);
        }
    }

    private ResponseEntity<Resource> downloadFromAlfresco(String documentoId, NodeEntry documentoNode) {
        byte[] contenido = alfrescoClient.downloadFile(documentoId);
        ByteArrayResource resource = new ByteArrayResource(contenido);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                documentoNode.entry().content() != null ?
                        documentoNode.entry().content().mimeType() : "application/octet-stream"));
        headers.setContentLength(contenido.length);
        headers.setContentDispositionFormData("attachment", documentoNode.entry().name());

        return ResponseEntity.ok().headers(headers).body(resource);
    }

    private String getStringProperty(Map<String, Object> properties, String key, String defaultValue) {
        Object value = properties.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}