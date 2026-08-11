package com.pcis.sync.support;

import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

@Component
public class H2SourceInitializer {

  private final DataSource sourceDataSource;

  public H2SourceInitializer(@Qualifier("sourceDataSource") DataSource sourceDataSource) {
    this.sourceDataSource = sourceDataSource;
  }

  public void resetSourceData() {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScript(new ClassPathResource("fixtures/source_customer_data.sql"));
    populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
    populator.setContinueOnError(false);
    populator.execute(sourceDataSource);
  }

  public void insertAdditionalSourceRow() {
    org.springframework.jdbc.core.JdbcTemplate jdbc =
        new org.springframework.jdbc.core.JdbcTemplate(sourceDataSource);
    jdbc.update(
        """
        INSERT INTO CUSTOMER_T (CUST_ID, CUST_NAME, CUST_STATUS, UPD_TIMESTAMP)
        VALUES (4, 'Delta Holdings', 'A', TIMESTAMP '2024-04-01 08:00:00')
        """);
  }
}
