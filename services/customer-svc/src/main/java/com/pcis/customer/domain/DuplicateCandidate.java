package com.pcis.customer.domain;

/** Summary of an existing customer matched by tax ID during duplicate detection. */
public record DuplicateCandidate(Integer custId, String custName, String custStatus) {}
