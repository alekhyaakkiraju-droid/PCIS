package com.pcis.claims.reconciliation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.pcis.claims.domain.ClaimEntity;
import com.pcis.claims.domain.ClaimPaymentEntity;
import com.pcis.claims.domain.repository.ClaimPaymentRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClaimsReconciliationServiceTest {

  @Mock private ClaimPaymentRepository claimPaymentRepository;

  private ClaimsReconciliationService service;

  @BeforeEach
  void setUp() {
    service = new ClaimsReconciliationService(claimPaymentRepository);
  }

  @Test
  void passesWhenLegacyAndTargetMatch() {
    when(claimPaymentRepository.findAll()).thenReturn(List.of(payment("CLM000000001", "1500.00")));

    ReconciliationReport report =
        service.reconcile(Map.of("CLM000000001", new BigDecimal("1500.00")));

    assertThat(report.overallStatus()).isEqualTo(ReconciliationReport.OverallStatus.PASS);
    assertThat(report.breaks()).isEmpty();
  }

  @Test
  void detectsMissingInTarget() {
    when(claimPaymentRepository.findAll()).thenReturn(List.of());

    ReconciliationReport report =
        service.reconcile(Map.of("CLM000000002", new BigDecimal("500.00")));

    assertThat(report.overallStatus()).isEqualTo(ReconciliationReport.OverallStatus.FAIL);
    assertThat(report.breaks()).hasSize(1);
    assertThat(report.breaks().getFirst().breakClass()).isEqualTo(BreakClass.MISSING_IN_TARGET);
  }

  @Test
  void detectsValueMismatch() {
    when(claimPaymentRepository.findAll()).thenReturn(List.of(payment("CLM000000003", "100.00")));

    ReconciliationReport report =
        service.reconcile(Map.of("CLM000000003", new BigDecimal("100.01")));

    assertThat(report.breaks().getFirst().breakClass()).isEqualTo(BreakClass.VALUE_MISMATCH);
  }

  @Test
  void emptyLegacyExtractFlagsMissingInLegacy() throws Exception {
    when(claimPaymentRepository.findAll()).thenReturn(List.of(payment("CLM000000004", "250.00")));

    ReconciliationReport report =
        service.reconcile(
            new java.io.ByteArrayInputStream(
                "claim_nbr,payment_amt\n".getBytes(StandardCharsets.UTF_8)));

    assertThat(report.breaks()).anyMatch(b -> b.breakClass() == BreakClass.MISSING_IN_LEGACY);
  }

  private static ClaimPaymentEntity payment(String claimNbr, String amount) {
    ClaimEntity claim = new ClaimEntity();
    claim.setClaimNbr(claimNbr);
    ClaimPaymentEntity payment = new ClaimPaymentEntity();
    payment.setClaim(claim);
    payment.setPaymentAmt(new BigDecimal(amount));
    return payment;
  }
}
