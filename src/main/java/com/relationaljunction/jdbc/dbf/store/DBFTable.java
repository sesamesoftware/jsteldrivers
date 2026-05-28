package com.relationaljunction.jdbc.dbf.store;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xBaseJ.xBaseJException;
import org.xBaseJ.fields.Field;
import org.xBaseJ.DBFTypes;

import com.relationaljunction.database.AbstractStoreSchema;
import com.relationaljunction.database.AbstractStoreTable;
import com.relationaljunction.database.DefaultStoreField;
import com.relationaljunction.database.StoreDataType;
import com.relationaljunction.database.StoreException;
import com.relationaljunction.database.StoreFieldIF;
import com.relationaljunction.database.StoreTableReaderIF;
import com.relationaljunction.database.StoreTableWriterIF;
import com.relationaljunction.database.dbf.DBFField;
import com.relationaljunction.database.dbf.DBFHeader;
import com.relationaljunction.database.dbf.DBFInfo;
import com.relationaljunction.database.index.IndexTableIF;
import com.relationaljunction.database.io.DirectoryManager;
import com.relationaljunction.database.io.LocalFileManager;
import com.relationaljunction.utils.OtherUtils;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 2.2
 */

public class DBFTable extends AbstractStoreTable {
   private final Logger log = LoggerFactory.getLogger("DBFTable");

   private final static String FPT_EXTENSION = ".fpt";
   private final static String DBT_EXTENSION = ".dbt";

   // limit of INTEGER type
   final static int INT_LIMIT = 9;
   // default values of types if they are not specified
   final static int DEFAULT_INT_LENGTH = 6;
   final static int DEFAULT_BIGINT_LENGTH = 12;
   final static int DEFAULT_DOUBLE_LENGTH = 10;
   final static int DEFAULT_DOUBLE_DECIMAL_COUNT = 2;
   final static int DEFAULT_STRING_LENGTH = 20;
   // values for containsDeletedRecords
   final static int CONTAINS_DELETED_RECS_UNKNOWN = 0;
   final static int CONTAINS_DELETED_RECS_YES = 1;
   final static int CONTAINS_DELETED_RECS_NO = 2;
   final static int CONTAINS_DELETED_RECS_DEFAULT =
           CONTAINS_DELETED_RECS_UNKNOWN;

   String tableName = null;
   DBFSchema schema = null;
   DirectoryManager dir = null;
   LocalFileManager fm = null;
   LocalFileManager memoFm = null;

   // memo file name
   String memoFile = null;
   boolean needsEOFCorrection = true;
   int containsDeletedRecords = CONTAINS_DELETED_RECS_DEFAULT;
   int dbfSignature = 0;

   // pattern for boolean true value
   Pattern trueValuePattern = AbstractStoreSchema.DEFAULT_TRUE_VALUE_PATTERN;

   public DBFTable(String tableName, DBFSchema schema, DirectoryManager dir) {
//    this.filePath = schema.path + tableName;
      this.tableName = tableName;
      this.schema = schema;
      this.dir = dir;
   }

   private void initDBFParameters() throws StoreException {
      try {
         fm = (LocalFileManager) dir.getFileManager(tableName);

         // check if a file exists
         if (!fm.exists()) {
            String exceptionMessage = "File '" + fm.getPath() + "' does not exist.";

            if (tableName.equalsIgnoreCase("DBF.DBF"))
               exceptionMessage += " Check your table name in a SQL query. " +
                       "A table name with a file extension must be in double quotes, " +
                       "i.e.: SELECT * FROM \"test.dbf\", but not SELECT * FROM test.dbf";
            throw new StoreException(exceptionMessage);
         }

         // get a dbf signature
         DBFInfo dbfInfo = new DBFInfo(fm.getPath(), true, schema.lockFiles);
         dbfSignature = dbfInfo.getSignature();

         if (schema.memoExtension == null) {
            // resolve a memo extension
            if (dbfSignature == DBFHeader.SIG_VFP ||
                    dbfSignature == DBFHeader.SIG_VFP_WITH_AUTOINCREMENT ||
                    dbfSignature == DBFHeader.SIG_FOXPRO_WITH_MEMO) {
               memoFile = com.relationaljunction.utils.StringUtils.getFileNameWithoutExtension(
                       tableName) + FPT_EXTENSION;
            } else {
               memoFile = com.relationaljunction.utils.StringUtils.getFileNameWithoutExtension(
                       tableName) + DBT_EXTENSION;
            }
         } else {
            // use an user-defined memo extension
            memoFile = com.relationaljunction.utils.StringUtils.
                    getFileNameWithoutExtension(tableName) + schema.memoExtension;
         }

         // getting a related memo file if exists
         if (dir.exists(memoFile)) {
            memoFm = (LocalFileManager) dir.getFileManager(memoFile);
         } else {
            OtherUtils.writeTraceInfo(log, "memo file '" + memoFile +
                    "' was not found in '" + dir.getPath() + "'");
         }

         dbfInfo.close();
      } catch (Exception ex) {
         throw new StoreException(ex);
      }
   }

