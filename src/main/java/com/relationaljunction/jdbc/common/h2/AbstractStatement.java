package com.relationaljunction.jdbc.common.h2;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

abstract public class AbstractStatement implements Statement {
  protected CommonConnection2 conn;
  protected Statement h2Stat;
  protected boolean closed = false;
  protected List<String> batch = new ArrayList<String>();

  //#######for registration###########
  protected final static int mr = 1000;
  private final static int mq = 50;

  private static final int a = mq;

  protected AbstractStatement(CommonConnection2 conn) {
    this.conn = conn;
  }

  protected AbstractStatement(CommonConnection2 conn, Statement h2Stat) {
    this.conn = conn;
    this.h2Stat = h2Stat;
  }

  public void init() {
    // ######for registration###########
//    try {
//      this.h2Stat.setMaxRows(mr);
//    } catch (SQLException ex) {
//    }
    //#################################
  }

  public void setMaxRows(int max) throws SQLException {
// ######for registration###########
//    if (max <= 0 || max > mr)
//      h2Stat.setMaxRows(mr);
//    else
//#################################
      h2Stat.setMaxRows(max);
  }

  protected void r() throws SQLException {
//    System.out.println("query= " + a);

    //#######for registration###########
//    if (a-- <= 1)
//      throw new SQLException(
//          "The trial version of the driver allows executing of not more than " +
//          mq + " queries at once. For more information about purchasing please visit http://www.csv-jdbc.com/order.htm");
    //#################################
  }

  protected void updateRecordsLoadedInfo() throws SQLException {
    //########for registration#####
//    Statement st = null;
//    try {
//      st = conn.getH2Connection().createStatement();
//      st.executeUpdate("UPDATE RJ_SCHEMA.TABLE_STAT SET name='table', info='records loaded', stat=rj_get_records_loaded()");
//    } catch (SQLException ex1) {
//      throw new SQLException("Unexpected error: can't find a function");
//    } finally {
//      if (st != null)
//        st.close();
//    }
    //#############################
  }

}
