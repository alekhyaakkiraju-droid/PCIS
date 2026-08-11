package com.pcis.batch.auth;

/**
 * Resolves OAuth2 client secrets from managed secret references (never plaintext in config).
 */
@FunctionalInterface
public interface ClientSecretProvider {

  String resolve(String secretRef);
}
