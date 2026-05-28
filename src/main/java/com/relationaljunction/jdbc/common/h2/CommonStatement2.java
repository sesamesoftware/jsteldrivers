package com.relationaljunction.jdbc.common.h2;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;

import org.h2.jdbc.JdbcResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.database.StoreException;
import com.relationaljunction.database.view.ViewSchemaIF;
import com.relationaljunction.database.view.ViewTableIF;
import com.relationaljunction.jdbc.common.h2.sql.CreateTableSQLCommand;
import com.relationaljunction.jdbc.common.h2.sql.SQLCommand;
import com.relationaljunction.utils.OtherUtils;
import com.relationaljunction.utils.UnexpectedException;

public class CommonStatement2 extends AbstractStatement {
   private final Logger log = LoggerFactory.getLogger("CommonStatement2");

   protected CommonStatement2(CommonConnection2 conn) {
      super(conn);
   }

   public CommonStatement2(CommonConnection2 conn, Statement h2Stat) {
      super(conn, h2Stat);
      init();
   }

   protected void checkClosed() throws SQLException {
      if (closed)
         throw new SQLException("The statement is already closed");
      if (conn == null || conn.isClosed())
         throw new SQLException("The connection is already closed");
   }


   /**
    * load tables and views being used in a SQL query
    *
    * @param tablesUsed - tables that should be processed for loading into H2 (they may be table and views)
    * @return - real tables excluding views that should be loaded into H2.
    *         <code>tablesUsed</code> contains only tables,
    *         resulting <code>realTablesUsed</code> will be the same.
    * @throws SQLException
    */
   protected Set<String> loadTablesFromStore(Set<String> tablesUsed) throws
           SQLException {
      Set<String> realTablesUsed = new HashSet<String>();

      for (final String tableName : tablesUsed) {
         if (conn.isSystemTable(tableName.toUpperCase())) {
            continue;
         }

         // check that a tableName may be a view
         ViewTableIF view;
         try {
            view = conn.getViews().get(tableName);
         } catch (Exception e) {
            throw new UnexpectedException("Error while getting a view " + tableName +
                    " Error was: " + e.getMessage());
         }

         if (view != null) {
            // tableName is a view that is already loaded to the cache
            realTablesUsed.addAll(view.getRealTablesUsedInView());
            // just in case, load tables again. They may be manually dropped from the cache
            loadTablesFromStore(view.getRealTablesUsedInView());
            continue;
         } else {
            // tableName is not found in the view cache
            // try to find external information about that view in ViewSchema
            ViewSchemaIF viewSchema = conn.getSchemaIF2().getViewSchema();

            if (viewSchema != null) {
               final ViewTableIF viewInfo = viewSchema.getViewByName(com.relationaljunction.utils.StringUtils.
                       unquote(tableName, CacheTableManager.QUOTE_CHARS));

               if (viewInfo != null) {
                  // external information about the view is found in ViewSchema
                  // load the view to the H2 and add its info to connection data
                  SQLCommand viewCommand;

                  try {
                     viewCommand = SQLCommand.parseSQLCommand(viewInfo.getQuery());
                  } catch (Exception ex) {
                     throw new SQLException("Can't parse the query used for a view '" +
                             viewInfo.getName() + "': " +
                             viewInfo.getQuery() + ".\nParser error was: " +
                             ex.getMessage() + ". Check the following points in your syntax:" +
                             "1) � column using an SQL reserved word as a name or containing spaces and other delimiters (-,.,;,:, etc) must be quoted in double quotes in a query, e.g.: SELECT \"DATE\", \"My integer-column\" FROM \"test.txt\"" + "\n" +
                             "2) To use single quotes (') within a string constant you should duplicate them, e.g.: SELECT 'a''bcd''efgh'");
                  }

                  OtherUtils.writeLogInfo(log,
                          "loading tables for the view '", viewCommand.getBaseTable(), "'. SQL: '",
                          viewCommand.getSqlText(), "'");

                  // load tables used in this view
                  final Set<String> tablesUsedInView = loadTablesFromStore(viewCommand.getTablesUsed());

                  try {
                     ViewTableIF viewAdded = conn.getViews().putIfAbsent(tableName,
                             new Callable<ViewTableIF>() {
                                public ViewTableIF call() throws Exception {
                                   Statement stTemp = conn.getH2Connection().createStatement();
                                   String createViewSQL = "CREATE VIEW " + tableName + " AS " +
                                           viewInfo.getQuery();
                                   stTemp.executeUpdate(createViewSQL);
                                   stTemp.close();
                                   OtherUtils.writeLogInfo(log, "creating the view '" +
                                           viewInfo.getName() + "' using the SQL query '" + createViewSQL + "'");
                                   return new com.relationaljunction.database.view.DefaultViewTable(
                                           tableName, createViewSQL, tablesUsedInView);
                                }
                             });

                     realTablesUsed.addAll(viewAdded.getRealTablesUsedInView());
                  } catch (Exception e) {
                     throw new SQLException("Error while creating a view " +
                             tableName + " Error was: " + e.getMessage());
                  }

                  /*
                  synchronized (viewInfo) {
                     // synchro check: if a view is already loaded by concurrent threads
                     if (conn.getViews().get(tableName) == null) {

                        Statement stTemp = conn.getH2Connection().createStatement();
                        String createViewSQL = "CREATE VIEW " + tableName + " AS " +
                                viewInfo.getQuery();
                        stTemp.executeUpdate(createViewSQL);
                        stTemp.close();
                        com.relationaljunction.utils.OtherUtils.writeLogInfo(log, "creating the view '" +
                                viewInfo.getName() + "' using the SQL query '" + createViewSQL + "'", true);

                        // now view is loaded
                        conn.getViews().add(new com.relationaljunction.database.view.DefaultViewTable(
                                tableName, createViewSQL, tablesUsedInView));
                        realTablesUsed.addAll(tablesUsedInView);
                     }
                  }
                  */


                  // view is loaded now
                  // iterate tables further
                  continue;
               }
            }
         }

         // a view is not found at all
         // otherwise load a table to H2, if it does not exist
         conn.getCacheTableManager().loadTable(tableName);
         // add tableName to set of tables being used
         realTablesUsed.add(tableName);
      } // for

      return realTablesUsed;
   }

