package com.pcis.audit;

import com.pcis.outbox.OutboxAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = OutboxAutoConfiguration.class)
public class AuditApplication {

  public static void main(String[] args) {
    SpringApplication.run(AuditApplication.class, args);
  }
}
