package com.relationaljunction.jdbc.mdb.h2;

import org.h2.command.ddl.CreateTableData;
import org.h2.table.TableBase;

import com.relationaljunction.database.StoreException;
import com.relationaljunction.database.StoreTableIF;
import com.relationaljunction.database.h2.StoreSchemaIF2;
import com.relationaljunction.database.index.IndexSchemaIF;
import com.relationaljunction.database.index.IndexTableIF;
import com.relationaljunction.jdbc.common.h2.engine.H2TableEngine;
import com.relationaljunction.utils.UnexpectedException;

public class MDBTableEngine extends H2TableEngine {
   private static StoreSchemaIF2 schema;

   public static StoreSchemaIF2 getSchema() {
      return schema;
   }

   public static void setSchema(StoreSchemaIF2 schema) {
      MDBTableEngine.schema = schema;
   }

   public MDBTableEngine() {
   }

   @Override
   public TableBase createTable(CreateTableData data) {
      StoreTableIF storeTable;
      IndexTableIF[] storeTableIndexes = null;

      try {
         storeTable = schema.getStoreTable(schema.getFileTableName(data.tableName));

         // get indexes for the table
         IndexSchemaIF indexSchema = schema.getIndexSchema();
         if (indexSchema != null) {
            storeTableIndexes = indexSchema.getStoreIndexes(schema.getFileTableName(data.tableName));
         }
      } catch (StoreException e) {
         throw new UnexpectedException(e);
      }

      return new MDBTableWrapper(data, storeTable, storeTableIndexes);
   }
}
