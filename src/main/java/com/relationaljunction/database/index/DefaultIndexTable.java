package com.relationaljunction.database.index;

import com.relationaljunction.database.*;

public class DefaultIndexTable
        implements IndexTableIF {
   private String name = null;
   private String indexedTable = null;
   private IndexTableIF referencedTable = null;
   private boolean isAllFieldsAscending = false;
   private boolean allowNulls = false;
   private boolean unique = false;
   private boolean primaryKey = false;
   private boolean foreignKey = false;
   private IndexFieldIF[] indexFields = null;
   private int[] indexFieldsPositions;
   private final String stringRepresentation;

   public DefaultIndexTable(String indexName, String indexedTable,
                            IndexFieldIF[] indexFields) {
      this.name = indexName;
      this.indexedTable = indexedTable;
      this.indexFields = indexFields;

      if (indexFields.length == 0) throw new IllegalArgumentException("indexFields has size = 0");

      StringBuilder stringRepresentationBuilder = new StringBuilder(indexName).
              append(" ON ").append(indexedTable).append("(");
      for (int i = 0; i < indexFields.length; i++) {
         stringRepresentationBuilder.append(indexFields[i].toString());
         if (i < indexFields.length - 1) stringRepresentationBuilder.append(", ");
      }
      stringRepresentationBuilder.append(")");
      this.stringRepresentation = stringRepresentationBuilder.toString();
   }

   public String getIndexedTable() {
      return indexedTable;
   }

   public String getIndexName() {
      return name;
   }

   public String getSQLString(boolean preserveColumnNames) {
      StringBuffer sqlString;

      if (isPrimaryKey()) sqlString = new StringBuffer("CONSTRAINT " + getIndexName() +
              " PRIMARY KEY(");
      else if (isUnique()) sqlString = new StringBuffer("CONSTRAINT " +
              getIndexName() + "UNIQUE(");
      else sqlString = new StringBuffer("(");

      sqlString.append(getFieldsCommaListString(preserveColumnNames));

      sqlString.append(")");

      return sqlString.toString();
   }

   public String getFieldsCommaListString(boolean preserveColumnNames) {
      StringBuilder sqlString = new StringBuilder();

      for (int i = 0; i < indexFields.length; i++) {
         sqlString.append(com.relationaljunction.utils.StringUtils.
                 quoteFieldAndTableName(indexFields[i].getStoreField().getName(), preserveColumnNames));
         if (i < indexFields.length - 1) sqlString.append(",");
      }

      return sqlString.toString();
   }

   public IndexFieldIF[] getIndexFields() {
      return indexFields;
   }

   public int[] getIndexFieldsPositions() {
      return indexFieldsPositions;
   }

   public void setIndexFieldsPositions(int[] indexFieldsPositions) {
      this.indexFieldsPositions = indexFieldsPositions;
   }

   public boolean isPrimaryKey() {
      return primaryKey;
   }

   public boolean isUnique() {
      return unique;
   }

   public boolean isForeignKey() {
      return foreignKey;
   }

   public IndexTableIF getReferencedIndex() {
      return referencedTable;
   }

   public boolean isAutonumber() {
      return indexFields.length == 1 &&
              indexFields[0].getStoreField().getType() == StoreDataType.IDENTITY;
   }

   public boolean isAllFieldsAscending() {
      return isAllFieldsAscending;
   }

   public void setAllFieldsAscending(boolean allFieldsAscending) {
      isAllFieldsAscending = allFieldsAscending;
   }

   public boolean allowNulls() {
      return allowNulls;
   }

   public void setPrimaryKey(boolean primaryKey) {
      this.primaryKey = primaryKey;
   }

   public void setAllowNulls(boolean allowNulls) {
      this.allowNulls = allowNulls;
   }

   public void setUnique(boolean unique) {
      this.unique = unique;
   }

   public void setForeignKey(boolean foreignKey) {
      this.foreignKey = foreignKey;
   }

   public void setReferencedIndexTable(IndexTableIF referencedTable) {
      this.referencedTable = referencedTable;
   }

   @Override
   public String toString() {
      return this.stringRepresentation;
   }
}
