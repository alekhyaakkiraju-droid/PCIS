package com.pcis.claims.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNoteRequest(@NotBlank @Size(max = 4000) String noteText) {}
