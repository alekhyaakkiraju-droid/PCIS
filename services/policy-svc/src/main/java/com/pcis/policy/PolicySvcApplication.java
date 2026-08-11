package com.pcis.policy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class PolicySvcApplication {

  public static void main(String[] args) {
    SpringApplication.run(PolicySvcApplication.class, args);
  }
}
