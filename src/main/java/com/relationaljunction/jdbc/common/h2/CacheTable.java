package com.relationaljunction.jdbc.common.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collections;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.h2.engine.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.database.AbstractStoreSchema;
import com.relationaljunction.database.StoreDataType;
import com.relationaljunction.database.StoreException;
import com.relationaljunction.database.StoreFieldIF;
import com.relationaljunction.database.StoreRecordIF;
import com.relationaljunction.database.StoreTableIF;
import com.relationaljunction.database.StoreTableReaderIF;
import com.relationaljunction.database.StoreTableWriterIF;
import com.relationaljunction.database.h2.StoreResultSet;
import com.relationaljunction.database.index.IndexSchemaIF;
import com.relationaljunction.database.index.IndexTableIF;
import com.relationaljunction.jdbc.common.h2.sql.DefaultH2SQLTranslator;
import com.relationaljunction.jdbc.common.h2.sql.H2TriggerIF;
import com.relationaljunction.jdbc.common.h2.sql.SQLCommand;
import com.relationaljunction.utils.OtherUtils;
import com.relationaljunction.utils.StringUtils;
import com.relationaljunction.utils.UnexpectedException;
import com.relationaljunction.utils.concurrency.ConsumerIF;
import com.relationaljunction.utils.concurrency.ProducerIF;
import com.relationaljunction.utils.concurrency.SingleConsumerProducer;

// class stores operations occured and synchronizes tables between H2 and stores
public class CacheTable {
   private final Logger log = LoggerFactory.getLogger("CacheTable");

   public static final String CONSTRAINT_PROPERTY = "constraint";

   protected CacheTableManager cacheTableManager = null;
   protected String sqlTableName;
   protected String cacheTableName;
   protected String triggerTableName;
   protected String fileTableName;
   protected StoreFieldIF[] fields = null;

   private boolean hasInserts = false;
   private boolean hasUpdates = false;
   private boolean hasDeletes = false;

   private int operationNumber = 0;

   private boolean hasIdentityColumn = false;

   private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
   private String insertOperationTableSQL;
   private PreparedStatement pstInsertOperationTable = null;
   private StringBuilder createOperationTableSQL;
   private String createOperationIndexSQL;
   private H2TriggerIF insertTrigger = null;
   private H2TriggerIF updateTrigger = null;
   private H2TriggerIF deleteTrigger = null;
   private int previousTransactCommandType = -1;
   private java.util.Date modificationDate = null;
   private boolean alreadyLoaded = false;
   private boolean inTransaction = false;

//  private volatile boolean dataLoaded = false;
//  private volatile boolean triggersLoaded = false;

   class RecordsConsumer implements ConsumerIF<StoreRecordIF> {
      private final PreparedStatement pstInsert;
      private int count = 0;

      RecordsConsumer(PreparedStatement pstInsert) {
         this.pstInsert = pstInsert;
      }

      public void consume(StoreRecordIF rec) throws Exception {
         count++;

         try {
            for (int i = 0; i < fields.length; i++) {
               pstInsert.setObject(i + 1, rec.getObject(i));
            }
            pstInsert.execute();

//            if (log.isTraceEnabled())
//               log.trace("Record " + count + " written");
         } catch (SQLException ex) {
//	ex.printStackTrace();
            throw new Exception("error while writing a record " +
                    count + ": " + ex.getMessage());
         }
      }
   }


   class RecordsProducer implements ProducerIF<StoreRecordIF> {
      private final StoreTableReaderIF reader;
      private int count = 0;

      RecordsProducer(StoreTableReaderIF reader) {
         this.reader = reader;
      }

      public StoreRecordIF produce() throws Exception {
         count++;
         StoreRecordIF rec;
         try {
            rec = reader.nextRecord();

//            if (log.isTraceEnabled())
//               log.trace("Record " + count + " read");

         } catch (StoreException ex) {
//	ex.printStackTrace();
            throw new Exception("error while reading a record " +
                    count + ": " + ex.getMessage());
         }

         return rec;
      }
   }


   public CacheTable(CacheTableManager cacheTableManager) {
      this.cacheTableManager = cacheTableManager;
   }

   public CacheTable(CacheTableManager cacheTableManager, String sqlTableName) {
      this.sqlTableName = sqlTableName;

      this.cacheTableName = CacheTableManager.getCacheTableName(sqlTableName);
      this.fileTableName = cacheTableManager.getFileTableName(sqlTableName);
      this.cacheTableManager = cacheTableManager;

      if (StringUtils.isDoubleQuoted(sqlTableName))
         this.triggerTableName = "\"" + sqlTableName + "\"";
      else
         this.triggerTableName = sqlTableName.toUpperCase();
   }

   /**
    * init an CacheTable instance from H2 for the persistent mode:
    * 1) read CacheTable members from H2
    * 2) clear the operations journal
    * <p/>
    * it is not necessary to be synchronized, because it is called in CacheTableManager constructor.
    *
    * @param rs ResultSet
    * @throws SQLException
    */
   void loadDataFromDB(ResultSet rs) throws SQLException {
      this.cacheTableName = rs.getString("table_name");
      this.sqlTableName = rs.getString("sql_table_name");
      this.fileTableName = cacheTableManager.getFileTableName(sqlTableName);

      if (StringUtils.isDoubleQuoted(sqlTableName))
         this.triggerTableName = "\"" + sqlTableName + "\"";
      else
         this.triggerTableName = sqlTableName.toUpperCase();

      this.insertOperationTableSQL = rs.getString("insert_oper_table_sql");

      if (this.insertOperationTableSQL != null)
         this.pstInsertOperationTable = cacheTableManager.conn.getH2Connection().
                 prepareStatement(this.
                         insertOperationTableSQL);

      this.createOperationTableSQL = rs.getString("create_oper_table_sql") != null ?
              new StringBuilder(rs.getString(
                      "create_oper_table_sql")) : null;
      this.createOperationIndexSQL = rs.getString("create_oper_index_sql");
      this.modificationDate = rs.getTimestamp("mod_date");

      Vector vFields = (Vector) rs.getObject("fields");
      fields = new StoreFieldIF[vFields.size()];
      for (int i = 0; i < vFields.size(); i++)
         fields[i] = (StoreFieldIF) vFields.get(i);

      if (supportsInsertOnRecord() ||
              supportsUpdateOnRecord() ||
              supportsDeleteOnRecord()) {
         Statement st = cacheTableManager.conn.getH2Connection().createStatement();

         // clear an operation table. Maybe it contains records since the last session.
         st.execute("DELETE FROM RJ_SCHEMA.\"RJ_OPERATIONS_" +
                 cacheTableName + "\"");
         st.close();
         // H2 in non auto commit mode
         cacheTableManager.conn.getH2Connection().commit();
      }

      OtherUtils.writeLogInfo(log,
              "##### loading the table '", cacheTableName, "' directly from H2. File = '" +
              cacheTableManager.getFileTableName(sqlTableName), "', modDate = ",
              OtherUtils.formatDate(modificationDate));
   }

