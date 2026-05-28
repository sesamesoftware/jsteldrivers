package com.relationaljunction.jdbc.common.h2;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.jdbc.common.h2.sql.SQLCommand;
import com.relationaljunction.utils.OtherUtils;
import com.relationaljunction.utils.UnexpectedException;

public class CacheTableManager {
   public final static char[] QUOTE_CHARS = new char[]{'"', '!', '`'};

   private final Logger log = LoggerFactory.getLogger("CacheTableManager");

   protected CommonConnection2 conn = null;
   PreparedStatement pstInsertTablesInfo = null;
   PreparedStatement pstRemoveTablesInfo = null;

   private Map<String,
           CacheTable> cacheTableMap = new ConcurrentHashMap<String, CacheTable>();

   private ThreadTransaction threadTransaction = new ThreadTransaction();

   /*
     private static class ThreadTransaction {
        private ConcurrentMap<Integer,
                SortedMap<String, CacheTable>> tableInTransactionMap =
                new ConcurrentHashMap<Integer, SortedMap<String, CacheTable>>();

        private ThreadTransaction() {
        }

        void addCacheTable(String sqlTableName, CacheTable cacheTable) {
           int key = Thread.currentThread().hashCode();

           SortedMap<String, CacheTable> tableMap =
                   tableInTransactionMap.get(key);

           if (tableMap == null) {
              SortedMap<String, CacheTable> newTableMap = new TreeMap<String, CacheTable>();
              newTableMap.put(sqlTableName, cacheTable);
              tableInTransactionMap.put(key, newTableMap);
           } else if (tableMap.get(sqlTableName) == null) {
              tableMap.put(sqlTableName, cacheTable);
           }
        }


        Collection<CacheTable> getSortedCacheTables() {
           int key = Thread.currentThread().hashCode();
           return tableInTransactionMap.get(key).values();
        }

        void removeAllCacheTables() {
           int key = Thread.currentThread().hashCode();
           tableInTransactionMap.remove(key);
        }

        void removeCacheTable(String sqlTableName) {
           int key = Thread.currentThread().hashCode();
           SortedMap<String, CacheTable> tableMap = tableInTransactionMap.get(key);

           if (tableMap != null) {
              tableMap.remove(sqlTableName);
           }
        }

        void removeCacheTables(Set<String> sqlTableNameSet) {
           int key = Thread.currentThread().hashCode();
           SortedMap<String, CacheTable> tableMap = tableInTransactionMap.get(key);

           if (tableMap == null) return;

           for (String sqlTableName : sqlTableNameSet) {
              tableMap.remove(sqlTableName);
           }
        }

        void clear() {
           tableInTransactionMap.clear();
        }
     }
   */

   private static class ThreadTransaction {
      private ThreadLocal<SortedMap<String, CacheTable>> tableInTransactionMap =
              new ThreadLocal<SortedMap<String, CacheTable>>() {
                 @Override
                 protected SortedMap<String, CacheTable> initialValue() {
                    return new TreeMap<String, CacheTable>();
                 }
              };

      private ThreadTransaction() {
      }

      void addCacheTable(String sqlTableName, CacheTable cacheTable) {
         SortedMap<String, CacheTable> tableMap = tableInTransactionMap.get();

         if (tableMap.get(sqlTableName) == null) {
            tableMap.put(sqlTableName, cacheTable);
         }
      }


      Collection<CacheTable> getSortedCacheTables() {
         SortedMap<String, CacheTable> tableMap = tableInTransactionMap.get();
         return tableMap.values();
      }

      void removeAllCacheTables() {
         SortedMap<String, CacheTable> tableMap = tableInTransactionMap.get();
         tableMap.clear();
      }

      void removeCacheTable(String sqlTableName) {
         SortedMap<String, CacheTable> tableMap = tableInTransactionMap.get();
         tableMap.remove(sqlTableName);
      }

      void removeCacheTables(Set<String> sqlTableNameSet) {
         SortedMap<String, CacheTable> tableMap = tableInTransactionMap.get();

         for (String sqlTableName : sqlTableNameSet) {
            tableMap.remove(sqlTableName);
         }
      }

      void clear() {
         tableInTransactionMap.get().clear();
         tableInTransactionMap.remove();
         tableInTransactionMap = null;
      }
   }

   public CacheTableManager(CommonConnection2 conn) {
      this.conn = conn;
   }

   public CommonConnection2 getConnection() {
      return conn;
   }

