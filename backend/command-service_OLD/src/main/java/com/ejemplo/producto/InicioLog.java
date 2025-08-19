package com.ejemplo.producto;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InicioLog {

    private static final Logger log = LoggerFactory.getLogger(InicioLog.class);

    @PostConstruct
    public void init() {
        log.info("✅ Microservicio iniciado correctamente y generando log de prueba.");
    }

}
