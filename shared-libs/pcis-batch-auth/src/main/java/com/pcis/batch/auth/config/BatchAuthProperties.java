package com.pcis.batch.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OAuth2 client-credentials settings for batch workloads.
 */
@ConfigurationProperties(prefix = "pcis.batch.oauth2")
public class BatchAuthProperties {

  private String tokenUri;
  private String clientId;
  private String clientSecretRef;
  private String scope;
  private int expirationBufferSeconds = 30;

  public String getTokenUri() {
    return tokenUri;
  }

  public void setTokenUri(String tokenUri) {
    this.tokenUri = tokenUri;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getClientSecretRef() {
    return clientSecretRef;
  }

  public void setClientSecretRef(String clientSecretRef) {
    this.clientSecretRef = clientSecretRef;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public int getExpirationBufferSeconds() {
    return expirationBufferSeconds;
  }

  public void setExpirationBufferSeconds(int expirationBufferSeconds) {
    if (expirationBufferSeconds < 0) {
      throw new IllegalArgumentException(
          "pcis.batch.oauth2.expiration-buffer-seconds must be >= 0");
    }
    this.expirationBufferSeconds = expirationBufferSeconds;
  }
}