   /*
    loads data from external file (store) to a cache table:
    1) creates an SQL table as a cache (synchronize) table.
    2) add related constraints and indexes if they exist
    3) load data from exernal file (store) to the created cache table.
    4) creates auxiliary tables to contain inserted, updated, deleted, etc records information.
   */
   public void loadDataFromStoreTable() throws SQLException {
      try {
    	 StringBuilder columnList = null;
    	 StringBuilder columnListWithDataType = null;
         StringBuilder valueList = null;
         Statement st = null;

         try {
            // locks schema if it is required (for RJMDB)
            if (cacheTableManager.conn.getSchemaIF2().requiresLockingForReadingOperations()) {
               cacheTableManager.conn.getSchemaIF2().lockForReading();
            }

            StoreTableIF storeTable = cacheTableManager.conn.getSchemaIF2().getStoreTable(
                    fileTableName);
            modificationDate = storeTable.refreshModificationDate();
            StoreTableReaderIF reader = storeTable.getReader();
            StoreFieldIF[] fields = reader.getFields();
            setFields(fields);

            OtherUtils.writeLogInfo(log, "##### loading the table '", cacheTableManager.getFileTableName(
                    sqlTableName), "', modDate = ", OtherUtils.formatDate(modificationDate));

            // ##### local table properties
            String tableConstraint = null;

            if (storeTable.getTableProperties() != null) {
               tableConstraint = storeTable.getTableProperties().getProperty(
                       "constraint");
            }
            // ##### end of local table properties

            StringBuilder createTableSql = new StringBuilder("CREATE TABLE ").append(
                    sqlTableName).append("(");
            columnList = new StringBuilder();
            columnListWithDataType = new StringBuilder();
            valueList = new StringBuilder();
            StringBuilder insertSql = new StringBuilder();

            if (cacheTableManager.conn.getSchemaIF2().includeRowIdColumn()) {
               // include a reserved row id column to get position of rows
               createTableSql.append(AbstractStoreSchema.ROW_ID_COLUMN_NAME).append(" IDENTITY, ");
            }

            for (int i = 0; i < fields.length; i++) {
               String fieldName = StringUtils.quoteFieldAndTableName(fields[i].getName(),
                       cacheTableManager.conn.preserveColumnNames());

               createTableSql.append(fieldName).append(" ").append(fields[i].getType().getH2Name());

               if (fields[i].getType() == StoreDataType.IDENTITY) {
                  columnListWithDataType.append(fieldName).append(" INT");
                  hasIdentityColumn = true;
               } else
                  columnListWithDataType.append(fieldName).append(" ").
                          append(fields[i].getType().getH2Name());

               // specify size and decimal count for columns
               if (fields[i].getLength() > 0 && fields[i].getType().getJdbcType() != Types.BOOLEAN) {
                  // append a column size
                  createTableSql.append("(").append(fields[i].getLength());
                  columnListWithDataType.append("(").append(fields[i].getLength());

                  // append a decimal count
                  if ((fields[i].getType().getJdbcType() == Types.NUMERIC ||
                          fields[i].getType().getJdbcType() == Types.DECIMAL) &&
                          fields[i].getDecimalCount() > 0) {
                     createTableSql.append(",").append(fields[i].getDecimalCount());
                     columnListWithDataType.append(",").append(fields[i].getDecimalCount());
                  }

                  createTableSql.append(")");
                  columnListWithDataType.append(")");
               }

               if (fields[i].getType().getJdbcType() == Types.BOOLEAN) {
                  // set default value 'FALSE' for boolean values
                  columnListWithDataType.append(" DEFAULT FALSE");
                  createTableSql.append(" DEFAULT FALSE");
               }

               // specify a check condition for a column
               if (fields[i].getCheckCondition() != null)
                  createTableSql.append(" ").append(fields[i].getCheckCondition());

               columnList.append(fieldName);
               valueList.append("?");
               if (i < (fields.length - 1)) {
                  createTableSql.append(", ");
                  columnListWithDataType.append(", ");
                  columnList.append(", ");
                  valueList.append(", ");
               }
            }

            /*
                    IndexSchemaIF indexSchema = cacheTableManager.conn.getSchema().
                                                getIndexSchema();
                    Vector<IndexTableIF> nonUniqueIndexes = new Vector<IndexTableIF>();
      
                    if (cacheTableManager.conn.loadIndexes &&
                        cacheTableManager.conn.getSchema().getIndexSchema() != null) {
      
               IndexTableIF[] allIndexTables = indexSchema.getStoreIndexes(sqlTableName);
      
               for (int i = 0; i < allIndexTables.length; i++) {
                 IndexTableIF indexTable = allIndexTables[i];
      
                        if (!indexTable.isAutonumber() &&
                            (indexTable.isPrimaryKey() || indexTable.isUnique()))
                          createTableSql.append(", " + indexTable.getSQLString());
                        else
                          nonUniqueIndexes.add(indexTable);
               }
                    }
               */

            // ##### add constraints
            // add a table constraint sql via local property
            if (tableConstraint != null)
               createTableSql.append(", ").append(tableConstraint);

            createTableSql.append(")");
            // ##### end of CREATE TABLE constructing

            insertSql.append("INSERT INTO ").append(sqlTableName).append("(").
                    append(columnList).append(") VALUES(").append(valueList).append(")");

            OtherUtils.writeLogInfo(log,
                    "creating cache table: " +
                            createTableSql);
            OtherUtils.writeLogInfo(log,
                    "initializing insert operation into cache table: " +
                            insertSql);

            // ####### create a cache table in H2 #######
            st = cacheTableManager.conn.getH2Connection().createStatement();
            st.executeUpdate(createTableSql.toString());

            // ####### load related indexes #######
            loadStoreIndexes(st);

            // ####### set special properties to speed up imports to H2 database
            // disable a lock mode to speed up insertions
            st.execute("SET LOCK_MODE 0");

            // ####### insert store data to the cache table #######
            PreparedStatement pst = cacheTableManager.conn.getH2Connection().
                    prepareStatement(insertSql.toString());
            InsertDataToCacheTable(reader, pst);
            pst.close();
            reader.close();
         } finally {
            if (cacheTableManager.conn.getSchemaIF2().requiresLockingForReadingOperations()) {
               cacheTableManager.conn.getSchemaIF2().unlock();
            }
         }

         // ####### create an operation table #######
         createOperationalTable(columnList, columnListWithDataType, valueList, st);

         // ####### reset log and transaction properties
         // restore default transaction isolation level
         int h2transactionMode = 3;

         switch (cacheTableManager.conn.getTransactionIsolation()) {
            case Connection.TRANSACTION_READ_UNCOMMITTED:
               h2transactionMode = Constants.LOCK_MODE_OFF;
               break;
            case Connection.TRANSACTION_READ_COMMITTED:
               h2transactionMode = Constants.LOCK_MODE_READ_COMMITTED;
               break;
            case Connection.TRANSACTION_REPEATABLE_READ:
            case Connection.TRANSACTION_SERIALIZABLE:
               h2transactionMode = Constants.LOCK_MODE_TABLE;
               break;
         }

         st.execute("SET LOCK_MODE " + h2transactionMode);

         // save data into H2 in the persistent mode
         if (cacheTableManager.conn.isPersistentMode())
            saveTableInfo();

         st.close();
      } catch (Exception ex) {
//         ex.printStackTrace();
         log.error("Can't load the file '" +
                 fileTableName + "' to H2 database. Error was: " + ex.getMessage(),
                 ex);
         throw new SQLException("Can't load the file '" +
                 fileTableName + "' to H2 database. Error was: " + ex.getMessage(), ex);
      }
   }

