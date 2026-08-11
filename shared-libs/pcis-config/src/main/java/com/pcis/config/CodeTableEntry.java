package com.pcis.config;

public record CodeTableEntry(
    String domainCode, String codeValue, String description, boolean active) {}
