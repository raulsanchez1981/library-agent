package com.libraryagent.ingestion.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libraryagent.config.security.SecurityConfig;
import com.libraryagent.ingestion.dto.GenreDto;
import com.libraryagent.ingestion.dto.VerifiedTitleDetailDto;
import com.libraryagent.ingestion.service.CasaDelLibroScraperService;
import com.libraryagent.ingestion.service.VerifiedTitleEnrichService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VerifiedTitleEnrichController.class)
@Import(SecurityConfig.class)
class VerifiedTitleEnrichControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    CasaDelLibroScraperService scraperService;

    @MockitoBean
    VerifiedTitleEnrichService enrichService;

    @Test
    void shouldEnrichVerifiedTitleAndReturnDetailDto() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        VerifiedTitleDetailDto dto = new VerifiedTitleDetailDto(
                id,
                "El Nombre del Viento",
                "https://img.casadellibro.com/portada.jpg",
                "Un libro épico.",
                "{\"ISBN\":\"978-84-12345-67-8\"}",
                "https://www.casadellibro.com/libro/el-nombre-del-viento",
                List.of(new GenreDto(UUID.randomUUID(), "Fantasía"))
        );
        when(enrichService.enrichFromCdl(eq(id), any())).thenReturn(dto);

        String requestBody = """
                {"casaDelLibroUrl": "https://www.casadellibro.com/libro/el-nombre-del-viento"}
                """;

        // When / Then
        mockMvc.perform(post("/api/v1/admin/verified-titles/{id}/enrich-cdl", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("El Nombre del Viento"))
                .andExpect(jsonPath("$.coverUrl").value("https://img.casadellibro.com/portada.jpg"))
                .andExpect(jsonPath("$.genres[0].name").value("Fantasía"));
    }

    @Test
    void shouldReturn403WhenCalledWithoutAdminRole() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        String requestBody = """
                {"casaDelLibroUrl": "https://www.casadellibro.com/libro/test"}
                """;

        // When / Then
        mockMvc.perform(post("/api/v1/admin/verified-titles/{id}/enrich-cdl", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn401WhenCalledWithoutAuthentication() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        String requestBody = """
                {"casaDelLibroUrl": "https://www.casadellibro.com/libro/test"}
                """;

        // When / Then
        mockMvc.perform(post("/api/v1/admin/verified-titles/{id}/enrich-cdl", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn400WhenCasaDelLibroUrlIsBlank() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        String requestBody = """
                {"casaDelLibroUrl": ""}
                """;

        // When / Then
        mockMvc.perform(post("/api/v1/admin/verified-titles/{id}/enrich-cdl", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
