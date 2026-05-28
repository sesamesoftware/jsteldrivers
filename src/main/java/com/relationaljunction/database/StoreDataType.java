package com.relationaljunction.database;

import java.sql.Types;
import java.util.HashMap;

import com.relationaljunction.utils.UnexpectedException;

public enum StoreDataType implements DataTypeIF {
   IDENTITY("IDENTITY", "IDENTITY", Types.BIGINT),
   INTEGER("INTEGER", "INTEGER", Types.INTEGER),
   BIGINT("BIGINT", "BIGINT", Types.BIGINT),
   FLOAT("FLOAT", "REAL", Types.FLOAT),
   DOUBLE("DOUBLE", "FLOAT", Types.DOUBLE),
   NUMERIC("NUMERIC", "NUMERIC", Types.NUMERIC),
   TIMESTAMP("TIMESTAMP", "TIMESTAMP", Types.TIMESTAMP),
   TIME("TIME", "TIME", Types.TIME),
   DATE("DATE", "DATE", Types.DATE),
   CHAR("CHAR", "VARCHAR", Types.CHAR),
   VARCHAR("VARCHAR", "VARCHAR", Types.VARCHAR),
   LONGVARCHAR("LONGVARCHAR", "LONGVARCHAR", Types.LONGVARCHAR),
   VARBINARY("VARBINARY", "VARBINARY", Types.VARBINARY),
   BLOB("BLOB", "BLOB", Types.BLOB),
   BOOLEAN("BOOLEAN", "BOOLEAN", Types.BOOLEAN),

   // additional custom types
   CURRENCY("CURRENCY", "NUMERIC", Types.NUMERIC),
   HYPERLINK("HYPERLINK", "VARCHAR", Types.VARCHAR),
   MEMO("MEMO", "VARCHAR", Types.VARCHAR),
   ATTACHMENT("ATTACHMENT", "OTHER", Types.OTHER),
   OLE("OLE", "BLOB", Types.BLOB);

   // type name
   private final String name;
   // type name in H2 database
   private final String h2name;
   // JDBC jdbcType
   private final int jdbcType;

   private static final HashMap<String, StoreDataType> nameToDataTypeHash =
           new HashMap<String, StoreDataType>();

