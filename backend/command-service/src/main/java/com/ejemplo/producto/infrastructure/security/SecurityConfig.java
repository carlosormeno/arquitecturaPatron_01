package com.ejemplo.producto.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.*;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${jwt.auth.converter.principal-attribute:preferred_username}")
    private String principalAttribute;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health", "/actuator/info",
                                "/v3/api-docs/**", "/swagger-ui/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                /*.oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(this::jwtAuthenticationConverter))
                );*/
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                    var conv = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter();
                    conv.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter()); // usa tu clase
                    // principal: preferred_username o sub (mantén tu property)
                    conv.setPrincipalClaimName(principalAttribute);
                    jwt.jwtAuthenticationConverter(conv);
                }));

        return http.build();
    }

    /*private AbstractAuthenticationToken jwtAuthenticationConverter(Jwt jwt) {
        // 1) scopes (SCOPE_…)
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        Collection<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(jwt));

        // 2) realm roles -> ROLE_…
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof Map<?, ?> ra) {
            Object roles = ra.get("roles");
            if (roles instanceof Collection<?> rs) {
                rs.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
            }
        }

        // 3) client roles -> ROLE_…
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess instanceof Map<?, ?> ra) {
            ra.values().forEach(val -> {
                if (val instanceof Map<?, ?> clientMap) {
                    Object roles = clientMap.get("roles");
                    if (roles instanceof Collection<?> rs) {
                        rs.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
                    }
                }
            });
        }

        // principal: preferred_username o sub
        String principal = Optional.ofNullable(jwt.getClaimAsString(principalAttribute))
                .orElse(jwt.getSubject());
        return new JwtAuthenticationToken(jwt, authorities, principal);
    }*/
}
