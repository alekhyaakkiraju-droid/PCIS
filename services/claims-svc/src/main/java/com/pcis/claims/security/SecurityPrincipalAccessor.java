package com.pcis.claims.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class SecurityPrincipalAccessor {

  public String currentSubject() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) {
      throw new IllegalStateException("No authenticated principal");
    }
    if (auth.getPrincipal() instanceof Jwt jwt) {
      return jwt.getSubject();
    }
    return auth.getName();
  }

  public boolean hasAuthority(String authority) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return false;
    }
    return auth.getAuthorities().stream()
        .anyMatch(granted -> granted.getAuthority().equals(authority));
  }
}
