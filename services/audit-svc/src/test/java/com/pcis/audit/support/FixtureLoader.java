package com.pcis.audit.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;

public final class FixtureLoader {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private FixtureLoader() {}

  public static Map<String, Map<String, Object>> loadAuditEvents() throws IOException {
    var resource = new ClassPathResource("fixtures/audit-events.json");
    return MAPPER.readValue(
        resource.getContentAsString(StandardCharsets.UTF_8), new TypeReference<>() {});
  }
}
