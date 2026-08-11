package com.pcis.premium;

import com.pcis.error.PcisExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(PcisExceptionHandler.class)
public class PremiumSvcApplication {

  public static void main(String[] args) {
    SpringApplication.run(PremiumSvcApplication.class, args);
  }
}
