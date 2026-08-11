package com.pcis.customer.domain.model;

public record UpdateCustomerCommand(Integer custId, String taxId, String custName, String custType, String custStatus) {}
