package com.example.query_service.infrastructure.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {


    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        // Realm roles
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        List<String> realmRoles = Optional.ofNullable(realmAccess)
                .map(m -> (List<String>) m.getOrDefault("roles", Collections.emptyList()))
                .orElse(Collections.emptyList());

        // Client roles (para todos los clients del token)
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        List<String> clientRoles = new ArrayList<>();
        if (resourceAccess != null) {
            for (Object v : resourceAccess.values()) {
                if (v instanceof Map<?,?> m) {
                    Object r = m.get("roles");
                    if (r instanceof List<?> l) {
                        l.forEach(x -> { if (x != null) clientRoles.add(x.toString()); });
                    }
                }
            }
        }

        return Stream.concat(realmRoles.stream(), clientRoles.stream())
                .distinct()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()))
                .collect(Collectors.toSet());
    }
}
