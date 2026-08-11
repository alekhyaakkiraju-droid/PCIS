package com.pcis.claims.dto;

import com.pcis.claims.domain.ApprovalEntity;
import com.pcis.claims.domain.ClaimEntity;
import com.pcis.claims.domain.ClaimReserveEntity;
import org.springframework.stereotype.Component;

@Component
public class ClaimResponseMapper {

  public ClaimResponse toClaimResponse(ClaimEntity entity) {
    return new ClaimResponse(
        entity.getClaimNbr(),
        entity.getPolNbr(),
        entity.getCustId(),
        entity.getLossDate(),
        entity.getClaimType(),
        entity.getClaimStatus());
  }

  public ReserveResponse toReserveResponse(ClaimReserveEntity entity) {
    return new ReserveResponse(
        entity.getReserveId(),
        entity.getClaim().getClaimNbr(),
        entity.getReserveType(),
        entity.getApprovedAmt(),
        entity.getPaidToDate(),
        entity.getReserveStatus());
  }

  public ApprovalResponse toApprovalResponse(ApprovalEntity entity) {
    return new ApprovalResponse(
        entity.getApprovalId(),
        entity.getClaim().getClaimNbr(),
        entity.getReserve().getReserveId(),
        entity.getApproverId(),
        entity.getApprovalStatus(),
        entity.getApprovalDate());
  }
}
