package com.relationaljunction.jdbc.csv;

import java.sql.SQLException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.jdbc.csv.h2.CsvConnection2;

public class CsvLogicalConnection2 extends CsvConnection2{
  private final Logger log = LoggerFactory.getLogger("CsvLogicalConnection2");

  private CsvPooledConnection2 poolConn = null;

  CsvLogicalConnection2(CsvPooledConnection2 poolConn, Properties info)
      throws Exception {
    super(poolConn.getDataSource().driver, info);
    this.poolConn = poolConn;
  }

  // not close the physical connection when closing this logical connection
  // Also notificates pooling listeners that the logical connection is closed.
  public void close(){
    if (log.isDebugEnabled())
      log.debug("Relational Junction CSV Driver -> Logical connection (" +
              this + ") -> close()");
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
