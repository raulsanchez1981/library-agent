package com.libraryagent.ingestion.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libraryagent.config.security.SecurityConfig;
import com.libraryagent.ingestion.entity.GenreEntity;
import com.libraryagent.ingestion.service.GenreService;
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

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GenreController.class)
@Import(SecurityConfig.class)
class GenreControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    GenreService genreService;

    @Test
    void shouldReturnAllGenresForAuthenticatedUser() throws Exception {
        // Given
        GenreEntity aventura = new GenreEntity("Aventura");
        GenreEntity fantasia = new GenreEntity("Fantasía");
        when(genreService.findAll()).thenReturn(List.of(aventura, fantasia));

        // When / Then
        mockMvc.perform(get("/api/v1/genres")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Aventura"))
                .andExpect(jsonPath("$[1].name").value("Fantasía"));
    }

    @Test
    void shouldReturn401ForUnauthenticatedGetRequest() throws Exception {
        // When / Then
        mockMvc.perform(get("/api/v1/genres"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCreateGenreWhenAdminRole() throws Exception {
        // Given
        GenreEntity created = new GenreEntity("Ciencia Ficción");
        when(genreService.findOrCreate("Ciencia Ficción")).thenReturn(created);

        String requestBody = """
                {"name": "Ciencia Ficción"}
                """;

        // When / Then
        mockMvc.perform(post("/api/v1/genres")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Ciencia Ficción"));
    }

    @Test
    void shouldReturn403WhenNonAdminTriesToCreateGenre() throws Exception {
        // Given
        String requestBody = """
                {"name": "Terror"}
                """;

        // When / Then
        mockMvc.perform(post("/api/v1/genres")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn400WhenGenreNameIsBlank() throws Exception {
        // Given
        String requestBody = """
                {"name": ""}
                """;

        // When / Then
        mockMvc.perform(post("/api/v1/genres")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
