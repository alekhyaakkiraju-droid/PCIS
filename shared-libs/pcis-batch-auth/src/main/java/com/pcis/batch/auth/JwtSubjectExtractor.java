package com.pcis.batch.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Extracts the {@code sub} claim from an unsigned JWT access token payload.
 */
public final class JwtSubjectExtractor {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private JwtSubjectExtractor() {}

  public static String extractSubject(String accessToken) {
    if (accessToken == null || accessToken.isBlank()) {
      throw new BatchConfigurationException("Access token is blank");
    }
    String[] parts = accessToken.split("\\.");
    if (parts.length < 2) {
      throw new BatchConfigurationException("Malformed JWT access token");
    }
    try {
      byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
      JsonNode payload =
          MAPPER.readTree(new String(payloadBytes, StandardCharsets.UTF_8));
      JsonNode sub = payload.get("sub");
      if (sub == null || sub.asText().isBlank()) {
        throw new BatchConfigurationException("JWT access token missing sub claim");
      }
      return sub.asText();
    } catch (BatchConfigurationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BatchConfigurationException("Unable to parse JWT access token", ex);
    }
  }
}