   public java.util.Date refreshModificationDate() throws StoreException {
      try {
         fileModificationDate = dir.getFileModificationDate(tableName);
      } catch (Exception ex) {
         ex.printStackTrace();
         throw new StoreException(ex);
      }

      return fileModificationDate;
   }

   public void create(StoreFieldIF[] fields, IndexTableIF[] indexTables) throws StoreException {
//    if (new File(filePath).exists())
//      throw new StoreException("File '" + filePath +
//                               "' already exists");

      if (tableName.equalsIgnoreCase("DBF.DBF"))
         throw new StoreException("Bad table name. Check your table name in a SQL query. " +
                 "A table name with a file extension must be in double quotes, " +
                 "i.e.: SELECT * FROM \"test.dbf\", but not SELECT * FROM test.dbf");

      if (dir.exists(tableName))
         throw new StoreException("File '" + tableName +
                 "' already exists");
      try {
         // create a file
         dir.createFile(tableName);
      } catch (Exception ex) {
         throw new StoreException("Can't create the file '" + tableName +
                 "'. Error was: " + ex.getMessage(), ex);
      }

      // just created file does not contain deleted records
      containsDeletedRecords = CONTAINS_DELETED_RECS_NO;

      if (schema.memoExtension != null)
         // use user-defined memo extension
         memoFile = com.relationaljunction.utils.StringUtils.
                 getFileNameWithoutExtension(tableName) + schema.memoExtension;

      if (schema.format == DBFTypes.VISUAL_FOXPRO) {
         if (schema.memoExtension == null)
            memoFile = com.relationaljunction.utils.StringUtils.getFileNameWithoutExtension(
                    tableName) + FPT_EXTENSION;

         VFPTableWriter writer = new VFPTableWriter(this, fields);
         writer.close();
         dbfSignature = DBFHeader.SIG_VFP;
      } else {
         if (schema.memoExtension == null)
            memoFile = com.relationaljunction.utils.StringUtils.getFileNameWithoutExtension(
                    tableName) + DBT_EXTENSION;

         DBFTableWriter writer = new DBFTableWriter(this, fields);
         writer.close();
      }
   }

   public void pack() throws StoreException {
      try {
         StoreTableWriterIF writer = getWriter(null);
         writer.pack();
         writer.close();
      } catch (StoreException ex) {
         throw new StoreException(ex);
      }
   }

   public java.util.Date getModificationDate() {
      return null;
   }

   public StoreTableReaderIF getReader() throws StoreException {
      StoreTableReaderIF tableReader;

      if (dbfSignature == 0)
         // set dbf signature (version) and other parameters
         initDBFParameters();

      if (dbfSignature == DBFHeader.SIG_DBASE_III_WITH_MEMO ||
              dbfSignature == DBFHeader.SIG_DBASE_III_WITHOUT_MEMO ||
              dbfSignature == DBFHeader.SIG_DBASE_IV_WITH_MEMO_2 ||
              dbfSignature == DBFHeader.SIG_DBASE_IV_WITHOUT_MEMO ||
              dbfSignature == DBFHeader.SIG_FOXPRO_WITH_MEMO)
         // DBF III/IV, FoxPro
         tableReader = new DBFTableReader(this);
      else
         // Visual FoxPro and others
         tableReader = new VFPTableReader(this);

      return tableReader;
   }

