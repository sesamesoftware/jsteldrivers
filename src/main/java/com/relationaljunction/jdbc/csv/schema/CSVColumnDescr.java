package com.relationaljunction.jdbc.csv.schema;

import com.relationaljunction.database.StoreDataType;

/**
 * <p>Title: StelsCSV JDBC driver</p>
 * <p>Copyright: Copyright (c) J-Stels Software 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author J-Stels Software
 * @version 2.0
 */

/*
 This class describes a column in the schema file
 */

public class CSVColumnDescr {
   private String name = null;
   private StoreDataType colType = StoreDataType.VARCHAR;
   private int pos = -1;
   private int begin = -1;
   private int end = -1;
   private int size = -1;
   private int decimalCount = -1;
   private String alias = null;
   private String sourceTypeName = null;
   private String dateFormatString = null;
   private String decimalFormatInput = null;
   private String decimalFormatOutput = null;

//  CSVColumnDescr(String name) {
//    this.name = name;
//  }

//  CSVColumnDescr(int pos) {
//    this(pos, 0);
//  }

   // defines a column by its name only
   public CSVColumnDescr(String name, StoreDataType colType) {
      this.name = name;
      this.colType = colType;
   }

   // defines a column by its pos only
   public CSVColumnDescr(int pos, StoreDataType colType) {
      this.pos = pos;
      this.colType = colType;
   }

   // defines a column by its name and pos
   public CSVColumnDescr(String name, int pos, StoreDataType colType) {
      this.name = name;
      this.pos = pos;
      this.colType = colType;
   }

   public int getBeginPos() {
      return begin;
   }

   public void setBeginPos(int begin) {
      this.begin = begin;
   }

   public int getEndPos() {
      return end;
   }

   public void setEndPos(int end) {
      this.end = end;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public int getPos() {
      return pos;
   }

   public void setPos(int pos) {
      this.pos = pos;
   }

   public String toString() {
      return name + "(type=" + colType + ")";
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == null)
         return false;

      if (!(obj instanceof CSVColumnDescr))
         return false;

      CSVColumnDescr descr = (CSVColumnDescr) obj;
      return name.equalsIgnoreCase(descr.getName());
   }

   public StoreDataType getColType() {
      return colType;
   }

//   public void setColType(int colType) {
//      this.colType = colType;
//   }

   public int getSize() {
      return size;
   }

   public void setSize(int size) {
      this.size = size;
   }

   public String getSourceTypeName() {
      return sourceTypeName;
   }

   public int getDecimalCount() {
      return decimalCount;
   }

   public void setSourceTypeName(String actualTypeName) {
      this.sourceTypeName = actualTypeName;
   }

   public void setDecimalCount(int decimalCount) {
      this.decimalCount = decimalCount;
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

   public String getAlias() {
      return alias;
   }

   public void setAlias(String alias) {
      this.alias = alias;
   }
}
