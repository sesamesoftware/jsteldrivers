package com.relationaljunction.jdbc.mdb.store;

import com.healthmarketscience.jackcess.impl.IndexImpl;
import com.healthmarketscience.jackcess.*;
import com.relationaljunction.database.*;
import com.relationaljunction.database.index.*;

import java.io.*;
import java.util.*;

public class MDBIndexSchema implements IndexSchemaIF {
   private MDBSchema schema = null;

   MDBIndexSchema(MDBSchema schema) {
      this.schema = schema;
   }

   public void addIndex(String indexName, String indexedTable,
                        IndexFieldIF[] fields, Properties props) {
   }


   public void dropIndex(String indexName) {
   }

   public Properties getSchemaProperties() {
      return null;
   }

   public IndexTableIF getStoreIndex(String indexName) {
      return null;
   }

   public IndexTableIF[] getStoreIndexes(String tableName) throws
           StoreException {
      Database db;
      Table t;

      try {
         // open another instance of Database to optimize concurrent reading operations
         db = schema.openDatabase();
         t = db.getTable(com.relationaljunction.utils.StringUtils.unDoubleQuote(tableName));
      } catch (IOException ex) {
         // error
         throw new StoreException(ex);
      }

      if (t == null)
         throw new StoreException(
                 "Error in MDBIndexSchema.getStoreIndexes(). Table is not found '" +
                         tableName + "'");

      Vector<IndexTableIF> indexVector = new Vector<IndexTableIF>();

      try {
         // fill index details
         List<? extends Index> indexes = t.getIndexes();

         for (Index index : indexes) {
            //      if (index.isForeignKey())continue;
            // if an index based on ATTACHMENT columns then ignore it
            if (index.getColumns().get(0).getColumn().getType() == DataType.COMPLEX_TYPE) {
               continue;
            }

            DefaultIndexTable indexTable = createIndexTable(t.getName(), index);

            if (index.isForeignKey()) {
               Index refIndex = index.getReferencedIndex();
               DefaultIndexTable referencedIndexTable = createIndexTable(refIndex.
                       getTable().getName(), refIndex);
               indexTable.setReferencedIndexTable(referencedIndexTable);
            }

            indexVector.add(indexTable);
         }

         db.close();
      } catch (Exception ex) {
         ex.printStackTrace();
         throw new StoreException("Error in MDBIndexSchema.getStoreIndexes()", ex);
      }

      IndexTableIF[] result = new IndexTableIF[indexVector.size()];
      for (int i = 0; i < result.length; i++)
         result[i] = indexVector.get(i);

      return result;
   }

   private DefaultIndexTable createIndexTable(String tableName, Index index) {
      List<? extends Index.Column> columnDescriptors = index.getColumns();
      IndexFieldIF[] indexFields = new IndexFieldIF[columnDescriptors.size()];
      int[] indexFieldPositions = new int[columnDescriptors.size()];

      boolean isAllFieldsAscending = true;

      for (int i = 0; i < columnDescriptors.size(); i++) {
         Index.Column columnDescriptor = columnDescriptors.get(i);
         if (!columnDescriptor.isAscending()) isAllFieldsAscending = false;
         DefaultIndexField indexField = new DefaultIndexField(schema.
                 getStoreField(columnDescriptor.getColumn()), columnDescriptor.isAscending());
         indexFields[i] = indexField;
         indexFieldPositions[i] = columnDescriptor.getColumnIndex();
      }

      String indexName;
      if (index.isPrimaryKey()) {
         indexName = "PRIMARY_KEY_";
      } else if (index.isForeignKey() && index.isUnique()) {
         indexName = "EXPORTED_FOREIGN_KEY_";
      } else if (index.isForeignKey() && !index.isUnique()) {
         indexName = "IMPORTED_FOREIGN_KEY_";
      } else if (index.isUnique()) {
         indexName = "UNIQUE_INDEX_";
      } else {
         indexName = "NON_UNIQUE_INDEX_";
      }

      indexName += com.relationaljunction.utils.StringUtils.replaceReservedChars(com.relationaljunction.utils.StringUtils.
              unDoubleQuote(tableName).toUpperCase() + "_N" + ((IndexImpl) index).getIndexNumber());

      DefaultIndexTable indexTable = new DefaultIndexTable(indexName, tableName,
              indexFields);
      indexTable.setPrimaryKey(index.isPrimaryKey());
      indexTable.setForeignKey(index.isForeignKey());
      indexTable.setUnique(index.isUnique());
      indexTable.setAllowNulls(index.shouldIgnoreNulls());
      indexTable.setAllFieldsAscending(isAllFieldsAscending);
      indexTable.setIndexFieldsPositions(indexFieldPositions);
      return indexTable;
   }

   /**
    * returns an addditional relationship data contained in MSysRelationships tables.
    * this data is required for linked tables, because their relationships are reflected only here.
    */
   public TablesRelationship[] getRelationships() throws StoreException {
      TablesRelationship[] relationshipArray;

      try {
         Database db = schema.openDatabase();
         Table t = db.getSystemTable("MSysRelationships");
//         t.reset();
         relationshipArray = new TablesRelationship[t.getRowCount()];

         for (int i = 0; i < t.getRowCount(); i++) {
            Map rowMap = t.getNextRow();
            String fromTable = rowMap.get("szReferencedObject").toString();
            String toTable = rowMap.get("szObject").toString();
            String fromColumn = rowMap.get("szReferencedColumn").toString();
            String toColumn = rowMap.get("szColumn").toString();

            relationshipArray[i] = new TablesRelationship(fromTable, toTable, fromColumn,
                    toColumn);
         }

         db.close();
      } catch (Exception ex) {
         throw new StoreException("Error in MDBIndexSchema.getRelationships()", ex);
      }

      return relationshipArray;
   }


   public void reIndex(String indexedTable) {
   }
}
