package com.relationaljunction.jdbc.mdb.h2;

import java.sql.SQLException;
import java.util.Properties;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.ColumnBuilder;
import com.healthmarketscience.jackcess.complex.ComplexDataType;
import com.healthmarketscience.jackcess.DataType;
import com.relationaljunction.database.DefaultStoreField;
import com.relationaljunction.database.StoreDataType;
import com.relationaljunction.database.StoreException;
import com.relationaljunction.database.StoreFieldIF;
import com.relationaljunction.database.h2.StoreSchemaIF2;
import com.relationaljunction.jdbc.common.h2.CommonConnection2;
import com.relationaljunction.jdbc.mdb.store.MDBSchema;


public class MDBSchema2 extends MDBSchema implements StoreSchemaIF2 {
   private boolean preserveColumnNames = CommonConnection2.DEFAULT_PRESERVE_COLUMN_NAMES;

   public MDBSchema2(Properties globalProps) throws
           SQLException {
      super(globalProps);

      // preserveColumnNames property
      if (globalProps.getProperty(CommonConnection2.PRESERVE_COLUMN_NAMES) != null) {
         preserveColumnNames =
                 Boolean.valueOf(globalProps.getProperty(CommonConnection2.PRESERVE_COLUMN_NAMES));
      }

      MDBTableEngine.setSchema(this);
   }

   /*
   requires exclusive locking for safely multi-thread writing data from Database instance
   It is needed for safely using reload() method of StoreSchemaIF.
   */
   public boolean requiresLockingForWritingOperations() {
      return true;
   }

   /*
   requires non-exclusive locking for safely multi-thread reading data from Database instance
   It is needed for safely using reload() method of StoreSchemaIF.
   */
   public boolean requiresLockingForReadingOperations() {
      return true;
   }

   public void lockForReading() {
      lock(false);
   }

   public void lockForWriting() {
      lock(true);
   }

   /*
   MDB -> Store
    */
   public StoreFieldIF getStoreField(Column mdbCol) {
      // default type of a column is VARCHAR
      DefaultStoreField storeField = new DefaultStoreField(mdbCol.getName());

      switch (mdbCol.getType()) {
         case BYTE: {
            storeField.setType(StoreDataType.INTEGER);
            storeField.setCheckCondition("CHECK " + com.relationaljunction.utils.StringUtils.
                    quoteFieldAndTableName(mdbCol.getName(), preserveColumnNames) +
                    " >= 0 AND " + com.relationaljunction.utils.StringUtils.
                    quoteFieldAndTableName(mdbCol.getName(), preserveColumnNames) +
                    " <= 255");
            break;
         }
         case INT: {
            storeField.setType(StoreDataType.INTEGER);
            storeField.setCheckCondition("CHECK " + com.relationaljunction.utils.StringUtils.
                    quoteFieldAndTableName(mdbCol.getName(), preserveColumnNames) +
                    " >= -32768 AND " + com.relationaljunction.utils.StringUtils.
                    quoteFieldAndTableName(mdbCol.getName(), preserveColumnNames) +
                    " <= 32767");
            break;
         }
         case LONG: {
            if (!mdbCol.isAutoNumber()) {
               storeField.setType(StoreDataType.INTEGER);
            } else
               storeField.setType(StoreDataType.IDENTITY);
            break;
         }
         case FLOAT: {
            storeField.setType(StoreDataType.FLOAT);
            break;
         }
         case DOUBLE: {
            storeField.setType(StoreDataType.DOUBLE);
            break;
         }
         case NUMERIC: {
            int precision = mdbCol.getPrecision();

            if (precision > DOUBLE_LIMIT) {
               storeField.setType(StoreDataType.NUMERIC);
               storeField.setLength(precision);
               storeField.setDecimalCount(mdbCol.getScale());
            } else
               storeField.setType(StoreDataType.DOUBLE);

            break;
         }
         case MONEY: {
            storeField.setType(StoreDataType.CURRENCY);
            break;
         }
         case BOOLEAN: {
            storeField.setType(StoreDataType.BOOLEAN);
            break;
         }
         case SHORT_DATE_TIME: {
            storeField.setType(StoreDataType.TIMESTAMP);
            break;
         }
         case TEXT: {
            storeField.setType(StoreDataType.VARCHAR);
            storeField.setLength(mdbCol.getLength() / 2);
            break;
         }
         case MEMO: {
            if (mdbCol.isHyperlink())
               storeField.setType(StoreDataType.HYPERLINK);
            else
               // size?
               storeField.setType(StoreDataType.MEMO);

            break;
         }
         case OLE: {
            storeField.setType(StoreDataType.OLE);
//      storeField.setType(java.sql.Types.JAVA_OBJECT);
//      storeField.setType(java.sql.Types.JAVA_OBJECT);
            // now it is converted to String with length = 9
//            storeField.setLength(9);
            break;
         }
         case COMPLEX_TYPE: {
            // ATTACHMENT
            if (mdbCol.getComplexInfo().getType() == ComplexDataType.ATTACHMENT) {
               storeField.setType(StoreDataType.ATTACHMENT);
               break;
            }
            // MULTI VALUE
            else if (mdbCol.getComplexInfo().getType() == ComplexDataType.MULTI_VALUE) {
               storeField.setType(StoreDataType.VARCHAR);
               break;
            }
         }
      }
      return storeField;
   }

