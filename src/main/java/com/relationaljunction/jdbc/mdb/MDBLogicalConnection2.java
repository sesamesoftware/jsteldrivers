package com.relationaljunction.jdbc.mdb;

import java.util.Properties;
import java.sql.SQLException;

import com.relationaljunction.jdbc.mdb.h2.MDBConnection2;

public class MDBLogicalConnection2 extends MDBConnection2{
  private MDBPooledConnection2 poolConn = null;
  MDBLogicalConnection2(MDBPooledConnection2 poolConn, Properties info)
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
