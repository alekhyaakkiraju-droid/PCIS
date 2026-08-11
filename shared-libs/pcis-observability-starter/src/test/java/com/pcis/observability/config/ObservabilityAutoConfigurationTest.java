package com.pcis.observability.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.observability.filter.CorrelationIdFilter;
import com.pcis.observability.propagation.CorrelationIdRestTemplateCustomizer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.client.RestTemplate;

class ObservabilityAutoConfigurationTest {

  private final WebApplicationContextRunner webRunner =
      new WebApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  ObservabilityAutoConfiguration.class,
                  MetricsAutoConfiguration.class,
                  CompositeMeterRegistryAutoConfiguration.class,
                  WebMvcAutoConfiguration.class))
          .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
          .withPropertyValues(
              "spring.application.name=claims-svc",
              "pcis.observability.trace-sample-rate=0.25",
              "otel.sdk.disabled=true");

  private final ApplicationContextRunner nonWebRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ObservabilityAutoConfiguration.class))
          .withPropertyValues("spring.application.name=batch-job", "otel.sdk.disabled=true");

  @Test
  void registersCorrelationFilterAndMeterCustomizerOnWebApp() {
    webRunner.run(
        context -> {
          assertThat(context).hasSingleBean(CorrelationIdFilter.class);
          assertThat(context).hasBean("correlationIdFilterRegistration");
          assertThat(context.getBean("correlationIdFilterRegistration", FilterRegistrationBean.class)
                  .getFilter())
              .isInstanceOf(CorrelationIdFilter.class);
          assertThat(context).hasBean("pcisCommonTagsMeterRegistryCustomizer");
          assertThat(context).hasSingleBean(ObservabilityProperties.class);
          assertThat(context.getBean(ObservabilityProperties.class).getTraceSampleRate())
              .isEqualTo(0.25d);
          assertThat(context.getBean(ObservabilityAutoConfiguration.TraceSampleRateConfigurer.class)
                  .getSampleRate())
              .isEqualTo(0.25d);
        });
  }

  @Test
  void canBeDisabled() {
    webRunner
        .withPropertyValues("pcis.observability.enabled=false")
        .run(context -> assertThat(context).doesNotHaveBean(CorrelationIdFilter.class));
  }

  @Test
  void nonWebContextSkipsServletFilterBeans() {
    nonWebRunner.run(
        context -> {
          assertThat(context).doesNotHaveBean(CorrelationIdFilter.class);
          assertThat(context).hasBean("pcisCommonTagsMeterRegistryCustomizer");
          assertThat(context).hasSingleBean(ObservabilityProperties.class);
        });
  }

  @Test
  void restTemplateCustomizerPropagatesCorrelationHeader() {
    webRunner.run(
        context -> {
          assertThat(context).hasSingleBean(CorrelationIdRestTemplateCustomizer.class);
          RestTemplateCustomizerProbe probe =
              new RestTemplateCustomizerProbe(
                  context.getBean(CorrelationIdRestTemplateCustomizer.class));
          RestTemplate restTemplate = new RestTemplateBuilder(probe).build();
          assertThat(restTemplate.getInterceptors()).isNotEmpty();
        });
  }

  private static final class RestTemplateCustomizerProbe
      implements org.springframework.boot.web.client.RestTemplateCustomizer {

    private final CorrelationIdRestTemplateCustomizer delegate;

    private RestTemplateCustomizerProbe(CorrelationIdRestTemplateCustomizer delegate) {
      this.delegate = delegate;
    }

    @Override
    public void customize(RestTemplate restTemplate) {
      delegate.customize(restTemplate);
    }
  }
}