   public StoreTableWriterIF getWriter(StoreFieldIF[] fields) throws
           StoreException {
      StoreTableWriterIF writer;

      // init parameteres it they are not set before
      if (dbfSignature == 0)
         // set dbf signature (version)
         initDBFParameters();

      //      if (fields != null) {
      //        // clear a DBF file
      //        FileManager fm = dir.getFileManager(tableName);
      //        DBFInfo dbfInfo = new DBFInfo(fm.getPath());
      //        dbfInfo.clearRecords();
      //        dbfInfo.close();
      //        fm.close();
      //      }

      if (dbfSignature == DBFHeader.SIG_DBASE_III_WITH_MEMO ||
              dbfSignature == DBFHeader.SIG_DBASE_III_WITHOUT_MEMO ||
              dbfSignature == DBFHeader.SIG_DBASE_IV_WITH_MEMO_2 ||
              dbfSignature == DBFHeader.SIG_DBASE_IV_WITHOUT_MEMO ||
              dbfSignature == DBFHeader.SIG_FOXPRO_WITH_MEMO) {
         // DBF III/IV, FoxPro
         writer = new DBFTableWriter(this, null);
      } else {
         // Visual FoxPro and others
         writer = new VFPTableWriter(this, null);
      }

      if (fields != null)
         writer.clearRecords();

      return writer;
   }

//   public LockFile lock(File f) throws Exception {
//      LockFile fl;
//      FileOutputStream fos = new FileOutputStream(f, true);
//      int time = 0;
//
//      while ((fl = fos.getChannel().tryLock()) == null) {
//         Thread.sleep(schema.lockCheckTime);
//         System.out.println("waiting... time = " + time);
//         time += schema.lockCheckTime;
//         if (schema.lockTimeout != 0 && time > schema.lockTimeout) throw new Exception(
//                 "Timeout while waiting for unlocking the file " + f.getName());
//      }
//
//      return fl;
//   }

   // DBF Object -> JDBC Object
   Object getJDBCObject(StoreFieldIF storeField,
                        String str,
                        SimpleDateFormat dateFormatter) throws
           StoreException {
      // NULL value
      // empty numbers and dates always treated as NULL
      // empty string is treated as NULL when 'emptyStringAsNull' = true only
      if (str.trim().isEmpty() &&
              (schema.emptyStringAsNull ||
                      (!schema.emptyStringAsNull &&
                              storeField.getType() != StoreDataType.LONGVARCHAR &&
                              storeField.getType() != StoreDataType.VARCHAR))) {
         return null;
      }
      // TIMESTAMP type
      else if (storeField.getType() == StoreDataType.TIMESTAMP) {
         try {
            return dateFormatter.parse(str.trim());
         } catch (Throwable ex) {
            return null;
//        throw new StoreException("Can't convert {" + str + "} to date type");
         }
      }
      // INTEGER type
      else if (storeField.getType() == StoreDataType.INTEGER) {
         try {
            return Integer.valueOf(str.trim());
         } catch (NumberFormatException ex1) {
            int ivalue;
            try {
               ivalue = Double.valueOf(str).intValue();
            } catch (Exception ex2) {
               return null;
//            throw new StoreException("Can't convert the value '" +
//                                     dbfValue.toString() +
//                                     "' to Integer");
            }
            return ivalue;
         }
      }
      // BIGINT type
      else if (storeField.getType() == StoreDataType.BIGINT) {
         try {
            return Long.valueOf(str.trim());
         } catch (NumberFormatException ex1) {
            long lvalue;
            try {
               lvalue = Double.valueOf(str).longValue();
            } catch (Exception ex2) {
               return null;
//            throw new StoreException("Can't convert the value '" +
//                                     dbfValue.toString() +
//                                     "' to Long");
            }
            return lvalue;
//        throw new StoreException("Can't convert {" + str + "} to bigint type");
         }
      }
      // FLOAT type (not used in the driver)
//    else if (storeField.getSQLType() == StoreDataType.FLOAT) {
//      try {
//        return new Float(str.trim());
//      }
//      catch (NumberFormatException ex1) {
//        return null;
//        throw new StoreException("Can't convert {" + str + "} to float type");
//      }
//    }
      // DOUBLE type
      else if (storeField.getType() == StoreDataType.DOUBLE) {
         try {
            return new Double(str.trim());
         } catch (NumberFormatException ex1) {
            return null;
//        throw new StoreException("Can't convert {" + str + "} to double type");
         }
      }
      // BIGDECIMAL (NUMERIC) type
      else if (storeField.getType() == StoreDataType.NUMERIC) {
         try {
            return new BigDecimal(str.trim());
         } catch (NumberFormatException ex1) {
            return null;
//        throw new StoreException("Can't convert {" + str + "} to double type");
         }
      }
      // BOOLEAN type
      else if (storeField.getType() == StoreDataType.BOOLEAN) {
         if (trueValuePattern.matcher(str.trim()).matches())
            return Boolean.TRUE;
         else
            return Boolean.FALSE;
      }
//    else if (storeField.getSQLType() == StoreDataType.LONGVARCHAR) {
//      return new com.relationaljunction.database.types.MemoType(str);
//    }
      // VARCHAR and other types
      else if (schema.trimBlanks)
         return com.relationaljunction.utils.StringUtils.rightTrim(str);

      return str;
   }

