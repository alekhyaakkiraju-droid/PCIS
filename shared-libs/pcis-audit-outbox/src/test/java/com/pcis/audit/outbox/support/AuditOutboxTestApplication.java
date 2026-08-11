package com.pcis.audit.outbox.support;

import com.pcis.audit.outbox.AuditOutboxAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(
    scanBasePackageClasses = {AuditOutboxAutoConfiguration.class, AuditOutboxTestApplication.class})
@EnableTransactionManagement
public class AuditOutboxTestApplication {

  public static void main(String[] args) {
    SpringApplication.run(AuditOutboxTestApplication.class, args);
  }
}
