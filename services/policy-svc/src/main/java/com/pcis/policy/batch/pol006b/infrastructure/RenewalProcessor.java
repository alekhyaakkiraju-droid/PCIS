package com.pcis.policy.batch.pol006b.infrastructure;

import com.pcis.policy.batch.pol006b.client.PremiumServiceClient;
import com.pcis.policy.batch.pol006b.client.RatingResponse;
import com.pcis.policy.batch.pol006b.domain.RenewalResult;
import com.pcis.policy.batch.pol006b.domain.RenewalTermCalculator;
import com.pcis.policy.batch.pol006b.exception.RenewalDeclinedException;
import com.pcis.policy.batch.pol006b.exception.RenewalException;
import com.pcis.policy.domain.entity.BillingPlanEntity;
import com.pcis.policy.domain.entity.CoverageEntity;
import com.pcis.policy.domain.entity.DeductibleEntity;
import com.pcis.policy.domain.entity.PolicyEntity;
import com.pcis.policy.domain.entity.PolicyHistoryEntity;
import com.pcis.policy.domain.repository.PolicyRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class RenewalProcessor implements ItemProcessor<String, RenewalResult> {

  private static final String STATUS_RENEWAL = "RNWL";
  private static final String STATUS_REFERRAL = "REFR";

  private final PolicyRepository policyRepository;
  private final PremiumServiceClient premiumServiceClient;

  public RenewalProcessor(
      PolicyRepository policyRepository, PremiumServiceClient premiumServiceClient) {
    this.policyRepository = policyRepository;
    this.premiumServiceClient = premiumServiceClient;
  }

  @Override
  public RenewalResult process(String polNbr) {
    PolicyEntity source =
        policyRepository
            .findWithDetailsByPolNbr(polNbr)
            .orElseThrow(
                () ->
                    new RenewalException(
                        polNbr, "NOT_FOUND", "Source policy not found"));

    String stateCode = PremiumServiceClient.resolveStateCode(source);
    RatingResponse rating = premiumServiceClient.rate(source, stateCode);

    if (rating.isInvalidInput()) {
      throw new RenewalException(
          source.getPolNbr(), "INVALID_INPUT", "premium-svc rejected rating input");
    }
    if (rating.isDeclined()) {
      throw new RenewalDeclinedException(
          source.getPolNbr(), "Underwriting declined renewal for " + source.getPolNbr());
    }

    BigDecimal newPremium =
        rating.finalPremium() != null ? rating.finalPremium() : source.getPremAnnual();
    String renewalPolNbr = RenewalTermCalculator.renewalPolicyNumber(source.getPolNbr());
    LocalDate newEffDate = RenewalTermCalculator.newEffectiveDate(source.getExpDate());
    LocalDate newExpDate =
        RenewalTermCalculator.newExpirationDate(source.getEffDate(), source.getExpDate());

    PolicyEntity renewalPolicy = new PolicyEntity();
    renewalPolicy.setPolNbr(renewalPolNbr);
    renewalPolicy.setCustId(source.getCustId());
    renewalPolicy.setAgtId(source.getAgtId());
    renewalPolicy.setPolicyType(source.getPolicyType());
    renewalPolicy.setPolStatus(rating.isReferral() ? STATUS_REFERRAL : STATUS_RENEWAL);
    renewalPolicy.setEffDate(newEffDate);
    renewalPolicy.setExpDate(newExpDate);
    renewalPolicy.setPremAnnual(newPremium);
    renewalPolicy.setRenewalOfPol(source.getPolNbr());
    renewalPolicy.setBillFreq(source.getBillFreq());

    List<CoverageEntity> renewalCoverages = copyCoverages(source, renewalPolicy);

    BillingPlanEntity renewalBillingPlan = copyBillingPlan(source, renewalPolicy);

    PolicyHistoryEntity history = new PolicyHistoryEntity();
    history.setPolicy(renewalPolicy);
    history.setEventCode("RENEW     ");
    history.setEventDate(LocalDate.now());
    history.setEventDesc("Renewal of " + source.getPolNbr());

    return new RenewalResult(
        source,
        renewalPolicy,
        renewalCoverages,
        renewalBillingPlan,
        history,
        rating.isReferral(),
        UUID.randomUUID());
  }

  private List<CoverageEntity> copyCoverages(PolicyEntity source, PolicyEntity renewalPolicy) {
    List<CoverageEntity> copied = new ArrayList<>();
    for (CoverageEntity sourceCoverage : source.getCoverages()) {
      CoverageEntity coverage = new CoverageEntity();
      coverage.setCoverageId(
          RenewalTermCalculator.deriveCoverageId(
              renewalPolicy.getPolNbr(), sourceCoverage.getCoverageId()));
      coverage.setPolicy(renewalPolicy);
      coverage.setCovType(sourceCoverage.getCovType());
      coverage.setLimitAmt(sourceCoverage.getLimitAmt());
      coverage.setDedAmt(sourceCoverage.getDedAmt());
      coverage.setCovPremium(sourceCoverage.getCovPremium());

      for (DeductibleEntity sourceDeductible : sourceCoverage.getDeductibles()) {
        DeductibleEntity deductible = new DeductibleEntity();
        deductible.setCoverage(coverage);
        deductible.setDedAmt(sourceDeductible.getDedAmt());
        deductible.setDedType(sourceDeductible.getDedType());
        coverage.getDeductibles().add(deductible);
      }
      copied.add(coverage);
    }
    return copied;
  }

  private BillingPlanEntity copyBillingPlan(PolicyEntity source, PolicyEntity renewalPolicy) {
    BillingPlanEntity sourcePlan = source.getBillingPlan();
    BillingPlanEntity billingPlan = new BillingPlanEntity();
    billingPlan.setPolicy(renewalPolicy);
    if (sourcePlan != null) {
      billingPlan.setBillFreq(sourcePlan.getBillFreq());
      billingPlan.setNbrInstallments(sourcePlan.getNbrInstallments());
      billingPlan.setInstallmentFee(sourcePlan.getInstallmentFee());
      billingPlan.setActiveFlag(sourcePlan.getActiveFlag());
    } else {
      billingPlan.setBillFreq(source.getBillFreq());
      billingPlan.setNbrInstallments(defaultInstallments(source.getBillFreq()));
      billingPlan.setInstallmentFee(BigDecimal.ZERO);
      billingPlan.setActiveFlag("Y");
    }
    return billingPlan;
  }

  private static short defaultInstallments(String billFreq) {
    if (billFreq == null) {
      return 1;
    }
    return switch (billFreq.trim()) {
      case "M" -> 12;
      case "Q" -> 4;
      case "S" -> 2;
      default -> 1;
    };
  }
}
