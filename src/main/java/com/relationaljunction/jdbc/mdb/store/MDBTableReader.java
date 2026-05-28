package com.relationaljunction.jdbc.mdb.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.CursorBuilder;
import com.healthmarketscience.jackcess.DataType;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Index;
import com.healthmarketscience.jackcess.IndexCursor;
import com.healthmarketscience.jackcess.Table;
import com.healthmarketscience.jackcess.complex.ComplexDataType;
import com.relationaljunction.database.DefaultStoreRecord;
import com.relationaljunction.database.StoreException;
import com.relationaljunction.database.StoreFieldIF;
import com.relationaljunction.database.StoreRecordIF;
import com.relationaljunction.database.StoreTableIF;
import com.relationaljunction.database.StoreTableReaderIF;
import com.relationaljunction.database.index.IndexTableIF;
import com.relationaljunction.jdbc.mdb.h2.MDBTableIndexCursor;
import com.relationaljunction.utils.UnexpectedException;

public class MDBTableReader implements StoreTableReaderIF {
   private final Logger log = LoggerFactory.getLogger("MDBTableReader");

   private MDBTable mdbTable = null;
   private StoreFieldIF[] storeFields = null;
   List<? extends Column> mdbCols = null;
   private int curRecord = 0;
   private Database db = null;
   private Table t = null;

   private List<? extends Index> indexes;

   public MDBTableReader(MDBTable mdbTable) throws StoreException {
      this.mdbTable = mdbTable;

      try {
         // open another instance of Database to optimize concurrent reading operations
         db = mdbTable.schema.openDatabase();
         t = db.getTable(mdbTable.getName());
      } catch (Exception ex) {
//         ex.printStackTrace();
         throw new StoreException("Can't read the MDB file. [Jackcess] " + ex.getMessage(), ex);
      }

      if (t == null) throw new StoreException("Table '" + mdbTable.getName() +
              "' does not exist!");

      // added in version 2.1, because it is required by Jackcess
      t.reset();

      initIndexes();
      initFields();
   }

   private void initIndexes() throws StoreException {
      indexes = t.getIndexes();
   }

   private void initFields() throws StoreException {
      try {
         mdbCols = t.getColumns();
         storeFields = new StoreFieldIF[mdbCols.size()];

         for (int i = 0; i < storeFields.length; i++) {
            Column col = mdbCols.get(i);
            storeFields[i] = mdbTable.schema.getStoreField(col);

            // store info about complex columns that will be useful while updating MS Access files
            if (col.getType() == DataType.COMPLEX_TYPE &&
                    col.getComplexInfo().getType() == ComplexDataType.ATTACHMENT) {
               mdbTable.complexColumns.add(col);
            } else if (col.getType() == DataType.OLE) {
               mdbTable.hasOLEcolumns = true;
            }
         }
      } catch (Exception ex) {
         ex.printStackTrace();
         throw new StoreException("Can't read fields from the table '" +
                 mdbTable.getName() + "' [Jackcess] " +
                 ex.getMessage(), ex);
      }
   }

   public StoreTableIF getStoreTable() {
      return mdbTable;
   }

   public MDBTableIndexCursor.MDBNativeIndex getIndexCursor(IndexTableIF indexTable) {
      for (Index index : indexes) {
         List<? extends Index.Column> columnDescriptors = index.getColumns();

         boolean indexFound = true;

         // compare columns to find a required index
         for (int i = 0; i < columnDescriptors.size(); i++) {
            if (!indexTable.getIndexFields()[i].getStoreField().getName().equalsIgnoreCase(
                    columnDescriptors.get(i).getColumn().getName())) {
               indexFound = false;
               break;
            }
         }

         if (!indexFound) continue;

         IndexCursor indexCursor;
         List<Integer> indexColumnPositions = new ArrayList<Integer>(columnDescriptors.size());

         List<Column> indexColumns = new ArrayList<Column>(columnDescriptors.size());
         for (Index.Column colDescr : columnDescriptors) {
            indexColumns.add(colDescr.getColumn());
            indexColumnPositions.add(colDescr.getColumnIndex());
         }

         try {
            CursorBuilder cursorBuilder = new CursorBuilder(t);
            cursorBuilder.setIndex(index);
            indexCursor = cursorBuilder.toIndexCursor();
//            indexCursor = IndexCursor.createCursor(t, index);
         } catch (Exception e) {
            throw new UnexpectedException("Can't create a cursor for the index '" + indexTable, e);
         }

         return new MDBTableIndexCursor.MDBNativeIndex(indexCursor, indexColumns, indexColumnPositions);
      }

      throw new UnexpectedException("Can't find the index " + indexTable);
   }

   public void close() {
      try {
         if (db != null) {
            db.close();
            db = null;
         }
      } catch (IOException e) {
         e.printStackTrace();
      }

      indexes = null;
      mdbTable = null;
      storeFields = null;
      mdbCols = null;
      t = null;
   }

   public StoreFieldIF[] getFields() {
      return storeFields;
   }

   public int getRecordCount() {
      return t.getRowCount();
   }

   public StoreRecordIF nextRecord() throws StoreException {
      StoreRecordIF storeRecord;

      try {
         curRecord++;

         Map<String, Object> rowMap = t.getNextRow();

         // no record found
         if (rowMap == null) return null;

         // convert to an H2 row
         storeRecord = getEngineRow(rowMap);
      } catch (Exception ex) {
         ex.printStackTrace();
         throw new StoreException("Can't read the record with the index " +
                 curRecord + ". [Jackcess] " + ex.getMessage(), ex);
      }

      return storeRecord;
   }

   public StoreRecordIF getEngineRow(Map<String, Object> rowMap) {
      Object[] objs = new Object[storeFields.length];

      for (int i = 0; i < mdbCols.size(); i++) {
         Column mdbCol = mdbCols.get(i);
         Object mdbValue = rowMap.get(mdbCol.getName());
         objs[i] = getEngineObject(mdbCol, mdbValue);
      }

      return new DefaultStoreRecord(getFields(), objs);
   }

   public Object getEngineObject(Column mdbCol, Object mdbValue) {
      return mdbTable.schema.getEngineObject(mdbCol, mdbValue);
   }

   public Object getMDBObject(Column mdbCol, Object engineValue) {
      return mdbTable.schema.getMDBObject(mdbCol, engineValue);
   }
}
