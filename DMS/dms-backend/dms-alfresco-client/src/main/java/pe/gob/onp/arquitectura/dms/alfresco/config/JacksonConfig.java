package pe.gob.onp.arquitectura.dms.alfresco.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuración de Jackson específica para el cliente de Alfresco
 * Maneja la deserialización de respuestas de la API de Alfresco
 */
@Configuration
public class JacksonConfig {

    @Bean(name = "alfrescoObjectMapper")
    @Primary
    public ObjectMapper alfrescoObjectMapper() {
        return new ObjectMapper()
                // CRÍTICO: No fallar si Alfresco envía propiedades que no conocemos
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

                // No convertir strings vacíos a null (Alfresco a veces envía "")
                .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, false)

                // Aceptar arrays de un solo elemento como elemento único
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)

                // Manejar valores null en primitivos
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)

                // Para fechas ISO-8601 que usa Alfresco
                .registerModule(new JavaTimeModule())

                // Alfresco generalmente usa camelCase
                .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
    }
}