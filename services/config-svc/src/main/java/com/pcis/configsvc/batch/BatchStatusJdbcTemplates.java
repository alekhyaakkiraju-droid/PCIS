package com.pcis.configsvc.batch;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Wraps the per-domain JdbcTemplate map. A plain Map&lt;String, JdbcTemplate&gt; bean would be
 * silently ignored by Spring's built-in "collect all beans of type JdbcTemplate by name"
 * autowiring for Map-typed injection points, so this dedicated type is used instead.
 */
public record BatchStatusJdbcTemplates(Map<String, JdbcTemplate> byDomain) {}
