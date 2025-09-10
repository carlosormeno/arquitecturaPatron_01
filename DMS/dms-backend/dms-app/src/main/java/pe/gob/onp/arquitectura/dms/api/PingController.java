package pe.gob.onp.arquitectura.dms.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class PingController {

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of(
                "status", "OK",
                "timestamp", LocalDateTime.now(),
                "service", "dms",
                "version", "1.0.0"
        );
    }
}