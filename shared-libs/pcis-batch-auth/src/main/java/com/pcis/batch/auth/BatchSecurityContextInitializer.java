package com.pcis.batch.auth;

import org.slf4j.MDC;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Initializes {@link org.springframework.security.core.context.SecurityContext} and MDC actor from
 * the batch workload JWT {@code sub} claim at job start.
 */
public class BatchSecurityContextInitializer implements JobExecutionListener {

  private final BatchAuthenticationService authenticationService;

  public BatchSecurityContextInitializer(BatchAuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @Override
  public void beforeJob(JobExecution jobExecution) {
    String accessToken = authenticationService.getAccessToken();
    String subject = JwtSubjectExtractor.extractSubject(accessToken);

    Jwt jwt =
        Jwt.withTokenValue(accessToken)
            .header("alg", "none")
            .subject(subject)
            .build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    MDC.put(BatchAuthMdcKeys.ACTOR, subject);
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    MDC.remove(BatchAuthMdcKeys.ACTOR);
    SecurityContextHolder.clearContext();
  }
}
