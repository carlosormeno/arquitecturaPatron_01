
package pe.gob.onp.arquitectura.dms.api;

import pe.gob.onp.arquitectura.dms.api.dto.ExpedienteDtos.*;
import pe.gob.onp.arquitectura.dms.service.ExpedientesService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/expedientes")
public class ExpedientesController {
    private final ExpedientesService svc;

    public ExpedientesController(ExpedientesService svc) {
        this.svc = svc;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DMS_ADMIN','EXPEDIENTE_ADMIN')")
    public Expediente create(@RequestBody CreateRequest req) {
        return svc.create(req);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DMS_ADMIN','EXPEDIENTE_ADMIN','EDITOR','REVISOR','LECTOR','EXTERNO')")
    public View get(@PathVariable("id") String id) {
        return svc.view(id);
    }
}
