package com.pcis.batch.reconciliation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReconciliationBatchApplication {

  public static void main(String[] args) {
    int exitCode =
        SpringApplication.exit(SpringApplication.run(ReconciliationBatchApplication.class, args));
    System.exit(exitCode);
  }
}
