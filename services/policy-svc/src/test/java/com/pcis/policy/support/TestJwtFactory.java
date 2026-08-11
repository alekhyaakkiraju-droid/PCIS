package com.pcis.policy.support;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Generates RSA-signed JWTs for use in policy-svc tests.
 * Downstream story tests should use this factory rather than constructing JWTs inline
 * to keep claim structure consistent across the policy test suite.
 *
 * <p>The {@link #jwksJson()} endpoint can be used to configure a mock JWKS server (e.g.
 * WireMock) that Spring Security's resource server uses for JWT verification.
 */
public final class TestJwtFactory {

  public static final String KEY_ID = "policy-test-key";
  public static final String TEST_ISSUER = "http://localhost/test-issuer";
  public static final String TEST_SUBJECT = "policy-test-user";

  private static final RSAKey RSA_KEY = generateKey();

  private TestJwtFactory() {}

  public static RSAKey rsaKey() {
    return RSA_KEY;
  }

  /** Returns the JWKS JSON for this factory's public key. Use to mock a JWKS endpoint. */
  public static String jwksJson() {
    return String.format("{\"keys\":[%s]}", RSA_KEY.toPublicJWK().toJSONString());
  }

  /** Generates a valid signed JWT with the given subject and Keycloak realm_access.roles. */
  public static String validToken(String subject, String... roles) {
    return signToken(
        new JWTClaimsSet.Builder()
            .issuer(TEST_ISSUER)
            .subject(subject)
            .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
            .claim("realm_access", Map.of("roles", List.of(roles)))
            .build());
  }

  /** Generates a valid token with default subject and POLICY_AGENT role. */
  public static String policyAgentToken() {
    return validToken(TEST_SUBJECT, "POLICY_AGENT");
  }

  /** Generates a valid token with underwriter role. */
  public static String underwriterToken() {
    return validToken(TEST_SUBJECT, "UNDERWRITER");
  }

  /** Generates a token that is already expired (for negative testing). */
  public static String expiredToken() {
    return signToken(
        new JWTClaimsSet.Builder()
            .issuer(TEST_ISSUER)
            .subject(TEST_SUBJECT)
            .expirationTime(Date.from(Instant.now().minusSeconds(60)))
            .build());
  }

  /** Generates a token signed with a different key (for issuer/signature mismatch testing). */
  public static String wrongIssuerToken() {
    return signToken(
        new JWTClaimsSet.Builder()
            .issuer("http://evil.example/realms/pcis")
            .subject(TEST_SUBJECT)
            .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
            .build());
  }

  private static String signToken(JWTClaimsSet claims) {
    try {
      SignedJWT signed =
          new SignedJWT(
              new JWSHeader.Builder(JWSAlgorithm.RS256)
                  .keyID(KEY_ID)
                  .type(JOSEObjectType.JWT)
                  .build(),
              claims);
      signed.sign(new RSASSASigner(RSA_KEY.toPrivateKey()));
      return signed.serialize();
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to sign test JWT", ex);
    }
  }

  private static RSAKey generateKey() {
    try {
      return new RSAKeyGenerator(2048).keyID(KEY_ID).generate();
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to generate RSA test key", ex);
    }
  }
}