   public void initTablesInPersistentMode() {
      try {
         pstInsertTablesInfo = conn.getH2Connection().prepareStatement(
                 "INSERT INTO RJ_SCHEMA.TABLES_INFO" +
                         "(table_name, insert_oper_table_sql, create_oper_table_sql, create_oper_index_sql, fields, sql_table_name, mod_date)" +
                         " VALUES(?,?,?,?,?,?,?)");

         pstRemoveTablesInfo = conn.getH2Connection().prepareStatement(
                 "DELETE FROM RJ_SCHEMA.TABLES_INFO WHERE table_name = ?");

         // load existing tables in H2
         Statement st = conn.getH2Connection().createStatement();
         ResultSet rs = st.executeQuery("SELECT * FROM RJ_SCHEMA.TABLES_INFO");

         while (rs.next()) {
            String cacheTableName = rs.getString("table_name");
            CacheTable newCacheTable = new CacheTable(this);
            cacheTableMap.put(cacheTableName, newCacheTable);
            newCacheTable.loadDataFromDB(rs);
            newCacheTable.initTriggers();
            newCacheTable.setAlreadyLoaded(true);
         }

         rs.close();
         st.close();
      } catch (SQLException ex) {
         throw new UnexpectedException(
                 "Unexpected error in CacheTableManager.loadOperationTablesInPersistentMode(). Error was " +
                         ex.getMessage());
      }
   }

   protected CacheTable createTable(String sqlTableName) {
      if (conn.isCachingMode())
         return new CacheTable(this, sqlTableName);
      else
         return new WrapperCacheTable(this, sqlTableName);
   }

   /**
    * return a table name in the cache in accordance with tableName specified in SQL
    */
   public static String getCacheTableName(String sqlTableName) {
      if (com.relationaljunction.utils.StringUtils.isDoubleQuoted(sqlTableName)) {
         return com.relationaljunction.utils.StringUtils.unquote(sqlTableName, QUOTE_CHARS);
      } else
         return com.relationaljunction.utils.StringUtils.unquote(sqlTableName, QUOTE_CHARS).
                 toUpperCase();
   }

//   public static String getCacheTableName2(String sqlTableName) {
//      if (com.relationaljunction.utils.StringUtils.isDoubleQuoted(sqlTableName)) {
//         return com.relationaljunction.utils.StringUtils.unquote(sqlTableName, QUOTE_CHARS);
//      } else
//         return com.relationaljunction.utils.StringUtils.unquote(sqlTableName, QUOTE_CHARS).
//                 toUpperCase();
//      if (com.relationaljunction.utils.StringUtils.isDoubleQuoted(sqlTableName)) {
//         return com.relationaljunction.utils.StringUtils.unquote(sqlTableName, QUOTE_CHARS);
//      } else
//         return com.relationaljunction.utils.StringUtils.unquote(sqlTableName, QUOTE_CHARS).
//                 toUpperCase();

//   }

   /**
    * return a file name in accordance with tableName specified in SQL
    */
   public String getFileTableName(String sqlTableName) {
      return conn.getSchemaIF2().getFileTableName(sqlTableName);
   }

   public CacheTable getTable(String sqlTableName) {
      String cacheTableName = getCacheTableName(sqlTableName);
      return cacheTableMap.get(cacheTableName);
   }

   public Collection<CacheTable> getSortedCacheTables() {
      return new TreeMap<String, CacheTable>(cacheTableMap).values();
   }

//   public Set<String> getSortedTableNamesSet() {
//      return new TreeMap<String, CacheTable>(cacheTableMap).keySet();
//   }

   public void loadTable(String sqlTableName) throws SQLException {
      String cacheTableName = getCacheTableName(sqlTableName);
      CacheTable newCacheTable;

      // lock a map of CacheTable instances
      synchronized (this) {
         CacheTable cacheTable = cacheTableMap.get(cacheTableName);

         if (cacheTable == null) {
            // an CacheTable for sqlTableName does not exist
            // data will be loaded from an external file
            newCacheTable = createTable(sqlTableName);
            // add a table to a map
            cacheTableMap.put(cacheTableName, newCacheTable);
         }
         // an CacheTable for sqlTableName exists
         else
            newCacheTable = cacheTable;
      } // synchronized (this)

      // lock an CacheTable instance
      newCacheTable.writeLock();
      try {
         // synchro check: if a table is already loaded by concurrent threads
         if (newCacheTable.isAlreadyLoaded()) {
            OtherUtils.writeLogInfo(log,
                    "Table '" + cacheTableName +
                            "' is already loaded to H2.");
            return;
         }

         // load data from an external file
         newCacheTable.loadDataFromStoreTable();
         newCacheTable.initTriggers();

         // set a table as being already loaded
         newCacheTable.setAlreadyLoaded(true);
      } catch (Exception ex) {
//         ex.printStackTrace();
         log.error(ex.getMessage(), ex);
         throw conn.driver.createException(ex);
      } finally {
         newCacheTable.writeUnlock();
      }
   }

