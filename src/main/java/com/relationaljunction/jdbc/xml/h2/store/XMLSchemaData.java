package com.relationaljunction.jdbc.xml.h2.store;

import java.util.*;

import com.relationaljunction.utils.StringUtils;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2006</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 2.5
 */

public class XMLSchemaData {
   private final Set<XMLTableDescr> tableSet = new HashSet<XMLTableDescr>();

   XMLSchemaData() {
   }

   void addTable(XMLTableDescr tableDescr) throws Exception {
//    if (tables.contains(tableDescr))
//      throw new Exception("The table '" + tableDescr.getName() +
//                          "' is already described");
      // seek the table

//      boolean found = false;
//      for (Object table : this.tables) {
//         XMLTableDescr elemDescr = (XMLTableDescr) table;
//         if ((elemDescr.getName() == null && tableDescr.getName() == null &&
//                 elemDescr.getFilePath().equalsIgnoreCase(tableDescr.getFilePath())) ||
//                 (elemDescr.getName() != null && tableDescr.getName() != null &&
//                         elemDescr.getName().equalsIgnoreCase(tableDescr.getName()))) {
//            found = true;
//            break;
//         }
//      }
//      if (!found)
//         tables.add(tableDescr);
//      else
//         throw new Exception("The table '" + ((tableDescr.getName() != null) ?
//                 tableDescr.getName() :
//                 tableDescr.getFilePath()) +
//                 "' is already described");

      if (tableSet.contains(tableDescr))
         throw new IllegalArgumentException("The table '" + ((tableDescr.getName() != null) ?
                 tableDescr.getName() : tableDescr.getFilePath()) + "' is already described");

      tableSet.add(tableDescr);
   }

   XMLTableDescr getTableDescription(String tableName) {
      for (Object table : this.tableSet) {
         XMLTableDescr tableDescr = (XMLTableDescr) table;
         if ((tableDescr.getName() == null &&
                 StringUtils.isLike(tableName, tableDescr.getFilePath(),
                         '*', '?')) ||
                 (tableDescr.getName() != null &&
                         StringUtils.isLike(tableName.toLowerCase(),
                                 tableDescr.getName(), '*', '?')))
            return tableDescr;
      }

      throw new IllegalArgumentException("The table '" + tableName +
              "' is not described in the schema file");
   }

   Set<XMLTableDescr> getTables() {
      return tableSet;
   }

}