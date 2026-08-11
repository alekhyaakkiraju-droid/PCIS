package com.pcis.customer.api.dto;

public record SectionWrapper<T>(SectionStatus status, T data, String message) {

  public enum SectionStatus {
    AVAILABLE,
    UNAVAILABLE,
    ERROR
  }

  public static <T> SectionWrapper<T> available(T data) {
    return new SectionWrapper<>(SectionStatus.AVAILABLE, data, null);
  }

  public static <T> SectionWrapper<T> unavailable(String message) {
    return new SectionWrapper<>(SectionStatus.UNAVAILABLE, null, message);
  }

  public static <T> SectionWrapper<T> error(String message) {
    return new SectionWrapper<>(SectionStatus.ERROR, null, message);
  }
}
