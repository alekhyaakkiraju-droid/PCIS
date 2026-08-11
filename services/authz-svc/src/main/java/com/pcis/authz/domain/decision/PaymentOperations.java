package com.pcis.authz.domain.decision;

/** Payment operations delegated to {@code PaymentAuthorityService}. */
public final class PaymentOperations {

  public static final String INITIATE_PAYMENT = "INITIATE_PAYMENT";
  public static final String APPROVE_PAYMENT = "APPROVE_PAYMENT";

  private PaymentOperations() {}

  public static boolean isPaymentOperation(String operation) {
    if (operation == null || operation.isBlank()) {
      return false;
    }
    return INITIATE_PAYMENT.equalsIgnoreCase(operation)
        || APPROVE_PAYMENT.equalsIgnoreCase(operation);
  }
}
