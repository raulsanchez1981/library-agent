package com.libraryagent.shared.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownUtilsTest {

    @Test
    void shouldReturnNullWhenInputIsNull() {
        assertThat(MarkdownUtils.stripFences(null)).isNull();
    }

    @Test
    void shouldReturnEmptyStringWhenInputIsEmpty() {
        assertThat(MarkdownUtils.stripFences("")).isEqualTo("");
    }

    @Test
    void shouldReturnTextUnchangedWhenNoFences() {
        String json = "[{\"title\": \"Dune\"}]";
        assertThat(MarkdownUtils.stripFences(json)).isEqualTo(json);
    }

    @Test
    void shouldStripJsonFenceWithLanguageTag() {
        String raw = "```json\n[{\"title\": \"Dune\"}]\n```";
        assertThat(MarkdownUtils.stripFences(raw)).isEqualTo("[{\"title\": \"Dune\"}]");
    }

    @Test
    void shouldStripGenericFenceWithoutLanguageTag() {
        String raw = "```\n[{\"title\": \"Dune\"}]\n```";
        assertThat(MarkdownUtils.stripFences(raw)).isEqualTo("[{\"title\": \"Dune\"}]");
    }

    @Test
    void shouldStripFencesAndTrimWhitespace() {
        String raw = "  ```json\n  [{\"title\": \"Dune\"}]  \n```  ";
        assertThat(MarkdownUtils.stripFences(raw)).isEqualTo("[{\"title\": \"Dune\"}]");
    }
}
