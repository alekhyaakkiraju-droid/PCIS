package com.pcis.claims.dto;

import jakarta.validation.constraints.NotNull;

public record CreateApprovalRequest(@NotNull Long reserveId) {}
