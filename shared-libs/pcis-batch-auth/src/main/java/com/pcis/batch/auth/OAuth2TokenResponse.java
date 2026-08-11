package com.pcis.batch.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

record OAuth2TokenResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("expires_in") long expiresIn,
    @JsonProperty("token_type") String tokenType) {}
