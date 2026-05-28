package com.relationaljunction.jdbc.dbf.h2;

import org.h2.command.ddl.CreateTableData;
import org.h2.table.TableBase;

import com.relationaljunction.database.StoreException;
import com.relationaljunction.database.StoreTableIF;
import com.relationaljunction.database.h2.StoreSchemaIF2;
import com.relationaljunction.jdbc.common.h2.engine.H2TableEngine;
import com.relationaljunction.jdbc.common.h2.engine.StoreTableWrapper;
import com.relationaljunction.utils.UnexpectedException;

public class DBFTableEngine extends H2TableEngine {
   private static StoreSchemaIF2 schema;

   public static StoreSchemaIF2 getSchema() {
      return schema;
   }

   public static void setSchema(StoreSchemaIF2 schema) {
      DBFTableEngine.schema = schema;
   }

   public DBFTableEngine() {
   }

   @Override
   public TableBase createTable(CreateTableData data) {
      StoreTableIF storeTable;

      try {
         storeTable = schema.getStoreTable(schema.getFileTableName(data.tableName));
      } catch (StoreException e) {
         throw new UnexpectedException(e);
      }

      return new StoreTableWrapper(data, storeTable, null);
   }
}
