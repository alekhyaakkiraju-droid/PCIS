package com.pcis.authz.util;

/** Masks principal identifiers for audit responses without exposing full identity. */
public final class PrincipalMaskingUtil {

  private PrincipalMaskingUtil() {}

  /**
   * Returns {@code ***} + last four characters for strings longer than four characters, the full
   * string when length is four or fewer, and {@code ***} for null or blank inputs.
   */
  public static String maskPrincipal(String principal) {
    if (principal == null || principal.isBlank()) {
      return "***";
    }
    if (principal.length() <= 4) {
      return principal;
    }
    return "***" + principal.substring(principal.length() - 4);
  }
}
