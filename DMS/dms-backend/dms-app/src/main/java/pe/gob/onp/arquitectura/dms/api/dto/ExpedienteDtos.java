
package pe.gob.onp.arquitectura.dms.api.dto;

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

    //public record Expediente(String expedienteId, String titulo, String estado, Map<String, String> metadatos) {
    //}

    public record Expediente(String expedienteFolderId, String expedienteId, String titulo, String estado, Map<String, String> metadatos) {
    }

    //public record View(Expediente expediente, PageDocumento documentos) {
    //}
    public record View(Expediente expediente, PageDocumento<DocumentoInfo> documentos) {}

    //public record PageDocumento(List<Documento> content, int page, int size, long totalElements) {
    //}
    public record PageDocumento<T>(List<T> content, int page, int size, long totalElements) {}

    public record Documento(String nodeId, String nombre, String tipoDoc, String estadoWf, boolean cifrado) {
    }

    public record CarpetaBase(String nombre, String descripcion, int totalExpedientes) {}

    // Cambiar este record existente:


    // Agregar este nuevo record:
    public record DocumentoInfo(
            String nodeId,
            String nombre,
            String tipoDocumento,
            String subcarpeta,
            long tamano,
            String mimeType,
            String fechaCreacion,
            String creadoPor
    ) {}

    // Actualizar el record View:

}
