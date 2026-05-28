package com.relationaljunction.jdbc.csv.schema;

import java.util.*;
import java.util.regex.Pattern;

/**
 * <p>Title: StelsCSV JDBC driver</p>
 * <p>Copyright: Copyright (c) J-Stels Software 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author J-Stels Software
 * @version 2.0
 */

/*
 This class describes a table in the schema file
*/

public class CSVTableDescr {
   private final List<CSVColumnDescr> columnsDescr = new Vector<CSVColumnDescr>();
   private String tableName;
   private Properties localProps = new Properties();
   private Pattern pattern = null;

   public CSVTableDescr(String tableName) {
      this.tableName = tableName;

      String patternString = null;
      try {
         if (tableName.startsWith("regex:")) {
            patternString = tableName.substring("regex:".length());
            pattern = Pattern.compile(patternString);
         }
      } catch (Exception e) {
         throw new IllegalArgumentException("Error in the schema file for the table description: '" + tableName +
                 "'. Can't compile the regex pattern '" + patternString + "'");
      }
   }

   void addColumn(CSVColumnDescr colDescr) throws Exception {
//    if (this.columns.contains(colDescr))
//      throw new Exception("[Relational Junction CSV Driver] Column '" + colDescr.getName() +
//                          "' with the position " +
//                          colDescr.getPos() +
//                          " is already described");
      this.columnsDescr.add(colDescr);
   }

//   public CSVColumnDescr findColumnDescr(String name, int pos) {
//      CSVColumnDescr result = null;
//      for (CSVColumnDescr colDescr : columnsDescr) {
//         if (colDescr.getPos() == pos) {
//            result = colDescr;
//            break;
//         } else if (name != null && name.equalsIgnoreCase(colDescr.getName())) {
//            result = colDescr;
//            break;
//         }
//      }
//      return result;
//   }

   public CSVColumnDescr findColumnDescrByPos(int pos) {
      if (pos < 0) return null;

      for (CSVColumnDescr colDescr : columnsDescr) {
         if (colDescr.getPos() == pos)
            return colDescr;
      }

      return null;
   }

   public CSVColumnDescr findColumnDescrByName(String name) {
      if (name == null) return null;
      for (CSVColumnDescr colDescr : columnsDescr) {
         if (colDescr.getName() != null && colDescr.getName().equalsIgnoreCase(name))
            return colDescr;
      }
      return null;
   }

   public List<CSVColumnDescr> getDescribedColumns() {
      return this.columnsDescr;
   }

   public String getTableName() {
      return this.tableName;
   }

   public void setTableName(String tableName) {
      this.tableName = tableName;
   }

   public Properties getLocalProps() {
      return localProps;
   }

   public void setLocalProps(Properties props) {
      this.localProps = props;
   }

   public Pattern getPattern() {
      return pattern;
   }

   @Override
   public boolean equals(Object obj) {
      return obj instanceof CSVTableDescr &&
              ((CSVTableDescr) obj).getTableName().equals(getTableName());
   }
}
