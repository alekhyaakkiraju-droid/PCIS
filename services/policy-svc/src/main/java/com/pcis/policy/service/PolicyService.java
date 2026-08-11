package com.pcis.policy.service;

import com.pcis.policy.domain.entity.BillingPlanEntity;
import com.pcis.policy.domain.entity.CoverageEntity;
import com.pcis.policy.domain.entity.DeductibleEntity;
import com.pcis.policy.domain.entity.EndorsementEntity;
import com.pcis.policy.domain.entity.PolicyEntity;
import com.pcis.policy.domain.entity.PolicyHistoryEntity;
import com.pcis.policy.domain.repository.BillingPlanRepository;
import com.pcis.policy.domain.repository.CoverageRepository;
import com.pcis.policy.domain.repository.DeductibleRepository;
import com.pcis.policy.domain.repository.EndorsementRepository;
import com.pcis.policy.domain.repository.PolicyHistoryRepository;
import com.pcis.policy.domain.repository.PolicyRepository;
import com.pcis.policy.dto.PolicyCancelRequest;
import com.pcis.policy.dto.PolicyCreateRequest;
import com.pcis.policy.dto.PolicyEndorseRequest;
import com.pcis.policy.dto.PolicyMapper;
import com.pcis.policy.exception.InvalidStateTransitionException;
import com.pcis.policy.exception.PolicyNotFoundException;
import com.pcis.policy.outbox.PolicyOutboxWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PolicyService {

  private static final String STATUS_NEW = "NEW ";
  private static final String STATUS_ACTIVE = "ACTV";
  private static final String STATUS_CANCELLED = "CANC";

  private final PolicyRepository policyRepository;
  private final CoverageRepository coverageRepository;
  private final DeductibleRepository deductibleRepository;
  private final PolicyHistoryRepository policyHistoryRepository;
  private final EndorsementRepository endorsementRepository;
  private final BillingPlanRepository billingPlanRepository;
  private final PolicyAuthorizationService policyAuthorizationService;
  private final PolicyOutboxWriter policyOutboxWriter;

  public PolicyService(
      PolicyRepository policyRepository,
      CoverageRepository coverageRepository,
      DeductibleRepository deductibleRepository,
      PolicyHistoryRepository policyHistoryRepository,
      EndorsementRepository endorsementRepository,
      BillingPlanRepository billingPlanRepository,
      PolicyAuthorizationService policyAuthorizationService,
      PolicyOutboxWriter policyOutboxWriter) {
    this.policyRepository = policyRepository;
    this.coverageRepository = coverageRepository;
    this.deductibleRepository = deductibleRepository;
    this.policyHistoryRepository = policyHistoryRepository;
    this.endorsementRepository = endorsementRepository;
    this.billingPlanRepository = billingPlanRepository;
    this.policyAuthorizationService = policyAuthorizationService;
    this.policyOutboxWriter = policyOutboxWriter;
  }

  @Transactional
  public PolicyEntity createPolicy(PolicyCreateRequest request) {
    if (request.billingPlan() == null) {
      throw new IllegalArgumentException("Billing plan is required");
    }
    String subject = currentSubject();
    policyAuthorizationService.requireMutationPermitted(subject, "new");

    String polNbr = formatPolicyNumber(policyRepository.nextPolicyNumberSequence());
    PolicyEntity policy = new PolicyEntity();
    policy.setPolNbr(polNbr);
    policy.setCustId(request.customerId());
    policy.setAgtId(request.agentId());
    policy.setPolicyType(PolicyMapper.padChar4(request.policyType()));
    policy.setPolStatus(STATUS_NEW);
    policy.setEffDate(request.effectiveDate());
    policy.setExpDate(request.expirationDate());
    policy.setPremAnnual(request.annualPremium());
    policy.setBillFreq(request.billingPlan().billingFrequency());
    policyRepository.save(policy);

    for (PolicyCreateRequest.CoverageRequest coverageRequest : request.coverages()) {
      persistCoverage(policy, coverageRequest);
    }

    BillingPlanEntity billingPlan = new BillingPlanEntity();
    billingPlan.setPolicy(policy);
    billingPlan.setBillFreq(request.billingPlan().billingFrequency());
    billingPlan.setNbrInstallments(request.billingPlan().installmentCount().shortValue());
    billingPlan.setInstallmentFee(BigDecimal.ZERO);
    billingPlan.setActiveFlag("Y");
    billingPlanRepository.save(billingPlan);
    policy.setBillingPlan(billingPlan);

    appendHistory(policy, "ISSUED   ", request.effectiveDate(), "Policy issued");

    UUID idempotencyKey = UUID.randomUUID();
    policyOutboxWriter.writeDomainEvent(
        polNbr,
        "PolicyCreated",
        Map.of(
            "policyNumber", polNbr,
            "customerId", request.customerId(),
            "status", "NEW"),
        idempotencyKey);

    return policyRepository.findWithDetailsByPolNbr(polNbr).orElseThrow();
  }

  @Transactional(readOnly = true)
  public PolicyEntity findByPolicyNumber(String policyNumber) {
    return policyRepository
        .findWithDetailsByPolNbr(policyNumber)
        .orElseThrow(() -> new PolicyNotFoundException(policyNumber));
  }

  @Transactional(readOnly = true)
  public Page<PolicyEntity> findPolicies(Integer customerId, String status, Pageable pageable) {
    String dbStatus = status != null ? PolicyMapper.toDbStatus(status) : null;
    return policyRepository.findByFilters(customerId, dbStatus, pageable);
  }

  @Transactional
  public PolicyEntity endorsePolicy(String policyNumber, PolicyEndorseRequest request) {
    String subject = currentSubject();
    policyAuthorizationService.requireMutationPermitted(subject, policyNumber);

    PolicyEntity policy =
        policyRepository
            .findWithDetailsByPolNbr(policyNumber)
            .orElseThrow(() -> new PolicyNotFoundException(policyNumber));

    if (STATUS_CANCELLED.equals(trimStatus(policy.getPolStatus()))) {
      throw new InvalidStateTransitionException("Cannot endorse a cancelled policy");
    }

    BigDecimal premiumDelta = BigDecimal.ZERO;
    if (request.coverageChanges() != null) {
      for (PolicyCreateRequest.CoverageRequest change : request.coverageChanges()) {
        persistCoverage(policy, change);
        premiumDelta = premiumDelta.add(change.premiumAmount());
      }
    }

    policy.setPremAnnual(policy.getPremAnnual().add(premiumDelta));
    if ("NEW".equals(trimStatus(policy.getPolStatus()))) {
      policy.setPolStatus(STATUS_ACTIVE);
    }

    EndorsementEntity endorsement = new EndorsementEntity();
    endorsement.setPolicy(policy);
    endorsement.setEndType(PolicyMapper.padChar4(request.endorsementType()));
    endorsement.setEffDate(request.effectiveDate());
    endorsement.setPremChg(premiumDelta);
    endorsementRepository.save(endorsement);

    appendHistory(
        policy,
        "ENDORSE  ",
        request.effectiveDate(),
        request.reason().length() > 100 ? request.reason().substring(0, 100) : request.reason());

    policyOutboxWriter.writeDomainEvent(
        policyNumber,
        "PolicyEndorsed",
        Map.of(
            "policyNumber", policyNumber,
            "endorsementType", request.endorsementType(),
            "premiumChange", premiumDelta.toPlainString()),
        UUID.randomUUID());

    try {
      return policyRepository.saveAndFlush(policy);
    } catch (OptimisticLockingFailureException ex) {
      throw new InvalidStateTransitionException(
          "Concurrent modification detected — retry the endorsement");
    }
  }

  @Transactional
  public PolicyEntity cancelPolicy(String policyNumber, PolicyCancelRequest request) {
    String subject = currentSubject();
    policyAuthorizationService.requireMutationPermitted(subject, policyNumber);

    PolicyEntity policy =
        policyRepository
            .findWithDetailsByPolNbr(policyNumber)
            .orElseThrow(() -> new PolicyNotFoundException(policyNumber));

    if (STATUS_CANCELLED.equals(trimStatus(policy.getPolStatus()))) {
      throw new InvalidStateTransitionException("Policy is already cancelled");
    }

    policy.setPolStatus(STATUS_CANCELLED);
    appendHistory(
        policy,
        "CANCEL   ",
        request.cancellationDate(),
        request.reason().length() > 100 ? request.reason().substring(0, 100) : request.reason());

    policyOutboxWriter.writeDomainEvent(
        policyNumber,
        "PolicyCancelled",
        Map.of(
            "policyNumber", policyNumber,
            "reason", request.reason(),
            "cancellationDate", request.cancellationDate().toString()),
        UUID.randomUUID());

    try {
      return policyRepository.saveAndFlush(policy);
    } catch (OptimisticLockingFailureException ex) {
      throw new InvalidStateTransitionException(
          "Concurrent modification detected — retry the cancellation");
    }
  }

  private void persistCoverage(PolicyEntity policy, PolicyCreateRequest.CoverageRequest request) {
    CoverageEntity coverage = new CoverageEntity();
    coverage.setCoverageId(formatCoverageId(policyRepository.nextCoverageIdSequence()));
    coverage.setPolicy(policy);
    coverage.setCovType(PolicyMapper.padChar4(request.coverageType()));
    coverage.setLimitAmt(request.coverageLimit());
    BigDecimal primaryDed =
        request.deductibles() == null || request.deductibles().isEmpty()
            ? BigDecimal.ZERO
            : request.deductibles().getFirst().deductibleAmount();
    coverage.setDedAmt(primaryDed);
    coverage.setCovPremium(request.premiumAmount());
    coverageRepository.save(coverage);
    policy.getCoverages().add(coverage);

    if (request.deductibles() != null) {
      for (PolicyCreateRequest.DeductibleRequest deductibleRequest : request.deductibles()) {
        DeductibleEntity deductible = new DeductibleEntity();
        deductible.setCoverage(coverage);
        deductible.setDedAmt(deductibleRequest.deductibleAmount());
        deductible.setDedType(PolicyMapper.padChar4(deductibleRequest.deductibleType()));
        deductibleRepository.save(deductible);
        coverage.getDeductibles().add(deductible);
      }
    }
  }

  private void appendHistory(
      PolicyEntity policy, String eventCode, LocalDate eventDate, String description) {
    PolicyHistoryEntity history = new PolicyHistoryEntity();
    history.setPolicy(policy);
    history.setEventCode(eventCode);
    history.setEventDate(eventDate);
    history.setEventDesc(description);
    policyHistoryRepository.save(history);
    policy.getHistory().add(history);
  }

  private static String formatPolicyNumber(long sequence) {
    return String.format("POL%08d", sequence);
  }

  private static String formatCoverageId(long sequence) {
    return String.format("COV%011d", sequence);
  }

  private static String trimStatus(String status) {
    return status == null ? "" : status.trim();
  }

  private static String currentSubject() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
      return jwt.getSubject();
    }
    return "system";
  }
}
