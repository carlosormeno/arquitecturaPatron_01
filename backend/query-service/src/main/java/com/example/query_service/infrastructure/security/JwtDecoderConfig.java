package com.example.query_service.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class JwtDecoderConfig {

    /*@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    String issuer;

    // Permite una o varias audiencias separadas por coma, ej: "cac4-api,otro-api"
    @Value("${SECURITY_EXPECTED_AUDIENCE_COMANDO:}")
    String expectedAudienceCsv;

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuer);

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);

        OAuth2TokenValidator<Jwt> withAudience = jwt -> {
            if (expectedAudienceCsv == null || expectedAudienceCsv.isBlank()) {
                return OAuth2TokenValidatorResult.success(); // sin audiencia → no se valida
            }
            Set<String> expected = Arrays.stream(expectedAudienceCsv.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
            List<String> aud = jwt.getAudience();
            boolean ok = aud != null && aud.stream().anyMatch(expected::contains);
            return ok
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Invalid audience", null));
        };*/

    // Tomamos las JWKs de un host alcanzable por el contenedor
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    // Validaremos el 'iss' exactamente como viene en el token (localhost...)
    @Value("${security.expected-issuer}")
    private String expectedIssuer;

    // Opcional: validar audiencia si se configura
    @Value("${security.expected-audience-consulta:}")
    private String expectedAudienceCsv;

    @Bean
    public JwtDecoder jwtDecoder() {
        // 1) Decoder con JWKs alcanzables
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        // 2) Validación por defecto + issuer exacto
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(expectedIssuer);

        // 3) (Opcional) Validación de audience
        OAuth2TokenValidator<Jwt> withAudience = jwt -> {
            if (expectedAudienceCsv == null || expectedAudienceCsv.isBlank()) {
                return OAuth2TokenValidatorResult.success();
            }
            Set<String> expected = Arrays.stream(expectedAudienceCsv.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
            List<String> aud = jwt.getAudience();
            boolean ok = aud != null && aud.stream().anyMatch(expected::contains);
            return ok
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Invalid audience", null));
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));
        return decoder;
    }
}
