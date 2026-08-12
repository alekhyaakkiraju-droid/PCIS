package com.pcis.batch.claims;

import com.pcis.batch.common.BatchCommonAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@Import({
  BatchCommonAutoConfiguration.class,
  com.pcis.claims.application.PaymentAuthorityService.class,
  com.pcis.claims.outbox.ClaimsOutboxWriter.class,
  com.pcis.claims.security.SecurityPrincipalAccessor.class
})
@EntityScan(basePackages = {"com.pcis.claims.domain", "com.pcis.outbox"})
@EnableJpaRepositories(basePackages = {"com.pcis.claims.domain.repository", "com.pcis.outbox"})
@ComponentScan(
    basePackages = {"com.pcis.batch.claims"},
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.pcis\\.claims\\..*"))
public class ClaimsBatchApplication {

  public static void main(String[] args) {
    int exitCode = org.springframework.boot.SpringApplication.exit(
        org.springframework.boot.SpringApplication.run(ClaimsBatchApplication.class, args));
    System.exit(exitCode);
  }
}
