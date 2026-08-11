package com.pcis.claims.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateApprovalRequest(
    @NotNull Long reserveId, @NotBlank @Size(max = 10) String approverId) {}
