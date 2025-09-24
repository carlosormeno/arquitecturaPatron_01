package pe.gob.onp.arquitectura.dms.alfresco;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets; // <- nuevo

@Configuration
public class AlfrescoConfig {

    @Bean
    public WebClient alfrescoWebClient(
            @Value("${app.alfresco.base-url}") String baseUrl,
            @Value("${app.alfresco.username}") String username,
            @Value("${app.alfresco.password}") String password,
            @Qualifier("alfrescoObjectMapper") ObjectMapper objectMapper
    ) {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> {
                    // Configurar decoder/encoder con nuestro ObjectMapper
                    configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
                    configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));

                    // Aumentar límite de memoria para archivos grandes
                    configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024); // 50MB
                })
                .build();

        return WebClient.builder()
                .baseUrl(baseUrl)
                // Authorization: Basic <base64(username:password)>
                .defaultHeaders(h -> h.setBasicAuth(username, password, StandardCharsets.UTF_8))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(strategies)  // Usar nuestras estrategias personalizadas
                .build();
    }
}
