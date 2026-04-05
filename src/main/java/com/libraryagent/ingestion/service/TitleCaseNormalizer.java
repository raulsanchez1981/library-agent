package com.libraryagent.ingestion.service;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Normaliza cadenas de texto a Title Case: primera letra de cada palabra en mayúscula,
 * resto en minúsculas. Elimina espacios extra.
 */
final class TitleCaseNormalizer {

    private TitleCaseNormalizer() {}

    private static String capitalizeWord(String word) {
        if (word.isEmpty()) return word;
        StringBuilder sb = new StringBuilder(word.length());
        boolean capitalizeNext = true;
        for (char c : word.toCharArray()) {
            if (capitalizeNext && Character.isLetter(c)) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else if (c == '.') {
                sb.append(c);
                capitalizeNext = true;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    static String normalize(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.strip().replaceAll("\\s+", " ");
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        return Arrays.stream(trimmed.split(" "))
                .map(TitleCaseNormalizer::capitalizeWord)
                .collect(Collectors.joining(" "));
    }
}
