package com.relationaljunction.jdbc.common.h2;

import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.database.StoreDataType;
import com.relationaljunction.database.StoreFieldIF;
import com.relationaljunction.database.StoreTableIF;
import com.relationaljunction.database.StoreTableReaderIF;
import com.relationaljunction.utils.OtherUtils;
import com.relationaljunction.utils.StringUtils;

public class WrapperCacheTable extends CacheTable {
    private final Logger log = LoggerFactory.getLogger("WrapperCacheTable");

   public WrapperCacheTable(CacheTableManager cacheTableManager, String sqlTableName) {
      super(cacheTableManager, sqlTableName);
   }

   /**
    * create a wrapper table backed by a store table
    *
    * @throws SQLException
    */
   public void loadDataFromStoreTable() throws SQLException {
      try {
         StringBuilder columnList = null;
         StringBuilder columnListWithDataType = null;
         StringBuilder valueList = null;
         Statement st = null;

         try {
            // locks schema (required for StelsMDB)
            if (cacheTableManager.conn.getSchemaIF2().requiresLockingForReadingOperations()) {
               cacheTableManager.conn.getSchemaIF2().lockForReading();
            }

            StoreTableIF storeTable = cacheTableManager.conn.getSchemaIF2().getStoreTable(
                    fileTableName);
            StoreTableReaderIF reader = storeTable.getReader();
            StoreFieldIF[] fields = reader.getFields();
            setFields(fields);

            OtherUtils.writeLogInfo(log, "##### loading the wrapper table '",
                    cacheTableManager.getFileTableName(sqlTableName), "'");

            // ##### local table properties
            String tableConstraint = null;

            if (storeTable.getTableProperties() != null) {
               tableConstraint = storeTable.getTableProperties().getProperty(
                       "constraint");
            }
            // ##### end of local table properties

            StringBuilder createTableSql = new StringBuilder("CREATE TABLE ").append(
                    sqlTableName).append("(");
            columnListWithDataType = new StringBuilder();
            columnList = new StringBuilder();
            valueList = new StringBuilder();

            for (int i = 0; i < fields.length; i++) {
               String fieldName = StringUtils.quoteFieldAndTableName(fields[i].getName(),
                       cacheTableManager.conn.preserveColumnNames());

               if (!cacheTableManager.conn.getSchemaIF2().supportsIdentityInNonCachingMode() &&
                       fields[i].getType() == StoreDataType.IDENTITY)
                  // H2 identity is not supported
                  createTableSql.append(fieldName).append(" ").append("BIGINT");
               else
                  createTableSql.append(fieldName).append(" ").append(fields[i].getType().getH2Name());

               if (fields[i].getType() == StoreDataType.IDENTITY) {
                  columnListWithDataType.append(fieldName).append(" INT");
               } else
                  columnListWithDataType.append(fieldName).append(" ").
                          append(fields[i].getType().getH2Name());

               // specify size and decimal count for columns
               if (fields[i].getLength() > 0) {
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

            // ##### add constraints
            // add a table constraint sql via local property
            if (tableConstraint != null)
               createTableSql.append(", ").append(tableConstraint);

            createTableSql.append(")");

            // ##### add engine reference
            createTableSql.append(" ENGINE \"").
                    append(cacheTableManager.conn.getSchemaIF2().getExternalEngineClass()).append("\"");

            // ##### end of CREATE TABLE constructing

            // ####### create a wrapper table in H2 #######
            st = cacheTableManager.conn.getH2Connection().createStatement();
            st.executeUpdate(createTableSql.toString());

            OtherUtils.writeLogInfo(log,
                    "creating a wrapper table: " +
                            createTableSql);

            reader.close();
         } finally {
            if (cacheTableManager.conn.getSchemaIF2().requiresLockingForReadingOperations()) {
               cacheTableManager.conn.getSchemaIF2().unlock();
            }
         }

         // ####### create an operation table #######
         createOperationalTable(columnList, columnListWithDataType, valueList, st);

         st.close();
      } catch (Exception ex) {
//         ex.printStackTrace();
         log.error("Can't load the file '" +
                 fileTableName + "' to H2 database. Error was: " + ex.getMessage(),
                 ex);
         throw new SQLException("Can't load the file '" +
                 fileTableName + "' to H2 database. Error was: " + ex.getMessage());
      }
   }
}
