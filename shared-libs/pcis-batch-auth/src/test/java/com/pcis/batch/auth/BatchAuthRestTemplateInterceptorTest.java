package com.pcis.batch.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

class BatchAuthRestTemplateInterceptorTest {

  @Test
  void addsBearerAuthorizationHeader() throws IOException {
    BatchAuthenticationService authService = mock(BatchAuthenticationService.class);
    when(authService.getAccessToken()).thenReturn("access-token-123");

    BatchAuthRestTemplateInterceptor interceptor =
        new BatchAuthRestTemplateInterceptor(authService);

    HttpRequest request = mock(HttpRequest.class);
    HttpHeaders headers = new HttpHeaders();
    when(request.getHeaders()).thenReturn(headers);

    ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
    ClientHttpResponse response = mock(ClientHttpResponse.class);
    when(execution.execute(any(), any())).thenReturn(response);

    ClientHttpResponse actual = interceptor.intercept(request, new byte[0], execution);

    assertThat(actual).isSameAs(response);
    assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer access-token-123");
    verify(execution).execute(eq(request), any());
  }
}
