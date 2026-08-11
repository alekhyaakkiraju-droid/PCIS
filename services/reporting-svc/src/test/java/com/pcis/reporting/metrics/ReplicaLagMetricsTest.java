package com.pcis.reporting.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class ReplicaLagMetricsTest {
  @Test
  void refreshLagUpdatesGauge() throws Exception {
    DataSource ds = mock(DataSource.class);
    Connection c = mock(Connection.class);
    Statement st = mock(Statement.class);
    ResultSet rs = mock(ResultSet.class);
    when(ds.getConnection()).thenReturn(c);
    when(c.createStatement()).thenReturn(st);
    when(st.executeQuery(anyString())).thenReturn(rs);
    when(rs.next()).thenReturn(true);
    when(rs.getDouble("lag_seconds")).thenReturn(3.0);
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    ReplicaLagMetrics metrics = new ReplicaLagMetrics(registry, ds);
    metrics.refreshLag();
    assertThat(metrics.lagSecondsValue()).isEqualTo(3.0);
    assertThat(registry.find(ReplicaLagMetrics.METRIC_NAME).gauge()).isNotNull();
  }
}