   void removeTable(String sqlTableName) throws SQLException {
      String cacheTableName = getCacheTableName(sqlTableName);
      CacheTable cacheTable = getTable(sqlTableName);
      cacheTable.writeLock();
      cacheTable.clear();
      cacheTable.writeUnlock();

      // delete an operation table from the map
      cacheTableMap.remove(cacheTableName);
      threadTransaction.removeCacheTable(sqlTableName);
   }

   /*
   private void printTablesInfo() {
      try {
         PrintStream ps = System.out;
         ps.println("---tables information----");

         Statement st = conn.getH2Connection().createStatement();

         ResultSet rs = st.executeQuery(
                 "SELECT * FROM RJ_SCHEMA.TABLES_INFO");
         com.relationaljunction.utils.TestUtils.printColumnsOut(rs, ps);
         com.relationaljunction.utils.TestUtils.printResultSetOut(rs, ps);

         ps.println();
         rs.close();
         st.close();
      } catch (SQLException ex) {
         //      ex.printStackTrace();
      }
   }
   */

   void clearTransactionFlag(SQLCommand sqlCommand) {
      String tableName = sqlCommand.getBaseTable();
      CacheTable cacheTable = getTable(tableName);
      cacheTable.clearTransactionFlag();
   }

   void beginOperation(SQLCommand sqlCommand) {
      String tableName = sqlCommand.getBaseTable();
      CacheTable cacheTable = getTable(tableName);
      cacheTable.beginOperation(sqlCommand);

      threadTransaction.addCacheTable(tableName, cacheTable);
   }

   void endOperation(SQLCommand sqlCommand,
                     long updateCount) {
      String tableName = sqlCommand.getBaseTable();
      CacheTable cacheTable = getTable(tableName);
      cacheTable.endOperation(sqlCommand, updateCount);
   }

   void readLockTables(Set<String> tableNames) {
      Set<String> sortedTablesSet = OtherUtils.getSortedSet(tableNames);

      for (String tableName : sortedTablesSet) {
         CacheTable cacheTable = getTable(tableName);

         if (cacheTable == null)
            throw new NullPointerException("table '" + tableName +
                    "' was not found in CacheTableManager");

         cacheTable.readLock();
      }

   }

//   void readLockTable(String tableName) {
//      CacheTable operationTable = getTable(tableName);
//      if (operationTable != null)
//         operationTable.readLock();
//   }

   void writeLockTables(Set<String> tableNames) {
      SortedSet<String> sortedTablesSet = OtherUtils.getSortedSet(tableNames);

      for (String tableName : sortedTablesSet) {
         CacheTable cacheTable = getTable(tableName);

         if (cacheTable == null)
            throw new NullPointerException("table '" + tableName +
                    "' was not found in CacheTableManager");

         cacheTable.writeLock();
      }
   }

//   void writeLockTable(String tableName) {
//      CacheTable operationTable = getTable(tableName);
//      if (operationTable != null)
//         operationTable.writeLock();
//   }

   void readUnlockTables(Set<String> tableNames) {
      SortedSet<String> sortedTablesSet = OtherUtils.getSortedSet(tableNames);

      for (String tableName : sortedTablesSet) {
         CacheTable cacheTable = getTable(tableName);
         try {
            if (cacheTable == null)
               throw new NullPointerException("table '" + tableName +
                       "' was not found in CacheTableManager");

            cacheTable.readUnlock();
         } catch (Exception e) {
            e.printStackTrace();
            log.warn("error while unlocking table '" + tableName +
                    "'. Error was " + e.getMessage(), e);
         }
      }
   }

//   void readUnlockTable(String tableName) {
//      CacheTable operationTable = getTable(tableName);
//      if (operationTable != null)
//         operationTable.readUnlock();
//   }

   void writeUnlockTables(Set<String> tableNames) {
      SortedSet<String> sortedTablesSet = OtherUtils.getSortedSet(tableNames);

      for (String tableName : sortedTablesSet) {
         CacheTable cacheTable = getTable(tableName);

         try {
            if (cacheTable == null)
               throw new NullPointerException("table '" + tableName +
                       "' was not found in CacheTableManager");

            cacheTable.writeUnlock();
         } catch (Exception e) {
            e.printStackTrace();
            log.warn("error while unlocking table '" + tableName +
                    "'. Error was " + e.getMessage(), e);
         }
      }
   }

//   void writeUnlockTable(String tableName) {
//      CacheTable operationTable = getTable(tableName);
//      if (operationTable != null)
//         operationTable.writeUnlock();
//   }

//   public void commitChanges(Set<String> tableNames) throws java.sql.SQLException {
//      Set<String> sortedTablesSet = OtherUtils.getSortedSet(tableNames);
//
//      for (String tableName : sortedTablesSet) {
//         CacheTable operationTable = getTable(tableName);
//         if (operationTable != null) {
//            operationTable.commit();
//         } else
//            throw new NullPointerException("table '" + tableName +
//                    "' was not found in CacheTableManager");
//      }
//   }