   static {
      // INTEGER
      nameToDataTypeHash.put("INT", INTEGER);
      nameToDataTypeHash.put("INTEGER", INTEGER);
      nameToDataTypeHash.put("MEDIUMINT", INTEGER);
      nameToDataTypeHash.put("INT4", INTEGER);
      nameToDataTypeHash.put("SIGNED", INTEGER);
      nameToDataTypeHash.put("SHORT", INTEGER);
      nameToDataTypeHash.put("TINYINT", INTEGER);
      nameToDataTypeHash.put("SMALLINT", INTEGER);
      nameToDataTypeHash.put("INT2", INTEGER);
      nameToDataTypeHash.put("YEAR", INTEGER);

      // LONG
      nameToDataTypeHash.put("LONG", BIGINT);
      nameToDataTypeHash.put("BIGINT", BIGINT);
      nameToDataTypeHash.put("INT8", BIGINT);

      // FLOAT
      nameToDataTypeHash.put("FLOAT", FLOAT);
      nameToDataTypeHash.put("FLOAT4", FLOAT);
      nameToDataTypeHash.put("FLOAT8", FLOAT);
      nameToDataTypeHash.put("REAL", FLOAT);

      // DOUBLE
      nameToDataTypeHash.put("DOUBLE", FLOAT);
      nameToDataTypeHash.put("DOUBLE PRECISION", FLOAT);

      // BIGDECIMAL/DECIMAL/NUMERIC
      nameToDataTypeHash.put("BIGDECIMAL", NUMERIC);
      nameToDataTypeHash.put("NUMBER", NUMERIC);
      nameToDataTypeHash.put("DECIMAL", NUMERIC);
      nameToDataTypeHash.put("NUMERIC", NUMERIC);
      nameToDataTypeHash.put("DEC", NUMERIC);

      // DATETIME (TIMESTAMP)
      nameToDataTypeHash.put("DATETIME", TIMESTAMP);
      nameToDataTypeHash.put("TIMESTAMP", TIMESTAMP);
      nameToDataTypeHash.put("SMALLDATETIME", TIMESTAMP);

      // TIME
      nameToDataTypeHash.put("TIME", TIME);

      // DATE
      nameToDataTypeHash.put("DATE", DATE);
      nameToDataTypeHash.put("YEAR", DATE);

      // STRING
      nameToDataTypeHash.put("VARCHAR", VARCHAR);
      nameToDataTypeHash.put("STRING", VARCHAR);
      nameToDataTypeHash.put("STR", VARCHAR);
      nameToDataTypeHash.put("VARCHAR2", VARCHAR);
      nameToDataTypeHash.put("NVARCHAR", VARCHAR);
      nameToDataTypeHash.put("NVARCHAR2", VARCHAR);
      nameToDataTypeHash.put("VARCHAR_IGNORECASE", VARCHAR);

      // CHAR
      nameToDataTypeHash.put("CHAR", CHAR);
      nameToDataTypeHash.put("NCHAR", CHAR);

      // LONGVARCHAR (MEMO)
      nameToDataTypeHash.put("LONGVARCHAR", LONGVARCHAR);
      nameToDataTypeHash.put("MEMO", LONGVARCHAR);
      nameToDataTypeHash.put("TEXT", LONGVARCHAR);

      // VARBINARY
      nameToDataTypeHash.put("VARBINARY", VARBINARY);

      // BLOB/OLE/ATTACHMENT
      nameToDataTypeHash.put("BLOB", BLOB);
      nameToDataTypeHash.put("OLE", OLE);
      nameToDataTypeHash.put("ATTACHMENT", ATTACHMENT);

      // BOOLEAN
      nameToDataTypeHash.put("BOOLEAN", BOOLEAN);
      nameToDataTypeHash.put("LOGICAL", BOOLEAN);
      nameToDataTypeHash.put("BIT", BOOLEAN);
      nameToDataTypeHash.put("BOOL", BOOLEAN);

      // AUTONUMBER/IDENTITY
      nameToDataTypeHash.put("COUNTER", IDENTITY);
      nameToDataTypeHash.put("AUTONUMBER", IDENTITY);
      nameToDataTypeHash.put("AUTOINCREMENT", IDENTITY);
      nameToDataTypeHash.put("IDENTITY", IDENTITY);

      // MONEY
      nameToDataTypeHash.put("MONEY", CURRENCY);
      nameToDataTypeHash.put("CURRENCY", CURRENCY);

      // HYPERLINK
      nameToDataTypeHash.put("HYPERLINK", HYPERLINK);
   }

   StoreDataType(String name, String h2name, int jdbcType) {
      this.name = name;
      this.h2name = h2name;
      this.jdbcType = jdbcType;
   }

   public String getName() {
      return name;
   }

   public String getH2Name() {
      return h2name;
   }

   public int getJdbcType() {
      return jdbcType;
   }

   /**
    * check if this type is a number type excluding IDENTITY
    *
    * @return
    */
   public boolean isNumberType() {
      return this == INTEGER || this == BIGINT || this == FLOAT || this == DOUBLE || this == NUMERIC;
   }

   /**
    * check if this type is a date/time type
    *
    * @return
    */
   public boolean isDateType() {
      return this == TIMESTAMP || this == TIME || this == DATE;
   }

   /**
    * check if this type is a text type, i.e. string, memo, etc
    *
    * @return
    */
   public boolean isTextType() {
      return this == VARCHAR || this == LONGVARCHAR || this == CHAR;
   }


   public static StoreDataType getDataTypeByName(String dataTypeString) {
      StoreDataType dataType = nameToDataTypeHash.get(dataTypeString.trim().toUpperCase());
      if (dataType == null) throw new UnexpectedException(
              "Unexpected error in StoreDataType.getSQLTypeCodeByName(): data type '" +
                      dataTypeString + "' is not supported");
      return dataType;
   }

   public static StoreDataType getDataTypeByJDBCTypeCode(int jdbcTypeCode) {
      switch (jdbcTypeCode) {
         case Types.INTEGER:
            return INTEGER;
         case Types.BIGINT:
            return BIGINT;
         case Types.FLOAT:
            return FLOAT;
         case Types.DOUBLE:
            return DOUBLE;
         case Types.NUMERIC:
            return NUMERIC;
         case Types.TIMESTAMP:
            return TIMESTAMP;
         case Types.BOOLEAN:
            return BOOLEAN;
         case Types.LONGVARCHAR:
            return LONGVARCHAR;
         case Types.VARBINARY:
            return VARBINARY;
         case Types.BLOB:
            return BLOB;
         default:
            return VARCHAR;
      }
   }


   @Override
   public String toString() {
      return name;
   }
}
