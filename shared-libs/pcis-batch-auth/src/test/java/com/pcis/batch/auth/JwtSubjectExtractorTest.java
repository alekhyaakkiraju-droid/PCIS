package com.pcis.batch.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pcis.batch.auth.support.TestJwtFactory;
import org.junit.jupiter.api.Test;

class JwtSubjectExtractorTest {

  @Test
  void extractsSubjectClaim() {
    String token = TestJwtFactory.tokenWithSubject("service-account-batch-claims");
    assertThat(JwtSubjectExtractor.extractSubject(token))
        .isEqualTo("service-account-batch-claims");
  }

  @Test
  void rejectsBlankToken() {
    assertThatThrownBy(() -> JwtSubjectExtractor.extractSubject(" "))
        .isInstanceOf(BatchConfigurationException.class);
  }

  @Test
  void rejectsMalformedToken() {
    assertThatThrownBy(() -> JwtSubjectExtractor.extractSubject("not-a-jwt"))
        .isInstanceOf(BatchConfigurationException.class);
  }
}
