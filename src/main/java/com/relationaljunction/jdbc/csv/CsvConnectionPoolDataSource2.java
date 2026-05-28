package com.relationaljunction.jdbc.csv;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsvConnectionPoolDataSource2
    extends CsvDataSource2
    implements ConnectionPoolDataSource {
  private final Logger log = LoggerFactory.getLogger("CsvConnectionPoolDataSource2");

  private PooledConnection poolConn = null;

  public CsvConnectionPoolDataSource2(){
    super();
  }

  synchronized public PooledConnection getPooledConnection() throws java.sql.SQLException {
    if (poolConn != null)
      return poolConn;
    poolConn = new CsvPooledConnection2(this);
    if (log.isDebugEnabled())
      log.debug("Relational Junction CSV Driver -> Connection Pool Data Source " + getProperties() +
                ", java.sql.Driver = [" + driver.toString() +
                "] -> getPooledConnection() (" + poolConn + ")");
    return poolConn;
  }

  public PooledConnection getPooledConnection(String user, String passw) throws
      java.sql.SQLException {
    return getPooledConnection();
  }

  protected void finalize() throws Throwable {
    try {
      if (poolConn != null)
	poolConn.close();
      poolConn = null;
    }
    catch (Exception ex) {
    }
  }

}
