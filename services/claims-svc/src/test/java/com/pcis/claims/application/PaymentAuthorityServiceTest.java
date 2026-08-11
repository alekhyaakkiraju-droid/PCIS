package com.pcis.claims.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.pcis.claims.domain.ApprovalEntity;
import com.pcis.claims.domain.ClaimAdjusterEntity;
import com.pcis.claims.domain.ClaimEntity;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentAuthorityServiceTest {

  @Mock private ApprovalRepository approvalRepository;
  @Mock private ClaimAdjusterRepository claimAdjusterRepository;
  @Mock private ClaimReserveRepository claimReserveRepository;

  private PaymentAuthorityService paymentAuthorityService;

  private ClaimEntity claim;
  private ClaimReserveEntity reserve;
  private ApprovalEntity approval;
  private ClaimAdjusterEntity adjuster;

  @BeforeEach
  void setUp() {
    paymentAuthorityService =
        new PaymentAuthorityService(
            approvalRepository, claimAdjusterRepository, claimReserveRepository);

    claim = new ClaimEntity();
    claim.setClaimNbr("CLM000000001");

    reserve = new ClaimReserveEntity();
    reserve.setClaim(claim);
    reserve.setApprovedAmt(new BigDecimal("10000.00"));
    reserve.setPaidToDate(BigDecimal.ZERO);

    approval = new ApprovalEntity();
    approval.setApproverId("SUP001");
    approval.setApprovalStatus("A");
    approval.setReserve(reserve);

    adjuster = new ClaimAdjusterEntity();
    adjuster.setAdjusterId("ADJ001");
    adjuster.setAuthorityLimit(new BigDecimal("5000.00"));
  }

  @Test
  void validatePayment_succeedsWithinLimit() {
    when(claimReserveRepository.findById(1L)).thenReturn(Optional.of(reserve));
    when(approvalRepository.findByReserveReserveIdAndApprovalStatus(1L, "A"))
        .thenReturn(Optional.of(approval));
    when(claimAdjusterRepository.findById("ADJ001")).thenReturn(Optional.of(adjuster));

    PaymentAuthorityService.PaymentAuthorizationResult result =
        paymentAuthorityService.validatePayment(
            "CLM000000001", 1L, new BigDecimal("5000.00"), "ADJ001");

    assertThat(result.approval()).isEqualTo(approval);
    assertThat(result.adjuster()).isEqualTo(adjuster);
  }

  @Test
  void validatePayment_rejectsWhenNoApproval() {
    when(claimReserveRepository.findById(1L)).thenReturn(Optional.of(reserve));
    when(approvalRepository.findByReserveReserveIdAndApprovalStatus(1L, "A"))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                paymentAuthorityService.validatePayment(
                    "CLM000000001", 1L, new BigDecimal("100.00"), "ADJ001"))
        .isInstanceOf(ApprovalRequiredException.class);
  }

  @Test
  void validatePayment_rejectsSegregationOfDutiesViolation() {
    when(claimReserveRepository.findById(1L)).thenReturn(Optional.of(reserve));
    approval.setApproverId("ADJ001");
    when(approvalRepository.findByReserveReserveIdAndApprovalStatus(1L, "A"))
        .thenReturn(Optional.of(approval));

    assertThatThrownBy(
            () ->
                paymentAuthorityService.validatePayment(
                    "CLM000000001", 1L, new BigDecimal("100.00"), "ADJ001"))
        .isInstanceOf(SegregationOfDutiesViolationException.class);
  }

  @Test
  void validatePayment_rejectsAmountAboveAuthorityLimit() {
    when(claimReserveRepository.findById(1L)).thenReturn(Optional.of(reserve));
    when(approvalRepository.findByReserveReserveIdAndApprovalStatus(1L, "A"))
        .thenReturn(Optional.of(approval));
    when(claimAdjusterRepository.findById("ADJ001")).thenReturn(Optional.of(adjuster));

    assertThatThrownBy(
            () ->
                paymentAuthorityService.validatePayment(
                    "CLM000000001", 1L, new BigDecimal("5000.01"), "ADJ001"))
        .isInstanceOf(AuthorityLimitExceededException.class)
        .satisfies(
            ex -> {
              AuthorityLimitExceededException limitEx = (AuthorityLimitExceededException) ex;
              assertThat(limitEx.getAuthorityLimit()).isEqualByComparingTo("5000.00");
              assertThat(limitEx.getRequestedAmount()).isEqualByComparingTo("5000.01");
            });
  }

  @Test
  void validatePayment_rejectsAmountAboveOutstanding() {
    reserve.setPaidToDate(new BigDecimal("9500.00"));
    adjuster.setAuthorityLimit(new BigDecimal("100000.00"));
    when(claimReserveRepository.findById(1L)).thenReturn(Optional.of(reserve));
    when(approvalRepository.findByReserveReserveIdAndApprovalStatus(1L, "A"))
        .thenReturn(Optional.of(approval));
    when(claimAdjusterRepository.findById("ADJ001")).thenReturn(Optional.of(adjuster));

    assertThatThrownBy(
            () ->
                paymentAuthorityService.validatePayment(
                    "CLM000000001", 1L, new BigDecimal("600.00"), "ADJ001"))
        .isInstanceOf(InsufficientReserveException.class);
  }

  @Test
  void validatePayment_rejectsZeroOutstanding() {
    reserve.setPaidToDate(new BigDecimal("10000.00"));
    when(claimReserveRepository.findById(1L)).thenReturn(Optional.of(reserve));
    when(approvalRepository.findByReserveReserveIdAndApprovalStatus(1L, "A"))
        .thenReturn(Optional.of(approval));
    when(claimAdjusterRepository.findById("ADJ001")).thenReturn(Optional.of(adjuster));

    assertThatThrownBy(
            () ->
                paymentAuthorityService.validatePayment(
                    "CLM000000001", 1L, new BigDecimal("1.00"), "ADJ001"))
        .isInstanceOf(InsufficientReserveException.class);
  }

  @Test
  void validatePayment_rejectsZeroAmount() {
    assertThatThrownBy(
            () ->
                paymentAuthorityService.validatePayment(
                    "CLM000000001", 1L, BigDecimal.ZERO, "ADJ001"))
        .isInstanceOf(InvalidPaymentAmountException.class);
  }
}
