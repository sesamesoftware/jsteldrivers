package com.relationaljunction.database;

public interface DataTypeIF {

   String getName();

   int getJdbcType();

   boolean isNumberType();

   boolean isDateType();

   boolean isTextType();
}
