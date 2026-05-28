package com.relationaljunction.database;

import java.util.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 2.2
 */

public class DefaultStoreField
        implements StoreFieldIF, java.io.Serializable {

   private String name = null;
   private StoreDataType sqlType = StoreDataType.VARCHAR;

   private Properties props = new Properties();
   private int decimalCount = -1;
   private int length = -1;
   private String sourceTypeName = null;
   private String checkCondition = null;
   private String stringRepresentation;

   public DefaultStoreField(String name) {
      this.name = name;
      formStringRepresentation();
   }

   public DefaultStoreField(String name, StoreDataType type) {
      this.name = name;
      this.sqlType = type;
      formStringRepresentation();
   }

   public DefaultStoreField(String name, String dataTypeName) {
      this(name, StoreDataType.getDataTypeByName(dataTypeName));
   }

   public String getName() {
      return this.name;
   }

   public StoreDataType getType() {
      return this.sqlType;
   }

   public String getSourceTypeName() {
      return sourceTypeName;
   }

   public Properties getFieldProperties() {
      return props;
   }

   public int getDecimalCount() {
      return decimalCount;
   }

   public int getLength() {
      return length;
   }

   public String getCheckCondition() {
      return checkCondition;
   }

   public void setDecimalCount(int decimalCount) {
      this.decimalCount = decimalCount;
      formStringRepresentation();
   }

   public void setLength(int length) {
      this.length = length;
      formStringRepresentation();
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setProps(Properties props) {
      this.props = props;
   }

   public void setSourceTypeName(String sourceTypeName) {
      this.sourceTypeName = sourceTypeName;
   }

   public void setType(StoreDataType sqlType) {
      this.sqlType = sqlType;
      formStringRepresentation();
   }

   public void setCheckCondition(String checkCondition) {
      this.checkCondition = checkCondition;
   }

   private void formStringRepresentation() {
      this.stringRepresentation = getName() + " " + getType() +
              "(" + getLength() + ", " + getDecimalCount() + ")";
   }

   @Override
   public String toString() {
      return this.stringRepresentation;
   }
}
