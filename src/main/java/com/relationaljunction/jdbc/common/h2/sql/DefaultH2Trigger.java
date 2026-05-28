package com.relationaljunction.jdbc.common.h2.sql;

import java.sql.*;

import org.h2.jdbc.*;

import com.relationaljunction.jdbc.common.h2.*;


public class DefaultH2Trigger implements H2TriggerIF {
   private String tableName = null;
   private CacheTable cacheTable = null;
   private int triggerType = INSERT;
   private boolean includeRowIdColumn;

   /**
    * This method is called by the database engine once when initializing the
    * trigger.
    *
    * @param conn        a connection to the database
    * @param schemaName  the name of the schema
    * @param triggerName the name of the trigger used in the CREATE TRIGGER
    *                    statement
    * @param tableName   the name of the table
    * @param before      whether the fire method is called before or after the
    *                    operation is performed
    * @param type        the operation type: INSERT, UPDATE, or DELETE
    * @throws SQLException
    * @todo Implement this org.h2.api.Trigger method
    */
   public void init(Connection conn, String schemaName, String triggerName,
                    String tableName, boolean before, int type) throws
           SQLException {
//    System.out.println("Initializing trigger " + triggerName + " (" +
//                       this.toString() + ")");
      this.tableName = tableName;

      CommonConnection2 commonConn = (CommonConnection2) ((org.h2.engine.Session)
              ((JdbcConnection) conn).getSession()).getWrapperConnection();

      this.includeRowIdColumn = commonConn.getSchemaIF2().includeRowIdColumn();

      // while opening connection in the persistent mode "commonConn" is null,
      // because H2 initializes triggers first
      if (commonConn == null)
         return;

      // extract a cache table name from a trigger name
      String tableNameToSearch = triggerName.substring("RJ_XXXXXX_TRIGGER_".
              length());

      cacheTable = commonConn.getCacheTableManager().getTable(tableNameToSearch);

      if (cacheTable == null) throw new SQLException(
              "Unexpected error in DefaultH2Trigger.init() method. Table '" +
                      tableName + "' is not loaded in the hash");

      this.triggerType = type;

      if (triggerType == INSERT)
         cacheTable.setInsertTrigger(this);
      else if (triggerType == UPDATE)
         cacheTable.setUpdateTrigger(this);
      else if (triggerType == DELETE)
         cacheTable.setDeleteTrigger(this);
   }

   /**
    * This method is called for each triggered action.
    *
    * @param conn   a connection to the database
    * @param oldRow the old row, or null if no old row is available (for INSERT)
    * @param newRow the new row, or null if no new row is available (for DELETE)
    * @throws SQLException if the operation must be undone
    * @todo Implement this org.h2.api.Trigger method
    */
   public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws
           SQLException {
      if (includeRowIdColumn) {
         Object[] oldRowWithoutRowId = null;
         Object[] newRowWithoutRowId = null;

         if (oldRow != null) {
            oldRowWithoutRowId = new Object[oldRow.length - 1];
            System.arraycopy(oldRow, 1, oldRowWithoutRowId, 0, oldRow.length - 1);
         }

         if (newRow != null) {
            newRowWithoutRowId = new Object[newRow.length - 1];
            System.arraycopy(newRow, 1, newRowWithoutRowId, 0, newRow.length - 1);
         }

         cacheTable.insertOperation(triggerType, oldRowWithoutRowId, newRowWithoutRowId);
      } else {
         cacheTable.insertOperation(triggerType, oldRow, newRow);
      }
   }

   public void closeTrigger() {
      cacheTable = null;
   }

   /**
    * This method is called when the database is closed.
    * If the method throws an exception, it will be logged, but
    * closing the database will continue.
    *
    * @throws SQLException
    */
   public void close() {
   }

   /**
    * This method is called when the trigger is dropped.
    *
    * @throws SQLException
    */
   public void remove() {
   }

}
