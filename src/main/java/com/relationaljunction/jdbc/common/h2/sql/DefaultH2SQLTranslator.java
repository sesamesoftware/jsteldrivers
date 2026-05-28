package com.relationaljunction.jdbc.common.h2.sql;

import java.math.BigDecimal;
import java.sql.*;

import org.h2.result.DefaultRow;
import org.h2.result.Row;
import org.h2.util.DateTimeUtils;
import org.h2.value.*;

import com.relationaljunction.database.*;
import com.relationaljunction.database.h2.StoreResultSet;

public class DefaultH2SQLTranslator {
   public DefaultH2SQLTranslator() {
   }

//  public static String getH2TypeName(String jdbcTypeName) {
//    if (jdbcTypeName.equalsIgnoreCase("FLOAT"))return "REAL";
//    return jdbcTypeName;
//  }

   public static Row convertStoreRecordToH2(StoreRecordIF rec) {
      Value[] values = new Value[rec.getSize()];

      for (int i = 0; i < rec.getSize(); i++) {
         values[i] = (rec.getObject(i) == null) ?
                 ValueNull.INSTANCE :
                 convertValueToH2(rec.getField(i).getType(), rec.getObject(i));
      }

      return new DefaultRow(values, Row.MEMORY_CALCULATE);
   }

   public static Value convertValueToH2(StoreDataType dataType, Object valueObj) {
      switch (dataType.getJdbcType()) {
         case Types.INTEGER: {
            return ValueInteger.get(((Number) valueObj).intValue());
         } 
         case Types.BIGINT: {
            return ValueBigint.get(((Number) valueObj).longValue());
         }
         case Types.FLOAT: {
            return ValueReal.get(((Number) valueObj).floatValue());
         }
         case Types.DOUBLE: {
            return ValueDouble.get(((Number) valueObj).doubleValue());
         }
         case Types.NUMERIC: {
            return ValueNumeric.get((BigDecimal) valueObj);
         }
         case Types.BLOB: {
            throw new UnsupportedOperationException();
         }
         case Types.BOOLEAN: {
            return ValueBoolean.get((Boolean) valueObj);
         }
         case Types.VARCHAR:
         case Types.CHAR: {
            return ValueChar.get((String) valueObj);
         }
         case Types.TIMESTAMP: {
            return ValueTimestamp.fromDateValueAndNanos(DateTimeUtils.dateValueFromAbsoluteDay(((java.util.Date) valueObj).getTime()), DateTimeUtils.normalizeNanosOfDay(((java.util.Date) valueObj).getTime()));
         }
         case Types.TIME: {
            return ValueTime.fromNanos(((java.util.Date) valueObj).getTime()); 
         }
         case Types.DATE: {
            return ValueDate.fromDateValue(DateTimeUtils.dateValueFromAbsoluteDay(((java.util.Date) valueObj).getTime()));
         }
         default:
            throw new UnsupportedOperationException();
      }
   }

   public static Object convertValue(StoreDataType dataType, Object valueObj) {
      switch (dataType.getJdbcType()) {
         case Types.INTEGER: {
            if (valueObj instanceof Integer) {
               return valueObj;
            } else if (valueObj instanceof String) {
               return Integer.valueOf((String) valueObj);
            } else if (valueObj instanceof Number) {
               return ((Number) valueObj).intValue();
            } else throw new
                    IllegalArgumentException("Can't convert '" + valueObj + "' to java.lang.Integer");
         }
         case Types.BIGINT: {
            if (valueObj instanceof Long) {
               return valueObj;
            } else if (valueObj instanceof String) {
               return Long.valueOf((String) valueObj);
            } else if (valueObj instanceof Number) {
               return ((Number) valueObj).longValue();
            } else throw new
                    IllegalArgumentException("Can't convert '" + valueObj + "' to java.lang.Long");
         }
         case Types.FLOAT: {
            if (valueObj instanceof Float) {
               return valueObj;
            } else if (valueObj instanceof String) {
               return Float.valueOf((String) valueObj);
            } else if (valueObj instanceof Number) {
               return ((Number) valueObj).floatValue();
            } else throw new
                    IllegalArgumentException("Can't convert '" + valueObj + "' to java.lang.Float");
         }
         case Types.DOUBLE: {
            if (valueObj instanceof Double) {
               return valueObj;
            } else if (valueObj instanceof String) {
               return Double.valueOf((String) valueObj);
            } else if (valueObj instanceof Number) {
               return ((Number) valueObj).doubleValue();
            } else throw new
                    IllegalArgumentException("Can't convert '" + valueObj + "' to java.lang.Integer");
         }
         case Types.NUMERIC: {
            if (valueObj instanceof BigDecimal) {
               return valueObj;
            } else if (valueObj instanceof Integer) {
               return new BigDecimal((Integer) valueObj);
            } else if (valueObj instanceof Long) {
               return new BigDecimal((Long) valueObj);
            } else if (valueObj instanceof String ||
                    valueObj instanceof Float ||
                    valueObj instanceof Double) {
               // better convert float numbers via String
               return new BigDecimal(valueObj.toString());
            } else throw new
                    IllegalArgumentException("Can't convert '" + valueObj + "' to java.lang.Integer");
         }
         default:
            return valueObj;
      }
   }

   public static Object[] getSuperTypeObjects(Object[] objs) {
      final int INT = 1;
      final int LONG = 2;
      final int STRING = 10;

      return null;
   }


   public static void rewriteStoreTable(ResultSet rs, StoreTableIF store,
                                        StoreFieldIF[] fields) throws Exception {
      StoreTableWriterIF writer = store.getWriter(fields);

      StoreResultSet storeRS = new StoreResultSet(rs, fields);

      // insert records to be inserted
      writer.insertRecords(storeRS);

      storeRS.clear(); // close resultSet as well
      writer.close();
   }

   public static void insertRecordsToStoreTable(ResultSet rsOperations, StoreTableIF store,
                                                StoreFieldIF[] fields) throws
           Exception {
      StoreTableWriterIF writer = store.getWriter(null);

      insertRecordsToStoreTable(rsOperations, writer, fields);
      writer.close();
   }

   public static void insertRecordsToStoreTable(ResultSet rsOperations,
                                                StoreTableWriterIF writer,
                                                StoreFieldIF[] fields
   ) throws
           StoreException {
      StoreResultSet storeRS = new StoreResultSet(rsOperations, fields);

      // insert records
      writer.insertRecords(storeRS);

      storeRS.clear(); // close resultSet as well
   }

   public static void updateRecordsInStoreTable(PreparedStatement pst,
                                                StoreTableIF store) throws
           Exception {
      StoreTableWriterIF writer = store.getWriter(null);

      updateRecordsInStoreTable(pst, writer);

      writer.close();
   }

   public static void updateRecordsInStoreTable(PreparedStatement pst,
                                                StoreTableWriterIF writer) throws
           StoreException {
      // update records
      writer.updateRecords(pst);
   }

   public static void deleteRecordsInStoreTable(PreparedStatement pst,
                                                StoreTableIF store) throws
           Exception {
      StoreTableWriterIF writer = store.getWriter(null);

      deleteRecordsInStoreTable(pst, writer);

      writer.close();
   }

   public static void deleteRecordsInStoreTable(PreparedStatement pst,
                                                StoreTableWriterIF writer) throws
           StoreException {
      // delete records
      writer.deleteRecords(pst);
   }
}
