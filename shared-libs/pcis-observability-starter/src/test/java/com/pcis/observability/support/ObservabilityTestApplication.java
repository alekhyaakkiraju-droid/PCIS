package com.pcis.observability.support;

import com.pcis.observability.MdcKeys;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class ObservabilityTestApplication {

  @RestController
  public static class ProbeController {

    private static final Logger log = LoggerFactory.getLogger(ProbeController.class);

    @GetMapping("/api/probe")
    Map<String, String> probe() {
      Map<String, String> body = new LinkedHashMap<>();
      body.put("correlationId", String.valueOf(MDC.get(MdcKeys.CORRELATION_ID)));
      body.put("service", String.valueOf(MDC.get(MdcKeys.SERVICE)));
      body.put("operation", String.valueOf(MDC.get(MdcKeys.OPERATION)));
      log.info(
          "probe ok correlationId={} email=jane.doe@example.com ssn=123-45-6789",
          MDC.get(MdcKeys.CORRELATION_ID));
      return body;
    }
  }
}