   protected void createOperationalTable(	StringBuilder columnList,
										    StringBuilder columnListWithDataType,
								            StringBuilder valueList, Statement st) throws
           SQLException {
      if (supportsInsertOnRecord() ||
              supportsUpdateOnRecord() ||
              supportsDeleteOnRecord()) {

         createOperationTableSQL = new StringBuilder(
                 "CREATE TABLE RJ_SCHEMA.\"RJ_OPERATIONS_").append(
                 cacheTableName).append("\"(");

         // RJ_OPER_ID - operation ID
         // RJ_OPER_TYPE - operation type (row to insert, row before update, row after update, row to delete)
         // RJ_RECORD_ID - record ID (primary key)
         createOperationTableSQL.append(
                 "RJ_OPER_ID INT, RJ_OPER_TYPE TINYINT, RJ_RECORD_ID IDENTITY, ");
         createOperationTableSQL.append(columnListWithDataType).append(")");

         this.insertOperationTableSQL = "INSERT INTO RJ_SCHEMA.\"RJ_OPERATIONS_" +
                 cacheTableName + "\"" +
                 "(RJ_OPER_ID, RJ_OPER_TYPE, " + columnList + ")" +
                 " VALUES(" + "?, ?, " + valueList +
                 ")";

         OtherUtils.writeLogInfo(log, "creating operation table: " +
                 createOperationTableSQL);

         st.execute(createOperationTableSQL.toString());

         if (supportsUpdateOnRecord() ||
                 supportsDeleteOnRecord()) {
            createOperationIndexSQL = "CREATE INDEX RJ_SCHEMA.\"RJ_INDEX_" +
                    cacheTableName +
                    "\" ON RJ_SCHEMA.\"RJ_OPERATIONS_" +
                    cacheTableName +
                    "\"(RJ_OPER_ID)";
//          System.out.println(createOperationIndexSQL);
            st.execute(createOperationIndexSQL);
         }

         this.pstInsertOperationTable = cacheTableManager.conn.getH2Connection().
                 prepareStatement(this.
                         insertOperationTableSQL);
      }
   }

   private void loadStoreIndexes(Statement st) throws
           StoreException {
      IndexSchemaIF indexSchema = cacheTableManager.conn.getSchemaIF2().getIndexSchema();

      if (cacheTableManager.conn.loadIndexes &&
              cacheTableManager.conn.getSchemaIF2().getIndexSchema() != null) {
         IndexTableIF[] allIndexTables = indexSchema.getStoreIndexes(sqlTableName);

         for (IndexTableIF indexTable : allIndexTables) {
            try {
               StringBuilder indexSQL = new StringBuilder();

               // ignore indexes based on IDENTITY column
               if (indexTable.isAutonumber())
                  continue;

               // index is primary key and table has no IDENTITY column
               if (indexTable.isPrimaryKey() && !hasIdentityColumn) {
                  alterColumnsToNotNull(st, indexTable);

                  indexSQL.append("ALTER TABLE ").append(sqlTableName).append(
                          " ADD CONSTRAINT ").append(indexTable.getIndexName()).append(
                          " PRIMARY KEY(").append(indexTable.getFieldsCommaListString(
                          cacheTableManager.conn.preserveColumnNames())).append(")");
               }
               // unique index or primary key with existing IDENTITY column
               else if (indexTable.isUnique()) {
                  indexSQL.append("CREATE UNIQUE INDEX ").append(indexTable.
                          getIndexName()).append(" ON ").append(sqlTableName).append(
                          "(").append(indexTable.getFieldsCommaListString(
                          cacheTableManager.conn.preserveColumnNames())).append(")");
               }
               // non unique index
               else
                  indexSQL.append("CREATE INDEX ").append(indexTable.
                          getIndexName()).append(" ON ").append(sqlTableName).append("(").
                          append(indexTable.getFieldsCommaListString(
                                  cacheTableManager.conn.preserveColumnNames())).append(")");

               st.executeUpdate(indexSQL.toString());

               OtherUtils.writeLogInfo(log,
                       "creating index: " +
                               indexSQL);
            } catch (SQLException ex) {
               System.err.println("Error while loading indexes: " + ex.getMessage());
               log.warn("error while loading indexes: " + ex.getMessage());
            }
         }
      }
   }