   Collection<CacheTable> getSortedCacheTables(Set<String> tableNames) throws SQLException {
      SortedSet<String> sortedTablesSet = OtherUtils.getSortedSet(tableNames);
      Collection<CacheTable> cacheTables = new LinkedList<CacheTable>();

      for (String tableName : sortedTablesSet) {
         CacheTable cacheTable = getTable(tableName);
         if (cacheTable == null)
            throw new NullPointerException("table '" + tableName +
                    "' was not found in CacheTableManager");

         cacheTables.add(cacheTable);
      }

      return cacheTables;
   }

//   public void reloadTables(Set<String> tableNames) throws java.sql.SQLException {
//      Set<String> sortedTablesSet = OtherUtils.getSortedSet(tableNames);
//
//      for (String tableName : sortedTablesSet) {
//         CacheTable operationTable = getTable(tableName);
//         operationTable.reloadData();
//      }
//   }

//   public Map<String, CacheTable> getOperationTablesMap() {
//      return cacheTableMap;
//   }


//   void commitChangesInAllTables() throws java.sql.SQLException {
//      Collection<CacheTable> tablesSorted = getSortedCacheTables();
//      commitChanges(tablesSorted);
//   }

   /*
   void commitChanges(Collection<CacheTable> tablesSorted) throws SQLException {
      for (CacheTable cacheTable : tablesSorted) {
         cacheTable.writeLock();
         cacheTable.commit();
         cacheTable.writeUnlock();
      }
   }

   void rollbackChangesInAllTables() {
      Collection<CacheTable> tablesSorted = getSortedCacheTables();

      for (CacheTable cacheTable : tablesSorted) {
         cacheTable.writeLock();
         cacheTable.rollback();
         cacheTable.writeUnlock();
      }
   }
   */

   void commit() throws SQLException {
      commit(threadTransaction.getSortedCacheTables());
      threadTransaction.removeAllCacheTables();
   }

   void commit(Set<String> tableNames) throws SQLException {
      commit(getSortedCacheTables(tableNames));
      threadTransaction.removeCacheTables(tableNames);
   }

   protected void commit(Collection<CacheTable> cacheTables) throws SQLException {
      Exception errException = null;

      // synchronize cache tables to external files
      for (CacheTable cacheTable : cacheTables) {
         cacheTable.writeLock();

         try {
            cacheTable.commit();
         } catch (Exception e) {
            errException = e;
            errException.printStackTrace();

            log.error("Error while synchronizing table changes to external file:" +
                    cacheTable.getFileTableName() + " Error was: " + e.getMessage(), e);

            cacheTable.rollback();
         } finally {
            cacheTable.writeUnlock();
         }
      }

      if (errException == null) {
         // all is OK. Commit H2 database
         conn.getH2Connection().commit();
      } else {
         // there was an error. Rollback H2 database
         conn.getH2Connection().rollback();
         throw new SQLException(
                 "Error while synchronizing table changes to external files. Error was: " +
                         errException.getMessage(), errException);
      }
   }

   void rollback() throws SQLException {
      Collection<CacheTable> tablesSorted = threadTransaction.getSortedCacheTables();
      Exception errException = null;

      // rollback changes in cache tables without any I/O operations with external files
      for (CacheTable cacheTable : tablesSorted) {

         try {
            cacheTable.writeLock();

            try {
               cacheTable.rollback();
            } catch (Exception e) {
               errException = e;
               errException.printStackTrace();

               log.error("Error while rolling table changes back for external file:" +
                       cacheTable.getFileTableName() + " Error was: " + e.getMessage(), e);
            } finally {
               cacheTable.writeUnlock();
            }
         } catch (Exception e) {
            e.printStackTrace();
            log.warn("error while unlocking table '" + cacheTable.getFileTableName() +
                    "'. Error was " + e.getMessage(), e);
         }
      }

      if (errException == null) {
         // all is OK. Rollback H2 database
         conn.getH2Connection().rollback();
      } else {
         // there was an error
         throw new SQLException(
                 "Error while rolling table changes back for external files. Error was: " +
                         errException.getMessage(), errException);
      }

      threadTransaction.removeAllCacheTables();
   }

   void clear() {
      if (cacheTableMap != null)
         cacheTableMap.clear();

      cacheTableMap = null;

      try {
         if (pstInsertTablesInfo != null) {
            pstInsertTablesInfo.close();
         }

         if (pstRemoveTablesInfo != null) {
            pstRemoveTablesInfo.close();
         }
      } catch (SQLException ex) {
         ex.printStackTrace();
      }

      if (threadTransaction != null) {
         threadTransaction.clear();
         threadTransaction = null;
      }

      pstInsertTablesInfo = null;
      pstRemoveTablesInfo = null;
      conn = null;
   }

}
