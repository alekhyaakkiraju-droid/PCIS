package com.pcis.configsvc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.pcis.configsvc", "com.pcis.config", "com.pcis.outbox", "com.pcis.error"})
@EntityScan(basePackages = {"com.pcis.configsvc", "com.pcis.config.entity", "com.pcis.outbox"})
@EnableJpaRepositories(
    basePackages = {"com.pcis.configsvc", "com.pcis.config.repository", "com.pcis.outbox"})
public class ConfigSvcApplication {

  public static void main(String[] args) {
    SpringApplication.run(ConfigSvcApplication.class, args);
  }
}
