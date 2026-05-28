package com.relationaljunction.jdbc.dbf.h2;

import java.sql.SQLException;
import java.util.Properties;

public class DBFLogicalConnection2 extends DBFConnection2{
  private DBFPooledConnection2 poolConn = null;
  DBFLogicalConnection2(DBFPooledConnection2 poolConn, Properties info)
      throws Exception {
    super(poolConn.getDataSource().driver, info);
    this.poolConn = poolConn;
  }

  // not close the physical connection when closing this logical connection
  // Also notificates pooling listeners that the logical connection is closed.
  public void close(){
    poolConn.getDataSource().driver.writeLog("Logical connection (" + this +
					     ") -> close()");
    poolConn.fireClosingEvent();
  }

  void closePhysicalConnection() throws SQLException{
    super.close();
  }

  protected void finalize() throws Throwable {
    try {
      if (poolConn != null)
	poolConn = null;
    }
    catch (Exception ex) {
    }
  }

}
