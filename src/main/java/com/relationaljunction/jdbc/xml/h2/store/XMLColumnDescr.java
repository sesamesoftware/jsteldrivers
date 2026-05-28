package com.relationaljunction.jdbc.xml.h2.store;

import com.relationaljunction.database.StoreDataType;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2006</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 2.5
 */

public class XMLColumnDescr {
   public static int ONE2ONE_RELATION = 0;
   public static int ONE2MANY_RELATION = 1;
   public static String ONE2ONE_RELATION_STRING = "1:1";
   public static String ONE2MANY_RELATION_STRING = "1:n";
   private final String name;
   private final String xPath;
   private String context;
   private StoreDataType type = StoreDataType.VARCHAR;
   private int size = -1;
   private int decimalCount = -1;
   private int relationCode = ONE2ONE_RELATION;
   private String dateFormatString = null;
   private String decimalFormatInput = null;
   private String decimalFormatOutput = null;
   private String locale = null;

   XMLColumnDescr(String name, String xPath, StoreDataType type) {
      this.name = name;
      this.type = type;
      this.xPath = xPath;
   }

   public String getName() {
      return name;
   }

   public StoreDataType getType() {
      return type;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == null)
         return false;

      if (!(obj instanceof XMLColumnDescr))
         return false;

      XMLColumnDescr descr = (XMLColumnDescr) obj;
      return name.equalsIgnoreCase(descr.getName());
   }

   public String getXPath() {
      return xPath;
   }

   public int getSize() {
      return size;
   }

   public String getContext() {
      return context;
   }

   public void setSize(int size) {
      this.size = size;
   }

   public void setContext(String context) {
      this.context = context;
   }

   public int getDecimalCount() {
      return decimalCount;
   }

   public void setDecimalCount(int decimalCount) {
      this.decimalCount = decimalCount;
   }

   public int getRelation() {
      return relationCode;
   }

   public void setRelation(String relation) throws Exception {
      if (relation.equalsIgnoreCase(ONE2ONE_RELATION_STRING)) relationCode =
              ONE2ONE_RELATION;
      else if (relation.equalsIgnoreCase(ONE2MANY_RELATION_STRING)) relationCode =
              ONE2MANY_RELATION;
      else throw new Exception("Unknown relation code");
   }

   public String getDateFormatString() {
      return dateFormatString;
   }

   public void setDateFormatString(String dateFormatString) {
      this.dateFormatString = dateFormatString;
   }

   public String getDecimalFormatInput() {
      return decimalFormatInput;
   }

   public void setDecimalFormatInput(String decimalFormatInput) {
      this.decimalFormatInput = decimalFormatInput;
   }

   public String getDecimalFormatOutput() {
      return decimalFormatOutput;
   }

   public void setDecimalFormatOutput(String decimalFormatOutput) {
      this.decimalFormatOutput = decimalFormatOutput;
   }

   public String getLocale() {
      return locale;
   }

   public void setLocale(String locale) {
      this.locale = locale;
   }
}
