package com.pcis.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pcis.error.ResourceNotFoundException;
import com.pcis.policy.domain.entity.CoverageTypeEntity;
import com.pcis.policy.domain.entity.PolicyEntity;
import com.pcis.policy.domain.repository.CoverageTypeRepository;
import com.pcis.policy.domain.repository.PolicyRepository;
import com.pcis.policy.support.PolicyTestSecurityConfig;
import com.pcis.policy.support.PostgresTestContainer;
import com.pcis.policy.support.TestEnvironment;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(PolicyTestSecurityConfig.class)
@EnabledIf("com.pcis.policy.support.TestEnvironment#isDockerAvailable")
class PolicyReadServiceIntegrationTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private PolicyReadService policyReadService;
  @Autowired private PolicyRepository policyRepository;
  @Autowired private CoverageTypeRepository coverageTypeRepository;

  @BeforeEach
  void seedReferenceData() {
    if (coverageTypeRepository.findById("DWEL").isEmpty()) {
      CoverageTypeEntity covType = new CoverageTypeEntity();
      covType.setCovType("DWEL");
      covType.setCovDesc("Dwelling");
      covType.setActiveFlag("Y");
      coverageTypeRepository.save(covType);
    }
    if (policyRepository.findById("POL000000001").isEmpty()) {
      PolicyEntity policy = new PolicyEntity();
      policy.setPolNbr("POL000000001");
      policy.setCustId(1001);
      policy.setAgtId("AGT00001");
      policy.setPolicyType("HOM ");
      policy.setPolStatus("ACTV");
      policy.setEffDate(LocalDate.of(2026, 1, 1));
      policy.setExpDate(LocalDate.of(2027, 1, 1));
      policy.setPremAnnual(new BigDecimal("1250.00"));
      policy.setBillFreq("M");
      policyRepository.save(policy);
    }
  }

  @Test
  @Transactional
  void findByPolNbrReturnsPolicy() {
    PolicyEntity policy = policyReadService.findByPolNbr("POL000000001");
    assertThat(policy.getPolNbr()).isEqualTo("POL000000001");
    assertThat(policy.getCustId()).isEqualTo(1001);
    assertThat(policy.getPremAnnual()).isEqualByComparingTo("1250.00");
  }

  @Test
  void findByPolNbrThrowsWhenMissing() {
    assertThatThrownBy(() -> policyReadService.findByPolNbr("POL999999999"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void findAllReturnsSeededPolicies() {
    assertThat(policyReadService.findAll()).isNotEmpty();
  }
}
