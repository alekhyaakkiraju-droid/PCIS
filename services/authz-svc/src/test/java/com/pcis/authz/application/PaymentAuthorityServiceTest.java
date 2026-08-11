package com.pcis.authz.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.pcis.authz.domain.decision.AuthorizationDecision;
import com.pcis.authz.domain.decision.ReasonCode;
import com.pcis.authz.infrastructure.persistence.entity.ApprovalEntity;
import com.pcis.authz.infrastructure.persistence.projection.AdjusterAuthorityProjection;
import com.pcis.authz.infrastructure.persistence.projection.ReservePaidToDateProjection;
import com.pcis.authz.infrastructure.persistence.repository.ApprovalRepository;
import com.pcis.authz.infrastructure.persistence.repository.ClaimAdjusterRepository;
import com.pcis.authz.infrastructure.persistence.repository.ClaimReserveRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentAuthorityServiceTest {

  private static final String CLAIM_ID = "CLM0001001";
  private static final Long RESERVE_ID = 1001L;
  private static final String ADJUSTER_ID = "ADJ1000001";

  @Mock private ApprovalRepository approvalRepository;
  @Mock private ClaimAdjusterRepository claimAdjusterRepository;
  @Mock private ClaimReserveRepository claimReserveRepository;
  @Mock private PermissionResolver permissionResolver;

  private PaymentAuthorityService service;

  @BeforeEach
  void setUp() {
    service =
        new PaymentAuthorityService(
            approvalRepository,
            claimAdjusterRepository,
            claimReserveRepository,
            permissionResolver);
  }

  @Test
  void deniesWhenApprovalMissing() {
    when(approvalRepository.findFirstByClaimIdAndReserveHistIdAndApprovalStatus(
            CLAIM_ID, RESERVE_ID, ApprovalEntity.STATUS_APPROVED))
        .thenReturn(Optional.empty());

    var result =
        service.checkPaymentAuthority(
            CLAIM_ID, RESERVE_ID, new BigDecimal("1000.00"), ADJUSTER_ID);

    assertThat(result.decision()).isEqualTo(AuthorizationDecision.DENY);
    assertThat(result.reasonCode()).isEqualTo(ReasonCode.APPROVAL_MISSING);
  }

  @Test
  void deniesWhenReserveMissing() {
    when(approvalRepository.findFirstByClaimIdAndReserveHistIdAndApprovalStatus(
            CLAIM_ID, RESERVE_ID, ApprovalEntity.STATUS_APPROVED))
        .thenReturn(Optional.of(approval(2001L, "ADJ1000002")));
    when(claimReserveRepository.findByReserveHistIdAndClaimId(RESERVE_ID, CLAIM_ID))
        .thenReturn(Optional.empty());

    var result =
        service.checkPaymentAuthority(
            CLAIM_ID, RESERVE_ID, new BigDecimal("1000.00"), ADJUSTER_ID);

    assertThat(result.reasonCode()).isEqualTo(ReasonCode.APPROVAL_MISSING);
  }

  @Test
  void pb01DeniesWhenCumulativePayoutExceedsLimit() {
    stubApprovedScenario(new BigDecimal("20000.00"), new BigDecimal("25000.00"));

    var result =
        service.checkPaymentAuthority(
            CLAIM_ID, RESERVE_ID, new BigDecimal("10000.00"), ADJUSTER_ID);

    assertThat(result.decision()).isEqualTo(AuthorizationDecision.DENY);
    assertThat(result.reasonCode()).isEqualTo(ReasonCode.AUTHORITY_LIMIT_EXCEEDED);
  }

  @Test
  void pb01PermitsWhenCumulativeWithinLimit() {
    stubApprovedScenario(new BigDecimal("15000.00"), new BigDecimal("25000.00"));

    var result =
        service.checkPaymentAuthority(
            CLAIM_ID, RESERVE_ID, new BigDecimal("10000.00"), ADJUSTER_ID);

    assertThat(result.decision()).isEqualTo(AuthorizationDecision.PERMIT);
    assertThat(result.reasonCode()).isEqualTo(ReasonCode.PAYMENT_AUTHORITY_GRANTED);
    assertThat(result.cumulativePaidToDate()).isEqualByComparingTo("25000.00");
  }

  @Test
  void pb01PermitsAtExactAuthorityLimit() {
    stubApprovedScenario(new BigDecimal("20000.00"), new BigDecimal("25000.00"));

    var result =
        service.checkPaymentAuthority(
            CLAIM_ID, RESERVE_ID, new BigDecimal("5000.00"), ADJUSTER_ID);

    assertThat(result.decision()).isEqualTo(AuthorizationDecision.PERMIT);
    assertThat(result.cumulativePaidToDate()).isEqualByComparingTo("25000.00");
  }

  @Test
  void pb01DeniesOneCentOverAuthorityLimit() {
    stubApprovedScenario(new BigDecimal("20000.00"), new BigDecimal("25000.00"));

    var result =
        service.checkPaymentAuthority(
            CLAIM_ID, RESERVE_ID, new BigDecimal("5000.01"), ADJUSTER_ID);

    assertThat(result.decision()).isEqualTo(AuthorizationDecision.DENY);
    assertThat(result.reasonCode()).isEqualTo(ReasonCode.AUTHORITY_LIMIT_EXCEEDED);
  }

  @Test
  void pb01PermitsOneCentUnderAuthorityLimit() {
    stubApprovedScenario(new BigDecimal("20000.00"), new BigDecimal("25000.00"));

    var result =
        service.checkPaymentAuthority(
            CLAIM_ID, RESERVE_ID, new BigDecimal("4999.99"), ADJUSTER_ID);

    assertThat(result.decision()).isEqualTo(AuthorizationDecision.PERMIT);
    assertThat(result.cumulativePaidToDate()).isEqualByComparingTo("24999.99");
  }

  @Test
  void permitIncludesApprovalMetadata() {
    stubApprovedScenario(new BigDecimal("0.00"), new BigDecimal("25000.00"));

    var result =
        service.checkPaymentAuthority(
            CLAIM_ID, RESERVE_ID, new BigDecimal("1000.00"), ADJUSTER_ID);

    assertThat(result.approvalId()).isEqualTo(2001L);
    assertThat(result.approverPrincipal()).isEqualTo("ADJ1000002");
    assertThat(result.authorityLimitApplied()).isEqualByComparingTo("25000.00");
  }

  @Test
  void deniesSelfApprovalWhenApproverEqualsDisburser() {
    stubApprovedScenario(new BigDecimal("0.00"), new BigDecimal("25000.00"));
    when(claimAdjusterRepository.findAuthorityByAdjusterId("ADJ1000002"))
        .thenReturn(Optional.of(adjuster("ADJ1000002", new BigDecimal("25000.00"))));

    var result =
        service.checkPaymentAuthority(
            CLAIM_ID, RESERVE_ID, new BigDecimal("1000.00"), "ADJ1000002");

    assertThat(result.decision()).isEqualTo(AuthorizationDecision.DENY);
    assertThat(result.reasonCode()).isEqualTo(ReasonCode.SELF_APPROVAL_FORBIDDEN);
    assertThat(result.maskedApproverPrincipal()).isEqualTo("***0002");
    assertThat(result.maskedDisburserPrincipal()).isEqualTo("***0002");
  }

  @Test
  void deniesSelfApprovalCaseInsensitively() {
    stubApprovedScenario(new BigDecimal("0.00"), new BigDecimal("25000.00"));
    when(claimAdjusterRepository.findAuthorityByAdjusterId("adj1000002"))
        .thenReturn(Optional.of(adjuster("adj1000002", new BigDecimal("25000.00"))));

    var result =
        service.checkPaymentAuthority(
            CLAIM_ID, RESERVE_ID, new BigDecimal("1000.00"), "adj1000002");

    assertThat(result.reasonCode()).isEqualTo(ReasonCode.SELF_APPROVAL_FORBIDDEN);
  }

  @Test
  void permitsDifferentApproverAndDisburser() {
    stubApprovedScenario(new BigDecimal("0.00"), new BigDecimal("25000.00"));

    var result =
        service.checkPaymentAuthority(
            CLAIM_ID, RESERVE_ID, new BigDecimal("1000.00"), ADJUSTER_ID);

    assertThat(result.decision()).isEqualTo(AuthorizationDecision.PERMIT);
    assertThat(result.reasonCode()).isEqualTo(ReasonCode.PAYMENT_AUTHORITY_GRANTED);
  }

  @Test
  void deniesBatchPrincipalAttemptingApprovePayment() {
    when(permissionResolver.resolveRoleCodes("svc-claim-payment-job"))
        .thenReturn(java.util.List.of("BATCH_SVC"));

    var response =
        service.evaluate(
            "svc-claim-payment-job",
            new com.pcis.authz.contract.AuthorizationRequest(
                "claim",
                "APPROVE_PAYMENT",
                java.util.Map.of(
                    "claimId", CLAIM_ID,
                    "reserveId", RESERVE_ID,
                    "originalRequesterId", "ADJ1000001")),
            "corr-1");

    assertThat(response.decision()).isEqualTo(AuthorizationDecision.DENY);
    assertThat(response.reasonCode()).isEqualTo(ReasonCode.BATCH_CANNOT_APPROVE);
  }

  @Test
  void deniesApprovePaymentWhenApproverEqualsOriginalRequester() {
    when(permissionResolver.resolveRoleCodes("ADJ1000001"))
        .thenReturn(java.util.List.of("SUPERVISOR"));

    var response =
        service.evaluate(
            "ADJ1000001",
            new com.pcis.authz.contract.AuthorizationRequest(
                "claim",
                "APPROVE_PAYMENT",
                java.util.Map.of(
                    "claimId", CLAIM_ID,
                    "reserveId", RESERVE_ID,
                    "originalRequesterId", "ADJ1000001")),
            "corr-2");

    assertThat(response.reasonCode()).isEqualTo(ReasonCode.SELF_APPROVAL_FORBIDDEN);
  }

  @Test
  void permitsBatchPrincipalDisbursingAgainstDifferentUserApproval() {
    stubApprovedScenario(new BigDecimal("0.00"), new BigDecimal("25000.00"));
    when(claimAdjusterRepository.findAuthorityByAdjusterId("svc-claim-payment-job"))
        .thenReturn(Optional.of(adjuster("svc-claim-payment-job", new BigDecimal("25000.00"))));

    var result =
        service.checkPaymentAuthority(
            CLAIM_ID, RESERVE_ID, new BigDecimal("1000.00"), "svc-claim-payment-job");

    assertThat(result.decision()).isEqualTo(AuthorizationDecision.PERMIT);
  }

  private void stubApprovedScenario(BigDecimal paidToDate, BigDecimal authorityLimit) {
    when(approvalRepository.findFirstByClaimIdAndReserveHistIdAndApprovalStatus(
            CLAIM_ID, RESERVE_ID, ApprovalEntity.STATUS_APPROVED))
        .thenReturn(Optional.of(approval(2001L, "ADJ1000002")));
    when(claimReserveRepository.findByReserveHistIdAndClaimId(RESERVE_ID, CLAIM_ID))
        .thenReturn(Optional.of(reserve(RESERVE_ID, CLAIM_ID, paidToDate)));
    when(claimAdjusterRepository.findAuthorityByAdjusterId(ADJUSTER_ID))
        .thenReturn(Optional.of(adjuster(ADJUSTER_ID, authorityLimit)));
  }

  private static ApprovalEntity approval(Long approvalId, String approverId) {
    return new ApprovalEntity() {
      @Override
      public Long getApprovalId() {
        return approvalId;
      }

      @Override
      public String getApproverId() {
        return approverId;
      }
    };
  }

  private static ReservePaidToDateProjection reserve(
      Long reserveHistId, String claimId, BigDecimal paidToDate) {
    return new ReservePaidToDateProjection() {
      @Override
      public Long getReserveHistId() {
        return reserveHistId;
      }

      @Override
      public String getClaimId() {
        return claimId;
      }

      @Override
      public BigDecimal getPaidToDate() {
        return paidToDate;
      }
    };
  }

  private static AdjusterAuthorityProjection adjuster(
      String adjusterId, BigDecimal authorityLimit) {
    return new AdjusterAuthorityProjection() {
      @Override
      public String getAdjusterId() {
        return adjusterId;
      }

      @Override
      public BigDecimal getAuthorityLimit() {
        return authorityLimit;
      }
    };
  }
}
