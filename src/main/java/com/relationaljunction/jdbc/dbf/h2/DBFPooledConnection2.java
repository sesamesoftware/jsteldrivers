package com.relationaljunction.jdbc.dbf.h2;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Vector;

import javax.sql.*;

public class DBFPooledConnection2 implements PooledConnection {
   private DBFLogicalConnection2 logicalConn = null;
   private DBFConnectionPoolDataSource2 poolDS = null;
   private final Vector listeners = new Vector();
   private boolean closed = false;

   public DBFPooledConnection2(DBFConnectionPoolDataSource2 poolDS) {
      this.poolDS = poolDS;
   }

   public DBFConnectionPoolDataSource2 getDataSource() {
      return poolDS;
   }

   // get a logical connection
   synchronized public Connection getConnection() throws SQLException {
      if (closed)
         throw new SQLException("PooledConnection is already closed!");
      if (logicalConn != null)
         return logicalConn;
      try {
         logicalConn = new DBFLogicalConnection2(this, poolDS.getProperties());
      } catch (Exception ex) {
         throw new SQLException(ex.getMessage());
      }
      poolDS.driver.writeLog("PooledConnection (" + this +
              ") -> getConnection() (" + logicalConn);
      return logicalConn;
   }

   synchronized public void close() throws SQLException {
      closed = true;
      if (logicalConn != null) {
//      logicalConn.closePhysicalConnection();
         logicalConn = null;
         poolDS.driver.writeLog("PooledConnection (" + this +
                 ") -> close()");
      }
   }

   protected void finalize() throws Throwable {
      try {
         close();
      } catch (Exception ex) {
      }
   }

   public void addConnectionEventListener(ConnectionEventListener listener) {
      listeners.add(listener);
   }

   public void removeConnectionEventListener(ConnectionEventListener listener) {
      listeners.remove(listener);
   }

   public void fireClosingEvent() {
      for (int i = 0; i < listeners.size(); i++) {
         ConnectionEventListener listener = (ConnectionEventListener) listeners.
                 get(i);
         ConnectionEvent event = new ConnectionEvent(this);
         listener.connectionClosed(event);
      }
   }

   // --- JDK 1.6 ---

   public void addStatementEventListener(StatementEventListener listener) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public void removeStatementEventListener(StatementEventListener listener) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

}
