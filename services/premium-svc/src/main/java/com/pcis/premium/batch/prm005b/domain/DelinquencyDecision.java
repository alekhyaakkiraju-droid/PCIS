package com.pcis.premium.batch.prm005b.domain;

public record DelinquencyDecision(DelinquencyCandidateRow candidate, String newStatus) {}