   private void alterColumnsToNotNull(Statement st, IndexTableIF indexTable) throws
           SQLException {
      for (int j = 0; j < indexTable.getIndexFields().length; j++) {
         String fieldName = StringUtils.quoteFieldAndTableName(indexTable.getIndexFields()[j].
                 getStoreField().getName(), cacheTableManager.conn.preserveColumnNames());

         String fieldType = indexTable.getIndexFields()[j].getStoreField().getType().getH2Name();

         StringBuilder alterSQL = new StringBuilder("ALTER TABLE ").append(
                 sqlTableName).append(" ALTER COLUMN ").append(fieldName).
                 append(" ").append(fieldType).append(
                 " NOT NULL");
         try {
            st.executeUpdate(alterSQL.toString());
         } catch (SQLException ex) {
            System.err.println("Error while altering column: " + ex.getMessage());
            log.warn("error while altering column: " + ex.getMessage());
         }

         OtherUtils.writeLogInfo(log,
                 "altering column '" + fieldName +
                         "' to NOT NULL: " +
                         alterSQL);
      }
   }

   protected void InsertDataToCacheTable(StoreTableReaderIF reader,
                                         PreparedStatement pst) throws Exception {

      if (cacheTableManager.conn.multiThreaded)
         insertDataToCacheTableUsingProducerConsumerThreads(reader, pst);
      else
         insertDataToCacheTableUsingOneThread(reader, pst);
   }

   private void insertDataToCacheTableUsingOneThread(StoreTableReaderIF reader,
                                                     PreparedStatement pst) throws Exception {
      StoreRecordIF rec;
      while ((rec = reader.nextRecord()) != null) {
         for (int i = 0; i < fields.length; i++) {
            pst.setObject(i + 1, rec.getObject(i));
         }
         pst.execute();
//        pst.addBatch();
      }
//      pst.executeBatch();
   }

   private void insertDataToCacheTableUsingProducerConsumerThreads(
           StoreTableReaderIF reader,
           PreparedStatement pst) throws Exception {
      SingleConsumerProducer<StoreRecordIF, RecordsConsumer, RecordsProducer>
              consumerProducer = new SingleConsumerProducer<StoreRecordIF,
              RecordsConsumer, RecordsProducer>(100);
      consumerProducer.setConsumer(new RecordsConsumer(pst));
      consumerProducer.setProducer(new RecordsProducer(reader));
      consumerProducer.setThreadPrefixName("'" + fileTableName + "'");
      consumerProducer.runProcess();
   }

   private void saveTableInfo() throws Exception {
      cacheTableManager.pstInsertTablesInfo.setString(1, cacheTableName);
      cacheTableManager.pstInsertTablesInfo.setString(2, insertOperationTableSQL);
      cacheTableManager.pstInsertTablesInfo.setString(3,
              createOperationTableSQL != null ?
                      createOperationTableSQL.
                              toString() : null);
      cacheTableManager.pstInsertTablesInfo.setString(4, createOperationIndexSQL);

      Vector<StoreFieldIF> vFields = new Vector<StoreFieldIF>();
      Collections.addAll(vFields, fields);

      cacheTableManager.pstInsertTablesInfo.setObject(5, vFields);
      cacheTableManager.pstInsertTablesInfo.setObject(6, sqlTableName);
      cacheTableManager.pstInsertTablesInfo.setObject(7, modificationDate);
      cacheTableManager.pstInsertTablesInfo.execute();

      // H2 in non auto commit mode
      cacheTableManager.conn.getH2Connection().commit();

      OtherUtils.writeLogInfo(log,
              "Table '" + cacheTableName +
                      "' information has been saved to H2 (persistent mode)");
   }

   // ####### creates triggers #######
   void initTriggers() throws
           SQLException {
      Statement st = cacheTableManager.conn.getH2Connection().createStatement();

      // ##### INSERT trigger
      if (supportsInsertOnRecord()) {
         try {
            if (cacheTableManager.conn.isPersistentMode()) {
               st.execute("DROP TRIGGER \"RJ_INSERT_TRIGGER_" +
                       triggerTableName + "\"");
            }
         } catch (SQLException ex) {
//	log.warn(ex.getMessage(), ex);
         }

         String createInsertTriggerSql =
                 "CREATE TRIGGER \"RJ_INSERT_TRIGGER_" +
                         triggerTableName + "\" AFTER INSERT ON " + sqlTableName +
                         " FOR EACH ROW CALL \"" +
                         cacheTableManager.conn.driver.getH2TriggerClassName() + "\"";
         st.execute(createInsertTriggerSql);

         OtherUtils.writeLogInfo(log,
                 "creating INSERT trigger: " +
                         createInsertTriggerSql);
      }

      // ##### UPDATE trigger
      if (supportsUpdateOnRecord()) {
         try {
            if (cacheTableManager.conn.isPersistentMode()) {
               st.execute("DROP TRIGGER \"RJ_UPDATE_TRIGGER_" +
                       triggerTableName + "\"");
            }
         } catch (SQLException ex) {
//	log.warn(ex.getMessage(), ex);
         }

         String createUpdateTriggerSql =
                 "CREATE TRIGGER \"RJ_UPDATE_TRIGGER_" +
                         triggerTableName + "\" AFTER UPDATE ON " + sqlTableName +
                         " FOR EACH ROW CALL \"" +
                         cacheTableManager.conn.driver.getH2TriggerClassName() + "\"";
         st.execute(createUpdateTriggerSql);

         OtherUtils.writeLogInfo(log,
                 "creating UPDATE trigger: " +
                         createUpdateTriggerSql);
      }

      // ##### DELETE trigger
      if (supportsDeleteOnRecord()) {
         try {
            if (cacheTableManager.conn.isPersistentMode()) {
               st.execute("DROP TRIGGER \"RJ_DELETE_TRIGGER_" +
                       triggerTableName + "\"");
            }
         } catch (SQLException ex) {
//	log.warn(ex.getMessage(), ex);
         }

         String createDeleteTriggerSql =
                 "CREATE TRIGGER \"RJ_DELETE_TRIGGER_" +
                         triggerTableName + "\" AFTER DELETE ON " + sqlTableName +
                         " FOR EACH ROW CALL \"" +
                         cacheTableManager.conn.driver.getH2TriggerClassName() + "\"";
         st.execute(createDeleteTriggerSql);

         OtherUtils.writeLogInfo(log,
                 "creating DELETE trigger: " +
                         createDeleteTriggerSql);
      }

      OtherUtils.writeLogInfo(log,
              "##### end of loading the table '" +
                      cacheTableManager.getFileTableName(
                              sqlTableName) + "'\n");
      st.close();
   }

