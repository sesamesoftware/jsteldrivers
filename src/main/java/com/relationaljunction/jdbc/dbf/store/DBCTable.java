package com.relationaljunction.jdbc.dbf.store;

import java.util.List;

import com.relationaljunction.database.StoreException;
import com.relationaljunction.database.StoreTableReaderIF;
import com.relationaljunction.database.io.DirectoryManager;
import com.relationaljunction.jdbc.dbf.h2.DBCSchema;

public class DBCTable extends DBFTable {
   private final List<String> newColumnsNames;

   public DBCTable(String tableName, DBCSchema schema, DirectoryManager dir,
                   List<String> newColumnsNames) {
      super(tableName, schema, dir);
      this.newColumnsNames = newColumnsNames;
   }

   @Override
   public StoreTableReaderIF getReader() throws StoreException {
      return new DBCTableReader(super.getReader(), newColumnsNames);
   }
}
