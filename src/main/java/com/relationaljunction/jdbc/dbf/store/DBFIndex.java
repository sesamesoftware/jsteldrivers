package com.relationaljunction.jdbc.dbf.store;

import java.util.Properties;

import org.xBaseJ.*;

import com.relationaljunction.database.*;
import com.relationaljunction.database.index.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 2.2
 */

public class DBFIndex
        implements IndexTableIF {
   public final static String MDX_TYPE = "MDX";
   public final static String NDX_TYPE = "NDX";
   String path;
   String indexName;
   String indexedTable;
   StoreFieldIF[] indexedFields;
   StoreFieldIF[] tableFields;
   String indexType;
   boolean isUnique = false;
   // props
   String charset;

   public DBFIndex(String path, String indexName, String indexedTable,
                   StoreFieldIF[] indexedFields, StoreFieldIF[] tableFields,
                   Properties props) {
      this.path = path;
      this.indexName = indexName;
      this.indexedTable = indexedTable;
      this.indexedFields = indexedFields;
      this.tableFields = tableFields;
      loadPropertiesInVars(props);
   }

   private void loadPropertiesInVars(Properties props) {
      if (props != null) {
         if (props.getProperty(IndexSchemaIF.INDEX_TYPE_PROPERTY) != null) {
            indexType = props.getProperty(IndexSchemaIF.INDEX_TYPE_PROPERTY);
         }
         if (props.getProperty(IndexSchemaIF.UNIQUE_PROPERTY) != null) {
            isUnique = Boolean.valueOf(props.getProperty(
                    IndexSchemaIF.UNIQUE_PROPERTY)).booleanValue();
         }
      }
   }

   public String getIndexedTable() {
      return indexedTable;
   }

   public String getIndexName() {
      return indexName;
   }

   public String getSQLString(boolean caseSensivity) {
      return "";
   }

   public String getFieldsCommaListString(boolean caseSensivity) {
      return "";
   }

   public IndexFieldIF[] getIndexFields() {
      return null;
   }

   public StoreTableReaderIF findRecords(Object[] objs) throws StoreException {
      return new DBFIndexReader(this, buildIndexKey(objs));
   }

   public void create(StoreFieldIF[] fields) throws StoreException {
      if (indexType.equals(NDX_TYPE)) {
         try {
            DBF dbf = new DBF(path + indexedTable, DBF.READ_ONLY,
                    charset);
            String columnsKey = buildColumnKey(fields);
            dbf.createIndex(path + indexName, columnsKey, true,
                    isUnique);
            dbf.close();
         } catch (Exception ex) {
            throw new StoreException("Can't create the index file '" + indexName +
                    "'. [XBaseJ] " + ex.getMessage(), ex);
         }
      }
   }

   public StoreTableWriterIF getWriter(StoreFieldIF[] fields) throws
           StoreException {
      /**@todo Implement this com.relationaljunction.database.StoreTableIF method*/
      throw new UnsupportedOperationException(
              "Method getWriter() not yet implemented.");
   }

   public boolean isReadOnly() {
      return true;
   }

   public Properties getTableProperties() {
      return new Properties();
   }

   public String getCharset() {
      return charset;
   }

   public void setCharset(String charset) {
      this.charset = charset;
   }

   private String buildColumnKey(StoreFieldIF[] fields) {
      StringBuffer columnsKey = new StringBuffer();
      for (int i = 0; i < fields.length; i++) {
         columnsKey.append(fields[i].getName());
         if (i != fields.length - 1)
            columnsKey.append("+");
      }
      return columnsKey.toString();
   }

   private String buildIndexKey(Object[] objs) throws StoreException {
       String indexKey = "";
      for (int i = 0; i < indexedFields.length; i++) {
//      indexKey.append(DBFTable.getDBFObject(indexedFields[i],
//                                            objs[i]));
      }
      return indexKey;
   }

   public void reIndex() throws StoreException {
      /**@todo Implement this com.relationaljunction.database.index.IndexTableIF method*/
      throw new UnsupportedOperationException("Method reIndex() not yet implemented.");
   }

   public boolean isPrimaryKey() {
      return false;
   }

   public boolean isForeignKey() {
      return false;
   }

   public boolean isUnique() {
      return false;
   }

   public IndexTableIF getReferencedIndex() {
      return null;
   }

   public boolean isAutonumber() {
      return false;
   }

   public boolean allowNulls() {
      return false;
   }

   public int[] getIndexFieldsPositions() {
      return new int[0];
   }

   public boolean isAllFieldsAscending() {
      return false;
   }
}
