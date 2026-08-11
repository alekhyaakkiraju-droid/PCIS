package com.pcis.batch.auth;

import java.io.IOException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Adds a Bearer access token to outbound {@link org.springframework.web.client.RestTemplate}
 * requests.
 */
public class BatchAuthRestTemplateInterceptor implements ClientHttpRequestInterceptor {

  private final BatchAuthenticationService authenticationService;

  public BatchAuthRestTemplateInterceptor(BatchAuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
      throws IOException {
    String token = authenticationService.getAccessToken();
    request.getHeaders().setBearerAuth(token);
    return execution.execute(request, body);
  }
}
