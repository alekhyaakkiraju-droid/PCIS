package com.pcis.classification.support;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class ClassificationTestApplication {

  public static void main(String[] args) {
    SpringApplication.run(ClassificationTestApplication.class, args);
  }
}
