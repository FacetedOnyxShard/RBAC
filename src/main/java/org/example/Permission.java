package org.example;


public record Permission(String name, String resource, String description) {
    public Permission(String name, String resource, String description) {
        String transformedName = name.trim().toUpperCase();
        String transformedResource = resource.trim().toLowerCase();
        String transformedDescription = description.trim();

        if (transformedName.contains(" ")) {
            throw new IllegalArgumentException("Name must not contain spaces");
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