   public void clearTransactionFlag() {
      setInTransaction(false);
   }

   public void readLock() {
      readWriteLock.readLock().lock();

      OtherUtils.writeTraceInfo(log, "table '" + sqlTableName + "' is LOCKED for reading");
   }

   public void readLock(int milliSeconds) throws InterruptedException {
      readWriteLock.readLock().tryLock(milliSeconds, TimeUnit.MILLISECONDS);
   }

   public void writeLock() {
      readWriteLock.writeLock().lock();

      OtherUtils.writeTraceInfo(log, "table '" + sqlTableName + "' is LOCKED for writing");
   }

   public boolean writeLock(int milliSeconds) throws InterruptedException {
      boolean lockSuccess = readWriteLock.writeLock().tryLock(milliSeconds, TimeUnit.MILLISECONDS);

      OtherUtils.writeTraceInfo(log, "table '" + sqlTableName + "' is LOCKED for writing (" + milliSeconds + " ms)");

      return lockSuccess;
   }

   public void readUnlock() {
      if (readWriteLock.getReadLockCount() > 0) {
         readWriteLock.readLock().unlock();

         OtherUtils.writeTraceInfo(log, "table '" + sqlTableName + "' is unlocked for reading");
      }
   }

   public void writeUnlock() {
      if (readWriteLock.isWriteLockedByCurrentThread()) {
         readWriteLock.writeLock().unlock();

         OtherUtils.writeTraceInfo(log, "table '" + sqlTableName + "' is unlocked for writing");
      }
   }

//   public boolean equals(Object o) {
//      return o != null && cacheTableName.equals((String) o);
//   }

   // check if a file was modified by some external process
//  void refreshTableModificationDate() {
//    try {
//      StoreTableIF storeTable = cacheTableManager.conn.getSchema().getStoreTable(
//          fileTableName);
//
//      java.util.Date newModificationDate = null;
//
//      synchronized (this) {
//        newModificationDate = storeTable.refreshModificationDate();
//      }
//
//      if (newModificationDate == null)
//        return;
//
//      if (modificationDate == null ||
//          !newModificationDate.equals(modificationDate)) {
//        com.relationaljunction.utils.OtherUtils.writeLogInfo(log,
//                                             "WatchModificationsThread: file '" + fileTableName +
//                                             "' has been modified. Old mod date = " +
//                                             OtherUtils.formatDate(modificationDate) +
//                                             ". New mod date = " +
//                                             OtherUtils.formatDate(newModificationDate), true);
//	// file was modified
//        modificationDate = newModificationDate;
//
//	// remove a table from cache to be loaded anew later
//	Statement st = cacheTableManager.conn.createStatement();
//        st.execute("DROP TABLE " + sqlTableName + " FROM CACHE");
//	st.close();
//
//	com.relationaljunction.utils.OtherUtils.writeLogInfo(log,
//                                             "WatchModificationsThread: table '" +
//                                             sqlTableName +
//                                             "' has been deleted from the cache, because it has been externally modified", true);
//      }
//    } catch (Exception ex) {
//      log.warn("error while refreshing a modification date for the file '" +
//               fileTableName + "':" + ex.getMessage(), ex);
//      ex.printStackTrace();
//    }
//  }


   /**
    * check if a file was modified by some external process
    * if so, deletes the current cache data in H2.
    */
   void refreshTableModificationDate() {
      if (isInTransaction() || !isAlreadyLoaded())
         return;

      try {
//         writeLock();

         boolean lockSuccess = writeLock(CommonConnection2.DEFAULT_LOCK_TIME_OUT - 10000);
         if (!lockSuccess) throw new Exception("lock timeout in refreshTableModificationDate()");

         // check for closed Connection.
         // It is required for WatchModificationsThread that executes this method concurrently
         if (cacheTableManager.conn == null || cacheTableManager.conn.getSchemaIF2() == null) return;

         StoreTableIF storeTable = cacheTableManager.conn.getSchemaIF2().getStoreTable(
                 fileTableName);

         java.util.Date newModificationDate = storeTable.refreshModificationDate();

         if (newModificationDate == null)
            return;

         if (!newModificationDate.equals(modificationDate)) {
            OtherUtils.writeLogInfo(log,
                    "WatchModificationsThread: file '" +
                            fileTableName +
                            "' has been modified. Old mod date = " +
                            OtherUtils.formatDate(
                                    modificationDate) +
                            ". New mod date = " +
                            OtherUtils.formatDate(
                                    newModificationDate));
            // file was modified
            modificationDate = newModificationDate;

            cacheTableManager.conn.getSchemaIF2().deleteStoreTableFromCache(fileTableName);

            reloadData();

            OtherUtils.writeLogInfo(log,
                    "WatchModificationsThread: table '" +
                            sqlTableName +
                            "' is reloaded");

//        com.relationaljunction.utils.OtherUtils.writeLogInfo(log,
//                                             "WatchModificationsThread: table '" +
//                                             sqlTableName +
//                                             "' has been deleted from the cache, because it has been externally modified", true);
         }
      } catch (Exception ex) {
         log.warn("error while refreshing a modification date for the file '" +
                 fileTableName + "':" + ex.getMessage(), ex);
         ex.printStackTrace();
      } finally {
         writeUnlock();
      }
   }

