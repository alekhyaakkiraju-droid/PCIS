package com.pcis.batch.claims.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pcis.batch.claims.domain.ClaimPaymentBatchItem;
import com.pcis.claims.application.PaymentAuthorityService;
import com.pcis.claims.domain.ApprovalEntity;
import com.pcis.claims.domain.ClaimAdjusterEntity;
import com.pcis.claims.domain.ClaimEntity;
import com.pcis.claims.domain.ClaimReserveEntity;
import com.pcis.claims.exception.ApprovalRequiredException;
import com.pcis.claims.exception.AuthorityLimitExceededException;
import com.pcis.claims.exception.InvalidPaymentAmountException;
import com.pcis.claims.exception.SegregationOfDutiesViolationException;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ClaimPaymentItemProcessorTest {

  @Mock private PaymentAuthorityService paymentAuthorityService;

  private ClaimPaymentItemProcessor processor;

  @BeforeEach
  void setUp() {
    processor = new ClaimPaymentItemProcessor(paymentAuthorityService);
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("BATCH_SVC", "n/a"));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void returnsBatchItemWhenAuthorized() {
    ClaimReserveEntity reserve = payableReserve("1500.00", "0.00");
    ApprovalEntity approval = mock(ApprovalEntity.class);
    ClaimAdjusterEntity adjuster = mock(ClaimAdjusterEntity.class);
    when(paymentAuthorityService.validatePayment(
            eq("CLM000000001"), any(), eq(new BigDecimal("1500.00")), eq("BATCH_SVC")))
        .thenReturn(new PaymentAuthorityService.PaymentAuthorizationResult(approval, reserve, adjuster));

    ClaimPaymentBatchItem item = processor.process(reserve);

    assertThat(item.paymentAmount()).isEqualByComparingTo("1500.00");
    assertThat(item.reserve()).isSameAs(reserve);
    assertThat(item.approval()).isSameAs(approval);
  }

  @Test
  void skipsWhenNoApproval() {
    ClaimReserveEntity reserve = payableReserve("500.00", "0.00");
    when(paymentAuthorityService.validatePayment(any(), any(), any(), any()))
        .thenThrow(new ApprovalRequiredException(1L));

    assertThatThrownBy(() -> processor.process(reserve))
        .isInstanceOf(ApprovalRequiredException.class);
  }

  @Test
  void skipsWhenSamePrincipal() {
    ClaimReserveEntity reserve = payableReserve("500.00", "0.00");
    when(paymentAuthorityService.validatePayment(any(), any(), any(), any()))
        .thenThrow(new SegregationOfDutiesViolationException());

    assertThatThrownBy(() -> processor.process(reserve))
        .isInstanceOf(SegregationOfDutiesViolationException.class);
  }

  @Test
  void skipsWhenExceedsAuthority() {
    ClaimReserveEntity reserve = payableReserve("5000.01", "0.00");
    when(paymentAuthorityService.validatePayment(any(), any(), any(), any()))
        .thenThrow(new AuthorityLimitExceededException(new BigDecimal("5000.00"), new BigDecimal("5000.01")));

    assertThatThrownBy(() -> processor.process(reserve))
        .isInstanceOf(AuthorityLimitExceededException.class);
  }

  @Test
  void skipsWhenZeroOutstanding() {
    ClaimReserveEntity reserve = payableReserve("1000.00", "1000.00");
    when(paymentAuthorityService.validatePayment(any(), any(), any(), any()))
        .thenThrow(new InvalidPaymentAmountException());

    assertThatThrownBy(() -> processor.process(reserve))
        .isInstanceOf(InvalidPaymentAmountException.class);
  }

  private static ClaimReserveEntity payableReserve(String approved, String paid) {
    ClaimEntity claim = new ClaimEntity();
    claim.setClaimNbr("CLM000000001");
    ClaimReserveEntity reserve = new ClaimReserveEntity();
    reserve.setClaim(claim);
    reserve.setApprovedAmt(new BigDecimal(approved));
    reserve.setPaidToDate(new BigDecimal(paid));
    return reserve;
  }
}
