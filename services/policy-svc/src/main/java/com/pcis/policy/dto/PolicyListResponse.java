package com.pcis.policy.dto;

import java.util.List;

public record PolicyListResponse(List<PolicyResponse> content, PageMetadata page) {

  public record PageMetadata(
      int number, int size, long totalElements, int totalPages) {}
}