   // DBF Field -> Store API Field
   StoreFieldIF getStoreField(Field field) throws xBaseJException {
      DefaultStoreField storeField = new DefaultStoreField(field.getName());

      switch (field.getType()) {
         case 'N': {
            StringBuilder maxNumberConstraint = new StringBuilder();
            StringBuilder minNumberConstraint;

            // if not integer, consider this value as DOUBLE
            if (field.getDecimalPositionCount() > 0) {
               if (schema.useBigDecimalType)
                  storeField.setType(StoreDataType.NUMERIC);
               else
                  storeField.setType(StoreDataType.DOUBLE);

               // calculate max number allowed for this column
               if (field.getLength() - field.getDecimalPositionCount() - 1 == 0)
                  maxNumberConstraint.append("0");
               else for (int i = 0;
                         i < field.getLength() - field.getDecimalPositionCount() - 1; i++)
                  maxNumberConstraint.append("9");
               maxNumberConstraint.append(".");
               for (int i = 0; i < field.getDecimalPositionCount(); i++)
                  maxNumberConstraint.append("9");

               // calculate min number allowed for this column
               if (field.getLength() <= 2)
                  minNumberConstraint = new StringBuilder("0");
               else if (field.getLength() == 3 && field.getDecimalPositionCount() == 1)
                  minNumberConstraint = new StringBuilder("-9");
//          else if (field.getLength() == 3 && field.getDecimalPositionCount() == 2)
//            minNumberConstraint = new StringBuffer("-0.9");
               else {
                  minNumberConstraint = new StringBuilder("-");
                  for (int i = 0;
                       i < field.getLength() - field.getDecimalPositionCount() - 2;
                       i++)
                     minNumberConstraint.append("9");
                  minNumberConstraint.append(".");
                  for (int i = 0; i < field.getDecimalPositionCount(); i++)
                     minNumberConstraint.append("9");
               }
            } else {
               // int types
               if (field.getLength() > DBFTable.INT_LIMIT)
                  storeField.setType(StoreDataType.BIGINT);
               else
                  storeField.setType(StoreDataType.INTEGER);

               // calculate max number allowed for this column
               for (int i = 0; i < field.getLength(); i++)
                  maxNumberConstraint.append("9");

               // calculate min number allowed for this column
               if (field.getLength() == 1)
                  minNumberConstraint = new StringBuilder("0");
               else {
                  minNumberConstraint = new StringBuilder("-");
                  for (int i = 0; i < field.getLength() - 1; i++)
                     minNumberConstraint.append("9");
               }
            }

            storeField.setLength(field.getLength());
            storeField.setDecimalCount(field.getDecimalPositionCount());
            String minCheckCondition = com.relationaljunction.utils.StringUtils.
                    quoteReservedFieldAndTableName(field.getName()) +
                    " >= " + minNumberConstraint;
            String maxCheckCondition = com.relationaljunction.utils.StringUtils.
                    quoteReservedFieldAndTableName(field.getName()) +
                    " <= " + maxNumberConstraint;

            if (schema.checkColumnSize)
               storeField.setCheckCondition("CHECK " + minCheckCondition + " AND " +
                       maxCheckCondition);
            break;
         }
         case 'F': {
            if (schema.useBigDecimalType)
               storeField.setType(StoreDataType.NUMERIC);
            else
               storeField.setType(StoreDataType.DOUBLE);
            storeField.setLength(field.getLength());
            storeField.setDecimalCount(field.getDecimalPositionCount());
            break;
         }
         case 'D': {
            storeField.setType(StoreDataType.TIMESTAMP);
            storeField.setLength(field.getLength());
            storeField.setDecimalCount(field.getDecimalPositionCount());
            break;
         }
         case 'L': {
            storeField.setType(StoreDataType.BOOLEAN);
            storeField.setLength(field.getLength());
            storeField.setDecimalCount(field.getDecimalPositionCount());
            break;
         }
         case 'M': {
//        storeField.setType(StoreDataType.LONGVARCHAR);
            storeField.setType(StoreDataType.VARCHAR);
            break;
         }
         case 'G':
         case 'P': {
            storeField.setType(StoreDataType.BLOB);
            break;
         }
         default: {
            storeField.setType(StoreDataType.VARCHAR);
            storeField.setLength(field.getLength());
            storeField.setDecimalCount(field.getDecimalPositionCount());
         }
      }

      return storeField;
   }

