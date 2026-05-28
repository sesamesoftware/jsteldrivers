package com.relationaljunction.jdbc.dbf.store;

import java.io.FileOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;
import org.xBaseJ.fields.CharField;
import org.xBaseJ.fields.DateField;
import org.xBaseJ.fields.Field;
import org.xBaseJ.fields.LogicalField;
import org.xBaseJ.fields.MemoField;
import org.xBaseJ.fields.NumField;

import com.relationaljunction.database.AbstractStoreTableWriter;
import com.relationaljunction.database.StoreDataType;
import com.relationaljunction.database.StoreException;
import com.relationaljunction.database.StoreFieldIF;
import com.relationaljunction.database.StoreRecordIF;
import com.relationaljunction.database.StoreRecordsIF;
import com.relationaljunction.database.StoreTableIF;
import com.relationaljunction.database.dbf.DBFInfo;
import com.relationaljunction.database.io.LocalFileManager;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 2.2
 */

public class DBFTableWriter
        extends AbstractStoreTableWriter {
   private final Logger log = LoggerFactory.getLogger("DBFTableWriter");

   private DBFTable dbfTable = null;
   private DBF writer = null;
   private StoreFieldIF[] storeFields = null;
   private Date memoFileModificationDate = null;
   private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd");

   DBFTableWriter(DBFTable dbfTable, StoreFieldIF[] storeFields) throws
           StoreException {
      this.dbfTable = dbfTable;
      try {
         // ##### open a file #####
         if (storeFields == null) {
            // don't destroy data, just open EXISTING file
            writer = new DBF(dbfTable.fm.getPath(),
                    dbfTable.schema.charset);
            initFields();

            if (dbfTable.memoFm != null)
               memoFileModificationDate = dbfTable.memoFm.getModificationTime();

//        if (dbfTable.schema.isLockEnabled())
//          writer.lock();

         }
         // ##### create a new file #####
         else {
            createDBFFile(dbfTable, storeFields);
         }

         // use index
//      if (dbfTable.schema.useTag != null)
//        writer.useTag(dbfTable.schema.useTag);
//      if (ndxFm.exists())
//        writer.useIndex(ndxFm.getPath());
      } catch (Exception ex) {
         ex.printStackTrace();
         throw new StoreException("Error writing the file " + dbfTable.tableName +
                 ": [XBaseJ] " + ex.getMessage(), ex);
      }
   }

   private void createDBFFile(DBFTable dbfTable, StoreFieldIF[] storeFields) throws Exception {
      dbfTable.fm = (LocalFileManager) dbfTable.dir.getFileManager(dbfTable.tableName);

      // destroy data = > delete a file and create it anew
      writer = new DBF(dbfTable.fm.getPath(), dbfTable.schema.format, true,
              dbfTable.schema.charset);

//        if (dbfTable.schema.isLockEnabled())
//          writer.lock();

      this.storeFields = storeFields;

      // add fields
      for (StoreFieldIF storeField : storeFields) {
         if (storeField.getName().length() > 10)
            throw new StoreException("Invalid field '" + storeField.getName() +
                    "'. Field name should be of length 1-10.");
         if (storeField.getType() == StoreDataType.VARCHAR &&
                 storeField.getLength() > 255)
            throw new StoreException("Invalid field '" + storeField.getName() +
                    "'. The maximum length for CHARACTER fields in the DBF format is 255 characters.");
         Field f = getDBFField(storeField);
         writer.addField(f);
      }

      // if a memo was created, set a reference on it
      if (dbfTable.fm.getDir().exists(dbfTable.memoFile))
         dbfTable.memoFm = (LocalFileManager) dbfTable.fm.getDir().getFileManager(dbfTable.memoFile);

      dbfTable.containsDeletedRecords = DBFTable.CONTAINS_DELETED_RECS_NO;
   }

   private void initFields() throws StoreException {
      try {
         storeFields = new StoreFieldIF[writer.getFieldCount()];

         for (int i = 0; i < storeFields.length; i++) {
            Field field = writer.getField(i + 1);
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

   public void clearRecords() throws StoreException {
      try {
         DBFInfo dbfInfo = new DBFInfo(dbfTable.fm.getPath(), false, dbfTable.schema.lockFiles);
         dbfInfo.clearRecords();
         dbfInfo.close();

         if (dbfTable.memoFm != null && dbfTable.memoFm.exists()) {
            FileOutputStream fos = new FileOutputStream(dbfTable.memoFm.getFile(), false);
            fos.close();
         }

         // reload a file
         writer = new DBF(dbfTable.fm.getPath(),
                 dbfTable.schema.charset);

         dbfTable.containsDeletedRecords = DBFTable.CONTAINS_DELETED_RECS_NO;
      } catch (Exception ex) {
//      ex.printStackTrace();
         throw new StoreException("Can't clear a table '" + dbfTable.getName() +
                 "'. [DBFTableWriter] Error was: " + ex.getMessage(), ex);
      }
   }

   // inserts record
   public void insertRecords(StoreRecordsIF recs) throws StoreException {
      dbfTable.needsEOFCorrection = true;
      try {
         recs.beforeFirst();
         while (recs.hasNext()) {
            StoreRecordIF rec = recs.nextRecord();

            setValues(rec);

            if (dbfTable.schema.isLockEnabled())
               writer.write(true);
            else
               writer.write();
         }
      } catch (Exception ex) {
//      ex.printStackTrace();
         throw new StoreException("Can't insert a record in the file '" +
                 dbfTable.fm.getPath() + "'. [XBaseJ] " +
                 ex.getMessage(), ex);
      }
   }

   // param: recs should contains ID of record to be updated
   // and also new values(objects) for this record
   @Deprecated
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
      int curPos = 1, realPos = 0;
      StoreRecordIF rec = null;
      try {
         recs.beforeFirst();
         while (recs.hasNext()) {
            rec = recs.nextRecord();
            int id = rec.getID();

            if (id < 0)
               throw new StoreException("The record has incorrect ID: " + id);
            else if ((id + 1) < realPos) {
               curPos = 1;
               realPos = 0;
            } else if ((id + 1) == realPos) {
               writer.gotoRecord(curPos - 1);
            }

            // searches a record till realPos = curPos.
            // Then record to be updated is found
            while (realPos < (id + 1)) {
               // go to the current record position
               writer.gotoRecord(curPos);

               curPos++;
               if (!writer.deleted()) {
                  realPos++;
               }
            }

            // update a record found
            setValues(rec);

            if (dbfTable.schema.isLockEnabled())
               writer.update(true);
            else
               writer.update();
         }
      } catch (Exception ex) {
         StringBuffer recStr = new StringBuffer("null");
         if (rec != null) {
            recStr = new StringBuffer();
            for (int i = 0; i < rec.getObjects().length; i++) {
               recStr.append(rec.getObject(i));
               if (i < rec.getObjects().length - 1) recStr.append(",");
            }
         }
         throw new StoreException("Can't update the record with number " + curPos +
                 " in the file '" +
                 dbfTable.fm.getPath() + "'. [XBaseJ] " +
                 ex.getMessage() + ". New objects was :" + recStr, ex);
      }
   }

   @Deprecated
   private void updateRecordsWithNoDeletedRecordsExist(StoreRecordsIF recs) throws StoreException {
      try {
         recs.beforeFirst();

         while (recs.hasNext()) {
            StoreRecordIF rec = recs.nextRecord();
            int id = rec.getID();
            if (id < 0)
               throw new StoreException("The record has incorrect ID: " + id);

            writer.gotoRecord(id + 1);

            setValues(rec);

            // update the current record
//	if (OtherUtils.TRACE_ENABLED &&
//	    log.isTraceEnabled())
//          log.trace("DBFTableWriter: updating row " + count++);

            if (dbfTable.schema.isLockEnabled())
               writer.update(true);
            else
               writer.update();
         }
      } catch (Exception ex) {
         throw new StoreException("Can't update a record in the file '" +
                 dbfTable.fm.getPath() + "'. [XBaseJ] " +
                 ex.getMessage(), ex);
      }
   }

   /**
    * update records in an external file via "operation log"
    * @param pst - preparedStatement that returns all rows contained in "operation log" for this table
    * @throws StoreException
    */
   public void updateRecords(PreparedStatement pst) throws
           StoreException {
      try {
         ResultSet rsOperations = pst.executeQuery();

         // get the count of operations in "operation log"
         rsOperations.last();
         int operationEndIndex = rsOperations.getRow();
         if (operationEndIndex == 0) return;

         for (int i = 1; i <= writer.getRecordCount(); i++) {
            writer.gotoRecord(i);

            // is the record deleted in a DBF/VFP file
            if (writer.deleted())
               continue;

            Object[] objs = getRowObjects();

            rsOperations.absolute(1);

            // compare the current record in an external DBF/VFP file
            // with the records that were changed (aka "operation log")

            // loop on records in "operation log"
            while (rsOperations.getRow() != 0) {
               // compare the current record to the current operation record
               boolean isRowsEqual = compareCurrentRow(objs, rsOperations);

               if (isRowsEqual) {
                  // records are equal
                  // read a new data for the record
                  rsOperations.relative(1);

                  // update the current record
//            if (OtherUtils.TRACE_ENABLED &&
//                log.isTraceEnabled())
//              log.trace("DBFTableWriter: updating row " + i);

                  // update the record in an external file
                  updateCurrentRow(rsOperations);

                  operationEndIndex -= 2;
                  if (operationEndIndex == 0)
                     // all records have been updated
                     return;

                  break;
               } else
                  // move to the next operation to compare
                  rsOperations.relative(2);
            }
         }
      } catch (Exception ex) {
         ex.printStackTrace();
         throw new StoreException("Can't update a record in the table '" +
                 dbfTable.getName() + "' [Jackcess] " +
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

         for (int i = 1; i <= writer.getRecordCount(); i++) {
            writer.gotoRecord(i);

            if (writer.deleted())
               continue;

            Object[] objs = getRowObjects();

            rsOperations.absolute(1);

            while (rsOperations.getRow() != 0) {
               // compare the current record to the current operation record
               boolean isRowsEqual = compareCurrentRow(objs, rsOperations);
               if (isRowsEqual) {
                  // delete the current record
                  writer.delete();

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
                 dbfTable.getName() + "' [Jackcess] " +
                 ex.getMessage(), ex);
      }
   }

   private Object[] getRowObjects() throws Exception {
      Object[] objs = new Object[storeFields.length];

      for (int i = 0; i < storeFields.length; i++) {
         objs[i] = dbfTable.getJDBCObject(storeFields[i], writer.getField(i + 1).get(), dateFormatter);
      }

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

         if (dbfValue != null && value2compare == null || dbfValue == null
                 || !dbfValue.equals(value2compare))
            return false;
      }

      return true;
   }

   private void updateCurrentRow(ResultSet rsOperations) throws
           Exception {
      for (int i = 1; i <= storeFields.length; i++) {
         Field field = writer.getField(i);
         putDBFObject(storeFields[i - 1], field, rsOperations.getObject(i + 3));
      }

      if (dbfTable.schema.isLockEnabled())
         writer.update(true);
      else
         writer.update();
   }

   private void setValues(StoreRecordIF rec) throws Exception {
      for (int j = 1; j <= rec.getSize(); j++) {
         Field field = writer.getField(j);
//      String str = getDBFObject(rec.getFields()[j - 1],
//                                rec.getObject(j - 1));
//      field.put(str);
         putDBFObject(rec.getFields()[j - 1], field, rec.getObject(j - 1));
      }
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
      int curPos = 1, realPos = 0;
      try {
         recs.beforeFirst();
         while (recs.hasNext()) {
            StoreRecordIF rec = recs.nextRecord();
            int id = rec.getID();

            if (id < 0)
               throw new StoreException("The record has incorrect ID: " + id);
            else if ((id + 1) < realPos) {
               curPos = 1;
               realPos = 0;
            } else if ((id + 1) == realPos) {
               writer.gotoRecord(curPos - 1);
            }

            /**
             searches a record till realPos = curPos. Then record to be deleted is found
             **/
            while (realPos < (id + 1)) {
               writer.gotoRecord(curPos);

               curPos++;
               if (!writer.deleted()) {
                  realPos++;
               }
            }

            // delete a record found
            writer.delete();
            dbfTable.containsDeletedRecords = DBFTable.CONTAINS_DELETED_RECS_YES;
         }
      } catch (Exception ex) {
//      System.out.println(id);
//      ex.printStackTrace();
         throw new StoreException("Can't delete a record in the file '" +
                 dbfTable.fm.getPath() + "'. [XBaseJ] " +
                 ex.getMessage(), ex);
      }
   }

   private void deleteRecordsWithNoDeletedRecordsExist(StoreRecordsIF recs) throws StoreException {
      int id;
      try {
         while (recs.hasNext()) {
            StoreRecordIF rec = recs.nextRecord();
            id = rec.getID();
            if (id < 0)
               throw new StoreException("The record has incorrect ID: " + id);

            writer.gotoRecord(id + 1);

            writer.delete();
            dbfTable.containsDeletedRecords = DBFTable.CONTAINS_DELETED_RECS_YES;
         }
      } catch (Exception ex) {
//      System.out.println(id);
//      ex.printStackTrace();
         throw new StoreException("Can't delete a record in the file '" +
                 dbfTable.fm.getPath() + "'. [XBaseJ] " +
                 ex.getMessage(), ex);
      }
   }

   public void pack() {
      if (dbfTable.containsDeletedRecords == DBFTable.CONTAINS_DELETED_RECS_NO)
         return;

      try {
         writer.pack();
         dbfTable.needsEOFCorrection = true;
         dbfTable.containsDeletedRecords = DBFTable.CONTAINS_DELETED_RECS_NO;
      } catch (Exception ex) {
         throw new RuntimeException("Can't pack a DBF file '" + dbfTable.tableName +
                 "'. Error was: " + ex.getMessage());
      }
   }

   public void close() {
      try {
         if (dbfTable.schema.packDBF)
            pack();

         writer.close();

         if (dbfTable.needsEOFCorrection) {
            // trim a dbf file to the proper size, because the XBase library adds superfluos data
            // to the end of file for unknown reason
            DBFInfo dbfFile = new DBFInfo(dbfTable.fm.
                    getPath(), false, dbfTable.schema.lockFiles);
            long size = dbfFile.getSize();
            long trueSize = dbfFile.getTrueSize();
            if (size != trueSize)
               dbfFile.trimToSize(trueSize);
            dbfFile.close();
            dbfTable.needsEOFCorrection = false;
         }

         // upload a memo file if it exists and its modification time is NULL (it didn't exist before)
         // or its modification time is changed.
         // thus unchanged memo file will not be uploaded.
         if (dbfTable.memoFm != null &&
                 dbfTable.memoFm.exists() &&
                 (memoFileModificationDate == null
                         || dbfTable.memoFm.getModificationTime().after(memoFileModificationDate)))
            dbfTable.dir.upload(dbfTable.memoFm);

         // upload DBF file
         dbfTable.dir.upload(dbfTable.fm);

         dbfTable = null;
         writer = null;
      } catch (Exception ex) {
         ex.printStackTrace();
         throw new com.relationaljunction.utils.UnexpectedException(ex);
      }
   }

   // SQLEngine -> DBF
   static Field getDBFField(StoreFieldIF storeField) throws xBaseJException,
           java.io.IOException {

      Field field;
      int length = storeField.getLength();
      int decimalCount = storeField.getDecimalCount();

      switch (storeField.getType()) {
         case INTEGER: {
            if (length == -1)
               length = DBFTable.DEFAULT_INT_LENGTH;
            else
               length = length > DBFTable.INT_LIMIT ?
                       DBFTable.INT_LIMIT : length;

            field = new NumField(storeField.getName(), length, 0);
            break;
         }
         case BIGINT: {
            if (length == -1)
               length = DBFTable.DEFAULT_BIGINT_LENGTH;
            else
               length = length <= DBFTable.INT_LIMIT ?
                       DBFTable.DEFAULT_BIGINT_LENGTH : length;

            field = new NumField(storeField.getName(), length, 0);
            break;
         }
         case FLOAT:
         case DOUBLE:
         case NUMERIC: {
            if (length == -1)
               length = DBFTable.DEFAULT_DOUBLE_LENGTH;
            if (decimalCount == -1)
               decimalCount = DBFTable.DEFAULT_DOUBLE_DECIMAL_COUNT;

//        field = new FloatField(storeField.getName(), length, decimalCount);
            field = new NumField(storeField.getName(), length, decimalCount);
            break;
         }
         case TIMESTAMP:
         case TIME:
         case DATE: {
            field = new DateField(storeField.getName());
            break;
         }
         case VARCHAR: {
            if (length == -1)
               length = DBFTable.DEFAULT_STRING_LENGTH;

            field = new CharField(storeField.getName(), length);
            break;
         }
         case BOOLEAN: {
            field = new LogicalField(storeField.getName());
            break;
         }
         case LONGVARCHAR: {
            field = new MemoField(storeField.getName());
            break;
         }
         default: {
            if (length == -1)
               length = DBFTable.DEFAULT_STRING_LENGTH;

            field = new CharField(storeField.getName(), length);
         }
      }

      return field;
   }

   private static String padString(String str, int stringSize, char padChar) {
      if (stringSize <= 0)
         return str;

      StringBuilder result = new StringBuilder(str);

      if (str != null && str.length() > stringSize)
         return str.substring(0, stringSize);

      while (result.length() < stringSize)
         result.append(padChar);

      return result.toString();
   }

   // JDBC -> DBF
   public void putDBFObject(StoreFieldIF storeField, Field dbfField, Object obj) throws
           Exception {
      // NULL values
      if (obj == null) {
         if (dbfTable.schema.padChar != '\0' &&
                 (storeField.getType() == StoreDataType.VARCHAR ||
                         storeField.getType() == StoreDataType.LONGVARCHAR))
            dbfField.put(padString("", storeField.getLength(), ' '));
         else
            dbfField.put("");
      }

//    if (obj == null && dbfTable.schema.padChar != '\0' &&
//        (storeField.getType() == StoreDataType.VARCHAR ||
//         storeField.getType() == StoreDataType.LONGVARCHAR))
//      dbfField.put(padString("", storeField.getLength(), ' '));
//    else if (obj == null) dbfField.put("");

      // INTEGER type
      else if (storeField.getType() == StoreDataType.INTEGER) {
//      ( (NumField) dbfField).put( ( (Integer) obj).intValue());
         String valueStr = String.valueOf(obj);
         dbfField.put(valueStr);
      }
      // BIGINT type
      else if (storeField.getType() == StoreDataType.BIGINT) {
//      ( (NumField) dbfField).put( ( (Long) obj).longValue());
         dbfField.put(String.valueOf(obj));
      }
      // TIMESTAMP type
      else if (storeField.getType() == StoreDataType.TIMESTAMP) {
//      ((DateField)dbfField).put((java.util.Date) obj);
         dbfField.put(dateFormatter.format((Date) obj));
      }
      // BOOLEAN type
      else if (storeField.getType() == StoreDataType.BOOLEAN) {
         ((LogicalField) dbfField).put((Boolean) obj);
      }
      // FLOAT, DOUBLE
      else if (storeField.getType() == StoreDataType.FLOAT ||
              storeField.getType() == StoreDataType.DOUBLE) {

//      StringBuffer decFormat = (StringBuffer) decFormats.get(storeField.getName());
//      if (decFormat == null) {
//        decFormat = new StringBuffer("#");
//        for (int i = 0; i < storeField.getDecimalCount(); i++) {
//          if (i == 0)
//            decFormat.append(".");
//          decFormat.append("#");
//        }
//        decFormats.put(storeField.getName(), decFormat);
//      }
//      DecimalFormat df = new DecimalFormat(decFormat.toString(), DBFTable.dcs);
//      String formattedDouble = df.format(obj);
//      dbfField.put(formattedDouble);

         dbfField.put(com.relationaljunction.database.dbf.DBFUtils.doubleFormating(((Number) obj).
                 doubleValue(),
                 dbfTable.schema.charset,
                 dbfField.getLength(),
                 dbfField.getDecimalPositionCount()));

         // ---- bug with a double value in XBaseJ
//      if (dbfField.getJdbcType() == 'F')
//        ( (FloatField) dbfField).put( ( (Double) obj).doubleValue());
//      else if (dbfField.getJdbcType() == 'N')
//        ( (NumField) dbfField).put( ( (Double) obj).doubleValue());
//      else throw new Exception("Can't insert a double value to the field " +
//                               dbfField.getName());
      }

      // BIGDECIMAL (NUMERIC) type
      else if (storeField.getType() == StoreDataType.NUMERIC) {
//      dbfField.put(com.relationaljunction.database.dbf.Utils.doubleFormating( ( (BigDecimal) obj).
//          doubleValue(),
//          dbfTable.schema.charset,
//          dbfField.getLength(),
//          dbfField.getDecimalPositionCount()));

         dbfField.put(obj.toString().getBytes());
      }
      // VARCHAR, LONGVARCHAR and other types
      else {
         dbfField.put(padString(obj.toString(), storeField.getLength(),
                 dbfTable.schema.padChar));
      }
   }

}
