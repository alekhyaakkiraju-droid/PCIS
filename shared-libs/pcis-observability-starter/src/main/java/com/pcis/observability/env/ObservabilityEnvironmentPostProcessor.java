package com.pcis.observability.env;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Registers low-precedence actuator / OTel defaults so applications can override them.
 */
public class ObservabilityEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

  public static final String PROPERTY_SOURCE_NAME = "pcisObservabilityDefaults";

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    Map<String, Object> defaults = new LinkedHashMap<>();

    defaults.put("management.endpoints.web.exposure.include", "health,info,prometheus,metrics");
    defaults.put("management.endpoint.health.probes.enabled", "true");
    defaults.put("management.endpoint.health.show-details", "when_authorized");
    defaults.put("management.endpoint.health.group.liveness.include", "ping,livenessState");
    // db / keycloak / flyway are included when those indicators exist; missing ones are ignored
    defaults.put("management.endpoint.health.group.readiness.include", "readinessState,db,keycloak");
    defaults.put("management.endpoint.health.group.startup.include", "ping,flyway");
    defaults.put("management.prometheus.metrics.export.enabled", "true");
    defaults.put("management.metrics.distribution.percentiles-histogram.http.server.requests", "true");

    defaults.put("pcis.observability.trace-sample-rate", "0.1");

    String sampleRate = environment.getProperty("pcis.observability.trace-sample-rate", "0.1");
    defaults.put("otel.traces.sampler", "parentbased_traceidratio");
    defaults.put("otel.traces.sampler.arg", sampleRate);

    // Prefer silent local/dev when no collector is configured
    defaults.put("otel.exporter.otlp.endpoint", "http://localhost:4317");
    defaults.put("otel.metrics.exporter", "none");
    defaults.put("otel.logs.exporter", "none");

    environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE - 100;
  }
}