   // VFP Field -> Store API Field
   StoreFieldIF getStoreField(DBFField field) throws Exception {
      DefaultStoreField storeField = new DefaultStoreField(field.getName());

      switch (field.getDataType()) {
         case DBFField.FIELD_TYPE_NUMERIC: {
            StringBuilder maxNumberConstraint = new StringBuilder();
            StringBuilder minNumberConstraint;

            // if not integer, consider this value as DOUBLE
            if (field.getDecimalCount() > 0) {
               if (schema.useBigDecimalType)
                  storeField.setType(StoreDataType.NUMERIC);
               else
                  storeField.setType(StoreDataType.DOUBLE);

               // calculate max number allowed for this column
               if (field.getFieldLength() - field.getDecimalCount() - 1 == 0)
                  maxNumberConstraint.append("0");
               else for (int i = 0;
                         i < field.getFieldLength() - field.getDecimalCount() - 1; i++)
                  maxNumberConstraint.append("9");
               maxNumberConstraint.append(".");
               for (int i = 0; i < field.getDecimalCount(); i++)
                  maxNumberConstraint.append("9");

               // calculate min number allowed for this column
               if (field.getFieldLength() <= 2)
                  minNumberConstraint = new StringBuilder("0");
               else if (field.getFieldLength() == 3 && field.getDecimalCount() == 1)
                  minNumberConstraint = new StringBuilder("-9");
//          else if (field.getLength() == 3 && field.getDecimalPositionCount() == 2)
//            minNumberConstraint = new StringBuffer("-0.9");
               else {
                  minNumberConstraint = new StringBuilder("-");
                  for (int i = 0;
                       i < field.getFieldLength() - field.getDecimalCount() - 2;
                       i++)
                     minNumberConstraint.append("9");
                  minNumberConstraint.append(".");
                  for (int i = 0; i < field.getDecimalCount(); i++)
                     minNumberConstraint.append("9");
               }
            } else {
               // int types
               if (field.getFieldLength() > DBFTable.INT_LIMIT)
                  storeField.setType(StoreDataType.BIGINT);
               else
                  storeField.setType(StoreDataType.INTEGER);

               // calculate max number allowed for this column
               for (int i = 0; i < field.getFieldLength(); i++)
                  maxNumberConstraint.append("9");

               // calculate min number allowed for this column
               if (field.getFieldLength() == 1)
                  minNumberConstraint = new StringBuilder("0");
               else {
                  minNumberConstraint = new StringBuilder("-");
                  for (int i = 0; i < field.getFieldLength() - 1; i++)
                     minNumberConstraint.append("9");
               }
            }

            storeField.setSourceTypeName("NUMERIC(" + field.
                    getFieldLength() +
                    ", " + field.getDecimalCount() + ")");
            storeField.setLength(field.getFieldLength());
            storeField.setDecimalCount(field.getDecimalCount());
            String minCheckCondition = com.relationaljunction.utils.StringUtils.
                    quoteReservedFieldAndTableName(field.getName()) +
                    " >= " + minNumberConstraint;
            String maxCheckCondition = com.relationaljunction.utils.StringUtils.
                    quoteReservedFieldAndTableName(field.getName()) +
                    " <= " + maxNumberConstraint;

            if (schema.checkColumnSize)
               storeField.setCheckCondition("CHECK " + minCheckCondition + " AND " +
                       maxCheckCondition);
            break;
         }
         case DBFField.FIELD_TYPE_INTEGER: {
            storeField.setSourceTypeName("INTEGER");
            storeField.setType(StoreDataType.INTEGER);
            break;
         }
         case DBFField.FIELD_TYPE_FLOAT: {
            storeField.setSourceTypeName("FLOAT");

            if (schema.useBigDecimalType)
               storeField.setType(StoreDataType.NUMERIC);
            else
               storeField.setType(StoreDataType.DOUBLE);

            storeField.setLength(field.getFieldLength());
            storeField.setDecimalCount(field.getDecimalCount());
            break;
         }
         case DBFField.FIELD_TYPE_DOUBLE:
         case DBFField.FIELD_TYPE_CURRENCY: {
            if (field.getDataType() == DBFField.FIELD_TYPE_DOUBLE)
               storeField.setSourceTypeName("DOUBLE");
            else if (field.getDataType() == DBFField.FIELD_TYPE_CURRENCY)
               storeField.setSourceTypeName("CURRENCY");

            if (schema.useBigDecimalType)
               storeField.setType(StoreDataType.NUMERIC);
            else
               storeField.setType(StoreDataType.DOUBLE);
            break;
         }
         case DBFField.FIELD_TYPE_DATE:
         case DBFField.FIELD_TYPE_DATETIME: {
            if (field.getDataType() == DBFField.FIELD_TYPE_DATE)
               storeField.setSourceTypeName("DATE");
            else if (field.getDataType() == DBFField.FIELD_TYPE_DATETIME)
               storeField.setSourceTypeName("DATETIME");

            storeField.setType(StoreDataType.TIMESTAMP);
            break;
         }
         case DBFField.FIELD_TYPE_LOGICAL: {
            storeField.setSourceTypeName("LOGICAL");
            storeField.setType(StoreDataType.BOOLEAN);
            break;
         }
         // MEMO
         case DBFField.FIELD_TYPE_MEMO: {
            if (field.getDataType() == DBFField.FIELD_TYPE_MEMO)
               storeField.setSourceTypeName("MEMO");

//        storeField.setType(StoreDataType.LONGVARCHAR);
            if (schema.getMemoAsBlob)
               storeField.setType(StoreDataType.BLOB);
            else
               storeField.setType(StoreDataType.VARCHAR);
            break;
         }
         // BLOB
         case DBFField.FIELD_TYPE_GENERAL:
         case DBFField.FIELD_TYPE_PICTURE: {
            if (field.getDataType() == DBFField.FIELD_TYPE_GENERAL)
               storeField.setSourceTypeName("GENERAL");
            else if (field.getDataType() == DBFField.FIELD_TYPE_PICTURE)
               storeField.setSourceTypeName("PICTURE");

            storeField.setType(StoreDataType.BLOB);
            break;
         }
         // STRING
         case DBFField.FIELD_TYPE_CHAR: {
            storeField.setSourceTypeName("CHAR(" + field.
                    getFieldLength() +
                    ")");
            storeField.setType(StoreDataType.VARCHAR);
            storeField.setLength(field.getFieldLength());
            break;
         }
         // SYSTEM
         case DBFField.FIELD_TYPE_SYSTEM: {
            storeField.setSourceTypeName("SYSTEM(" + field.
                    getFieldLength() +
                    ")");
            storeField.setType(StoreDataType.VARCHAR);
            storeField.setLength(field.getFieldLength());
            break;
         }
         default:
            throw new Exception("[VFPTableReader] Unknown field type " + (char) field.getDataType());
      }

      return storeField;
   }

