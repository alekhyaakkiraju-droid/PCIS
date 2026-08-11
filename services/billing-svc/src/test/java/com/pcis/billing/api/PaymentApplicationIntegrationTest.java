package com.pcis.billing.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.billing.api.dto.PaymentRequest;
import com.pcis.billing.support.BillingTestSecurityConfig;
import com.pcis.billing.support.PostgresTestContainer;
import com.pcis.billing.support.TestEnvironment;
import com.pcis.billing.support.TestJwtFactory;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(BillingTestSecurityConfig.class)
@EnabledIf("com.pcis.billing.support.TestEnvironment#isDockerAvailable")
class PaymentApplicationIntegrationTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private DataSource dataSource;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void loadFixture() {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScript(new ClassPathResource("fixtures/payment-application-scenario.sql"));
    populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
    populator.execute(dataSource);
  }

  @Test
  void applyPaymentAllocatesOldestDueFirst() throws Exception {
    PaymentRequest request =
        new PaymentRequest(
            "POLPAY001",
            "CUST001",
            "600.00",
            "CH",
            LocalDate.parse("2024-06-15"),
            "idem-600");

    mockMvc
        .perform(
            post("/v1/payments")
                .with(TestJwtFactory.asBillingWriter())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.paymentId").value(org.hamcrest.Matchers.startsWith("PAY")))
        .andExpect(jsonPath("$.allocations.length()").value(3));

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM PAYMENT_T p "
                    + "JOIN INVOICE_T i ON p.INVOICE_ID = i.INVOICE_ID "
                    + "WHERE i.POL_NBR = 'POLPAY001'",
                Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM PAYMENT_APPLICATION_T pa "
                    + "JOIN PAYMENT_T p ON pa.PAYMENT_ID = p.PAYMENT_ID WHERE p.POL_NBR = 'POLPAY001'",
                Integer.class))
        .isEqualTo(3);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT AMT_PAID FROM BILLING_SCHEDULE_T WHERE POL_NBR = 'POLPAY001' AND INSTALLMENT_NBR = 1",
                BigDecimal.class))
        .isEqualByComparingTo("300.00");
    String status =
        jdbcTemplate.queryForObject(
            "SELECT SCHED_STATUS FROM BILLING_SCHEDULE_T WHERE POL_NBR = 'POLPAY001' AND INSTALLMENT_NBR = 1",
            String.class);
    assertThat(status.trim()).isEqualTo("P");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE EVENT_TYPE = 'PaymentApplied'", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void duplicatePaymentReturns409() throws Exception {
    PaymentRequest request =
        new PaymentRequest(
            "POLPAY001",
            "CUST001",
            "200.00",
            "AC",
            LocalDate.parse("2024-06-15"),
            "idem-dup");

    mockMvc
        .perform(
            post("/v1/payments")
                .with(TestJwtFactory.asBillingWriter())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/v1/payments")
                .with(TestJwtFactory.asBillingWriter())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM PAYMENT_T WHERE PAYMENT_TOKEN = 'idem-dup'", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void overApplicationReturns400() throws Exception {
    PaymentRequest request =
        new PaymentRequest(
            "POLPAY001",
            "CUST001",
            "2000.00",
            "WI",
            LocalDate.parse("2024-06-15"),
            "idem-over");

    mockMvc
        .perform(
            post("/v1/payments")
                .with(TestJwtFactory.asBillingWriter())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.maxApplicableAmount").exists());

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM PAYMENT_T WHERE PAYMENT_TOKEN = 'idem-over'", Integer.class))
        .isZero();
  }

  @Test
  void unauthorizedCallerReturns403() throws Exception {
    PaymentRequest request =
        new PaymentRequest(
            "POLPAY001",
            "CUST001",
            "100.00",
            "CH",
            LocalDate.parse("2024-06-15"),
            "idem-403");

    mockMvc
        .perform(
            post("/v1/payments")
                .with(TestJwtFactory.asBillingReader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  void outboxFailureRollsBackPayment() throws Exception {
    jdbcTemplate.execute("DROP TABLE IF EXISTS outbox_events CASCADE");
    PaymentRequest request =
        new PaymentRequest(
            "POLPAY001",
            "CUST001",
            "100.00",
            "CH",
            LocalDate.parse("2024-06-15"),
            "idem-audit-fail");

    mockMvc
        .perform(
            post("/v1/payments")
                .with(TestJwtFactory.asBillingWriter())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().is5xxServerError());

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM PAYMENT_T WHERE PAYMENT_TOKEN = 'idem-audit-fail'",
                Integer.class))
        .isZero();

    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS outbox_events (
            ID BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            AGGREGATE_TYPE VARCHAR(100) NOT NULL,
            AGGREGATE_ID VARCHAR(100) NOT NULL,
            EVENT_TYPE VARCHAR(100) NOT NULL,
            PAYLOAD JSONB NOT NULL,
            IDEMPOTENCY_KEY UUID NOT NULL,
            STATUS VARCHAR(20) NOT NULL DEFAULT 'PENDING',
            ATTEMPT_COUNT INTEGER NOT NULL DEFAULT 0,
            NEXT_ATTEMPT_AT TIMESTAMP,
            LAST_ERROR VARCHAR(500),
            CRT_USER VARCHAR(10),
            CRT_TIMESTAMP TIMESTAMP,
            UPD_USER VARCHAR(10),
            UPD_TIMESTAMP TIMESTAMP,
            CONSTRAINT uq_outbox_idempotency UNIQUE (IDEMPOTENCY_KEY)
        )
        """);
  }
}
