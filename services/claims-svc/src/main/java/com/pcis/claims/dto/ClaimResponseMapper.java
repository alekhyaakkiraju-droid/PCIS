package com.pcis.claims.dto;

import com.pcis.claims.domain.ApprovalEntity;
import com.pcis.claims.domain.ClaimEntity;
import com.pcis.claims.domain.ClaimNoteEntity;
import com.pcis.claims.domain.ClaimPaymentEntity;
import com.pcis.claims.domain.ClaimReserveEntity;
import java.math.BigDecimal;
import java.util.List;
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

  public ClaimDetailResponse toClaimDetailResponse(
      ClaimEntity entity,
      BigDecimal authorityLimit,
      List<ClaimReserveEntity> reserves,
      List<ClaimPaymentEntity> payments,
      List<ClaimNoteEntity> notes) {
    return new ClaimDetailResponse(
        entity.getClaimNbr(),
        entity.getPolNbr(),
        entity.getCustId(),
        entity.getLossDate(),
        entity.getClaimType(),
        entity.getClaimStatus(),
        entity.getVersion(),
        authorityLimit,
        reserves.stream().map(this::toReserveResponse).toList(),
        payments.stream().map(this::toPaymentResponse).toList(),
        notes.stream().map(this::toNoteResponse).toList());
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

  public PaymentResponse toPaymentResponse(ClaimPaymentEntity entity) {
    return new PaymentResponse(
        entity.getPaymentId(),
        entity.getClaim().getClaimNbr(),
        entity.getPaymentAmt(),
        entity.getPaymentStatus(),
        entity.getApproval() != null ? entity.getApproval().getApprovalId() : null,
        entity.getPayeeId(),
        entity.getAdjuster() != null ? entity.getAdjuster().getAdjusterId() : null);
  }

  public NoteResponse toNoteResponse(ClaimNoteEntity entity) {
    return new NoteResponse(
        entity.getNoteId(),
        entity.getClaim().getClaimNbr(),
        entity.getNoteText(),
        entity.getCrtTimestamp());
  }
}