   // VFP -> JDBC
   public Object getJDBCObjectForVFP(StoreFieldIF storeField, Object dbfValue) throws
           Exception {
//      if (dbfValue != null && dbfValue.toString().equalsIgnoreCase("00.000E")) {
//         System.out.println("aaa");
//      }

      // NULL value
      if (dbfValue == null) {
         return null;
      }
      // TIMESTAMP type
      else if (storeField.getType() == StoreDataType.TIMESTAMP) {
         return dbfValue;
      }
      // INTEGER type
      else if (storeField.getType() == StoreDataType.INTEGER) {
         if (dbfValue instanceof Number)
            return ((Number) dbfValue).intValue();
         else {
            try {
               return Integer.valueOf(dbfValue.toString());
            } catch (NumberFormatException ex) {
               int ivalue;
               try {
                  ivalue = Double.valueOf(dbfValue.toString()).intValue();
               } catch (Exception ex2) {
                  return null;
//            throw new StoreException("Can't convert the value '" +
//                                     dbfValue.toString() +
//                                     "' to Integer");
               }
               return ivalue;
            }
         }
//      Number n = (Number) dbfValue;
//      return new Integer(n.intValue());
      }
      // BIGINT type
      else if (storeField.getType() == StoreDataType.BIGINT) {
         if (dbfValue instanceof Number)
            return ((Number) dbfValue).longValue();
         else {
            try {
               return Long.valueOf(dbfValue.toString());
            } catch (NumberFormatException ex3) {
               long lvalue;
               try {
                  lvalue = Double.valueOf(dbfValue.toString()).longValue();
               } catch (Exception ex2) {
                  return null;
//            throw new StoreException("Can't convert the value '" +
//                                     dbfValue.toString() +
//                                     "' to Long");
               }
               return lvalue;
            }
         }
//      Number n = (Number) dbfValue;
//      return new Long(n.longValue());
      }
      // FLOAT type (not used in the driver)
      // DOUBLE type
      else if (storeField.getType() == StoreDataType.DOUBLE) {
         if (dbfValue instanceof Double)
            return dbfValue;
         else {
            try {
               return new Double(dbfValue.toString());
            } catch (NumberFormatException e) {
               return null;
               //        throw new StoreException("Can't convert {" + str + "} to double type");
            }
         }
      }
      // BIGDECIMAL (NUMERIC) type
      else if (storeField.getType() == StoreDataType.NUMERIC) {
         try {
            return new BigDecimal(dbfValue.toString());
         } catch (NumberFormatException ex1) {
            return null;
//        throw new StoreException("Can't convert {" + str + "} to bigdecimal type");
         }
      }
      // BOOLEAN type
      else if (storeField.getType() == StoreDataType.BOOLEAN) {
         return dbfValue;
      }
      // BLOB
      else if (storeField.getType() == StoreDataType.BLOB) {
         // byte[]
         return dbfValue;
      }
      // VARCHAR and other types
      else if (storeField.getType() == StoreDataType.VARCHAR) {
         String strValue = (String) dbfValue;

         if (strValue.trim().isEmpty() && schema.emptyStringAsNull)
            return null;

         if (schema.trimBlanks)
            return com.relationaljunction.utils.StringUtils.rightTrim((String) dbfValue);

         return dbfValue;
      } else
         throw new StoreException("[VFPTableReader] Unknown column data type " +
                 storeField.getType());
   }

   public String getName() {
      return tableName;
   }

   public boolean isReadOnly() {
      return false;
   }

   public Properties getTableProperties() {
      return null;
   }

}
