package com.pcis.billing;

import com.pcis.billing.api.GlobalExceptionHandler;
import com.pcis.error.PcisExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({PcisExceptionHandler.class, GlobalExceptionHandler.class})
public class BillingSvcApplication {

  public static void main(String[] args) {
    SpringApplication.run(BillingSvcApplication.class, args);
  }
}
