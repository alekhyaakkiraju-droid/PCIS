package com.pcis.error;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemErrorEntry(String code, String detail, String field) {}