   public boolean execute(String sql) throws SQLException {
      return execute(sql, -1, null, null);
   }

   public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
      return execute(sql, autoGeneratedKeys, null, null);
   }

   public boolean execute(String sql, int[] columnIndexes) throws SQLException {
      return execute(sql, -1, columnIndexes, null);
   }

   public boolean execute(String sql, String[] columnNames) throws SQLException {
      return execute(sql, -1, null, columnNames);
   }

   public boolean execute(String sql, int autoGeneratedKeys, int[] columnIndexes,
                          String[] columnNames) throws SQLException {
      checkClosed();

      OtherUtils.writeLogInfo(log,
              "Connection(" + conn.toString() +
                      ") -> Statement -> execute(): sql= " +
                      sql);

      if (sql.trim().toUpperCase().startsWith("SELECT") ||
              sql.trim().toUpperCase().startsWith("EXPLAIN")) {
         this.executeQuery(sql);
         return true;
      } else {
         this.executeUpdate(sql, autoGeneratedKeys, columnIndexes, columnNames);
         return false;
      }
   }

   public ResultSet executeQuery(String sql) throws SQLException {
      checkClosed();

      OtherUtils.writeLogInfo(log,
              "Connection(" + conn.toString() +
                      ") -> Statement(" +
                      this +
                      ") -> executeQuery(): sql= " +
                      sql);

      //#######for registration###########
      r();
      //#################################

      // parse a sql to get tables that should be loaded to H2
      SQLCommand command;
      try {
         command = SQLCommand.parseSQLCommand(sql);
      } catch (Exception ex) {
         throw new SQLException("Can't parse SQL query '" + sql + "'. Error was " + ex.getMessage() +
                 ". Check the following points in your SQL syntax:\n" +
                 "1) � column using an SQL reserved word as a name or containing spaces and other delimiters (-,.,;,:, etc) must be quoted in double quotes in a query, e.g.: SELECT \"DATE\", \"My integer-column\" FROM \"test.txt\"" + "\n" +
                 "2) To use single quotes (') within a string constant you should duplicate them, e.g.: SELECT 'a''bcd''efgh'", "42001");
      }

//      com.relationaljunction.utils.OtherUtils.writeLogInfo(log,
//              "execute the query: '" + sqlText + "'", true);

      if (command.getType() != SQLCommand.SELECT &&
              command.getType() != SQLCommand.EXPLAIN)
         throw conn.driver.createException(
                 "The executeQuery() method is only allowed for a query. " +
                         "Use execute() or executeUpdate() instead of executeQuery().");

      Set<String> realTablesUsed = loadTablesFromStore(command.getTablesUsed());

      if (realTablesUsed.isEmpty())
         log.warn("there are no tables to be loaded for the query '" + sql + "'");

      OtherUtils.writeTraceInfo(log, "tables used in the query: " + realTablesUsed);

      //#######for registration###########
      updateRecordsLoadedInfo();
      //#################################

//      if (log.isTraceEnabled())
//         log.trace("waiting for read locks in CommonStatement2.executeQuery()");

      conn.cacheTableManager.readLockTables(realTablesUsed);

      OtherUtils.writeLogInfo(log, "execute the query directly in H2: '"
              , command.getSqlText(), "' (autoCommit = " + conn.getAutoCommit(), ")");

      ResultSet rs = null;

      try {
         rs = h2Stat.executeQuery(command.getSqlText());

         // set a wrapper connection to allow updating a result set.
         if (h2Stat.getResultSetConcurrency() == ResultSet.CONCUR_UPDATABLE) {
            ((JdbcResultSet) rs).setWrapperConnection(conn);
         }
      } catch (SQLException ex) {
//         ex.printStackTrace();
         throw conn.driver.createException(
                 "Error while executing an SQL query. [H2 Database] " + ex.getMessage(),
                 ex.getSQLState(), ex);
      } finally {
         // unlock tables for reading
         conn.cacheTableManager.readUnlockTables(realTablesUsed);
      }

      OtherUtils.writeLogInfo(log, "end of executing the query: '" +
              command.getSqlText() + "'");
      return rs;
   }

   public int executeUpdate(String sql) throws SQLException {
      return executeUpdate(sql, -1, null, null);
   }

   public int executeUpdate(String sql, int autoGeneratedKeys) throws
           SQLException {
      return executeUpdate(sql, autoGeneratedKeys, null, null);
   }

   public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
      return executeUpdate(sql, -1, columnIndexes, null);
   }

   public int executeUpdate(String sql, String[] columnNames) throws
           SQLException {
      return executeUpdate(sql, -1, null, columnNames);
   }

   public int executeUpdate(String sql,
                            int autoGeneratedKeys,
                            int[] columnIndexes,
                            String[] columnNames) throws SQLException {
      checkClosed();

      OtherUtils.writeLogInfo(log,
              "(" + conn.toString() +
                      ") -> Statement(" + this +
                      ") -> executeUpdate(): sql= '" +
                      sql + "'");

      //#######for registration###########
      r();
      //#################################

      // parse a sql to get tables that should be loaded to H2
      SQLCommand command;
      try {
         command = SQLCommand.parseSQLCommand(sql);
      } catch (Exception ex) {
         throw new SQLException("Can't parse SQL query '" + sql + "'. Error was " + ex.getMessage() +
                 ". Check the following points in your SQL syntax:\n" +
                 "1) � column using an SQL reserved word as a name or containing spaces and other delimiters (-,.,;,:, etc) must be quoted in double quotes in a query, e.g.: SELECT \"DATE\", \"My integer-column\" FROM \"test.txt\"" + "\n" +
                 "2) To use single quotes (') within a string constant you should duplicate them, e.g.: SELECT 'a''bcd''efgh'", "42001", ex);
      }

      OtherUtils.writeLogInfo(log,
              "execute the update: '" + command.getSqlText() + "'");

      //#######for registration###########
      updateRecordsLoadedInfo();
      //#################################


      switch (command.getType()) {
         // #### CREATE TABLE ####
         case SQLCommand.CREATE_TABLE: {
            processCreateTable(command);
            return 0;
         }

         // #### RELOAD TABLE ####
         case SQLCommand.RELOAD_TABLE: {
            processReloadTable(command);
            return 0;
         }

         // #### DROP TABLE FROM CACHE ####
         case SQLCommand.DROP_TABLE_FROM_CACHE: {
            processDropTableFromCache(command);
            return 0;
         }

         // #### RELOAD CACHE ####
         case SQLCommand.RELOAD_CACHE: {
            try {
               conn.reloadCache();
            } catch (Exception e) {
               log.error(e.getMessage(), e);
               throw new SQLException(e.getMessage());
            }
            return 0;
         }

         // #### DROP TABLE ####
         case SQLCommand.DROP_TABLE: {
            processDropTable(command);
            return 0;
         }

         // #### CREATE VIEW ####
         case SQLCommand.CREATE_VIEW: {
            processCreateView(command);
            return 0;
         }

         // #### DROP VIEW ####
         case SQLCommand.DROP_VIEW: {
            processDropView(command);
            return 0;
         }

         // #### SHUTDOWN ####
         case SQLCommand.SHUTDOWN: {
            processShutdown();
            return 0;
         }

         // #### SHUTDOWN ####
         case SQLCommand.LOCK_DATABASE: {
            try {
               conn.getSchemaIF2().lockDatabase();
            } catch (Exception e) {
               throw new SQLException("can't lock a database. Error was: " + e.getMessage(), e);
            }
            return 0;
         }

         // #### SHUTDOWN ####
         case SQLCommand.UNLOCK_DATABASE: {
            try {
               conn.getSchemaIF2().unlockDatabase();
            } catch (Exception e) {
               throw new SQLException("can't unlock a database. Error was: " + e.getMessage(), e);
            }
            return 0;
         }

         // #### SAVE #### (used in StelsXML
         case SQLCommand.SAVE: {
            processSave(command);
            return 0;
         }

         // #### CREATE INDEX ####
         case SQLCommand.CREATE_INDEX: {
            loadTablesFromStore(command.getTablesUsed());
            h2Stat.executeUpdate(command.getSqlText());
            return 0;
         }

         // #### SELECT (not allowed) ####
         case SQLCommand.SELECT:
         case SQLCommand.EXPLAIN: {
            throw conn.driver.createException(
                    "This method is not allowed for a SELECT query. " +
                            "Use executeQuery() instead of executeUpdate() for SELECT queries.");
         }

         case SQLCommand.INSERT:
         case SQLCommand.UPDATE:
         case SQLCommand.DELETE: {
            // #### INSERT, UPDATE, DELETE and other DML operations not mentioned above ####
            processOtherDML(command, autoGeneratedKeys, columnIndexes, columnNames);
            break;
         }

         default: {
            // other commands
            loadTablesFromStore(command.getTablesUsed());
            h2Stat.executeUpdate(command.getSqlText());
         }
      }

      OtherUtils.writeLogInfo(log,
              "end of executing the update. Row processed " +
                      h2Stat.getUpdateCount());

      return h2Stat.getUpdateCount();
   }

   protected void processSave(SQLCommand command) throws SQLException {
      throw new UnsupportedOperationException("Not supported");
   }

   protected void processCreateTable(SQLCommand command) throws SQLException {
      // create a table (file) on a disk
//    synchronized (conn.getH2Connection()) {
      try {
         // create a corresponding external file (table)
         conn.getSchemaIF2().createStoreTable(conn.getCacheTableManager().
                 getFileTableName(command.getBaseTable()),
                 ((CreateTableSQLCommand) command).getStoreFields(),
                 ((CreateTableSQLCommand) command).getStoreIndexTables(),
                 new Properties());

         // load that file to H2
         loadTablesFromStore(command.getTablesUsed());

         OtherUtils.writeLogInfo(log, "creating the file '" +
                 conn.getCacheTableManager().
                         getFileTableName(
                                 command.getBaseTable()) + "'");
      } catch (StoreException ex) {
//	ex.printStackTrace();
         log.error("Can't create the file '" +
                 conn.getCacheTableManager().
                         getFileTableName(command.getBaseTable()) +
                 "'. Error was: " + ex.getMessage(), ex);
         throw conn.driver.createException("Can't create the file '" +
                 conn.getCacheTableManager().
                         getFileTableName(command.getBaseTable()) +
                 "'. Error was: " + ex.getMessage());
      }
//    } // synchronized
   }

   private void processDropTable(SQLCommand command) throws SQLException {
      // drop a table (file) on a disk
//    synchronized (conn.getH2Connection()) {
      try {
         conn.getSchemaIF2().deleteStoreTable(conn.getCacheTableManager().
                 getFileTableName(command.getBaseTable()));
         OtherUtils.writeLogInfo(log, "deleting the file/table '" +
                 conn.getCacheTableManager().
                         getFileTableName(
                                 command.getBaseTable()) + "'");
      } catch (Exception ex) {
         log.error("Can't drop the table '" +
                 conn.getCacheTableManager().
                         getFileTableName(command.getBaseTable()) +
                 "'. Error was: " + ex.getMessage(), ex);
         throw conn.driver.createException("Can't drop the table '" +
                 conn.getCacheTableManager().
                         getFileTableName(
                                 command.getBaseTable()) +
                 "'. Error was: " + ex.getMessage());
      }

      // drop a table in the cache
      if (conn.getCacheTableManager().getTable(command.getBaseTable()) != null)
         conn.getCacheTableManager().removeTable(command.getBaseTable());
//    } // synchronized
   }

   private void processReloadTable(SQLCommand command) throws
           SQLException {
      CacheTable table = conn.getCacheTableManager().getTable(command.getBaseTable());
      if (table == null)
         throw conn.driver.createException("The table '" +
                 command.getBaseTable() + "' does not exist in the cache (H2 database)");

      // lock a table to be reloaded
      table.writeLock();
      try {
         // drop a table in the cache only
         if (!table.isAlreadyLoaded())
            return;

         if (table.isInTransaction())
            throw conn.driver.createException("The table '" + command.getBaseTable() +
                    "' is in transaction now");

         conn.getSchemaIF2().deleteStoreTableFromCache(conn.getCacheTableManager().
                 getFileTableName(command.getBaseTable()));
         table.reloadData();

         OtherUtils.writeLogInfo(log, "The table '" +
                 conn.getCacheTableManager().
                         getFileTableName(
                                 command.getBaseTable()) +
                 "' was dropped from the cache");
      } catch (Exception ex) {
         log.error(ex.getMessage(), ex);
         throw conn.driver.createException(ex);
      } finally {
         // unlock a table
         table.writeUnlock();
      }
   }

   private void processDropTableFromCache(SQLCommand command) throws
           SQLException {
      CacheTable table = conn.getCacheTableManager().getTable(command.
              getBaseTable());
      if (table == null)
         throw conn.driver.createException("The table '" +
                 command.getBaseTable() +
                 "' does not exist in the cache (H2 database)");

      // lock a table to be dropped from the cache
      table.writeLock();
      try {
         if (!table.isAlreadyLoaded())
            return;

         if (table.isInTransaction())
            throw conn.driver.createException("The table '" +
                    command.getBaseTable() +
                    "' is in transaction now");

         // drop a table in the cache only
         conn.getCacheTableManager().removeTable(command.getBaseTable());

         conn.getSchemaIF2().deleteStoreTableFromCache(conn.getCacheTableManager().
                 getFileTableName(command.
                         getBaseTable()));
//      table.reloadData();

         OtherUtils.writeLogInfo(log, "The table '" +
                 conn.getCacheTableManager().
                         getFileTableName(
                                 command.getBaseTable()) +
                 "' was dropped from the cache");
      } catch (Exception ex) {
         log.error(ex.getMessage(), ex);
         throw new SQLException(ex.getMessage());
      } finally {
         // unlock a table for writing
         table.writeUnlock();
      }
   }

   private void processCreateView(SQLCommand command) throws SQLException {
      OtherUtils.writeLogInfo(log, "creating a view '" +
              command.getBaseTable() +
              "' using an SQL query '" +
              command.getSqlText() + "'");

      Set<String> realTablesUsed = loadTablesFromStore(command.getTablesUsed());

//    synchronized (conn.getH2Connection()) {
      h2Stat.executeUpdate(command.getSqlText());

      try {
         conn.getViews().put(new com.relationaljunction.database.view.DefaultViewTable(command.
                 getBaseTable(), command.getSqlText(), realTablesUsed));
      } catch (Exception e) {
         throw new UnexpectedException("Error while getting a view '" + command.
                 getBaseTable() + "'. Error was: " + e.getMessage(), e);
      }
//    } // synchronized
   }

   private void processDropView(SQLCommand command) throws SQLException {
//    synchronized (conn.getH2Connection()) {
      h2Stat.executeUpdate(command.getSqlText());
      conn.getViews().remove(command.getBaseTable());
//    } // synchronized
   }

   private void processShutdown() throws SQLException {
      OtherUtils.writeLogInfo(log, "executing SHUTDOWN command");
      conn.closePhysically();
      close();
   }

   protected void processOtherDML(SQLCommand command,
                                  int autoGeneratedKeys,
                                  int[] columnIndexes,
                                  String[] columnNames) throws SQLException {
      int updateCount = 0;

//    synchronized (conn.getH2Connection()) {

      OtherUtils.writeTraceInfo(log, "waiting for write locks in CommonStatement.processOtherDML()");

      // load tables used for other operations
      loadTablesFromStore(command.getTablesUsed());

      conn.getCacheTableManager().writeLockTables(command.getTablesUsed());

      try {
         conn.getCacheTableManager().beginOperation(command);

         OtherUtils.writeLogInfo(log,
                 "execute an update directly in H2: '" +
                         command.getSqlText() + "'");
         try {
            if (autoGeneratedKeys != -1)
               updateCount = h2Stat.executeUpdate(command.getSqlText(), autoGeneratedKeys);
            else if (columnIndexes != null)
               updateCount = h2Stat.executeUpdate(command.getSqlText(), columnIndexes);
            else if (columnNames != null)
               updateCount = h2Stat.executeUpdate(command.getSqlText(), columnNames);
            else
               updateCount = h2Stat.executeUpdate(command.getSqlText());
         } catch (SQLException ex) {
            throw conn.driver.createException("[H2 Database] " + ex.getMessage(),
                    ex.getSQLState(), ex);
         }

         conn.getCacheTableManager().endOperation(command, updateCount);

         if (conn.autoCommit)
            conn.commit(command.getTablesUsed());

      } catch (Exception ex) {
         // error ocurred in a transaction

         if (conn.getAutoCommit()) {
            // just clear transaction flag for the table involved
            // rollback should be executed on the customer's side
            // if (autoCommit = false) this action is not required,
            // because a transaction goes in progress
            conn.getCacheTableManager().clearTransactionFlag(command);
         }

         log.error("Error while executing an SQL query. " +
                 ex.getMessage(), ex);
         throw new SQLException("Error while executing an SQL query. " +
                 ex.getMessage(), ex);
      } finally {
         conn.getCacheTableManager().writeUnlockTables(command.getTablesUsed());
      }

//    } // synchronized
   }

   public void close() throws SQLException {
      if (conn != null)
         OtherUtils.writeLogInfo(log,
                 "Connection(" + conn +
                         ") -> Statement(" + this + ") -> close()");

      conn = null;

      if (batch != null)
         batch.clear();
      batch = null;

      if (h2Stat != null)
         h2Stat.close();
      h2Stat = null;

      closed = true;
   }

   public int getMaxFieldSize() throws SQLException {
      return 0;
   }

   public void setMaxFieldSize(int max) throws SQLException {
      h2Stat.setMaxFieldSize(max);
   }

   public int getMaxRows() throws SQLException {
      return h2Stat.getMaxRows();
   }

   public void setEscapeProcessing(boolean enable) throws SQLException {
      h2Stat.setEscapeProcessing(enable);
   }

   public int getQueryTimeout() throws SQLException {
      return h2Stat.getQueryTimeout();
   }

   public void setQueryTimeout(int seconds) throws SQLException {
      h2Stat.setQueryTimeout(seconds);
   }

   public void cancel() throws SQLException {
      h2Stat.cancel();
   }

   public SQLWarning getWarnings() throws SQLException {
      return h2Stat.getWarnings(); // ignored by H2
   }

   public void clearWarnings() throws SQLException {
      h2Stat.clearWarnings(); // ignored by H2
   }

   public void setCursorName(String name) throws SQLException {
      h2Stat.setCursorName(name); // ignored by H2
   }

   public ResultSet getResultSet() throws SQLException {
      return h2Stat.getResultSet();
   }

   public int getUpdateCount() throws SQLException {
      return h2Stat.getUpdateCount();
   }

   public boolean getMoreResults() throws SQLException {
      return h2Stat.getMoreResults(); // ignored by H2
   }

   public void setFetchDirection(int direction) throws SQLException {
      h2Stat.setFetchDirection(direction);
   }

   public int getFetchDirection() throws SQLException {
      return h2Stat.getFetchDirection();
   }

   public void setFetchSize(int rows) throws SQLException {
      h2Stat.setFetchSize(rows);
   }

   public int getFetchSize() throws SQLException {
      return h2Stat.getFetchSize();
   }

   public int getResultSetConcurrency() throws SQLException {
      return h2Stat.getResultSetConcurrency();
   }

   public int getResultSetType() throws SQLException {
      return ResultSet.TYPE_SCROLL_INSENSITIVE;
   }

   public void addBatch(String sql) throws SQLException {
      checkClosed();
      batch.add(sql);
   }

   public void clearBatch() throws SQLException {
      checkClosed();
      batch.clear();
   }

   public int[] executeBatch() throws SQLException {
      checkClosed();

      OtherUtils.writeLogInfo(log,
              "Connection(" + conn.toString() +
                      ") -> Statement -> executeBatch()");

      int[] result = new int[batch.size()];
      boolean error = false;
      StringBuilder errQueries = new StringBuilder();
      String lastError = "";

      // current auto commit mode
      boolean currentAutoCommit = conn.getAutoCommit();

      // If auto commit mode = 'true', then change it temporarily to 'false'.
      // It is required to speed up processing of many DML operation
      // due to the driver will commit and rewrite a table once.
      if (currentAutoCommit)
         conn.setAutoCommit(false);

      for (int i = 0; i < batch.size(); i++) {
         String sql = batch.get(i);
         try {
            result[i] = executeUpdate(sql);
         } catch (SQLException ex) {
            lastError = ex.getMessage();

            if (errQueries.length() == 0)
               errQueries.append(i + 1);
            else
               errQueries.append(",").append(i + 1);

            result[i] = Statement.EXECUTE_FAILED;
            error = true;
         }
      }

      if (currentAutoCommit) {
         conn.commit();
//         if (!conn.persistentMode && conn.transactMode == 0)
         // revert back to autoCommit mode
         conn.setAutoCommit(true);
      }

      clearBatch();
      if (error) {
         throw new BatchUpdateException("[" + conn.driver.getDriverName() +
                 "] Execution of query(ies) " + errQueries +
                 " is failed. Last error is " + lastError,
                 result);
      }

      return result;
   }

   public Connection getConnection() throws SQLException {
      return conn;
   }

   public boolean getMoreResults(int current) throws SQLException {
      return h2Stat.getMoreResults(current); //ignore by H2
   }

   public ResultSet getGeneratedKeys() throws SQLException {
      return h2Stat.getGeneratedKeys();
   }

   public int getResultSetHoldability() throws SQLException {
      return h2Stat.getResultSetHoldability();
   }

   // --- JDK 1.6 ---

   public boolean isClosed() throws SQLException {
      return closed;
   }

   public void setPoolable(boolean poolable) throws SQLException {
      throw new UnsupportedOperationException();
   }

   public boolean isPoolable() throws SQLException {
      return false;
   }

   public <T> T unwrap(Class<T> iface) throws SQLException {
      throw new UnsupportedOperationException();
   }

   public boolean isWrapperFor(Class<?> iface) throws SQLException {
      throw new UnsupportedOperationException();
   }
   
   /**
    * Unsupported
    */
	@Override
	public void closeOnCompletion() throws SQLException {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Unsupported
	 */
	@Override
	public boolean isCloseOnCompletion() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}
}
