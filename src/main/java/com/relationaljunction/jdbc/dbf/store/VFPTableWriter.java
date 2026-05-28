package com.relationaljunction.jdbc.dbf.store;

import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.database.AbstractStoreTableWriter;
import com.relationaljunction.database.StoreException;
import com.relationaljunction.database.StoreFieldIF;
import com.relationaljunction.database.StoreRecordIF;
import com.relationaljunction.database.StoreRecordsIF;
import com.relationaljunction.database.StoreTableIF;
import com.relationaljunction.database.dbf.DBFField;
import com.relationaljunction.database.dbf.DBFHeader;
import com.relationaljunction.database.dbf.VFPFileWriter;
import com.relationaljunction.database.io.LocalFileManager;

public class VFPTableWriter
        extends AbstractStoreTableWriter {
   private final Logger log = LoggerFactory.getLogger("VFPTableWriter");

   private DBFTable dbfTable = null;
   private VFPFileWriter writer = null;
   private StoreFieldIF[] storeFields = null;
   private Date memoFileModificationDate = null;

   VFPTableWriter(DBFTable dbfTable, StoreFieldIF[] storeFields) throws
           StoreException {
      this.dbfTable = dbfTable;

      try {
         // ##### open a file #####
         if (storeFields == null) {
            // open an existing file
            writer = new VFPFileWriter(dbfTable.fm.getFile(),
                    dbfTable.schema.memoExtension, dbfTable.schema.charset, dbfTable.schema.lockFiles);
            writer.setReadMemoInBytes(dbfTable.schema.getMemoAsBlob);
            writer.setMaxMemoSizeInBytes(dbfTable.schema.maxMemoSizeInBytes);
            writer.setWriteEOF(dbfTable.schema.writeEOF);
            initFields();

            if (dbfTable.memoFm != null)
               memoFileModificationDate = dbfTable.memoFm.getModificationTime();
         }
         // ##### create a new file #####
         else {
            createVFPFile(dbfTable, storeFields);
         }
      } catch (Exception ex) {
         throw new StoreException("Error writing the file " + dbfTable.tableName +
                 ": [VFPTableWriter] " + ex.getMessage(), ex);
      }
   }

   void createVFPFile(DBFTable dbfTable, StoreFieldIF[] storeFields) throws Exception {
      // destroy data = > delete a file and create it anew
      this.storeFields = storeFields;

      DBFField[] fields = new DBFField[storeFields.length];
      for (int i = 0; i < storeFields.length; i++)
         fields[i] = getDBFField(storeFields[i]);

      dbfTable.fm = (LocalFileManager) dbfTable.dir.getFileManager(dbfTable.tableName);

      writer = new VFPFileWriter(dbfTable.fm.getFile(), fields,
              dbfTable.schema.charset, DBFHeader.SIG_VFP,
              dbfTable.schema.memoExtension,
              dbfTable.schema.memoBlockSize,
              dbfTable.schema.dbfCodepage);

      // if a memo was created, set a reference on it
      if (dbfTable.fm.getDir().exists(dbfTable.memoFile))
         dbfTable.memoFm = (LocalFileManager) dbfTable.fm.getDir().getFileManager(dbfTable.memoFile);

      dbfTable.containsDeletedRecords = DBFTable.CONTAINS_DELETED_RECS_NO;
   }

   private void initFields() throws StoreException {
      try {
         storeFields = new StoreFieldIF[writer.getFieldCount()];

         for (int i = 0; i < storeFields.length; i++) {
            DBFField field = writer.getField(i);
            storeFields[i] = dbfTable.getStoreField(field);
         }
      } catch (Exception ex) {
//      ex.printStackTrace();
         throw new StoreException("Can't read fields " + ex.getMessage(), ex);
      }
   }

   public void clearRecords() throws StoreException {
      try {
         writer.clearRecords();
         dbfTable.containsDeletedRecords = DBFTable.CONTAINS_DELETED_RECS_NO;
      } catch (Exception ex) {
         throw new StoreException("Can't clear a table '" + dbfTable.getName() +
                 "'. [VFPTableWriter] Error was: " + ex.getMessage(), ex);
      }
   }

   public StoreTableIF getStoreTable() {
      return dbfTable;
   }

   // inserts record
   public void insertRecords(StoreRecordsIF recs) throws StoreException {
      try {
         recs.beforeFirst();
         while (recs.hasNext()) {
            StoreRecordIF rec = recs.nextRecord();
            writer.insertRecord(rec.getObjects());
         }
      } catch (Exception ex) {
//         ex.printStackTrace();
         throw new StoreException("Can't insert a record in the file '" +
                 dbfTable.fm.getPath() + "'. [VFPTableWriter] " +
                 ex.getMessage(), ex);
      }
   }

   // param: recs should contains ID of record to be updated
   // and also new values(objects) for this record
   public void updateRecords(StoreRecordsIF recs) throws StoreException {
      if (dbfTable.schema.scanDeletedRecords ==
              DBFSchema.NEVER_SCAN_DELETED_RECORDS ||
              (dbfTable.schema.scanDeletedRecords ==
                      DBFSchema.AUTO_SCAN_DELETED_RECORDS &&
                      dbfTable.containsDeletedRecords == DBFTable.CONTAINS_DELETED_RECS_NO)) {
         updateRecordsWithNoDeletedRecordsExist(recs);
         return;
      }

      // ### the method searches records taking into account that DBF may
      // ### contain deleted records
      // curPos - current record position including deleted records
      // realPos - record position excluding deleted records
      int curPos = 0, realPos = -1;
      try {
         recs.beforeFirst();
         while (recs.hasNext()) {
            StoreRecordIF rec = recs.nextRecord();
            int id = rec.getID();

            if (id < 0)
               throw new StoreException("The record has incorrect ID: " + id);
            else if (id < realPos) {
               curPos = 0;
               realPos = -1;
            } else if (id == realPos) {
               writer.gotoRecord(curPos - 1);
            }

            while (realPos < id) {
               // go to the current record position
               boolean deleted = writer.gotoRecord(curPos);

               curPos++;
               if (!deleted) {
                  realPos++;
               }
            }

//          writer.gotoRecord(curPos - 1);
            writer.updateRecord(rec.getObjects());
         }
      } catch (Exception ex) {
         //      System.out.println(id);
         //      ex.printStackTrace();
         throw new StoreException("Can't update a record in the file '" +
                 dbfTable.fm.getPath() + "'. [VFPTableWriter] " +
                 ex.getMessage(), ex);
      }
   }

   private void updateRecordsWithNoDeletedRecordsExist(StoreRecordsIF recs) throws
           StoreException {
      try {
         recs.beforeFirst();
         while (recs.hasNext()) {
            StoreRecordIF rec = recs.nextRecord();
            int id = rec.getID();
            if (id < 0)
               throw new StoreException("The record has incorrect ID: " + id);

            writer.gotoRecord(id);
            writer.updateRecord(rec.getObjects());
         }
      } catch (Exception ex) {
         throw new StoreException("Can't update a record in the file '" +
                 dbfTable.fm.getPath() + "'. [VFPTableWriter] " +
                 ex.getMessage(), ex);
      }
   }

   public void updateRecords(PreparedStatement pst) throws
           StoreException {
      try {
         ResultSet rsOperations = pst.executeQuery();

//      com.relationaljunction.utils.TestUtils.printColumnsOut(rsOperations, System.out);
//      com.relationaljunction.utils.TestUtils.printResultSetOut(rsOperations, System.out);

         // last update operation
         rsOperations.last();
         int operationEndIndex = rsOperations.getRow();
         if (operationEndIndex == 0) return;

         for (int i = 0; i < writer.getRecordCount(); i++) {
            boolean deleted = writer.gotoRecord(i);
            if (deleted) continue;
            Object[] objs = getRowObjects(writer.nextRecord());

            rsOperations.absolute(1);

            while (rsOperations.getRow() != 0) {
               // compare the current record to the current operation record
               boolean isRowsEqual = compareCurrentRow(objs, rsOperations);
               if (isRowsEqual) {
                  // read a new record
                  rsOperations.relative(1);
                  // update the current record
                  writer.gotoRecord(i);
                  updateCurrentRow(rsOperations);

                  operationEndIndex -= 2;
                  if (operationEndIndex == 0)
                     // all records have been updated
                     return;

                  break;
               } else
                  // move to the next operation
                  rsOperations.relative(2);
            }
         }
      } catch (Exception ex) {
         ex.printStackTrace();
         throw new StoreException("Can't update a record in the table '" +
                 dbfTable.getName() + "' [VFPTableWriter] " +
                 ex.getMessage(), ex);
      }
   }

   public void deleteRecords(PreparedStatement pst) throws
           StoreException {
      try {
         ResultSet rsOperations = pst.executeQuery();
         // last update operation
         rsOperations.last();
         int operationEndIndex = rsOperations.getRow();
         if (operationEndIndex == 0) return;

         for (int i = 0; i < writer.getRecordCount(); i++) {
            boolean deleted = writer.gotoRecord(i);
            if (deleted) continue;
            Object[] objs = getRowObjects(writer.nextRecord());

            rsOperations.absolute(1);

            while (rsOperations.getRow() != 0) {
               // compare the current record to the current operation record
               boolean isRowsEqual = compareCurrentRow(objs, rsOperations);
               if (isRowsEqual) {
                  // delete the current record
                  writer.gotoRecord(i);
                  writer.deleteRecord();

                  operationEndIndex -= 1;
                  if (operationEndIndex == 0)
                     // all records have been updated
                     return;

                  break;
               } else
                  // move to the next operation
                  rsOperations.relative(1);
            }
         }
      } catch (Exception ex) {
         ex.printStackTrace();
         throw new StoreException("Can't update a record in the table '" +
                 dbfTable.getName() + "' [VFPTableWriter] " +
                 ex.getMessage(), ex);
      }
   }

   private Object[] getRowObjects(Object[] dbfObjs) throws Exception {
      Object[] objs = new Object[storeFields.length];

      for (int i = 0; i < storeFields.length; i++)
         objs[i] = dbfTable.getJDBCObjectForVFP(storeFields[i], dbfObjs[i]);

      return objs;
   }

   private boolean compareCurrentRow(Object[] objs, ResultSet rsOperations) throws
           Exception {
      for (int i = 0; i < storeFields.length; i++) {
         Object dbfValue = objs[i];

         Object value2compare = rsOperations.getObject(i + 4);

         if (dbfValue == null && value2compare == null)
            continue;

         // consider 'null' and 'false' values are equal
         if (dbfValue instanceof Boolean &&
                 !(Boolean) dbfValue && value2compare == null)
            continue;

         // BLOB
         if (value2compare instanceof Blob) {
            Blob blob = (Blob) value2compare;
            byte[] arrayToCompare = blob.getBytes(1, (int) blob.length());

            if (Arrays.equals((byte[]) dbfValue, arrayToCompare)) continue;
            else return false;
         }

         if (dbfValue != null && value2compare == null || dbfValue == null
                 || !dbfValue.equals(value2compare))
            return false;
      }

      return true;
   }

   private void updateCurrentRow(ResultSet rsOperations) throws
           Exception {
      Object[] objs = new Object[storeFields.length];
      for (int i = 0; i < storeFields.length; i++)
         objs[i] = rsOperations.getObject(i + 4);

      writer.updateRecord(objs);
   }


   // param: recs should contains ID of record to be deleted
   public void deleteRecords(StoreRecordsIF recs) throws StoreException {
      if (dbfTable.schema.scanDeletedRecords ==
              DBFSchema.NEVER_SCAN_DELETED_RECORDS ||
              (dbfTable.schema.scanDeletedRecords ==
                      DBFSchema.AUTO_SCAN_DELETED_RECORDS &&
                      dbfTable.containsDeletedRecords == DBFTable.CONTAINS_DELETED_RECS_NO)) {
         deleteRecordsWithNoDeletedRecordsExist(recs);
         return;
      }

      // ### the method searches records taking into account that DBF may
      // ### contain deleted records
      // curPos - current record position including deleted records
      // realPos - record position excluding deleted records
      int curPos = 0, realPos = -1;
      try {
         recs.beforeFirst();
         while (recs.hasNext()) {
            StoreRecordIF rec = recs.nextRecord();
            int id = rec.getID();

            if (id < 0)
               throw new StoreException("The record has incorrect ID: " + id);
            else if (id < realPos) {
               curPos = 0;
               realPos = -1;
            } else if (id == realPos) {
               writer.gotoRecord(curPos - 1);
            }

            while (realPos < id) {
               // go to the current record position
               boolean deleted = writer.gotoRecord(curPos);

               curPos++;
               if (!deleted) {
                  realPos++;
               }
            }

//        writer.gotoRecord(curPos - 1);
            writer.deleteRecord();
            dbfTable.containsDeletedRecords = DBFTable.CONTAINS_DELETED_RECS_YES;
         }
      } catch (Exception ex) {
         //      System.out.println(id);
         //      ex.printStackTrace();
         throw new StoreException("Can't delete a record in the file '" +
                 dbfTable.fm.getPath() + "'. [VFPTableWriter] " +
                 ex.getMessage(), ex);
      }
   }

   private void deleteRecordsWithNoDeletedRecordsExist(StoreRecordsIF recs) throws
           StoreException {
      int id;
      try {
         while (recs.hasNext()) {
            StoreRecordIF rec = recs.nextRecord();
            id = rec.getID();
            if (id < 0)
               throw new StoreException("The record has incorrect ID: " + id);

            writer.gotoRecord(id);
            writer.deleteRecord();
            dbfTable.containsDeletedRecords = DBFTable.CONTAINS_DELETED_RECS_YES;
         }
      } catch (Exception ex) {
//      System.out.println(id);
//      ex.printStackTrace();
         throw new StoreException("Can't delete a record in the file '" +
                 dbfTable.fm.getPath() + "'. [VFPTableWriter] " +
                 ex.getMessage(), ex);
      }
   }

   public void pack() {
      throw new RuntimeException("Not supported for Visual FoxPro files");
   }

   public void close() {
      try {
         writer.close();

         // upload a memo file if it exists and its modification time is NULL (it didn't exist before)
         // or its modification time is changed.
         // thus unchanged memo file will not be uploaded.
         if (dbfTable.memoFm != null &&
                 dbfTable.memoFm.exists() &&
                 (memoFileModificationDate == null
                         || dbfTable.memoFm.getModificationTime().after(memoFileModificationDate)))
            dbfTable.dir.upload(dbfTable.memoFm);

         // upload a DBF file
         dbfTable.dir.upload(dbfTable.fm);

//      java.util.Date newModificationDate = dbfTable.dir.getFileManager(
//	  dbfTable.tableName).getModificationTime();
//      if (log.isTraceEnabled())
//	log.trace("VFPTableWriter: set new modified time '" +
//		  OtherUtils.formatDate(newModificationDate) +
//		  "' for the file '" + dbfTable.getName() + "'. Old time = "+
//		  OtherUtils.formatDate(dbfTable.fileModificationDate));
//      dbfTable.fileModificationDate = newModificationDate;

         writer = null;
         dbfTable = null;
      } catch (Exception ex) {
         throw new com.relationaljunction.utils.UnexpectedException(ex);
      }
   }

//   Object[] getVFPObjects(StoreFieldIF[] storeFields, Object objs[]) {
//      Object[] result = new Object[objs.length];
//
//      try {
//         for (int i = 0; i < objs.length; i++)
//            if (storeFields[i].getType() == StoreDataType.BLOB) {
//               Blob blob = ((Blob) objs[i]);
//               result[i] = (blob == null) ? null : blob.getBytes(1, (int) blob.length());
//            } else {
//               result[i] = objs[i];
//            }
//      } catch (SQLException e) {
//         throw new UnexpectedException("Unexpected exception in getVFPObject().", e);
//      }
//
//      return result;
//   }

   // SQLEngine -> DBF
   DBFField getDBFField(StoreFieldIF storeField) throws StoreException {
      DBFField field = new DBFField(dbfTable.schema.charset);
      field.setName(storeField.getName());
      int length = storeField.getLength();
      int decimalCount = storeField.getDecimalCount();

      switch (storeField.getType()) {
         case INTEGER: {
            field.setDataType(DBFField.FIELD_TYPE_INTEGER);
            break;
         }
         case BIGINT: {
            if (length == -1)
               length = DBFTable.DEFAULT_BIGINT_LENGTH;
            else
               length = length <= DBFTable.INT_LIMIT ?
                       DBFTable.DEFAULT_BIGINT_LENGTH : length;

            field.setDataType(DBFField.FIELD_TYPE_NUMERIC);
            field.setFieldLength(length);
            field.setDecimalCount(0);
            break;
         }
         case FLOAT:
         case DOUBLE: {
            field.setDataType(DBFField.FIELD_TYPE_DOUBLE);
            break;
         }
         case NUMERIC: {
            if (length == -1)
               length = DBFTable.DEFAULT_DOUBLE_LENGTH;
            if (decimalCount == -1)
               decimalCount = DBFTable.DEFAULT_DOUBLE_DECIMAL_COUNT;

            field.setDataType(DBFField.FIELD_TYPE_NUMERIC);
            field.setFieldLength(length);
            field.setDecimalCount(decimalCount);
            break;
         }
         case TIMESTAMP:
         case TIME:
         case DATE: {
            if (dbfTable.schema.vfpMapToDateType)
               field.setDataType(DBFField.FIELD_TYPE_DATE);
            else
               field.setDataType(DBFField.FIELD_TYPE_DATETIME);
            break;
         }
         case VARCHAR: {
            if (length == -1)
               length = DBFTable.DEFAULT_STRING_LENGTH;

            field.setDataType(DBFField.FIELD_TYPE_CHAR);
            field.setFieldLength(length);
            break;
         }
         case BOOLEAN: {
            field.setDataType(DBFField.FIELD_TYPE_LOGICAL);
            break;
         }
         case LONGVARCHAR: {
            field.setDataType(DBFField.FIELD_TYPE_MEMO);
            break;
         }
         case BLOB: {
            field.setDataType(DBFField.FIELD_TYPE_GENERAL);
            break;
         }
         default: {
            throw new StoreException("[VFPTableWriter] Unknown column data type " +
                    storeField.getType());
         }
      }
      return field;
   }
}
