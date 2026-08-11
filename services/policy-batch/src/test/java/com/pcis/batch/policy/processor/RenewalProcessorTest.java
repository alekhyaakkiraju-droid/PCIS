package com.pcis.batch.policy.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.pcis.batch.policy.client.PremiumRatingClient;
import com.pcis.batch.policy.client.PremiumRatingResponse;
import com.pcis.batch.policy.domain.RatingDeclinedException;
import com.pcis.batch.policy.domain.RatingUnavailableException;
import com.pcis.batch.policy.domain.RenewalCandidateRow;
import com.pcis.batch.policy.domain.RenewalDecision;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RenewalProcessorTest {

  @Mock private PremiumRatingClient premiumRatingClient;
  @InjectMocks private RenewalProcessor renewalProcessor;

  @Test
  void process_createsRenewalDecisionOnApprove() {
    RenewalCandidateRow candidate = candidate();
    when(premiumRatingClient.rateRenewal(candidate))
        .thenReturn(
            new PremiumRatingResponse("c1", "00", "APPROVE", new BigDecimal("1323.00")));

    RenewalDecision decision = renewalProcessor.process(candidate);

    assertThat(decision.newPremium()).isEqualByComparingTo("1323.00");
    assertThat(decision.referralFlag()).isFalse();
    assertThat(decision.newPolNbr()).isEqualTo("POL00010001R");
  }

  @Test
  void process_setsReferralFlag() {
    RenewalCandidateRow candidate = candidate();
    when(premiumRatingClient.rateRenewal(candidate))
        .thenReturn(new PremiumRatingResponse("c2", "00", "REFER", new BigDecimal("1400.00")));

    RenewalDecision decision = renewalProcessor.process(candidate);

    assertThat(decision.referralFlag()).isTrue();
  }

  @Test
  void process_throwsOnDecline() {
    RenewalCandidateRow candidate = candidate();
    when(premiumRatingClient.rateRenewal(candidate))
        .thenReturn(new PremiumRatingResponse("c3", "02", "DECLINE", new BigDecimal("0.00")));

    assertThatThrownBy(() -> renewalProcessor.process(candidate))
        .isInstanceOf(RatingDeclinedException.class);
  }

  @Test
  void process_propagatesUnavailable() {
    RenewalCandidateRow candidate = candidate();
    when(premiumRatingClient.rateRenewal(candidate))
        .thenThrow(new RatingUnavailableException(candidate.polNbr(), new RuntimeException("down")));

    assertThatThrownBy(() -> renewalProcessor.process(candidate))
        .isInstanceOf(RatingUnavailableException.class);
  }

  private static RenewalCandidateRow candidate() {
    return new RenewalCandidateRow(
        "POL00010001",
        "CUS0001",
        "AGT001",
        "HOME",
        LocalDate.of(2025, 1, 1),
        LocalDate.of(2026, 1, 1),
        new BigDecimal("1260.00"),
        "M",
        "TX");
  }
}
