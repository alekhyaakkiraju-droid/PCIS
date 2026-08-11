package com.pcis.schema.classification;

import java.util.Locale;
import java.util.Optional;

public enum ClassificationTier {
    PUBLIC,
    INTERNAL,
    CONFIDENTIAL,
    RESTRICTED;

    public static Optional<ClassificationTier> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(ClassificationTier.valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public int defaultRetentionDays() {
        return switch (this) {
            case PUBLIC -> 90;
            case INTERNAL -> 180;
            case CONFIDENTIAL, RESTRICTED -> 365;
        };
    }

    public boolean requiresMinimumRetention(int retentionDays) {
        return switch (this) {
            case CONFIDENTIAL, RESTRICTED -> retentionDays >= 365;
            default -> retentionDays > 0;
        };
    }
}
