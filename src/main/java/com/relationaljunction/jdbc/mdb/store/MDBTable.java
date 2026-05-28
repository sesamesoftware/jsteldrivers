package com.relationaljunction.jdbc.mdb.store;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.ColumnBuilder;
import com.healthmarketscience.jackcess.IndexBuilder;
import com.healthmarketscience.jackcess.Table;
import com.healthmarketscience.jackcess.TableBuilder;
import com.relationaljunction.database.AbstractStoreTable;
import com.relationaljunction.database.StoreException;
import com.relationaljunction.database.StoreFieldIF;
import com.relationaljunction.database.StoreTableReaderIF;
import com.relationaljunction.database.StoreTableWriterIF;
import com.relationaljunction.database.index.IndexTableIF;
import com.relationaljunction.utils.OtherUtils;
import com.relationaljunction.utils.UnexpectedException;

public class MDBTable extends AbstractStoreTable {
   private final Logger log = LoggerFactory.getLogger("MDBTable");
   String tableName = null;
   MDBSchema schema = null;
   List<Column> complexColumns = new LinkedList<Column>();
   boolean hasOLEcolumns = false;

   public MDBTable(String tableName, MDBSchema schema) {
      this.tableName = tableName;
      this.schema = schema;
   }

   public void create(StoreFieldIF[] fields, IndexTableIF[] indexTables) throws StoreException {
      Table t;
      try {
         t = schema.getDatabase().getTable(getName());
      } catch (Exception ex) {
         throw new StoreException("Can't read the MDB file. [Jackcess] " + ex.getMessage(), ex);
      }
      if (t != null) throw new StoreException("Table '" + getName() +
              "' is already exist!");
      try {
         IndexBuilder autoNumberIndex = null;
         TableBuilder tableBuilder = new TableBuilder(getName());

         for (StoreFieldIF field : fields) {
            ColumnBuilder column = schema.getMDBColumn(field);

            // create a primary key for the first encountered AUTONUMBER column
            if (column.isAutoNumber() && autoNumberIndex == null) {
               autoNumberIndex = new IndexBuilder("autonumber_index");
               autoNumberIndex.addColumns(column.getName());
               autoNumberIndex.setPrimaryKey();
               tableBuilder.addIndex(autoNumberIndex);

               OtherUtils.writeLogInfo(log, "Table: ", tableName,
                       ". Created a primary index for AUTONUMBER column in a MDB file: ", column.getName());
            }

            tableBuilder.addColumn(column);
         }

         for (IndexTableIF indexTable : indexTables) {
            IndexBuilder mdbIndex = new IndexBuilder(indexTable.getIndexName());
            String[] colNames = new String[indexTable.getIndexFields().length];

            for (int i = 0; i < indexTable.getIndexFields().length; i++) {
               colNames[i] = indexTable.getIndexFields()[i].getStoreField().getName();
            }

            if (indexTable.isPrimaryKey()) {
               if (autoNumberIndex != null)
                  throw new UnexpectedException("Primary key is already set for AUTONUMBER column");
               mdbIndex.setPrimaryKey();
            }
            else if(indexTable.isUnique()){
               mdbIndex.setUnique();
            }

            mdbIndex.addColumns(colNames);
            tableBuilder.addIndex(mdbIndex);
            OtherUtils.writeLogInfo(log, "Table: ", tableName,
                    ". Created an index in a MDB file: " + indexTable);
         }

         tableBuilder.toTable(schema.getDatabase());

//         if (autoNumberIndex != null) {
         // create a table with AUTONUMBER primary key
//            List<IndexBuilder> indexBuilderList = new ArrayList<IndexBuilder>();
//            indexBuilderList.add(autoNumberIndex);
//            tableBuilder.addIndex(autoNumberIndex);
//            schema.getDatabase().createTable(getName(), columnList, indexBuilderList);
//         } else
//            schema.getDatabase().createTable(getName(), columnList);
      } catch (Exception ex) {
         ex.printStackTrace();
         throw new StoreException("Can't create the table '" + getName() +
                 "'. [Jackcess] " + ex.getMessage(), ex);
      }

      try {
         schema.fm.flush();
      } catch (Exception ex) {
         throw new StoreException("Can't synchronize the file '" + schema.fm.getPath() +
                 "' while creating a table. Error was: " +
                 ex.getMessage(), ex);
      }
   }

   public java.util.Date getModificationDate() {
      return schema.getModificationDate();
   }

   public java.util.Date refreshModificationDate() throws StoreException {
      return schema.getModificationDate();
   }

   public String getName() {
      return tableName;
   }

   public StoreTableReaderIF getReader() throws StoreException {
      return new MDBTableReader(this);
   }

   public Properties getTableProperties() {
      return null;
   }

   public StoreTableWriterIF getWriter(StoreFieldIF[] fields) throws
           StoreException {
      return new MDBTableWriter(this, fields);
   }

   public boolean isReadOnly() {
      return false;
   }

}
