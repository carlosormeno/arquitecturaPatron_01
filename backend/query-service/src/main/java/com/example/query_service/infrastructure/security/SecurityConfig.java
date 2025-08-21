package com.example.query_service.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.http.SessionCreationPolicy;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${jwt.auth.converter.principal-attribute:preferred_username}")
    private String principalAttribute;

    //@Bean
    /*public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll() // rutas de autenticación (si las hay)
                        .requestMatchers("/actuator/health", "/actuator/prometheus", "/actuator/metrics").permitAll()
                        .requestMatchers("/actuator/health").permitAll() // health check
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll() // swagger
                        .requestMatchers("/api/mongoProductos/**").authenticated() // endpoints de productos
                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }*/
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health", "/actuator/info",
                                "/actuator/prometheus", "/actuator/metrics",
                                "/v3/api-docs/**", "/swagger-ui/**"
                        ).permitAll()
                        //.requestMatchers("/actuator/**").permitAll()
                        //.anyRequest().authenticated()
                        .requestMatchers("/api/**", "/api/mongoProductos/**").authenticated()
                        .anyRequest().permitAll()
                )
                /*.oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(this::jwtAuthenticationConverter))
                );*/
                /*.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                    var conv = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter();
                    conv.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter()); // usa tu clase
                    // principal: preferred_username o sub (mantén tu property)
                    conv.setPrincipalClaimName(principalAttribute);
                    jwt.jwtAuthenticationConverter(conv);
                }))*/
                .oauth2ResourceServer(oauth -> oauth.jwt(withDefaults()));

        //;

        return http.build();
    }

}