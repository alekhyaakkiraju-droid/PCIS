package com.pcis.policy.batch.pol006b;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.pcis.policy.batch.pol006b.client.PremiumServiceClient;
import com.pcis.policy.batch.pol006b.client.RatingResponse;
import com.pcis.policy.batch.pol006b.domain.RenewalResult;
import com.pcis.policy.batch.pol006b.exception.RenewalDeclinedException;
import com.pcis.policy.batch.pol006b.exception.RenewalException;
import com.pcis.policy.batch.pol006b.infrastructure.RenewalProcessor;
import com.pcis.policy.domain.entity.BillingPlanEntity;
import com.pcis.policy.domain.entity.CoverageEntity;
import com.pcis.policy.domain.entity.DeductibleEntity;
import com.pcis.policy.domain.entity.PolicyEntity;
import com.pcis.policy.domain.entity.PolicyPropertyEntity;
import com.pcis.policy.domain.repository.PolicyRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RenewalProcessorTest {

  @Mock private PolicyRepository policyRepository;
  @Mock private PremiumServiceClient premiumServiceClient;
  @InjectMocks private RenewalProcessor renewalProcessor;

  private PolicyEntity sourcePolicy;

  @BeforeEach
  void setUp() {
    sourcePolicy = buildPolicyWithCoveragesAndDeductibles();
    when(policyRepository.findWithDetailsByPolNbr("POL00010001"))
        .thenReturn(Optional.of(sourcePolicy));
  }

  @Test
  void carriesForwardThreeCoveragesAndFiveDeductibles() {
    when(premiumServiceClient.rate(eq(sourcePolicy), eq("TX")))
        .thenReturn(
            new RatingResponse("c1", "00", "APPROVE", new BigDecimal("1323.00")));

    RenewalResult result = renewalProcessor.process("POL00010001");

    assertThat(result.renewalCoverages()).hasSize(3);
    long deductibleCount =
        result.renewalCoverages().stream()
            .mapToLong(coverage -> coverage.getDeductibles().size())
            .sum();
    assertThat(deductibleCount).isEqualTo(5);
    assertThat(result.renewalPolicy().getPremAnnual()).isEqualByComparingTo("1323.00");
    result
        .renewalCoverages()
        .forEach(
            coverage -> {
              assertThat(coverage.getPolicy()).isEqualTo(result.renewalPolicy());
              coverage
                  .getDeductibles()
                  .forEach(
                      deductible ->
                          assertThat(deductible.getCoverage().getPolicy())
                              .isEqualTo(result.renewalPolicy()));
            });
  }

  @Test
  void declineSkipsPolicy() {
    when(premiumServiceClient.rate(any(), any()))
        .thenReturn(new RatingResponse("c2", "02", "DECLINE", BigDecimal.ZERO));

    assertThatThrownBy(() -> renewalProcessor.process("POL00010001"))
        .isInstanceOf(RenewalDeclinedException.class);
  }

  @Test
  void referSetsReferralFlag() {
    when(premiumServiceClient.rate(any(), any()))
        .thenReturn(new RatingResponse("c3", "01", "REFER", new BigDecimal("1452.00")));

    RenewalResult result = renewalProcessor.process("POL00010001");

    assertThat(result.referralFlag()).isTrue();
    assertThat(result.renewalPolicy().getPolStatus().trim()).isEqualTo("REFR");
  }

  @Test
  void invalidInputThrowsRenewalException() {
    when(premiumServiceClient.rate(any(), any()))
        .thenReturn(new RatingResponse("c4", "99", "ERROR", BigDecimal.ZERO));

    assertThatThrownBy(() -> renewalProcessor.process("POL00010001"))
        .isInstanceOf(RenewalException.class);
  }

  @Test
  void createsAuditPayloadInputs() {
    when(premiumServiceClient.rate(any(), any()))
        .thenReturn(new RatingResponse("c5", "00", "APPROVE", new BigDecimal("1400.00")));

    RenewalResult result = renewalProcessor.process("POL00010001");

    assertThat(result.idempotencyKey()).isNotNull();
    assertThat(result.renewalHistory().getEventCode().trim()).isEqualTo("RENEW");
  }

  private static PolicyEntity buildPolicyWithCoveragesAndDeductibles() {
    PolicyEntity policy = new PolicyEntity();
    policy.setPolNbr("POL00010001");
    policy.setCustId(1001);
    policy.setAgtId("AGT00001");
    policy.setPolicyType("HO-1");
    policy.setPolStatus("ACTV");
    policy.setEffDate(LocalDate.of(2025, 1, 1));
    policy.setExpDate(LocalDate.of(2026, 1, 1));
    policy.setPremAnnual(new BigDecimal("1260.00"));
    policy.setBillFreq("M");

    PolicyPropertyEntity property = new PolicyPropertyEntity();
    property.setPolicy(policy);
    property.setStateCode("TX");
    policy.getProperties().add(property);

    BillingPlanEntity billingPlan = new BillingPlanEntity();
    billingPlan.setPolicy(policy);
    billingPlan.setBillFreq("M");
    billingPlan.setNbrInstallments((short) 12);
    policy.setBillingPlan(billingPlan);

    policy.getCoverages().add(coverage(policy, "COV001", 2));
    policy.getCoverages().add(coverage(policy, "COV002", 2));
    policy.getCoverages().add(coverage(policy, "COV003", 1));
    return policy;
  }

  private static CoverageEntity coverage(PolicyEntity policy, String id, int deductibles) {
    CoverageEntity coverage = new CoverageEntity();
    coverage.setCoverageId(id);
    coverage.setPolicy(policy);
    coverage.setCovType("HO-1");
    coverage.setLimitAmt(new BigDecimal("250000.00"));
    coverage.setDedAmt(new BigDecimal("1000.00"));
    coverage.setCovPremium(new BigDecimal("400.00"));
    Set<DeductibleEntity> deductibleEntities = new LinkedHashSet<>();
    for (int i = 0; i < deductibles; i++) {
      DeductibleEntity deductible = new DeductibleEntity();
      deductible.setCoverage(coverage);
      deductible.setDedAmt(new BigDecimal("500.00").add(BigDecimal.valueOf(i * 100L)));
      deductible.setDedType("FLAT");
      deductibleEntities.add(deductible);
    }
    coverage.getDeductibles().addAll(deductibleEntities);
    return coverage;
  }
}
