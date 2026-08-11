package com.pcis.billing.batch.bil003b.domain;

import java.time.LocalDate;
import java.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Maps legacy CHAR(1) billing frequency codes to {@link Period} intervals. */
public final class FrequencyIntervalMapper {

  private static final Logger log = LoggerFactory.getLogger(FrequencyIntervalMapper.class);

  private FrequencyIntervalMapper() {}

  public static Period toPeriod(String billFreq) {
    if (billFreq == null || billFreq.isBlank()) {
      log.warn("Unknown billing frequency '{}'; defaulting to annual interval", billFreq);
      return Period.ofYears(1);
    }
    return switch (billFreq.trim().toUpperCase()) {
      case "M" -> Period.ofMonths(1);
      case "Q" -> Period.ofMonths(3);
      case "S" -> Period.ofMonths(6);
      case "A" -> Period.ofYears(1);
      default -> {
        log.warn("Unknown billing frequency '{}'; defaulting to annual interval", billFreq);
        yield Period.ofYears(1);
      }
    };
  }

  public static LocalDate nextDueDate(
      LocalDate lastDueDate, String billFreq, LocalDate referenceDate) {
    if (lastDueDate == null) {
      return referenceDate;
    }
    return lastDueDate.plus(toPeriod(billFreq));
  }
}
