package com.libraryagent.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import com.libraryagent.ingestion.repository.ExtractedBookRepository;
import com.libraryagent.ingestion.service.BibliotecaService;
import com.libraryagent.ingestion.service.ExtractedBookAdminService;
import com.libraryagent.ingestion.service.CasaDelLibroScraperService;
import com.libraryagent.ingestion.service.CdlAutoSearchService;
import com.libraryagent.ingestion.service.AuthorService;
import com.libraryagent.ingestion.service.GenreEnrichmentService;
import com.libraryagent.ingestion.service.GenreService;
import com.libraryagent.ingestion.service.VerifiedTitleEnrichService;
import com.libraryagent.recommendation.service.RecommendationService;
import com.libraryagent.userprofile.service.ReadingHistoryService;
import com.libraryagent.userprofile.service.UserProfileService;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    MockMvc mockMvc;

    // Evita que Spring intente contactar con Authentik al construir el JwtDecoder
    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    RecommendationService recommendationService;

    @MockitoBean
    UserProfileService userProfileService;

    @MockitoBean
    ReadingHistoryService readingHistoryService;

    @MockitoBean
    ExtractedBookRepository extractedBookRepository;

    @MockitoBean
    ExtractedBookAdminService extractedBookAdminService;

    @MockitoBean
    BibliotecaService bibliotecaService;

    @MockitoBean
    GenreService genreService;

    @MockitoBean
    VerifiedTitleEnrichService verifiedTitleEnrichService;

    @MockitoBean
    CasaDelLibroScraperService casaDelLibroScraperService;

    @MockitoBean
    CdlAutoSearchService cdlAutoSearchService;

    @MockitoBean
    GenreEnrichmentService genreEnrichmentService;

    @MockitoBean
    AuthorService authorService;

    // Registra el TestController en el contexto — @WebMvcTest no escanea clases internas
    @TestConfiguration
    static class TestControllerConfig {
        @Bean
        TestController testController() {
            return new TestController();
        }
    }

    @Test
    void shouldPermitActuatorWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn401WhenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/api/test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn200WithValidJwtToken() throws Exception {
        mockMvc.perform(get("/api/test")
                .with(jwt()))
                .andExpect(status().isOk());
    }

    // jwt() inyecta el Authentication directamente (bypasa el JwtAuthenticationConverter),
    // así que las authorities hay que darlas explícitamente. El mapeo grupos→roles
    // se testa en LibraryAgentJwtConverterTest.
    @Test
    void shouldReturn200WhenUserHasAdminRole() throws Exception {
        mockMvc.perform(get("/api/test/admin")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn403WhenUserLacksAdminRole() throws Exception {
        mockMvc.perform(get("/api/test/admin")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VIEWER"))))
                .andExpect(status().isForbidden());
    }

    @RestController
    static class TestController {

        @GetMapping("/actuator/health")
        String health() {
            return "UP";
        }

        @GetMapping("/api/test")
        String protectedEndpoint() {
            return "ok";
        }

        @GetMapping("/api/test/admin")
        @PreAuthorize("hasRole('ADMIN')")
        String adminEndpoint() {
            return "admin-ok";
        }
    }
}
