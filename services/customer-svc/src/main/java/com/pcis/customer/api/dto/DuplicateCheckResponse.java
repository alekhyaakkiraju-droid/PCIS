package com.pcis.customer.api.dto;

import com.pcis.customer.domain.DuplicateCandidate;
import java.util.Optional;

public record DuplicateCheckResponse(
    Integer custId, boolean duplicateFound, DuplicateCandidateResponse existingCustomer) {

  public static DuplicateCheckResponse from(Integer custId, Optional<DuplicateCandidate> duplicate) {
    return new DuplicateCheckResponse(
        custId,
        duplicate.isPresent(),
        duplicate.map(DuplicateCandidateResponse::from).orElse(null));
  }

  public record DuplicateCandidateResponse(Integer custId, String custName, String custStatus) {
    static DuplicateCandidateResponse from(DuplicateCandidate candidate) {
      return new DuplicateCandidateResponse(
          candidate.custId(), candidate.custName(), candidate.custStatus());
    }
  }
}
