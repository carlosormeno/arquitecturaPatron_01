
package pe.gob.onp.arquitectura.dms.service;
import pe.gob.onp.arquitectura.dms.api.dto.ExpedienteDtos.*;
import reactor.core.publisher.Mono;

public interface ExpedientesService {
  Expediente create(CreateRequest req);
  View view(String id);
  Mono<String> ensureExpedienteFolder(String expedienteCodigo);
}