   /*
    Store -> MDB
   */
   public ColumnBuilder getMDBColumn(StoreFieldIF storeField) throws StoreException {
      ColumnBuilder mdbCol = new ColumnBuilder(storeField.getName());
      int length = storeField.getLength();
      int scale = storeField.getDecimalCount();

      try {
         // NUMERIC || DOUBLE with length > 0 => native DECIMAL(length,decimalCount)
         if (storeField.getType() == StoreDataType.NUMERIC ||
                 (storeField.getType() == StoreDataType.DOUBLE && length > 0)) {
            if (length <= 0) length = DEFAULT_DECIMAL_PRECISION;
            if (scale <= 0) scale = DEFAULT_DECIMAL_DECIMAL_SCALE;
            mdbCol.setType(DataType.NUMERIC);
            mdbCol.setPrecision((byte) length);
            mdbCol.setScale((byte) scale);
         }
         // AUTONUMBER
         else if (storeField.getType() == StoreDataType.IDENTITY) {
            mdbCol.setType(DataType.LONG);
            mdbCol.setAutoNumber(true);
         }
         // CURRENCY
         else if (storeField.getType() == StoreDataType.CURRENCY) {
            mdbCol.setType(DataType.MONEY);
         }
         // HYPERLINK
         else if (storeField.getType() == StoreDataType.HYPERLINK) {
            mdbCol.setType(DataType.MEMO);
            mdbCol.setHyperlink(true);
         }
         // date types
         else if (storeField.getType().isDateType()) {
            mdbCol.setType(DataType.SHORT_DATE_TIME);
         }
         // VARCHAR
         else if (storeField.getType() == StoreDataType.VARCHAR) {
            mdbCol.setType(DataType.TEXT);
            // default size
            if (length <= 0) {
               mdbCol.setLength((short) (DEFAULT_VARCHAR_LENGTH * 2));
            } else {
               mdbCol.setLength((short) (length * 2));
            }
         }
         // ATTACHMENT
         else if (storeField.getType() == StoreDataType.ATTACHMENT) {
            mdbCol.setType(DataType.COMPLEX_TYPE);
         }
         // other types
         else
            mdbCol.setSQLType(storeField.getType().getJdbcType());
      } catch (Exception ex) {
         throw new StoreException(ex);
      }

      return mdbCol;
   }

   public String getExternalEngineClass() {
      return MDBTableEngine.class.getName();
   }

   public boolean supportsExternalEngine() {
      return true;
   }

}
