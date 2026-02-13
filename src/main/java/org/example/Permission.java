package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record Permission(String name, String resource, String description) {
    private static final Set<String> ALLOWED_NAMES = Set.of("READ", "WRITE", "DELETE");

    public Permission(String name, String resource, String description) {
        String transformedName = name.trim().toUpperCase();
        String transformedResource = resource.trim().toLowerCase();
        String transformedDescription = description.trim();

        if (transformedName.contains(" ")) {
            throw new IllegalArgumentException("Name must not contain spaces");
        }

        if (!ALLOWED_NAMES.contains(transformedName)) {
            throw new IllegalArgumentException("Name must be in allowed name range: " + ALLOWED_NAMES);
        }

        if (transformedDescription.isBlank()) {
            throw new IllegalArgumentException("Description must be not blank");
        }

        this.name = transformedName;
        this.resource = transformedResource;
        this.description = transformedDescription;
    }

    String format() {
        return String.format("%s on %s: %s", name, resource, description);
    }

    boolean matches(String namePattern, String resourcePattern) {
        boolean nameMatches = namePattern == null || name.contains(namePattern) || name.matches(namePattern);
        boolean resourceMatches = resourcePattern == null || resource.contains(resourcePattern) || resource.matches(resourcePattern);
        return nameMatches && resourceMatches;
    }
}
