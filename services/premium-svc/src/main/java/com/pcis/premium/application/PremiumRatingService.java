package com.pcis.premium.application;

import org.springframework.stereotype.Service;

@Service
public class PremiumRatingService {

  public void ensureReadPathWired(String calculationId) {
    if (calculationId == null || calculationId.isBlank()) {
      throw new IllegalArgumentException("calculationId is required");
    }
  }
}
