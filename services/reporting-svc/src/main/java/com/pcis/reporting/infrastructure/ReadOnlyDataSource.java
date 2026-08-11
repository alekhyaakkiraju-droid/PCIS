package com.pcis.reporting.infrastructure;

import com.pcis.reporting.config.ReadOnlyViolationException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.regex.Pattern;
import javax.sql.DataSource;

public class ReadOnlyDataSource implements DataSource {
  private static final String ACTOR = "reporting-svc";
  private static final String RESOURCE = "aurora-reader";
  private static final Pattern WRITE_SQL =
      Pattern.compile(
          "^\\s*(INSERT|UPDATE|DELETE|MERGE|TRUNCATE|CREATE|ALTER|DROP|GRANT|REVOKE|COPY\\s+\\S+\\s+FROM)\\b",
          Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

  private final DataSource delegate;
  private final ReadOnlyViolationLogger violationLogger;

  public ReadOnlyDataSource(DataSource delegate, ReadOnlyViolationLogger violationLogger) {
    this.delegate = delegate;
    this.violationLogger = violationLogger;
  }

  static void assertReadOnlySql(String sql) {
    if (sql != null && !sql.isBlank() && WRITE_SQL.matcher(sql.stripLeading()).find()) {
      throw new ReadOnlyViolationException(
          "Write operation rejected on reporting read replica datasource");
    }
  }

  @Override
  public Connection getConnection() throws java.sql.SQLException {
    return wrap(delegate.getConnection());
  }

  @Override
  public Connection getConnection(String username, String password) throws java.sql.SQLException {
    return wrap(delegate.getConnection(username, password));
  }

  private Connection wrap(Connection connection) {
    return (Connection)
        Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class[] {Connection.class},
            (proxy, method, args) -> {
              switch (method.getName()) {
                case "prepareStatement", "prepareCall" -> assertReadOnlySql((String) args[0]);
                case "nativeSQL" -> assertReadOnlySql((String) args[0]);
                case "setReadOnly" -> {
                  if (args.length == 1 && Boolean.FALSE.equals(args[0])) {
                    violationLogger.logViolation(ACTOR, RESOURCE, "SET_READ_WRITE");
                    throw new ReadOnlyViolationException(
                        "Write operation rejected on reporting read replica datasource");
                  }
                }
                case "commit" -> {
                  violationLogger.logViolation(ACTOR, RESOURCE, "COMMIT");
                  throw new ReadOnlyViolationException(
                      "Write operation rejected on reporting read replica datasource");
                }
                default -> {}
              }
              return method.invoke(connection, args);
            });
  }

  @Override
  public java.io.PrintWriter getLogWriter() throws java.sql.SQLException {
    return delegate.getLogWriter();
  }

  @Override
  public void setLogWriter(java.io.PrintWriter out) throws java.sql.SQLException {
    delegate.setLogWriter(out);
  }

  @Override
  public void setLoginTimeout(int seconds) throws java.sql.SQLException {
    delegate.setLoginTimeout(seconds);
  }

  @Override
  public int getLoginTimeout() throws java.sql.SQLException {
    return delegate.getLoginTimeout();
  }

  @Override
  public java.util.logging.Logger getParentLogger() {
    try {
      return delegate.getParentLogger();
    } catch (java.sql.SQLException ex) {
      throw new IllegalStateException(ex);
    }
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws java.sql.SQLException {
    return iface.isInstance(this) ? iface.cast(this) : delegate.unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws java.sql.SQLException {
    return iface.isInstance(this) || delegate.isWrapperFor(iface);
  }
}
