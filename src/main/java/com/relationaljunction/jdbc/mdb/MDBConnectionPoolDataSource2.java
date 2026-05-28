package com.relationaljunction.jdbc.mdb;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

public class MDBConnectionPoolDataSource2 extends MDBDataSource2 implements
    ConnectionPoolDataSource {
  private PooledConnection poolConn = null;

  public MDBConnectionPoolDataSource2(){
    super();
  }

  synchronized public PooledConnection getPooledConnection() throws java.sql.SQLException {
    if (poolConn != null)
      return poolConn;
    poolConn = new MDBPooledConnection2(this);
    driver.writeLog("Connection Pool Data Source " + getProperties() +
		    ", java.sql.Driver = [" +
		    driver.toString() + "] -> getPooledConnection() (" + poolConn +
		    ")");
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
