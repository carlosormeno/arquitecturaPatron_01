package pe.gob.onp.arquitectura.dms.api.dto;
import java.time.OffsetDateTime;
import java.util.Map;
public class DocumentoDtos {
  public record Documento(String nodeId, String nombre, String tipoDoc, String estadoWf, boolean cifrado, Links links){}
  public record Links(String preview, String download){}
  public record UploadResponse(String nodeId, String version){}

    // Agregar estos records a DocumentoDtos:

    public record UploadDocumentoRequest(
            byte[] contenido,
            String nombreArchivo,
            String mimeType,
            String subcarpeta,
            String tipoDocumento,
            String descripcion,
            Map<String, String> metadatos
    ) {
        public UploadDocumentoRequest {
            if (contenido == null || contenido.length == 0) {
                throw new IllegalArgumentException("El contenido del archivo no puede estar vacío");
            }
            if (nombreArchivo == null || nombreArchivo.trim().isEmpty()) {
                throw new IllegalArgumentException("El nombre del archivo es requerido");
            }
            if (subcarpeta == null || subcarpeta.trim().isEmpty()) {
                throw new IllegalArgumentException("La subcarpeta es requerida");
            }
        }
    }

    public record UploadResponseCompleto(
            String nodeId,
            String version,
            String nombreArchivo,
            String subcarpeta,
            String tipoDocumento,
            long tamano,
            String mimeType,
            OffsetDateTime fechaSubida,
            String usuarioSubida
    ) {}

    public record SubcarpetaInfo(
            String nombre,
            String nodeId,
            String descripcion,
            int cantidadDocumentos,
            long tamanoTotal,
            OffsetDateTime ultimaModificacion
    ) {}

    public record ConfiguracionSubcarpeta(
            String nombre,
            String descripcion,
            String[] tiposDocumentosPermitidos,
            String[] extensionesPermitidas,
            long tamanoMaximoMB,
            boolean requiereAprobacion
    ) {}
}
