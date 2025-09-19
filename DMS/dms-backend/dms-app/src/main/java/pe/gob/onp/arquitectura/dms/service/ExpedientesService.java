
package pe.gob.onp.arquitectura.dms.service;

import pe.gob.onp.arquitectura.dms.api.dto.DocumentoDtos;
import pe.gob.onp.arquitectura.dms.api.dto.ExpedienteDtos.*;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ExpedientesService {
    Expediente create(CreateRequest req);

    View view(String id);

    Mono<String> ensureExpedienteFolder(String expedienteCodigo);

    List<CarpetaBase> getCarpetasBase();

    DocumentoDtos.UploadResponseCompleto uploadDocumento(String expedienteId, DocumentoDtos.UploadDocumentoRequest request);

    PageDocumento<DocumentoInfo> getDocumentos(String expedienteId, String subcarpeta, int page, int size);

    List<DocumentoDtos.SubcarpetaInfo> getSubcarpetas(String expedienteId);

    boolean isSubcarpetaValida(String subcarpeta);
}
