package com.pcis.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = {"com.pcis.reporting", "com.pcis.notification"})
@ComponentScan(basePackages = {"com.pcis.reporting", "com.pcis.notification"})
public class ReportingApplication {
  public static void main(String[] args) {
    SpringApplication.run(ReportingApplication.class, args);
  }
}