   void reloadData() throws SQLException {
      // delete data in a table to be loaded later again
      // commits an opened transaction!!!
      deleteData();

      // load data from an external file
      loadDataFromStoreTable();
      initTriggers();
   }

   void beginOperation(SQLCommand sqlCommand) {
      setInTransaction(true);
   }

   void endOperation(SQLCommand sqlCommand, long updateCount) {
      if (updateCount == 0L)
         return;

      if (sqlCommand.getType() == SQLCommand.INSERT)
         hasInserts = true;
      else if (sqlCommand.getType() == SQLCommand.UPDATE)
         hasUpdates = true;
      else if (sqlCommand.getType() == SQLCommand.DELETE)
         hasDeletes = true;

      // consecutive INSERT operations in a transaction is considered to be one operation
      if (supportsUpdateOnRecord() ||
              supportsDeleteOnRecord()) {

//      if (sqlCommand.getJdbcType() == SQLCommand.INSERT &&
//          (previousTransactCommandType == -1 ||
//           previousTransactCommandType == SQLCommand.INSERT))
//  return;

         operationNumber++;
         previousTransactCommandType = sqlCommand.getType();
      }
   }


   public void commit() throws SQLException {
      // no records to insert, update, delete
      if (!hasInserts && !hasUpdates && !hasDeletes) {
         setInTransaction(false);
         return;
      }

//      printOperationTable();

      try {
         // locks schema (required for StelsMDB)
         if (cacheTableManager.conn.getSchemaIF2().requiresLockingForWritingOperations()) {
            cacheTableManager.conn.getSchemaIF2().lockForWriting();
         }

         StoreTableIF store = cacheTableManager.conn.getSchemaIF2().getStoreTable(
                 fileTableName);

         // inserts only
         if (supportsInsertOnRecord() && hasInserts &&
                 !hasUpdates && !hasDeletes) {
            // inserts last inserted records
            processInsertOnly(store);
         }
         // updates only
         else if (supportsUpdateOnRecord() &&
                 !hasInserts && hasUpdates && !hasDeletes) {
            processUpdateOnly(store);
         }
         // deletes only
         else if (supportsDeleteOnRecord() &&
                 !hasInserts && !hasUpdates && hasDeletes) {
            processDeleteOnly(store);
         }
         // all operations
         else if (supportsInsertOnRecord() &&
                 supportsUpdateOnRecord() &&
                 supportsDeleteOnRecord()) {
            processAll(store);
         }
         // full rewrite
         else {
            fullyRewrite(store);
         }

         // set a new modification date
         StoreTableIF storeTable = cacheTableManager.conn.getSchemaIF2().getStoreTable(
                 fileTableName);
         java.util.Date newModificationDate = storeTable.refreshModificationDate();

         OtherUtils.writeTraceInfo(log, "CacheTable.commit(): set new modified time '" +
                 OtherUtils.formatDate(newModificationDate) +
                 "' for the file '" + fileTableName + "'. Old time = " +
                 OtherUtils.formatDate(modificationDate));

         modificationDate = newModificationDate;

         clearOperations();

         setInTransaction(false);

         OtherUtils.writeLogInfo(log,
                 "table '" + sqlTableName +
                         "' data in H2 was succefully synchronized to the external file '" +
                         fileTableName + "'");
      } catch (Exception ex) {
         setInTransaction(false);
         ex.printStackTrace();
         throw cacheTableManager.conn.driver.createException(
                 "Error in CacheTable.commit(): can't write '" +
                         sqlTableName + "' on the disk. Error was: " + ex.getMessage());
      } finally {
         if (cacheTableManager.conn.getSchemaIF2().requiresLockingForWritingOperations()) {
            cacheTableManager.conn.getSchemaIF2().unlock();
         }
      }

      operationNumber = 0;
      hasInserts = false;
      hasUpdates = false;
      hasDeletes = false;
      previousTransactCommandType = -1;
   }

   private void clearOperations() throws SQLException {
      if (!supportsInsertOnRecord() &&
              !supportsUpdateOnRecord() &&
              !supportsDeleteOnRecord())
         return;

      Statement st = cacheTableManager.conn.getH2Connection().createStatement();

      st.execute("DELETE FROM RJ_SCHEMA.\"RJ_OPERATIONS_" +
              cacheTableName + "\"");

      // create/drop method is considerably faster for clearing a table then delete operation
      // but it commits an opened transaction!!!
//    st.execute("DROP TABLE RJ_SCHEMA.\"RJ_OPERATIONS_" + cacheTableName +
//               "\"");
//    st.execute(createOperationTableSQL.toString());
//    if (supportsUpdateOnRecord() ||
//        supportsDeleteOnRecord())
//      st.execute(createOperationIndexSQL);

      st.close();

      // if autoCommit = false then we have to commit deletion of operation records
      // (not necessary because DROP TABLE above commits an opened transaction)
//    if (!cacheTableManager.conn.getH2Connection().getAutoCommit())
//      cacheTableManager.conn.getH2Connection().commit();
   }

   private void processInsertOnly(StoreTableIF store) throws
           Exception {
      OtherUtils.writeLogInfo(log,
              "inserting data to the table: " + sqlTableName);

      Statement st = cacheTableManager.conn.getH2Connection().createStatement();
      ResultSet rs = st.executeQuery(
              "SELECT * FROM RJ_SCHEMA.\"RJ_OPERATIONS_" +
                      cacheTableName + "\"");
      DefaultH2SQLTranslator.insertRecordsToStoreTable(rs, store, fields);
      st.close();
   }

