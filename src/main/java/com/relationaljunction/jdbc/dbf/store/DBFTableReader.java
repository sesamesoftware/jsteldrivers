package com.relationaljunction.jdbc.dbf.store;

import org.h2.index.Cursor;
import org.xBaseJ.*;
import org.xBaseJ.fields.*;

import com.relationaljunction.database.*;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 2.2
 */

public class DBFTableReader
        implements StoreTableReaderIF {
   private DBFTable dbfTable = null;
   private DBF reader = null;
   private StoreFieldIF[] storeFields = null;
   private int curRecord = 0;
   private int recordCount = 0;
   private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd");

   DBFTableReader(DBFTable dbfTable) throws StoreException {
      try {
         this.dbfTable = dbfTable;

         this.reader = new DBF(dbfTable.fm.getPath(), DBF.READ_ONLY,
                 dbfTable.schema.charset);

         // lock the entire file. It maybe locked exclusive or shared.
         // shared allows other process to read the file, exclusive not
         // the file will be unlocked after invoking close() method or having process closed
         if (dbfTable.schema.isLockEnabled())
            reader.lock();

         // use index
//      if (dbfTable.schema.useTag != null)
//        this.reader.useTag(dbfTable.schema.useTag);


         recordCount = reader.getRecordCount();
         initFields();
      } catch (Exception ex) {
         ex.printStackTrace();
         String errMsg = ex.getMessage();
         throw new StoreException("Can't open the file '" +
                 dbfTable.fm.getPath() + "'. [XBaseJ] " + errMsg, ex);
      }
   }

   private void initFields() throws StoreException {
      try {
         storeFields = new StoreFieldIF[reader.getFieldCount()];

         for (int i = 0; i < storeFields.length; i++) {
            Field field = reader.getField(i + 1);
            storeFields[i] = dbfTable.getStoreField(field);
         }
      } catch (Exception ex) {
         throw new StoreException("Can't read the fields from the file '" +
                 dbfTable.fm.getPath() + "' [XBaseJ] " +
                 ex.getMessage(), ex);
      }
   }

   public StoreTableIF getStoreTable() {
      return dbfTable;
   }

   public Cursor getIndexCursor(List<String> columnNames) {
      return null;
   }

   public StoreRecordIF nextRecord() throws StoreException {
      Object[] objs;
      try {
         objs = new Object[storeFields.length];
         do {
            curRecord++;

            if (curRecord > recordCount)
               return null;

//        if (dbfTable.schema.isLockEnabled())
//          reader.gotoRecord(curRecord, true);
//        else
            reader.gotoRecord(curRecord);

            if (reader.deleted())
               dbfTable.containsDeletedRecords = DBFTable.CONTAINS_DELETED_RECS_YES;
         }
         while ((reader.deleted() &&
                 dbfTable.schema.useDeletedRecords ==
                         DBFSchema.NO_USE_DELETED_RECORDS) ||
                 (!reader.deleted() &&
                         dbfTable.schema.useDeletedRecords ==
                                 DBFSchema.ONLY_USE_DELETED_RECORDS));

         for (int j = 0; j < storeFields.length; j++) {
            Field field = reader.getField(j + 1);
            String str = field.get();
            objs[j] = dbfTable.getJDBCObject(storeFields[j], str, dateFormatter);
         }

         if (dbfTable.containsDeletedRecords != DBFTable.CONTAINS_DELETED_RECS_YES)
            dbfTable.containsDeletedRecords = DBFTable.CONTAINS_DELETED_RECS_NO;
      } catch (Exception ex) {
         ex.printStackTrace();
         throw new StoreException("Can't read in the file '" + dbfTable.fm.getPath() +
                 "' the record with the index " +
                 curRecord + ". [XBaseJ] " + ex.getMessage(), ex);
      }
      return new DefaultStoreRecord(storeFields, objs);
   }

   public int getRecordCount() {
      return reader.getRecordCount();
   }

   public StoreFieldIF[] getFields() {
      return storeFields;
   }

   public void close() {
      try {
         reader.close();
      } catch (Exception ex) {
         throw new RuntimeException("Can't close the input stream. [XBaseJ] " +
                 ex.getMessage());
      }

      reader = null;
      dbfTable = null;
      storeFields = null;
   }

}
