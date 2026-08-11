package com.pcis.batch.policy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PolicyBatchApplication {

  public static void main(String[] args) {
    int exitCode = SpringApplication.exit(SpringApplication.run(PolicyBatchApplication.class, args));
    System.exit(exitCode);
  }
}
