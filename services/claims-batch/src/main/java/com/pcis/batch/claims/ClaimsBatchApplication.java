package com.pcis.batch.claims;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ClaimsBatchApplication {

  public static void main(String[] args) {
    int exitCode = SpringApplication.exit(SpringApplication.run(ClaimsBatchApplication.class, args));
    System.exit(exitCode);
  }
}
