package com.pcis.outbox.support;

import com.pcis.outbox.OutboxAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackageClasses = {OutboxAutoConfiguration.class, OutboxTestApplication.class})
@EnableTransactionManagement
public class OutboxTestApplication {

  public static void main(String[] args) {
    SpringApplication.run(OutboxTestApplication.class, args);
  }
}
