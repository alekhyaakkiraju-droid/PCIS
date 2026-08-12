package com.pcis.configsvc.config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Profile("!local")
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .requestMatchers("/actuator/readiness", "/actuator/readiness/**")
                    .permitAll()
                    .requestMatchers("/actuator/prometheus")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
        .build();
  }

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(new ScopeAndRoleConverter());
    return converter;
  }

  static final class ScopeAndRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final java.util.Set<String> CONFIG_ADMIN_ROLES =
        java.util.Set.of("ADMIN", "COMPLIANCE", "BATCH_SVC", "CLAIMS_SUPERVISOR");

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
      Object scopeClaim = jwt.getClaim("scope");
      Stream<String> scopes;
      if (scopeClaim instanceof String scopeString) {
        scopes = Stream.of(scopeString.split("\\s+"));
      } else if (scopeClaim instanceof Collection<?> scopeCollection) {
        scopes = scopeCollection.stream().map(Object::toString);
      } else {
        scopes = Stream.empty();
      }

      Object rolesClaim = jwt.getClaim("roles");
      Stream<String> roles;
      if (rolesClaim instanceof Collection<?> roleCollection) {
        roles = roleCollection.stream().map(Object::toString);
      } else {
        roles = Stream.empty();
      }

      Object realmAccess = jwt.getClaim("realm_access");
      Stream<String> realmRoles = Stream.empty();
      if (realmAccess instanceof java.util.Map<?, ?> realmAccessMap) {
        Object nestedRoles = realmAccessMap.get("roles");
        if (nestedRoles instanceof Collection<?> realmRoleCollection) {
          realmRoles = realmRoleCollection.stream().map(Object::toString);
        }
      }

      List<GrantedAuthority> authorities =
          Stream.concat(Stream.concat(scopes, roles), realmRoles)
              .filter(s -> s != null && !s.isBlank())
              .map(SimpleGrantedAuthority::new)
              .collect(Collectors.toCollection(java.util.ArrayList::new));

      boolean configAdmin =
          Stream.concat(roles, realmRoles).anyMatch(CONFIG_ADMIN_ROLES::contains);
      if (configAdmin) {
        authorities.add(new SimpleGrantedAuthority("configuration-admin"));
      }

      return authorities.isEmpty() ? Collections.emptyList() : authorities;
    }
  }
}
