package com.relationaljunction.jdbc.csv.schema;

import java.util.*;
import java.util.regex.Matcher;

import com.relationaljunction.utils.StringUtils;

/**
 * <p>Title: StelsCSV JDBC driver</p>
 * <p>Copyright: Copyright (c) J-Stels Software 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author J-Stels Software
 * @version 2.0
 */

/*
 This class describes the structure of the schema file
*/

public class CSVSchemaData {
   List<CSVTableDescr> tableDescriptions = new Vector<CSVTableDescr>();

   public CSVSchemaData() {
   }

   void addTableDescription(CSVTableDescr tableDescr) {
      this.tableDescriptions.add(tableDescr);
   }

   void removeTableDescription(CSVTableDescr tableDescr) {
      this.tableDescriptions.remove(tableDescr);
   }

   public CSVTableDescr getTableDescription(String tableName) {
      for (CSVTableDescr tableDescr : this.tableDescriptions) {
         if (tableDescr.getPattern() != null) {
            // table name can be set via RegEx
            Matcher m = tableDescr.getPattern().matcher(tableName);
            if (m.matches()) return tableDescr;
         } else if (StringUtils.isLike(tableName.toLowerCase(),
            // or table name can be set via wildcards '*' and '?'
                 tableDescr.getTableName().toLowerCase(), '*', '?')) {
            return tableDescr;
         }
      }

      return null;
   }

   public List getDescribedTables() {
      return this.tableDescriptions;
   }

}
