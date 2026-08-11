package com.pcis.schema.migration;

/**
 * Structured PASS/FAIL result for one monetary column precision check.
 */
public record ColumnCheckResult(
        String tableName,
        String columnName,
        boolean passed,
        String message,
        MonetaryColumnSpec expected,
        String actualDataType,
        Integer actualPrecision,
        Integer actualScale) {

    public String status() {
        return passed ? "PASS" : "FAIL";
    }

    public String formatLine() {
        StringBuilder sb = new StringBuilder();
        sb.append(tableName.toLowerCase())
                .append('.')
                .append(columnName.toLowerCase())
                .append(": ")
                .append(status());
        if (expected != null) {
            sb.append(" [expected ")
                    .append(expected.expectedPgType())
                    .append(' ')
                    .append(expected.kind())
                    .append(']');
        }
        if (actualDataType != null) {
            sb.append(" [actual ").append(describeActual()).append(']');
        }
        if (message != null && !message.isBlank()) {
            sb.append(" — ").append(message);
        }
        return sb.toString();
    }

    private String describeActual() {
        if ("numeric".equalsIgnoreCase(actualDataType)
                && actualPrecision != null
                && actualScale != null) {
            return "NUMERIC(" + actualPrecision + "," + actualScale + ")";
        }
        return actualDataType;
    }
}
