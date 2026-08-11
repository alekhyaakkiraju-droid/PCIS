package com.pcis.policy.batch.pol006b.domain;

import com.pcis.policy.domain.entity.BillingPlanEntity;
import com.pcis.policy.domain.entity.CoverageEntity;
import com.pcis.policy.domain.entity.PolicyEntity;
import com.pcis.policy.domain.entity.PolicyHistoryEntity;
import java.util.List;
import java.util.UUID;

public record RenewalResult(
    PolicyEntity sourcePolicy,
    PolicyEntity renewalPolicy,
    List<CoverageEntity> renewalCoverages,
    BillingPlanEntity renewalBillingPlan,
    PolicyHistoryEntity renewalHistory,
    boolean referralFlag,
    UUID idempotencyKey) {}
