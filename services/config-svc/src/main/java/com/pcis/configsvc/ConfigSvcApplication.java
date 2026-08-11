package com.pcis.configsvc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.pcis.configsvc", "com.pcis.config", "com.pcis.outbox", "com.pcis.error"})
public class ConfigSvcApplication {

  public static void main(String[] args) {
    SpringApplication.run(ConfigSvcApplication.class, args);
  }
}
