package com.relationaljunction.jdbc.dbf.store;

import java.util.List;

import org.h2.index.Cursor;

import com.relationaljunction.database.*;
import com.relationaljunction.utils.UnexpectedException;

public class DBCTableReader implements StoreTableReaderIF {
   private final StoreTableReaderIF dbfReader;
   private final List<String> newColumnsNames;

   DBCTableReader(StoreTableReaderIF dbfReader, List<String> newColumnsNames)
           throws StoreException {
      this.dbfReader = dbfReader;
      this.newColumnsNames = newColumnsNames;
   }

   public StoreTableIF getStoreTable() {
      return dbfReader.getStoreTable();
   }

   public Cursor getIndexCursor(List<String> columnNames) {
      return null;
   }

   public StoreRecordIF nextRecord() throws StoreException {
      return dbfReader.nextRecord();
   }

   public StoreFieldIF[] getFields() {
      StoreFieldIF[] newFields = dbfReader.getFields();

      if (newFields.length != newColumnsNames.size())
         throw new UnexpectedException("Columns count in the DBC file does not equal " +
                 "to columns count in a DBF file");

      for (int i = 0; i < newFields.length; i++) {
         StoreFieldIF newField = newFields[i];
         newField.setName(newColumnsNames.get(i));
      }

      return newFields;
   }

   public int getRecordCount() {
      return dbfReader.getRecordCount();
   }

   public void close() {
      dbfReader.close();
   }
}
