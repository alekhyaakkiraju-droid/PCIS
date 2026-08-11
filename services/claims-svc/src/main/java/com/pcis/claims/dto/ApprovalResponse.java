package com.pcis.claims.dto;

import java.time.Instant;

public record ApprovalResponse(
    Long approvalId,
    String claimNbr,
    Long reserveId,
    String approverId,
    String approvalStatus,
    Instant approvalDate) {}
