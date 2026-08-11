package com.pcis.premium.contract;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.jupiter.api.Test;

class PremiumRatingContractLintTest {

  @Test
  void premiumRatingContractIsValidOpenApi31WithZeroErrors() {
    ParseOptions options = new ParseOptions();
    options.setResolve(true);

    SwaggerParseResult result =
        new OpenAPIV3Parser().readContents(PremiumRatingContractSupport.contractYaml(), null, options);

    assertThat(result.getMessages())
        .as("OpenAPI linter messages: %s", result.getMessages())
        .isEmpty();
    assertThat(result.getOpenAPI()).isNotNull();
    assertThat(result.getOpenAPI().getOpenapi()).isEqualTo("3.1.0");
    assertThat(result.getOpenAPI().getPaths()).containsKeys("/calculations", "/calculations/{calculationId}");
  }
}
