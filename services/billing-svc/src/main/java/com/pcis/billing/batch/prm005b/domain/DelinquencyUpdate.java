package com.pcis.billing.batch.prm005b.domain;

public record DelinquencyUpdate(DelinquencyCandidateRow candidate, StatusTransition transition) {}
