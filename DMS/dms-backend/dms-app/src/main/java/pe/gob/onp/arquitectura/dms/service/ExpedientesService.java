
package pe.gob.onp.arquitectura.dms.service;

import pe.gob.onp.arquitectura.dms.api.dto.ExpedienteDtos.*;
import reactor.core.publisher.Mono;
import pe.gob.onp.arquitectura.dms.api.dto.DocumentoDtos.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ExpedientesService {
    Expediente create(CreateRequest req);

    View view(String id);

    Mono<String> ensureExpedienteFolder(String expedienteCodigo);

    List<CarpetaBase> getCarpetasBase();

    UploadResponseCompleto uploadDocumento(String expedienteId, UploadDocumentoRequest request);

    PageDocumento<DocumentoInfo> getDocumentos(String expedienteId, String subcarpeta, int page, int size);

    List<SubcarpetaInfo> getSubcarpetas(String expedienteId);

    boolean isSubcarpetaValida(String subcarpeta);
}
