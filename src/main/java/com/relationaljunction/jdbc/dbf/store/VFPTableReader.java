package com.relationaljunction.jdbc.dbf.store;

import java.util.List;

import org.h2.index.Cursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.database.DefaultStoreRecord;
import com.relationaljunction.database.StoreException;
import com.relationaljunction.database.StoreFieldIF;
import com.relationaljunction.database.StoreRecordIF;
import com.relationaljunction.database.StoreTableIF;
import com.relationaljunction.database.StoreTableReaderIF;
import com.relationaljunction.database.dbf.DBFField;
import com.relationaljunction.database.dbf.VFPFileReader;

/**
 * <p>Title: </p>
 * <p/>
 * <p>Description: </p>
 * <p/>
 * <p>Copyright: Copyright (c) 2002-2006</p>
 * <p/>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 2.5
 */

public class VFPTableReader
        implements StoreTableReaderIF {
   private final Logger log = LoggerFactory.getLogger("VFPTableReader");

   private DBFTable dbfTable = null;
   private VFPFileReader reader = null;
   private StoreFieldIF[] storeFields = null;
   private int curRecord = 0;
   private int recordCount = 0;

   public VFPTableReader(DBFTable dbfTable) throws StoreException {
      try {
         this.dbfTable = dbfTable;

         reader = new VFPFileReader(dbfTable.fm.getFile(),
                 dbfTable.schema.memoExtension,
                 dbfTable.schema.charset,
                 dbfTable.schema.ignoreMemoFile.equalsIgnoreCase(DBFSchema.ALWAYS_IGNORE_MEMO_FILE),
                 dbfTable.schema.lockFiles);
         reader.setReadMemoInBytes(dbfTable.schema.getMemoAsBlob);
         reader.setMaxMemoSizeInBytes(dbfTable.schema.maxMemoSizeInBytes);
         recordCount = reader.getRecordCount();
         initFields();
      } catch (Exception ex) {
         ex.printStackTrace();
         throw new StoreException("Can't open the file '" + dbfTable.fm.getPath() +
                 "' [VFPTableReader] " + ex.getMessage(), ex);
      }
   }


   private void initFields() throws StoreException {
      try {
         storeFields = new StoreFieldIF[reader.getFieldCount()];

         for (int i = 0; i < storeFields.length; i++) {
            DBFField field = reader.getField(i);
            storeFields[i] = dbfTable.getStoreField(field);
         }
      } catch (Exception ex) {
         ex.printStackTrace();
         throw new StoreException("Can't read fields " + ex.getMessage(), ex);
      }
   }

   public StoreTableIF getStoreTable() {
      return dbfTable;
   }

   public Cursor getIndexCursor(List<String> columnNames) {
      return null;
   }

   /**
    * getFields
    *
    * @return StoreFieldIF[]
    * @throws StoreException
    */
   public StoreFieldIF[] getFields() {
      return storeFields;
   }

   /**
    * getRecordCount
    *
    * @return int
    * @throws StoreException
    */
   public int getRecordCount() {
      return recordCount;
   }

   /**
    * nextRecord
    *
    * @return StoreRecordIF
    * @throws StoreException
    */
   public StoreRecordIF nextRecord() throws StoreException {
      Object[] objs;
      try {
         objs = new Object[storeFields.length];
         Object[] dbfObjs = reader.nextRecord();
         if (dbfObjs == null) return null;

         curRecord++;

//      if (com.relationaljunction.utils.OtherUtils.TRACE_ENABLED && log.isTraceEnabled()){
//        log.trace("##### Reading record " + curRecord);
//      }

         for (int i = 0; i < storeFields.length; i++) {
            objs[i] = dbfTable.getJDBCObjectForVFP(storeFields[i], dbfObjs[i]);

//        if (com.relationaljunction.utils.OtherUtils.TRACE_ENABLED && log.isTraceEnabled()){
//          log.trace("Reading the value '" + dbfObjs[i] + "' from the field " +
//                    storeFields[i].getName() + "[" +
//                    storeFields[i].getSourceTypeName() + "]");
//	}
         }


         if (reader.containsDeletedRecords())
            dbfTable.containsDeletedRecords = DBFTable.CONTAINS_DELETED_RECS_YES;
         else
            dbfTable.containsDeletedRecords = DBFTable.CONTAINS_DELETED_RECS_NO;
      } catch (Exception ex) {
//      ex.printStackTrace();
         throw new StoreException("Can't read the record with the index " +
                 curRecord + ". [VFPTableReader] " + ex.getMessage(), ex);
      }
      return new DefaultStoreRecord(storeFields, objs);
   }


   public void close() {
      try {
         reader.close();
      } catch (Exception ex) {
         throw new com.relationaljunction.utils.UnexpectedException(ex);
      }

      reader = null;
      dbfTable = null;
      storeFields = null;
   }

}
