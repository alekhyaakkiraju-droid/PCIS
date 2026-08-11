package com.pcis.gateway.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Maps Keycloak {@code realm_access.roles} to {@code ROLE_*} authorities and exposes the
 * {@code authority_limit} claim as a principal attribute.
 */
@Component
public class PcisJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  static final String REALM_ACCESS = "realm_access";
  static final String ROLES = "roles";
  static final String AUTHORITY_LIMIT = "authority_limit";
  static final String ROLE_PREFIX = "ROLE_";

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
    JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    Object authorityLimit = jwt.getClaim(AUTHORITY_LIMIT);
    if (authorityLimit != null) {
      token.setDetails(Map.of(AUTHORITY_LIMIT, authorityLimit));
    }
    return token;
  }

  Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
    List<String> roles = extractRoles(jwt);
    if (roles.isEmpty()) {
      return List.of();
    }
    List<GrantedAuthority> authorities = new ArrayList<>(roles.size());
    for (String role : roles) {
      if (role == null || role.isBlank()) {
        continue;
      }
      String normalized = role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role;
      authorities.add(new SimpleGrantedAuthority(normalized));
    }
    return authorities;
  }

  @SuppressWarnings("unchecked")
  List<String> extractRoles(Jwt jwt) {
    Object realmAccess = jwt.getClaim(REALM_ACCESS);
    if (!(realmAccess instanceof Map<?, ?> realmAccessMap)) {
      return Collections.emptyList();
    }
    Object roles = realmAccessMap.get(ROLES);
    if (!(roles instanceof Collection<?> roleCollection)) {
      return Collections.emptyList();
    }
    List<String> result = new ArrayList<>();
    for (Object role : roleCollection) {
      if (role instanceof String roleName) {
        result.add(roleName);
      }
    }
    return result;
  }
}
