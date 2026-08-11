package com.pcis.authz;

import com.pcis.outbox.OutboxAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = OutboxAutoConfiguration.class)
public class AuthzApplication {

  public static void main(String[] args) {
    SpringApplication.run(AuthzApplication.class, args);
  }
}