   private void processUpdateOnly(StoreTableIF store) throws
           Exception {
      OtherUtils.writeLogInfo(log,
              "updating data in the table: " + sqlTableName);

//    ResultSet rs = cacheTableManager.conn.getH2Connection().createStatement().
//                   executeQuery(
//        "EXPLAIN SELECT * FROM RJ_SCHEMA.\"RJ_OPERATIONS_" +
//        cacheTableName + "\" WHERE RJ_OPER_ID=1");
//    com.relationaljunction.utils.TestUtils.printColumnsOut(rs, System.out);
//    com.relationaljunction.utils.TestUtils.printResultSetOut(rs, System.out);

      PreparedStatement pst = cacheTableManager.conn.getH2Connection().
              prepareStatement(
                      "SELECT * FROM RJ_SCHEMA.\"RJ_OPERATIONS_" +
                              cacheTableName + "\" WHERE RJ_OPER_ID=?",
                      ResultSet.TYPE_SCROLL_INSENSITIVE,
                      ResultSet.CONCUR_UPDATABLE);

      int operationIndex = 0;
      while (operationIndex < operationNumber) {
         pst.setInt(1, operationIndex);
         DefaultH2SQLTranslator.updateRecordsInStoreTable(pst, store);
         operationIndex++;
      }

      pst.close();
   }

   private void processDeleteOnly(StoreTableIF store) throws
           Exception {
      OtherUtils.writeLogInfo(log,
              "deleting data in the table: " + sqlTableName);

      PreparedStatement pst = cacheTableManager.conn.getH2Connection().
              prepareStatement(
                      "SELECT * FROM RJ_SCHEMA.\"RJ_OPERATIONS_" +
                              cacheTableName + "\" WHERE RJ_OPER_ID=?",
                      ResultSet.TYPE_SCROLL_INSENSITIVE,
                      ResultSet.CONCUR_UPDATABLE);

      int operationIndex = 0;
      while (operationIndex < operationNumber) {
         pst.setInt(1, operationIndex);
         DefaultH2SQLTranslator.deleteRecordsInStoreTable(pst, store);
         operationIndex++;
      }

      pst.close();
   }

   private void processAll(StoreTableIF store) throws
           Exception {
      PreparedStatement pst = cacheTableManager.conn.getH2Connection().
              prepareStatement(
                      "SELECT * FROM RJ_SCHEMA.\"RJ_OPERATIONS_" +
                              cacheTableName + "\" WHERE RJ_OPER_ID=?",
                      ResultSet.TYPE_SCROLL_INSENSITIVE,
                      ResultSet.CONCUR_UPDATABLE);

      StoreTableWriterIF writer = store.getWriter(null);

      int operationIndex = 0;
      while (operationIndex < operationNumber) {
         pst.setInt(1, operationIndex);
         ResultSet rsOperations = pst.executeQuery();
         rsOperations.next();
         int operationType = rsOperations.getInt("RJ_OPER_TYPE");

         switch (operationType) {
            case H2TriggerIF.INSERT: {
               rsOperations.beforeFirst();
               DefaultH2SQLTranslator.insertRecordsToStoreTable(rsOperations, writer,
                       fields);
               break;
            }
            case H2TriggerIF.UPDATE: {
               // update records
               DefaultH2SQLTranslator.updateRecordsInStoreTable(pst, writer);
               break;
            }
            case H2TriggerIF.DELETE: {
               // update records
               DefaultH2SQLTranslator.deleteRecordsInStoreTable(pst, writer);
               break;
            }
         }

         operationIndex++;
      }

      pst.close();
      writer.close();
   }

   protected void fullyRewrite(StoreTableIF store) throws Exception {
      OtherUtils.writeLogInfo(log, "fully rewriting the table: " +
              sqlTableName);

      Statement st = cacheTableManager.conn.getH2Connection().createStatement();
      ResultSet rs = st.executeQuery("SELECT * FROM " + sqlTableName);
      // ResultSet should be closed in fullyRewriteStoreTable()
      fullyRewriteStoreTable(rs, store, fields);
      st.close();
   }

   protected void fullyRewriteStoreTable(ResultSet rs, StoreTableIF store,
                                         StoreFieldIF[] fields) throws
           Exception {
//      DefaultH2SQLTranslator.rewriteStoreTable(rs, store, fields);

      StoreTableWriterIF writer = store.getWriter(fields);

      StoreResultSet storeRS = new StoreResultSet(rs, fields);

      // rewrite records
      writer.rewrite(storeRS);

      storeRS.clear(); // close resultSet as well
      writer.close();
   }

   public void rollback() {
      //! do not require to call clearOperations(), because rollback() of H2 itself rollback changes

//      printOperationTable();

      setInTransaction(false);

      operationNumber = 0;
      hasInserts = false;
      hasUpdates = false;
      hasDeletes = false;
      previousTransactCommandType = -1;
   }

   void clear() throws SQLException {
      deleteData();

      if (pstInsertOperationTable != null)
         pstInsertOperationTable.close();

      pstInsertOperationTable = null;
      insertTrigger = null;
      updateTrigger = null;
      deleteTrigger = null;
      cacheTableManager = null;
   }

