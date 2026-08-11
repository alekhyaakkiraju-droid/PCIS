package com.pcis.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pcis.policy.domain.entity.PolicyEntity;
import com.pcis.policy.domain.repository.BillingPlanRepository;
import com.pcis.policy.domain.repository.CoverageRepository;
import com.pcis.policy.domain.repository.DeductibleRepository;
import com.pcis.policy.domain.repository.EndorsementRepository;
import com.pcis.policy.domain.repository.PolicyHistoryRepository;
import com.pcis.policy.domain.repository.PolicyRepository;
import com.pcis.policy.dto.PolicyCancelRequest;
import com.pcis.policy.dto.PolicyCreateRequest;
import com.pcis.policy.exception.InvalidStateTransitionException;
import com.pcis.policy.exception.PolicyNotFoundException;
import com.pcis.policy.outbox.PolicyOutboxWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

  @Mock private PolicyRepository policyRepository;
  @Mock private CoverageRepository coverageRepository;
  @Mock private DeductibleRepository deductibleRepository;
  @Mock private PolicyHistoryRepository policyHistoryRepository;
  @Mock private EndorsementRepository endorsementRepository;
  @Mock private BillingPlanRepository billingPlanRepository;
  @Mock private PolicyAuthorizationService policyAuthorizationService;
  @Mock private PolicyOutboxWriter policyOutboxWriter;

  @InjectMocks private PolicyService policyService;

  @BeforeEach
  void setSecurityContext() {
    Jwt jwt =
        Jwt.withTokenValue("test")
            .header("alg", "none")
            .subject("underwriter-1")
            .build();
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(jwt, null, List.of()));
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createPolicyWithoutBillingPlanThrowsIllegalArgumentException() {
    PolicyCreateRequest request =
        new PolicyCreateRequest(
            1001,
            "AGT00001",
            "HO-1",
            new BigDecimal("2400.00"),
            LocalDate.of(2027, 1, 1),
            LocalDate.of(2028, 1, 1),
            List.of(
                new PolicyCreateRequest.CoverageRequest(
                    "HO-1",
                    new BigDecimal("500000.00"),
                    new BigDecimal("2400.00"),
                    List.of(
                        new PolicyCreateRequest.DeductibleRequest(
                            "STD", new BigDecimal("1000.00"))))),
            null);

    assertThatThrownBy(() -> policyService.createPolicy(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Billing plan");

    verify(policyAuthorizationService, never()).requireMutationPermitted(any(), any());
  }

  @Test
  void createPolicyPersistsEntitiesAndOutboxEvent() {
    PolicyCreateRequest request = validCreateRequest();
    when(policyRepository.nextPolicyNumberSequence()).thenReturn(10_000_003L);
    when(policyRepository.nextCoverageIdSequence()).thenReturn(1L);
    when(policyRepository.save(any(PolicyEntity.class)))
        .thenAnswer(
            invocation -> {
              PolicyEntity saved = invocation.getArgument(0);
              return saved;
            });
    when(policyRepository.findWithDetailsByPolNbr("POL10000003"))
        .thenAnswer(
            invocation -> {
              PolicyEntity policy = new PolicyEntity();
              policy.setPolNbr("POL10000003");
              policy.setPremAnnual(new BigDecimal("2400.00"));
              return Optional.of(policy);
            });

    PolicyEntity created = policyService.createPolicy(request);

    assertThat(created.getPolNbr()).isEqualTo("POL10000003");
    verify(policyAuthorizationService).requireMutationPermitted("underwriter-1", "new");
    verify(policyOutboxWriter)
        .writeDomainEvent(eq("POL10000003"), eq("PolicyCreated"), any(), any(UUID.class));
    ArgumentCaptor<PolicyEntity> policyCaptor = ArgumentCaptor.forClass(PolicyEntity.class);
    verify(policyRepository).save(policyCaptor.capture());
    assertThat(policyCaptor.getValue().getPremAnnual()).isEqualByComparingTo("2400.00");
  }

  @Test
  void endorseNonExistentPolicyThrowsPolicyNotFoundException() {
    when(policyRepository.findWithDetailsByPolNbr("POL99999999")).thenReturn(Optional.empty());

    PolicyCreateRequest.CoverageRequest coverage =
        new PolicyCreateRequest.CoverageRequest(
            "HO-1",
            new BigDecimal("100000.00"),
            new BigDecimal("100.00"),
            List.of());
    var request =
        new com.pcis.policy.dto.PolicyEndorseRequest(
            "COV_ADD", LocalDate.of(2027, 6, 1), List.of(coverage), "Added coverage");

    assertThatThrownBy(() -> policyService.endorsePolicy("POL99999999", request))
        .isInstanceOf(PolicyNotFoundException.class);
  }

  @Test
  void cancelAlreadyCancelledPolicyThrowsInvalidStateTransitionException() {
    PolicyEntity policy = new PolicyEntity();
    policy.setPolNbr("POL10000001");
    policy.setPolStatus("CANC");
    when(policyRepository.findWithDetailsByPolNbr("POL10000001")).thenReturn(Optional.of(policy));

    var request = new PolicyCancelRequest(LocalDate.of(2027, 3, 15), "NONPAY");

    assertThatThrownBy(() -> policyService.cancelPolicy("POL10000001", request))
        .isInstanceOf(InvalidStateTransitionException.class)
        .hasMessageContaining("already cancelled");
  }

  private static PolicyCreateRequest validCreateRequest() {
    return new PolicyCreateRequest(
        1001,
        "AGT00001",
        "HO-1",
        new BigDecimal("2400.00"),
        LocalDate.of(2027, 1, 1),
        LocalDate.of(2028, 1, 1),
        List.of(
            new PolicyCreateRequest.CoverageRequest(
                "HO-1",
                new BigDecimal("500000.00"),
                new BigDecimal("2400.00"),
                List.of(
                    new PolicyCreateRequest.DeductibleRequest("STD", new BigDecimal("1000.00"))))),
        new PolicyCreateRequest.BillingPlanRequest("M", 12));
  }
}
