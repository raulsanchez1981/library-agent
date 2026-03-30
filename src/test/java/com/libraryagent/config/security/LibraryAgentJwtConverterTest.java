package com.libraryagent.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LibraryAgentJwtConverterTest {

    private final LibraryAgentJwtConverter converter = new LibraryAgentJwtConverter();

    @Test
    void shouldMapLibraryAdminGroupToAdminRole() {
        Jwt jwt = buildJwt(List.of("library-admin"));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN");
    }

    @Test
    void shouldMapUnknownGroupToViewerRole() {
        Jwt jwt = buildJwt(List.of("other-group"));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_VIEWER")
                .doesNotContain("ROLE_ADMIN");
    }

    @Test
    void shouldMapMultipleGroupsIncludingAdmin() {
        Jwt jwt = buildJwt(List.of("library-admin", "some-other-group"));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN");
    }

    @Test
    void shouldReturnDefaultAuthoritiesWhenNoGroupsClaim() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Sin claim "groups" ni claim "scope", el JwtGrantedAuthoritiesConverter devuelve vacío
        assertThat(authorities).isEmpty();
    }

    private Jwt buildJwt(List<String> groups) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("groups", groups)
                .build();
    }
}
