package com.pcis.batch.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pcis.batch.auth.support.TestJwtFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.batch.core.JobExecution;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class BatchSecurityContextInitializerTest {

  @AfterEach
  void cleanup() {
    MDC.clear();
    SecurityContextHolder.clearContext();
  }

  @Test
  void beforeJobSetsSecurityContextAndMdcActorFromJwtSub() {
    String token = TestJwtFactory.tokenWithSubject("service-account-batch-billing");
    BatchAuthenticationService authService = mock(BatchAuthenticationService.class);
    when(authService.getAccessToken()).thenReturn(token);

    BatchSecurityContextInitializer initializer = new BatchSecurityContextInitializer(authService);
    JobExecution jobExecution = mock(JobExecution.class);

    initializer.beforeJob(jobExecution);

    assertThat(SecurityContextHolder.getContext().getAuthentication())
        .isInstanceOf(JwtAuthenticationToken.class);
    JwtAuthenticationToken authentication =
        (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication.getToken().getSubject())
        .isEqualTo("service-account-batch-billing");
    assertThat(MDC.get(BatchAuthMdcKeys.ACTOR)).isEqualTo("service-account-batch-billing");
  }

  @Test
  void afterJobClearsSecurityContextAndMdc() {
    BatchAuthenticationService authService = mock(BatchAuthenticationService.class);
    when(authService.getAccessToken())
        .thenReturn(TestJwtFactory.tokenWithSubject("service-account-batch-audit"));

    BatchSecurityContextInitializer initializer = new BatchSecurityContextInitializer(authService);
    initializer.beforeJob(mock(JobExecution.class));

    initializer.afterJob(mock(JobExecution.class));

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(MDC.get(BatchAuthMdcKeys.ACTOR)).isNull();
  }
}
