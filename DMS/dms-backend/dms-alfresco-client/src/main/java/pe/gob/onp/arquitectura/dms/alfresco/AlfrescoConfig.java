package pe.gob.onp.arquitectura.dms.alfresco;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets; // <- nuevo

@Configuration
public class AlfrescoConfig {

    @Bean
    public WebClient alfrescoWebClient(
            @Value("${app.alfresco.base-url}") String baseUrl,
            @Value("${app.alfresco.username}") String username,
            @Value("${app.alfresco.password}") String password
    ) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                // Authorization: Basic <base64(username:password)>
                .defaultHeaders(h -> h.setBasicAuth(username, password, StandardCharsets.UTF_8))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(50 * 1024 * 1024)) // 50MB
                        .build())
                .build();
    }
}
