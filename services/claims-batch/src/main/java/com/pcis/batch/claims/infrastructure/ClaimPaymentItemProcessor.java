package com.pcis.batch.claims.infrastructure;

import com.pcis.batch.claims.domain.ClaimPaymentBatchItem;
import com.pcis.batch.claims.domain.ClaimPaymentCalculator;
import com.pcis.batch.claims.domain.SkipReasonCode;
import com.pcis.batch.claims.domain.SkipRecord;
import com.pcis.claims.application.PaymentAuthorityService;
import com.pcis.claims.domain.ClaimReserveEntity;
import com.pcis.claims.exception.ApprovalRequiredException;
import com.pcis.claims.exception.AuthorityLimitExceededException;
import com.pcis.claims.exception.InsufficientReserveException;
import com.pcis.claims.exception.InvalidPaymentAmountException;
import com.pcis.claims.exception.PaymentAuthorizationException;
import com.pcis.claims.exception.SegregationOfDutiesViolationException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.security.core.context.SecurityContextHolder;

public class ClaimPaymentItemProcessor
    implements ItemProcessor<ClaimReserveEntity, ClaimPaymentBatchItem> {

  private static final Logger log = LoggerFactory.getLogger(ClaimPaymentItemProcessor.class);
  public static final String SKIP_RECORDS_KEY = "skipRecords";

  private final PaymentAuthorityService paymentAuthorityService;
  private StepExecution stepExecution;

  public ClaimPaymentItemProcessor(PaymentAuthorityService paymentAuthorityService) {
    this.paymentAuthorityService = paymentAuthorityService;
  }

  @BeforeStep
  public void beforeStep(StepExecution stepExecution) {
    this.stepExecution = stepExecution;
  }

  @Override
  public ClaimPaymentBatchItem process(ClaimReserveEntity reserve) {
    String claimNbr = reserve.getClaim().getClaimNbr();
    String batchPrincipal = SecurityContextHolder.getContext().getAuthentication().getName();
    BigDecimal paymentAmount =
        ClaimPaymentCalculator.outstandingAmount(
            reserve.getApprovedAmt(), reserve.getPaidToDate());

    try {
      PaymentAuthorityService.PaymentAuthorizationResult auth =
          paymentAuthorityService.validatePayment(
              claimNbr, reserve.getReserveId(), paymentAmount, batchPrincipal);
      return new ClaimPaymentBatchItem(
          reserve, paymentAmount, auth.approval(), auth.adjuster());
    } catch (PaymentAuthorizationException ex) {
      SkipReasonCode reason = mapReason(ex);
      recordSkip(new SkipRecord(claimNbr, reserve.getReserveId(), reason, ex.getMessage()));
      throw ex;
    }
  }

  private static SkipReasonCode mapReason(PaymentAuthorizationException ex) {
    if (ex instanceof ApprovalRequiredException) {
      return SkipReasonCode.NO_APPROVAL;
    }
    if (ex instanceof SegregationOfDutiesViolationException) {
      return SkipReasonCode.SAME_PRINCIPAL;
    }
    if (ex instanceof AuthorityLimitExceededException) {
      return SkipReasonCode.EXCEEDS_AUTHORITY;
    }
    if (ex instanceof InvalidPaymentAmountException) {
      return SkipReasonCode.ZERO_OUTSTANDING;
    }
    if (ex instanceof InsufficientReserveException) {
      return SkipReasonCode.INSUFFICIENT_RESERVE;
    }
    return SkipReasonCode.NO_APPROVAL;
  }

  private void recordSkip(SkipRecord skipRecord) {
    log.warn(
        "Skipping claim payment claimNbr={} reserveId={} reason={} detail={}",
        skipRecord.claimNbr(),
        skipRecord.reserveId(),
        skipRecord.reasonCode(),
        skipRecord.detail());
    if (stepExecution == null) {
      return;
    }
    var context = stepExecution.getExecutionContext();
    @SuppressWarnings("unchecked")
    List<SkipRecord> skips =
        context.containsKey(SKIP_RECORDS_KEY)
            ? (List<SkipRecord>) context.get(SKIP_RECORDS_KEY)
            : new ArrayList<>();
    if (!context.containsKey(SKIP_RECORDS_KEY)) {
      context.put(SKIP_RECORDS_KEY, skips);
    }
    skips.add(skipRecord);
  }
}
