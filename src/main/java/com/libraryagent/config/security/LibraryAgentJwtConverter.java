package com.libraryagent.config.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Mapea el claim "groups" de Authentik a GrantedAuthority de Spring Security.
 * Requiere que el provider de Authentik tenga configurado el scope mapping de grupos.
 *
 * Mapeo:
 *   library-admin  → ROLE_ADMIN
 *   (resto)        → ROLE_VIEWER
 */
public class LibraryAgentJwtConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String GROUPS_CLAIM = "groups";
    private static final String GROUP_ADMIN = "library-admin";

    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Collection<GrantedAuthority> defaultAuthorities = defaultConverter.convert(jwt);

        List<String> groups = jwt.getClaimAsStringList(GROUPS_CLAIM);
        if (groups == null || groups.isEmpty()) {
            return defaultAuthorities;
        }

        List<GrantedAuthority> groupAuthorities = groups.stream()
                .map(this::toRole)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();

        return Stream.concat(defaultAuthorities.stream(), groupAuthorities.stream()).toList();
    }

    private String toRole(String group) {
        return GROUP_ADMIN.equals(group) ? "ROLE_ADMIN" : "ROLE_VIEWER";
    }
}
