package com.pcis.batch.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuditBatchApplication {

  public static void main(String[] args) {
    int exitCode = SpringApplication.exit(SpringApplication.run(AuditBatchApplication.class, args));
    System.exit(exitCode);
  }
}
