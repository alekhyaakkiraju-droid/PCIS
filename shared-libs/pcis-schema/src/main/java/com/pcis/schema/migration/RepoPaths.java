package com.pcis.schema.migration;

import java.nio.file.Files;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class RepoPaths {

    private RepoPaths() {}

    public static Path findRepoRoot() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        Path[] candidates = {
                cwd,
                cwd.getParent(),
                cwd.getParent() != null ? cwd.getParent().getParent() : null
        };
        for (Path candidate : candidates) {
            if (candidate != null && Files.isRegularFile(candidate.resolve("docs/data-dictionary.yaml"))) {
                return candidate;
            }
        }
        throw new IllegalStateException("Cannot locate repo root (docs/data-dictionary.yaml)");
    }

    public static Path dataDictionary() {
        return findRepoRoot().resolve("docs/data-dictionary.yaml");
    }

    public static Path flywayBaselineSql() {
        Path fromModule = Path.of("db/migration/V1__baseline_schema.sql").toAbsolutePath();
        if (Files.isRegularFile(fromModule)) {
            return fromModule;
        }
        return findRepoRoot()
                .resolve("shared-libs/pcis-schema/db/migration/V1__baseline_schema.sql");
    }
}