   private void deleteData() throws SQLException {
      Statement st = cacheTableManager.conn.getH2Connection().createStatement();

      if (supportsInsertOnRecord() ||
              supportsUpdateOnRecord() ||
              supportsDeleteOnRecord()) {
         st.execute("DROP TABLE RJ_SCHEMA.\"RJ_OPERATIONS_" +
                 cacheTableName + "\"");
         OtherUtils.writeLogInfo(log, "dropping operation table: " +
                 "DROP TABLE RJ_SCHEMA.\"RJ_OPERATIONS_" +
                 cacheTableName + "\"");
      }

      if (insertTrigger != null) {
         st.execute("DROP TRIGGER \"RJ_INSERT_TRIGGER_" +
                 triggerTableName + "\"");
         insertTrigger.closeTrigger();
         OtherUtils.writeLogInfo(log, "dropping INSERT trigger: " +
                 "DROP TRIGGER \"RJ_INSERT_TRIGGER_" +
                 triggerTableName +
                 "\"");
      }

      if (updateTrigger != null) {
         st.execute("DROP TRIGGER \"RJ_UPDATE_TRIGGER_" +
                 triggerTableName + "\"");
         updateTrigger.closeTrigger();
         OtherUtils.writeLogInfo(log, "dropping UPDATE trigger: " +
                 "DROP TRIGGER \"RJ_UPDATE_TRIGGER_" +
                 triggerTableName +
                 "\"");
      }

      if (deleteTrigger != null) {
         st.execute("DROP TRIGGER \"RJ_DELETE_TRIGGER_" +
                 triggerTableName + "\"");
         deleteTrigger.closeTrigger();

         OtherUtils.writeLogInfo(log, "dropping DELETE trigger: " +
                 "DROP TRIGGER \"RJ_DELETE_TRIGGER_" +
                 triggerTableName + "\"");
      }

      st.execute("DROP TABLE " + sqlTableName);
      st.close();

      OtherUtils.writeLogInfo(log,
              "droping the table '" + sqlTableName +
                      "' from H2");

      // delete a table info from H2 for the persistent mode
      if (cacheTableManager.conn.isPersistentMode()) {
         try {
            cacheTableManager.pstRemoveTablesInfo.setString(1, cacheTableName);
            cacheTableManager.pstRemoveTablesInfo.execute();
            cacheTableManager.conn.getH2Connection().commit();
         } catch (SQLException ex) {
            throw new UnexpectedException(
                    "Unexpected error in CacheTable.remove(). Error was " +
                            ex.getMessage(), ex);
         }
      }
   }

   public void insertOperation(int operationType, Object[] oldRow,
                               Object[] newRow) throws
           SQLException {
      if (operationType == org.h2.api.Trigger.INSERT) {
         try {
            pstInsertOperationTable.clearParameters();
            pstInsertOperationTable.setInt(1, getOperationNumber());
            pstInsertOperationTable.setInt(2, operationType);
            for (int i = 0; i < newRow.length; i++) {
               pstInsertOperationTable.setObject(i + 3, newRow[i]);
            }
            pstInsertOperationTable.execute();
         } catch (SQLException ex) {
            ex.printStackTrace();
            throw new SQLException(
                    "Unexpected error in CacheTable.insertOperation() method for the table '" +
                            sqlTableName + "'. Error was " + ex.getMessage(), ex);
         }
      } else if (operationType == org.h2.api.Trigger.UPDATE) {
         try {
            pstInsertOperationTable.clearParameters();

            // insert an old row
            pstInsertOperationTable.setInt(1, getOperationNumber());
            pstInsertOperationTable.setInt(2, operationType);
            for (int i = 0; i < newRow.length; i++) {
               pstInsertOperationTable.setObject(i + 3, oldRow[i]);
            }
            pstInsertOperationTable.execute();

            // insert a new row
            pstInsertOperationTable.setInt(1, getOperationNumber());
            pstInsertOperationTable.setInt(2, operationType + 1);
            for (int i = 0; i < newRow.length; i++) {
               pstInsertOperationTable.setObject(i + 3, newRow[i]);
            }
            pstInsertOperationTable.execute();
         } catch (SQLException ex) {
            ex.printStackTrace();
            throw new SQLException(
                    "Unexpected error in CacheTable.insertOperation() method for the table '" +
                            sqlTableName + "'. Error was: " + ex.getMessage(), ex);
         }
      } else if (operationType == org.h2.api.Trigger.DELETE) {
         try {
            pstInsertOperationTable.clearParameters();

            // insert an old row
            pstInsertOperationTable.setInt(1, getOperationNumber());
            pstInsertOperationTable.setInt(2, operationType);
            for (int i = 0; i < oldRow.length; i++) {
               pstInsertOperationTable.setObject(i + 3, oldRow[i]);
            }
            pstInsertOperationTable.execute();
         } catch (SQLException ex) {
            ex.printStackTrace();
            throw new SQLException(
                    "Unexpected error in CacheTable.insertOperation() method for the table '" +
                            sqlTableName + "'. Error was: " + ex.getMessage(), ex);
         }
      }
   }

/*
   private void printOperationTable() {
      try {
         PrintStream ps = System.out;
         ps.println("---operation table----");

         Statement st = cacheTableManager.conn.getH2Connection().createStatement();

         ResultSet rs = st.executeQuery(
                 "SELECT * FROM RJ_SCHEMA.\"RJ_OPERATIONS_" +
                         cacheTableName + "\"");
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

   public boolean supportsInsertOnRecord() {
      return cacheTableManager.conn.driver.supportsInsertOnRecord();
   }

   public boolean supportsUpdateOnRecord() {
      return cacheTableManager.conn.driver.supportsUpdateOnRecord();
   }

   public boolean supportsDeleteOnRecord() {
      return cacheTableManager.conn.driver.supportsDeleteOnRecord();
   }

   public void setInsertTrigger(H2TriggerIF trigger) {
      this.insertTrigger = trigger;
   }

   public void setUpdateTrigger(H2TriggerIF trigger) {
      this.updateTrigger = trigger;
   }

   public void setDeleteTrigger(H2TriggerIF trigger) {
      this.deleteTrigger = trigger;
   }

   protected void setFields(StoreFieldIF[] fields) {
      this.fields = fields;
   }

   public void setAlreadyLoaded(boolean alreadyLoaded) {
      this.alreadyLoaded = alreadyLoaded;
   }

   public void setInTransaction(boolean inTransaction) {
      this.inTransaction = inTransaction;
   }

   public void increaseOperationNumber() {
      this.operationNumber++;
   }

   public String getTableName() {
      return sqlTableName;
   }

   public String getFileTableName() {
      return fileTableName;
   }

   public String getInsertOperationTableSQL() {
      return insertOperationTableSQL;
   }

   public int getOperationNumber() {
      return operationNumber;
   }

   public boolean isAlreadyLoaded() {
      return alreadyLoaded;
   }

   public boolean isInTransaction() {
      return inTransaction;
   }

   public boolean isWriteLockedByCurrentThread() {
      return readWriteLock.isWriteLockedByCurrentThread();
   }

   public String toString() {
      return "CacheTable instance: sqlName='" + sqlTableName + "'";
   }
}
