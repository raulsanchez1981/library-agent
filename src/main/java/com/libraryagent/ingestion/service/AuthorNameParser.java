package com.libraryagent.ingestion.service;

import java.util.Arrays;
import java.util.List;

/**
 * Parsea cadenas de texto con uno o varios autores separados por coma, ampersand o "and".
 * El split es secuencial: primero por ", ", luego cada fragmento por " & ", luego por " and ".
 */
final class AuthorNameParser {

    private AuthorNameParser() {}

    static List<String> parse(String rawAuthors) {
        if (rawAuthors == null || rawAuthors.isBlank()) {
            return List.of();
        }
        return Arrays.stream(rawAuthors.split(", "))
                .flatMap(fragment -> Arrays.stream(fragment.split(" & ")))
                .flatMap(fragment -> Arrays.stream(fragment.split(" and ")))
                .map(String::strip)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
    }
}
