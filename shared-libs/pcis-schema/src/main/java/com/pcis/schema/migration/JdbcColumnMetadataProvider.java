package com.pcis.schema.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class JdbcColumnMetadataProvider implements ColumnMetadataProvider {

    private final Map<String, ColumnMetadata> cache = new HashMap<>();

    public JdbcColumnMetadataProvider(Connection connection, List<MonetaryColumnSpec> specs)
            throws SQLException {
        for (MonetaryColumnSpec spec : specs) {
            String key = key(spec.tableName(), spec.columnName());
            cache.put(key, loadColumn(connection, spec.tableName(), spec.columnName()).orElse(null));
        }
    }

    @Override
    public Optional<ColumnMetadata> getColumn(String tableName, String columnName) {
        ColumnMetadata metadata = cache.get(key(tableName, columnName));
        return Optional.ofNullable(metadata);
    }

    private static Optional<ColumnMetadata> loadColumn(
            Connection connection, String tableName, String columnName) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                SELECT data_type, numeric_precision, numeric_scale
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                  AND column_name = ?
                """)) {
            ps.setString(1, tableName.toLowerCase(Locale.ROOT));
            ps.setString(2, columnName.toLowerCase(Locale.ROOT));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new ColumnMetadata(
                        rs.getString("data_type"),
                        rs.getObject("numeric_precision") != null ? rs.getInt("numeric_precision") : null,
                        rs.getObject("numeric_scale") != null ? rs.getInt("numeric_scale") : null));
            }
        }
    }

    private static String key(String tableName, String columnName) {
        return tableName.toLowerCase(Locale.ROOT) + "." + columnName.toLowerCase(Locale.ROOT);
    }
}
