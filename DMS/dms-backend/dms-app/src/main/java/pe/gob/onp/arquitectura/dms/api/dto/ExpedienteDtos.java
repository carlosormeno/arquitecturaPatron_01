
package pe.gob.onp.arquitectura.dms.api.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /*public record CreateRequest(
            String carpetaBase,          // "Expedientes", "Repositorio", "Plantillas", etc.
            String titulo,
            String descripcion,
            Map<String, String> metadatos
    ) {}*/

    public record CreateRequest(
            String carpetaBase,          // Carpeta donde se creará
            String titulo,               // Título del expediente
            String descripcion,          // Descripción opcional

            // PROPIEDADES OBLIGATORIAS DEL MODELO DMS:EXPEDIENTE
            String area,                 // dms:area (OBLIGATORIO)
            String solicitante,          // dms:solicitante (OBLIGATORIO)
            String asunto,               // dms:asunto (OBLIGATORIO - puede ser igual a título)

            // PROPIEDADES OPCIONALES CON DEFAULTS
            String prioridad,            // dms:prioridad (OPCIONAL - default: NORMAL)
            String confidencialidad,     // dms:confidencialidad (OPCIONAL - default: PUBLICO)
            String categoria,            // dms:categoria (OPCIONAL)
            String observaciones,        // dms:observaciones (OPCIONAL)

            Map<String, String> metadatos // Metadatos adicionales
    ) {
        // Validaciones
        public CreateRequest {
            if (carpetaBase == null || carpetaBase.trim().isEmpty()) {
                throw new IllegalArgumentException("Carpeta base es obligatoria");
            }
            if (titulo == null || titulo.trim().isEmpty()) {
                throw new IllegalArgumentException("Título es obligatorio");
            }
            if (area == null || area.trim().isEmpty()) {
                throw new IllegalArgumentException("Área es obligatoria según modelo DMS");
            }
            if (solicitante == null || solicitante.trim().isEmpty()) {
                throw new IllegalArgumentException("Solicitante es obligatorio según modelo DMS");
            }
            if (asunto == null || asunto.trim().isEmpty()) {
                throw new IllegalArgumentException("Asunto es obligatorio según modelo DMS");
            }

            // Validar valores permitidos
            if (prioridad != null && !Set.of("ALTA", "NORMAL", "BAJA").contains(prioridad)) {
                throw new IllegalArgumentException("Prioridad debe ser: ALTA, NORMAL, BAJA");
            }
        }
    }

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
