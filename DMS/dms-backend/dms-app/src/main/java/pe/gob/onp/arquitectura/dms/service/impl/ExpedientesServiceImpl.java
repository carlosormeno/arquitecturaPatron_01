
package pe.gob.onp.arquitectura.dms.service.impl;

import org.springframework.beans.factory.annotation.Value;
import pe.gob.onp.arquitectura.dms.api.dto.ExpedienteDtos.*;
import pe.gob.onp.arquitectura.dms.alfresco.AlfrescoClient;
import pe.gob.onp.arquitectura.dms.service.ExpedientesService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

@Service
public class ExpedientesServiceImpl implements ExpedientesService {

    private final AlfrescoClient alfrescoClient;
    private final String rootPath;

    public ExpedientesServiceImpl(
            AlfrescoClient alfrescoClient,
            @Value("${app.alfresco.root-path:/Company Home/Expedientes}") String rootPath
    ) {
        this.alfrescoClient = alfrescoClient;
        this.rootPath = rootPath;
    }

    @Override
    public Expediente create(CreateRequest req) {
        // TODO: validar plantilla y crear carpeta de expediente en Alfresco
        return new Expediente("EXP-2025-0001", req.titulo(), "VIGENTE", req.metadatos());
    }

    @Override
    public View view(String id) {
        System.err.println("DEBUG: view() llamado con id=" + id);
        try {
            System.err.println("DEBUG: Creando Expediente...");
            Expediente ex = new Expediente(id, "Expediente de prueba", "VIGENTE", Map.of());
            System.err.println("DEBUG: Expediente creado: " + ex);

            System.err.println("DEBUG: Creando PageDocumento...");
            PageDocumento page = new PageDocumento(java.util.List.of(), 0, 20, 0);
            System.err.println("DEBUG: PageDocumento creado: " + page);

            System.err.println("DEBUG: Creando View...");
            View result = new View(ex, page);
            System.err.println("DEBUG: View creado exitosamente");
            return result;
        } catch (Exception e) {
            System.err.println("ERROR en view(): " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public Mono<String> ensureExpedienteFolder(String expedienteCodigo) {
        String code = sanitizeSegment(expedienteCodigo);
        String path = rootPath.endsWith("/") ? (rootPath + code) : (rootPath + "/" + code);

        // Como el cliente es bloqueante, lo movemos a un scheduler apto para tareas bloqueantes
        return Mono.fromCallable(() -> alfrescoClient.ensurePath(path))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private String sanitizeSegment(String s) {
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException("expedienteCodigo vacío");
        }
        String trimmed = s.trim();
        if (trimmed.contains("/") || trimmed.contains("\\")) {
            throw new IllegalArgumentException("expedienteCodigo no debe contener / ni \\");
        }
        return trimmed;
    }
}
