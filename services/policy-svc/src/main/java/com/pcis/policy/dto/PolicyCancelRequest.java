package com.pcis.policy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PolicyCancelRequest(
    @NotNull LocalDate cancellationDate,
    @NotBlank @Size(max = 10) @Pattern(regexp = "[A-Z0-9]{2,10}") String reason) {}
