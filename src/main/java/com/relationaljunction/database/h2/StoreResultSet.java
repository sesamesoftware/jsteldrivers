package com.relationaljunction.database.h2;

import java.sql.*;

import com.relationaljunction.database.*;

public class StoreResultSet implements StoreRecordsIF {
   private ResultSet rs = null;
   private StoreFieldIF[] fields = null;
   private boolean hasReservedInfo = false;

//  public StoreResultSet(ResultSet rs) throws StoreException {
//    this.rs = rs;
//    try {
//      ResultSetMetaData rsmd = rs.getMetaData();
//      this.colCount = rsmd.getColumnCount();
//
//      fields = new StoreFieldIF[colCount - 2];
//      for (int i = 1; i < colCount - 2; i++) {
//        rsmd.getColumnName(i + 1);
//      }
//
//    } catch (SQLException ex) {
//      throw new StoreException(
//	  "Error while initializing a StoreResultSet instance: " +
//	  ex.getMessage());
//    }
//  }

   public StoreResultSet(ResultSet rs, StoreFieldIF[] fields) throws StoreException {
      this.rs = rs;
      this.fields = fields;

      try {
         ResultSetMetaData rsmd = rs.getMetaData();
         hasReservedInfo = rsmd.getColumnName(1).equalsIgnoreCase("RJ_OPER_ID");
      } catch (SQLException ex) {
         throw new StoreException(
                 "Error while initializing a StoreResultSet instance: " +
                         ex.getMessage(), ex);
      }

   }

   public void beforeFirst() {
      try {
         rs.beforeFirst();
      } catch (SQLException ex) {
         throw new RuntimeException(
                 "Error in StoreResultSet.beforeFirst() method: " +
                         ex.getMessage());
      }
   }

   public boolean hasNext() {
      try {
         return rs.next();
      } catch (SQLException ex) {
         throw new RuntimeException("Error in StoreResultSet.hasNext() method: " +
                 ex.getMessage());
      }
   }

   // creates a record deducting reserved columns OPER_ID, OPER_TYPE
   public StoreRecordIF nextRecord() {
      Object[] objs = new Object[fields.length];
      try {
         for (int i = 0; i < fields.length; i++) {
            if (hasReservedInfo)
               objs[i] = rs.getObject(i + 4);
            else
               objs[i] = rs.getObject(i + 1);
         }
      } catch (SQLException ex) {
         throw new RuntimeException("Error in StoreResultSet.nextRecord() method: " +
                 ex.getMessage());
      }
      return new DefaultStoreRecord(fields, objs);
   }

   // close and clear object
   public void clear() {
      try {
         if (rs != null) rs.close();
         rs = null;
         fields = null;
      } catch (SQLException ex) {
         ex.printStackTrace();
      }
   }
}
