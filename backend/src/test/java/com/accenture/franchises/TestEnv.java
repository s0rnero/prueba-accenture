package com.accenture.franchises;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class TestEnv {

    private static final Map<String, String> FILE = load();

    private TestEnv() {
    }

    static String get(String key) {
        String fromEnvironment = System.getenv(key);
        if (fromEnvironment != null && !fromEnvironment.isBlank()) {
            return fromEnvironment;
        }
        String fromFile = FILE.get(key);
        if (fromFile == null || fromFile.isBlank()) {
            throw new IllegalStateException("Falta " + key + " en el .env de la raiz del proyecto.");
        }
        return fromFile;
    }

    private static Map<String, String> load() {
        for (String candidate : List.of(".env", "../.env", "../../.env")) {
            Path path = Path.of(candidate);
            if (Files.isReadable(path)) {
                return parse(path);
            }
        }
        return Map.of();
    }

    private static Map<String, String> parse(Path path) {
        Map<String, String> values = new HashMap<>();
        try {
            for (String line : Files.readAllLines(path)) {
                String clean = line.trim();
                if (clean.isEmpty() || clean.startsWith("#") || !clean.contains("=")) {
                    continue;
                }
                int separator = clean.indexOf('=');
                values.put(clean.substring(0, separator).trim(),
                        clean.substring(separator + 1).trim());
            }
        } catch (IOException error) {
            throw new IllegalStateException("No se pudo leer " + path, error);
        }
        return values;
    }
}
