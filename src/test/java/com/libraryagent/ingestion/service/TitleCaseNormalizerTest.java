package com.libraryagent.ingestion.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TitleCaseNormalizerTest {

    @Test
    void shouldNormalizeToTitleCase() {
        assertThat(TitleCaseNormalizer.normalize("el problema de los TRES cuerpos"))
                .isEqualTo("El Problema De Los Tres Cuerpos");
    }

    @Test
    void shouldReturnNullWhenInputIsNull() {
        assertThat(TitleCaseNormalizer.normalize(null)).isNull();
    }

    @Test
    void shouldHandleExtraWhitespace() {
        assertThat(TitleCaseNormalizer.normalize("  dune   el  despertar  "))
                .isEqualTo("Dune El Despertar");
    }

    @Test
    void shouldReturnEmptyStringWhenInputIsBlank() {
        assertThat(TitleCaseNormalizer.normalize("   ")).isEqualTo("");
    }

    @Test
    void shouldCapitalizeEachWord() {
        assertThat(TitleCaseNormalizer.normalize("FUNDACIÓN E IMPERIO"))
                .isEqualTo("Fundación E Imperio");
    }

    @Test
    void shouldHandleSingleWord() {
        assertThat(TitleCaseNormalizer.normalize("dune")).isEqualTo("Dune");
    }

    @Test
    void shouldCapitalizeLettersAfterPeriodForInitials() {
        assertThat(TitleCaseNormalizer.normalize("j.k. rowling")).isEqualTo("J.K. Rowling");
        assertThat(TitleCaseNormalizer.normalize("j.r.r. tolkien")).isEqualTo("J.R.R. Tolkien");
        assertThat(TitleCaseNormalizer.normalize("e.f. mendoza")).isEqualTo("E.F. Mendoza");
    }
}
