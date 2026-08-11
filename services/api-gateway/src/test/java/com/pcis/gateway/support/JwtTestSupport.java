package com.pcis.gateway.support;

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

/** Generates RSA keys and signed JWT fixtures for gateway tests. */
public final class JwtTestSupport {

  public static final String KEY_ID = "test-key-id";
  public static final String ISSUER = "http://localhost:8080/realms/pcis";
  public static final String SUBJECT = "test-user-001";

  private static final RSAKey RSA_KEY = generateKey();

  private JwtTestSupport() {}

  public static RSAKey rsaKey() {
    return RSA_KEY;
  }

  public static String jwksJson() {
    return String.format(
        "{\"keys\":[%s]}",
        RSA_KEY.toPublicJWK().toJSONString());
  }

  public static String validToken() {
    return signToken(
        new JWTClaimsSet.Builder()
            .issuer(ISSUER)
            .subject(SUBJECT)
            .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
            .claim("realm_access", Map.of("roles", List.of("claims-adjuster", "ROLE_SUPERVISOR")))
            .claim("authority_limit", 50000)
            .build());
  }

  public static String expiredToken() {
    return signToken(
        new JWTClaimsSet.Builder()
            .issuer(ISSUER)
            .subject(SUBJECT)
            .expirationTime(Date.from(Instant.now().minusSeconds(60)))
            .build());
  }

  public static String wrongIssuerToken() {
    return signToken(
        new JWTClaimsSet.Builder()
            .issuer("http://evil.example/realms/pcis")
            .subject(SUBJECT)
            .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
            .build());
  }

  private static String signToken(JWTClaimsSet claims) {
    try {
      SignedJWT signedJwt =
          new SignedJWT(
              new JWSHeader.Builder(JWSAlgorithm.RS256)
                  .keyID(KEY_ID)
                  .type(JOSEObjectType.JWT)
                  .build(),
              claims);
      signedJwt.sign(new RSASSASigner(RSA_KEY.toPrivateKey()));
      return signedJwt.serialize();
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
