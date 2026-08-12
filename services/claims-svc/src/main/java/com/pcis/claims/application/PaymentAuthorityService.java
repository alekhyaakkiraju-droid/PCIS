package com.pcis.claims.application;

import com.pcis.claims.domain.ApprovalEntity;
import com.pcis.claims.domain.ClaimAdjusterEntity;
import com.pcis.claims.domain.ClaimReserveEntity;
import com.pcis.claims.domain.repository.ApprovalRepository;
import com.pcis.claims.domain.repository.ClaimAdjusterRepository;
import com.pcis.claims.domain.repository.ClaimReserveRepository;
import com.pcis.claims.exception.ApprovalRequiredException;
import com.pcis.claims.exception.AuthorityLimitExceededException;
import com.pcis.claims.exception.InsufficientReserveException;
import com.pcis.claims.exception.InvalidPaymentAmountException;
import com.pcis.claims.exception.SegregationOfDutiesViolationException;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class PaymentAuthorityService {

  private final ApprovalRepository approvalRepository;
  private final ClaimAdjusterRepository claimAdjusterRepository;
  private final ClaimReserveRepository claimReserveRepository;

  public PaymentAuthorityService(
      ApprovalRepository approvalRepository,
      ClaimAdjusterRepository claimAdjusterRepository,
      ClaimReserveRepository claimReserveRepository) {
    this.approvalRepository = approvalRepository;
    this.claimAdjusterRepository = claimAdjusterRepository;
    this.claimReserveRepository = claimReserveRepository;
  }

  public PaymentAuthorizationResult validatePayment(
      String claimNbr, Long reserveId, BigDecimal requestedAmount, String disburserPrincipal) {
    if (requestedAmount == null || requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new InvalidPaymentAmountException();
    }

    ClaimReserveEntity reserve =
        claimReserveRepository
            .findById(reserveId)
            .filter(r -> r.getClaim().getClaimNbr().equals(claimNbr))
            .orElseThrow(() -> new ApprovalRequiredException(reserveId));

    ApprovalEntity approval =
        approvalRepository
            .findByReserveReserveIdAndApprovalStatus(reserveId, "A")
            .orElseThrow(() -> new ApprovalRequiredException(reserveId));

    if (approval.getApproverId().equals(disburserPrincipal)) {
      throw new SegregationOfDutiesViolationException();
    }

    ClaimAdjusterEntity adjuster =
        claimAdjusterRepository
            .findById(disburserPrincipal)
            .orElseThrow(
                () ->
                    new AuthorityLimitExceededException(
                        BigDecimal.ZERO, requestedAmount));

    BigDecimal outstanding = reserve.getApprovedAmt().subtract(reserve.getPaidToDate());
    if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
      throw new InsufficientReserveException(outstanding);
    }

    if (requestedAmount.compareTo(outstanding) > 0) {
      throw new InsufficientReserveException(outstanding);
    }

    if (requestedAmount.compareTo(adjuster.getAuthorityLimit()) > 0) {
      throw new AuthorityLimitExceededException(adjuster.getAuthorityLimit(), requestedAmount);
    }

    return new PaymentAuthorizationResult(approval, reserve, adjuster);
  }

  public record PaymentAuthorizationResult(
      ApprovalEntity approval, ClaimReserveEntity reserve, ClaimAdjusterEntity adjuster) {}
}
