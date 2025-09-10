
package pe.gob.onp.arquitectura.dms.security;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realm = jwt.getClaim("realm_access");
        if (realm == null) return List.of();
        Object rolesObj = realm.get("roles");
        if (!(rolesObj instanceof Collection<?> roles)) return List.of();
        return roles.stream().filter(x -> x instanceof String).map(x -> new SimpleGrantedAuthority("ROLE_" + x)).collect(Collectors.toList());
    }
}
