package com.relationaljunction.database;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 2.2
 */

public interface StoreFieldIF {

   String getName();

   StoreDataType getType();

   void setName(String name);

   void setType(StoreDataType type);

//   public String getSQLTypeName();

   String getSourceTypeName();

   String getCheckCondition();

   int getDecimalCount();

   int getLength();

//   returns a java.sql.Types constant
//   public int getSQLType();

   // returns additional properties, if it needs
   java.util.Properties getFieldProperties();

}
