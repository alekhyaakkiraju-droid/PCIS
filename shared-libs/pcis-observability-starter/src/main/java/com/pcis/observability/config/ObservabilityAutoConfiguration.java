package com.pcis.observability.config;

import com.pcis.observability.filter.CorrelationIdFilter;
import com.pcis.observability.metrics.BatchJobExitCodeListener;
import com.pcis.observability.metrics.BatchJobExitCodeMetrics;
import com.pcis.observability.metrics.OutboxEventMetricsRepository;
import com.pcis.observability.metrics.OutboxMetrics;
import com.pcis.observability.propagation.CorrelationIdRestTemplateCustomizer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Auto-configuration for correlation MDC, Micrometer common tags, and correlation propagation.
 */
@AutoConfiguration
@EnableConfigurationProperties(ObservabilityProperties.class)
@ConditionalOnProperty(prefix = "pcis.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ObservabilityAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
  public CorrelationIdFilter correlationIdFilter(
      ObservabilityProperties properties, Environment environment) {
    String serviceName = environment.getProperty("spring.application.name", "pcis-service");
    return new CorrelationIdFilter(properties, serviceName);
  }

  @Bean
  @ConditionalOnMissingBean(name = "correlationIdFilterRegistration")
  @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
  public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration(
      CorrelationIdFilter correlationIdFilter) {
    FilterRegistrationBean<CorrelationIdFilter> registration =
        new FilterRegistrationBean<>(correlationIdFilter);
    registration.setName("pcisCorrelationIdFilter");
    registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
    registration.addUrlPatterns("/*");
    return registration;
  }

  @Bean
  @ConditionalOnMissingBean(name = "pcisCommonTagsMeterRegistryCustomizer")
  public MeterRegistryCustomizer<MeterRegistry> pcisCommonTagsMeterRegistryCustomizer(
      Environment environment) {
    String service = environment.getProperty("spring.application.name", "pcis-service");
    String env =
        firstNonBlank(
            environment.getProperty("pcis.observability.environment"),
            environment.getProperty("spring.profiles.active"),
            "default");
    return registry ->
        registry.config().commonTags("service", service, "environment", env.split(",")[0].trim());
  }

  @Bean
  @ConditionalOnMissingBean(name = "pcisDenyHighCardinalityMeterFilter")
  public MeterFilter pcisDenyHighCardinalityMeterFilter() {
    return MeterFilter.denyNameStartsWith("tomcat.sessions.");
  }

  @Bean
  @ConditionalOnClass(RestTemplate.class)
  @ConditionalOnMissingBean(CorrelationIdRestTemplateCustomizer.class)
  public RestTemplateCustomizer correlationIdRestTemplateCustomizer(
      ObservabilityProperties properties) {
    return new CorrelationIdRestTemplateCustomizer(properties);
  }

  @Bean
  @ConditionalOnMissingBean(name = "pcisTraceSampleRateConfigurer")
  public TraceSampleRateConfigurer pcisTraceSampleRateConfigurer(
      ObservabilityProperties properties, ObjectProvider<Environment> environment) {
    return new TraceSampleRateConfigurer(properties, environment.getIfAvailable());
  }

  @Bean
  @ConditionalOnBean(OutboxEventMetricsRepository.class)
  @ConditionalOnMissingBean
  public OutboxMetrics outboxMetrics(
      MeterRegistry registry,
      OutboxEventMetricsRepository repository,
      Environment environment) {
    String serviceName = environment.getProperty("spring.application.name", "pcis-service");
    return new OutboxMetrics(registry, repository, serviceName);
  }

  @Bean
  @ConditionalOnClass(name = "org.springframework.batch.core.JobExecutionListener")
  @ConditionalOnBean(MeterRegistry.class)
  @ConditionalOnMissingBean
  public BatchJobExitCodeMetrics batchJobExitCodeMetrics(MeterRegistry registry) {
    return new BatchJobExitCodeMetrics(registry);
  }

  @Bean
  @ConditionalOnClass(name = "org.springframework.batch.core.JobExecutionListener")
  @ConditionalOnBean(BatchJobExitCodeMetrics.class)
  @ConditionalOnMissingBean
  public BatchJobExitCodeListener batchJobExitCodeListener(
      BatchJobExitCodeMetrics batchJobExitCodeMetrics) {
    return new BatchJobExitCodeListener(batchJobExitCodeMetrics);
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return null;
  }

  /**
   * Applies {@code pcis.observability.trace-sample-rate} to the OTel sampler argument when the
   * process property is unset.
   */
  public static final class TraceSampleRateConfigurer {

    private final double sampleRate;

    TraceSampleRateConfigurer(ObservabilityProperties properties, Environment environment) {
      this.sampleRate = properties.getTraceSampleRate();
      String argKey = "otel.traces.sampler.arg";
      boolean alreadySet =
          System.getProperty(argKey) != null
              || (environment != null && environment.getProperty(argKey) != null);
      if (!alreadySet) {
        System.setProperty(argKey, Double.toString(sampleRate));
      }
      if (System.getProperty("otel.traces.sampler") == null
          && (environment == null || environment.getProperty("otel.traces.sampler") == null)) {
        System.setProperty("otel.traces.sampler", "parentbased_traceidratio");
      }
    }

    public double getSampleRate() {
      return sampleRate;
    }
  }
}
