package com.pcis.claims;

import com.pcis.error.PcisExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(PcisExceptionHandler.class)
public class ClaimsSvcApplication {

  public static void main(String[] args) {
    SpringApplication.run(ClaimsSvcApplication.class, args);
  }
}
