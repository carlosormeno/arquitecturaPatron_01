
package pe.gob.onp.arquitectura.dms.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

public class ExpedienteDtos {
    /*public record CreateRequest(
            String plantillaId,
            String businessUnit,
            @NotBlank String titulo,
            String descripcion,
            @NotBlank String area,
            @NotBlank String confidencialidad,
            Map<String, String> metadatos) {
    }*/

    public record CreateRequest(
            String carpetaBase,          // "Expedientes", "Repositorio", "Plantillas", etc.
            String titulo,
            String descripcion,
            Map<String, String> metadatos
    ) {}

    public record Expediente(String expedienteId, String titulo, String estado, Map<String, String> metadatos) {
    }

    /*public record View(Expediente expediente, PageDocumento documentos) {
    }*/

    public record PageDocumento<T>(List<T> content, int page, int size, long totalElements) {
    }

    @Deprecated
    public record Documento(String nodeId, String nombre, String tipoDoc, String estadoWf, boolean cifrado) {
    }

    public record DocumentoInfo(
            String nodeId,
            String nombre,
            String tipoDocumento,
            String subcarpeta,
            long tamaño,
            String mimeType,
            String fechaCreacion,
            String creadoPor
    ) {}

    public record View(Expediente expediente, PageDocumento<DocumentoInfo> documentos) {}

    public record CarpetaBase(String nombre, String descripcion, int totalExpedientes) {}
}
