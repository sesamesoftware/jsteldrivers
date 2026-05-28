package com.relationaljunction.jdbc.common.h2.sql;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import com.relationaljunction.database.DefaultStoreField;
import com.relationaljunction.database.StoreFieldIF;
import com.relationaljunction.database.index.DefaultIndexField;
import com.relationaljunction.database.index.DefaultIndexTable;
import com.relationaljunction.database.index.IndexFieldIF;
import com.relationaljunction.database.index.IndexTableIF;
import com.relationaljunction.utils.StringUtils;
import com.relationaljunction.utils.UnexpectedException;

public class CreateTableSQLCommand extends SQLCommand {
   public enum ConstraintType {
      PRIMARY_KEY, FOREIGN_KEY, UNIQUE;

      public static ConstraintType parseConstraintType(String type) {
         if (type.equals("PRIMARY_KEY_CONSTRAINT")) return PRIMARY_KEY;
         else if (type.equals("FOREIGN_KEY_CONSTRAINT")) return FOREIGN_KEY;
         return UNIQUE;
      }
   }

   private final List<StoreFieldIF> fields = new LinkedList<StoreFieldIF>();
   private final List<IndexTableIF> indexTables = new LinkedList<IndexTableIF>();
   private final static char[] QUOTE_CHARS = new char[]{'"', '!', '`'};
   private boolean usingSpecification = false;

   protected CreateTableSQLCommand(String sqlText,
                                   HashSet<String> tablesUsed, SQLNode rootNode) {
      super(SQLCommand.CREATE_TABLE, sqlText, tablesUsed, rootNode);

      SQLNode tableClauseNode = rootNode.getUniqueChildWithName("TABLE_CLAUSE");

      // StelsXML extension
      if (!tableClauseNode.getProperty("USING_SPECIFICATION").isEmpty()) {
         usingSpecification = true;
         return;
      }

      List<SQLNode> createItemNodes = new LinkedList<SQLNode>();
      rootNode.getNodesByName("CREATE_ITEM", createItemNodes);

      boolean primaryKeyConstraintDefined = false;

      int indexNumber = 0;
      for (SQLNode createItemNode : createItemNodes) {
         String createItemType = createItemNode.getProperty("TYPE");

         if (createItemType.equals("COLUMN_DESCR")) {
            processColumnItem(createItemNode);
         } else if (createItemType.equals("TABLE_CONSTR")) {
            SQLNode tableConstraintNode = createItemNode.getUniqueChildWithName("TABLE_CONSTRAINT");
            ConstraintType type = ConstraintType.parseConstraintType(
                    tableConstraintNode.getProperty("TYPE"));

            if (type == ConstraintType.PRIMARY_KEY) {
               if (primaryKeyConstraintDefined)
                  throw new UnexpectedException("Multiple primary key constraints are not allowed");
               primaryKeyConstraintDefined = true;
               processConstraint(tableConstraintNode, ConstraintType.PRIMARY_KEY, 0);
            } else if (type == ConstraintType.FOREIGN_KEY) {
               indexNumber++;
               processConstraint(tableConstraintNode, ConstraintType.FOREIGN_KEY, indexNumber);
            } else if (type == ConstraintType.UNIQUE) {
               indexNumber++;
               processConstraint(tableConstraintNode, ConstraintType.UNIQUE, indexNumber);
            }
         }
      }

      processIndexItems(rootNode);
   }

   private void processConstraint(SQLNode tableConstraintNode, ConstraintType type, int indexNumber) {
      List<SQLNode> primaryCols = new LinkedList<SQLNode>();
      tableConstraintNode.getNodesByName("COLUMN_DESCR", primaryCols);

      IndexFieldIF[] indexFields = new IndexFieldIF[primaryCols.size()];

      for (int i = 0; i < primaryCols.size(); i++) {
         String fieldName = primaryCols.get(i).getProperty("COLUMN_NAME");
         indexFields[i] = new DefaultIndexField(new DefaultStoreField(fieldName));
      }

      DefaultIndexTable indexTable = new DefaultIndexTable(type.name() + indexNumber,
              getBaseTable(), indexFields);

      if (type == ConstraintType.PRIMARY_KEY)
         indexTable.setPrimaryKey(true);
      else if (type == ConstraintType.UNIQUE)
         indexTable.setUnique(true);

      indexTables.add(indexTable);
   }

   private void processColumnItem(SQLNode createItemNode) {
      // unquote outside quotes if exist
      String fieldName = StringUtils.unquote(createItemNode.
              getUniqueChildWithName("COLUMN_DESCR").getProperty("COLUMN_NAME"),
              QUOTE_CHARS);

//         String fieldName = createItemNodes.get(i).
//                 getUniqueChildWithName("COLUMN_DESCR").getProperty("COLUMN_NAME");

//      String fieldName = createItemNodes.get(i).getUniqueChildWithName(
//          "COLUMN_DESCR").getProperty("COLUMN_NAME");
      String dataType = createItemNode.getUniqueChildWithName(
              "COL_DATA_TYPE").getProperty("DATA_TYPE_NAME");
      String lengthStr = createItemNode.getUniqueChildWithName(
              "COL_DATA_TYPE").getProperty("PARAM_ONE");
      String decimalCountStr = createItemNode.getUniqueChildWithName(
              "COL_DATA_TYPE").getProperty("PARAM_TWO");

      DefaultStoreField storeField = new DefaultStoreField(fieldName, dataType);
      if (!lengthStr.isEmpty())
         storeField.setLength(Integer.parseInt(lengthStr));
      if (!decimalCountStr.isEmpty())
         storeField.setDecimalCount(Integer.parseInt(decimalCountStr));

      fields.add(storeField);
   }

   /**
    * CREATE TABLE can contain driver-specific WITH INDEX clause
    * in order to alow creating indexes simultaneously with table creating
    *
    * @param rootNode
    */
   private void processIndexItems(SQLNode rootNode) {
      List<SQLNode> indexTableItems = new LinkedList<SQLNode>();
      rootNode.getNodesByName("INDEX_ITEM", indexTableItems);

      for (SQLNode indexTableItem : indexTableItems) {
         String indexName = indexTableItem.getProperty("INDEX_NAME");

         List<SQLNode> indexColumnDescrs = new LinkedList<SQLNode>();
         indexTableItem.getNodesByName("INDEX_COLUMN_DESCR", indexColumnDescrs);

         IndexFieldIF[] indexFields = new IndexFieldIF[indexColumnDescrs.size()];
         for (int j = 0; j < indexColumnDescrs.size(); j++) {
            boolean ascending = !indexColumnDescrs.get(j).getProperty("TYPE").equals("DESC");
            String fieldName = indexColumnDescrs.get(j).getUniqueChildWithName("COLUMN_DESCR").getProperty("COLUMN_NAME");
            indexFields[j] = new DefaultIndexField(new DefaultStoreField(fieldName), ascending);
         }

         indexTables.add(new DefaultIndexTable(indexName, getBaseTable(), indexFields));
      }
   }

   public StoreFieldIF[] getStoreFields() {
      return fields.toArray(new StoreFieldIF[0]);
   }

   public IndexTableIF[] getStoreIndexTables() {
      return indexTables.toArray(new IndexTableIF[0]);
   }

   public boolean isUsingSpecification() {
      return usingSpecification;
   }

}
