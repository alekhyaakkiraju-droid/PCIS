package com.pcis.customer.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DuplicateOverrideRequest(
    @NotBlank @Size(min = 10, max = 500) String overrideReason,
    @Valid CreateCustomerRequest customer) {}
