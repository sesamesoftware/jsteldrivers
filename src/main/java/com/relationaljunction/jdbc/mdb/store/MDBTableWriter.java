package com.relationaljunction.jdbc.mdb.store;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.Cursor;
import com.healthmarketscience.jackcess.CursorBuilder;
import com.healthmarketscience.jackcess.DataType;
import com.healthmarketscience.jackcess.Index;
import com.healthmarketscience.jackcess.IndexCursor;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import com.healthmarketscience.jackcess.complex.ComplexValueForeignKey;
import com.healthmarketscience.jackcess.util.OleBlob;
import com.relationaljunction.database.AbstractStoreTableWriter;
import com.relationaljunction.database.StoreException;
import com.relationaljunction.database.StoreFieldIF;
import com.relationaljunction.database.StoreRecordIF;
import com.relationaljunction.database.StoreRecordsIF;
import com.relationaljunction.database.StoreTableIF;
import com.relationaljunction.utils.OtherUtils;

public class MDBTableWriter extends AbstractStoreTableWriter {
   
   private final Logger log = LoggerFactory.getLogger("MDBTableWriter");

   private static final boolean USE_PRIMARY_KEYS_TO_UPDATE = true;

   private MDBTable mdbTable = null;
   private List<? extends Column> mdbCols = null;
   private Table t = null;
   private int colsCount = -1;
   private Column[] primaryKeyColumns = null;
   private Index primaryKeyIndex = null;

   public MDBTableWriter(MDBTable mdbTable, StoreFieldIF[] storeFields) throws
           StoreException {
      this.mdbTable = mdbTable;
      try {
         t = mdbTable.schema.getDatabase().getTable(mdbTable.getName());
      } catch (Exception ex) {
         ex.printStackTrace();
         throw new StoreException("Can't read the MDB file. [Jackcess] " + ex.getMessage(), ex);
      }
      if (storeFields == null && t == null) throw new StoreException("Table '" +
              mdbTable.getName() + "' does not exist!");

      mdbCols = t.getColumns();
      colsCount = mdbCols.size();

      // if a table should be rewrited => clear the table for new records to insert
      if (storeFields != null)
         clearRecords();

      initPrimaryKey();
   }

   private void initPrimaryKey() {
      List<? extends Index> indexes = t.getIndexes();

      for (Index index : indexes) {
         if (index.isPrimaryKey()) {
            primaryKeyIndex = index;
            List<? extends Index.Column> columnDescriptors = index.getColumns();
            primaryKeyColumns = new Column[columnDescriptors.size()];
            for (int i = 0; i < primaryKeyColumns.length; i++) {
               primaryKeyColumns[i] = columnDescriptors.get(i).getColumn();
            }
            break;
         }
      }
   }

   public StoreTableIF getStoreTable() {
      return mdbTable;
   }

   public void clearRecords() throws StoreException {
      try {
//    t.reset();
         Row row;

         while ((row = t.getNextRow()) != null) {
            t.deleteRow(row);
         }
         if (!mdbTable.schema.autoSync) {
            mdbTable.schema.getDatabase().flush();
         }
      } catch (Exception ex) {
         ex.printStackTrace();
         throw new StoreException("Can't clear a table '" + t.getName() +
                 "'. [Jackcess] Error was: " + ex.getMessage(), ex);
      }
   }

   private boolean deleteUsingPrimaryKey(ResultSet rsOperations) throws
           StoreException {
      boolean success = true;

      OtherUtils.writeTraceInfo(log, "deleting records from the table '" + mdbTable.getName() +
              "' using a primary key");

      try {
         CursorBuilder cursorBuilder = new CursorBuilder(t);
         cursorBuilder.setIndex(primaryKeyIndex);
         IndexCursor primaryKeyCursor = cursorBuilder.toIndexCursor();

//         IndexCursor primaryKeyCursor = IndexCursor.createCursor(t, primaryKeyIndex);

         rsOperations.absolute(1);
         Object[] keyValues = new Object[primaryKeyColumns.length];

         while (rsOperations.getRow() != 0) {
            for (int i = 0; i < keyValues.length; i++) {
               keyValues[i] = mdbTable.schema.getMDBObject(primaryKeyColumns[i],
                       rsOperations.getObject(primaryKeyColumns[i].getName()));
            }

            boolean found = primaryKeyCursor.findFirstRowByEntry(keyValues);

            if (!found) {
               log.warn("Can't find a row using a primary key " + primaryKeyIndex.toString());
               return false;
            }

            primaryKeyCursor.deleteCurrentRow();

            rsOperations.relative(1);
         }

      } catch (Exception ex) {
         throw new StoreException("Error in deleteUsingPrimaryKey(). Error was: " +
                 ex.getMessage(), ex);
      }

      return success;
   }

   public void deleteRecords(PreparedStatement pst) throws StoreException {
      try {
         ResultSet rsOperations = pst.executeQuery();
         // last update operation
         rsOperations.last();
         int operationEndIndex = rsOperations.getRow();
         if (operationEndIndex == 0) return;

         // try to delete records using a primary key
         if (USE_PRIMARY_KEYS_TO_UPDATE && primaryKeyIndex != null &&
                 deleteUsingPrimaryKey(rsOperations)) {
            return;
         }

         t.reset();

         OtherUtils.writeTraceInfo(log, "deleting from the table '" + mdbTable.getName() +
                 "' using a full scan");

         Cursor cursor = t.getDefaultCursor();

         while (cursor.moveToNextRow()) {
            //	  com.relationaljunction.utils.TestUtils.printColumnsOut(rsOperations, System.out);
            //	  com.relationaljunction.utils.TestUtils.printResultSetOut(rsOperations, System.out);
            Row row = cursor.getCurrentRow();

            rsOperations.absolute(1);

            while (rsOperations.getRow() != 0) {
               // compare the current record to the current operation record
               boolean isRowsEqual = compareCurrentRow(row, rsOperations);
               if (isRowsEqual) {
                  cursor.deleteCurrentRow();

                  operationEndIndex -= 1;
                  if (operationEndIndex == 0)
                     // all records have been updated
                     return;

                  break;
               } else
                  // move to the next operation
                  rsOperations.relative(1);
            }
         }
      } catch (Exception ex) {
         ex.printStackTrace();
         throw new StoreException("Can't delete a record in the table '" +
                 mdbTable.getName() + "' [Jackcess] " +
                 ex.getMessage(), ex);
      }
   }

   private boolean updateUsingPrimaryKey(ResultSet rsOperations) throws
           StoreException {
      boolean success = true;

      OtherUtils.writeTraceInfo(log, "updating the table '" + mdbTable.getName() +
              "' using a primary key");

      try {
         CursorBuilder cursorBuilder = new CursorBuilder(t);
         cursorBuilder.setIndex(primaryKeyIndex);
         IndexCursor primaryKeyCursor = cursorBuilder.toIndexCursor();

         rsOperations.absolute(1);
         Object[] keyValues = new Object[primaryKeyColumns.length];

         while (rsOperations.getRow() != 0) {
            for (int i = 0; i < keyValues.length; i++) {
               keyValues[i] = mdbTable.schema.getMDBObject(primaryKeyColumns[i],
                       rsOperations.getObject(primaryKeyColumns[i].getName()));
            }

            boolean found = primaryKeyCursor.findFirstRowByEntry(keyValues);

            if (!found) {
               log.warn("Can't find a row using a primary key " + primaryKeyIndex.toString());
               return false;
            }

            rsOperations.relative(1);

            updateCurrentRow(primaryKeyCursor, rsOperations);

            rsOperations.relative(1);
         }

      } catch (Exception ex) {
         throw new StoreException("Error in updateUsingPrimaryKey(). Error was: " +
                 ex.getMessage(), ex);
      }

      return success;
   }

   public void updateRecords(PreparedStatement pst) throws
           StoreException {
      try {
         ResultSet rsOperations = pst.executeQuery();
         // last update operation
         rsOperations.last();
         int operationEndIndex = rsOperations.getRow();
         if (operationEndIndex == 0) return;

         // try to update a table using a primary key
         if (USE_PRIMARY_KEYS_TO_UPDATE && primaryKeyIndex != null &&
                 updateUsingPrimaryKey(rsOperations)) {
            return;
         }

         t.reset();

         OtherUtils.writeTraceInfo(log, "updating the table '" + mdbTable.getName() +
                 "' using a full scan");

         Cursor cursor = t.getDefaultCursor();

         while (cursor.moveToNextRow()) {
            //	  com.relationaljunction.utils.TestUtils.printColumnsOut(rsOperations, System.out);
            //	  com.relationaljunction.utils.TestUtils.printResultSetOut(rsOperations, System.out);
            Row row = cursor.getCurrentRow();

            rsOperations.absolute(1);

            while (rsOperations.getRow() != 0) {
               // compare the current record to the current operation record
               boolean isRowsEqual = compareCurrentRow(row, rsOperations);
               if (isRowsEqual) {
                  // read a new record
                  rsOperations.relative(1);
                  // update the current record
                  updateCurrentRow(cursor, rsOperations);

                  operationEndIndex -= 2;
                  if (operationEndIndex == 0)
                     // all records have been updated
                     return;

                  break;
               } else
                  // move to the next operation
                  rsOperations.relative(2);
            }
         }
      } catch (Exception ex) {
         ex.printStackTrace();
         throw new StoreException("Can't update a record in the table '" +
                 mdbTable.getName() + "' [Jackcess] " +
                 ex.getMessage(), ex);
      }
   }

   /**
    * compare a current row in MS Access with a current row in operations log
    * for updating/deleting if a primary key does not exist.
    *
    * @param rowMap       - current row in MS Access
    * @param rsOperations - ResultSet retrived for operations log
    * @return
    * @throws SQLException
    */
   private boolean compareCurrentRow(Map rowMap, ResultSet rsOperations) throws
           SQLException {
      for (int i = 0; i < mdbCols.size(); i++) {
         Column mdbCol = mdbCols.get(i);

         // ignore OLE and COMPLEX types while comparing
         if (mdbCol.getType() == DataType.OLE || mdbCol.getType() == DataType.COMPLEX_TYPE) {
            continue;
         }

         Object mdbValue = mdbTable.schema.getEngineObject(mdbCol, rowMap.get(mdbCol.getName()));
         Object value2compare = rsOperations.getObject(i + 4);

         if (mdbValue == null && value2compare == null) {
            continue;
         }

         // consider 'null' and 'false' values are equal
         if (mdbValue instanceof Boolean &&
                 !(Boolean) mdbValue && value2compare == null) {
            continue;
         }

         // compare values
         if (mdbValue != null && value2compare == null ||
                 mdbValue == null && value2compare != null ||
                 !mdbValue.equals(value2compare)) {
            return false;
         }
      }

      return true;
   }

   private void updateCurrentRow(Cursor cursor, ResultSet rsOperations) throws
           Exception {
      Object[] sourceObjs = new Object[colsCount];
      Object[] objsToUpdate = new Object[colsCount];

      for (int i = 0; i < colsCount; i++) {
         Column mdbCol = mdbCols.get(i);
         sourceObjs[i] = rsOperations.getObject(i + 4);
         objsToUpdate[i] = mdbTable.schema.getMDBObject(mdbCol, sourceObjs[i]);
      }

      cursor.updateCurrentRow(objsToUpdate);

      processBinaryColumns(sourceObjs, objsToUpdate);
   }

   /**
    * tricky postprocessing of complex columns (Attachment, OLE) if they exists
    *
    * @param rsOperations
    * @param objs
    * @throws IOException
    * @throws SQLException
    */
   private void processBinaryColumns(Object[] sourceObjs, Object[] objsToUpdate) throws Exception {
      // add/update Attachments
      for (Column complexColumn : mdbTable.complexColumns) {
         ComplexValueForeignKey complexValue = (ComplexValueForeignKey)
                 complexColumn.getRowValue(objsToUpdate);

         if (complexValue.countValues() > 0) {
            complexValue.deleteAllValues();
         }

         MDBAttachmentContainer mdbAttachmentContainer = (MDBAttachmentContainer)
                 sourceObjs[complexColumn.getColumnIndex()];

         // add attachments
         for (MDBAttachmentContainer.Attachment attachment :
                 mdbAttachmentContainer.getAttachments()) {
            complexValue.addAttachment(attachment.getFileUrl(),
                    attachment.getFileName(),
                    attachment.getFileType(),
                    attachment.getData(),
                    attachment.getFileTimeStamp(),
                    attachment.getFileFlags()).update();
         }
      }

      // close OLE (BLOB) objects if they exist
      if (mdbTable.hasOLEcolumns) {
         for (Object obj : objsToUpdate) {
            if (obj instanceof OleBlob) {
               ((OleBlob) obj).close();
            }
         }
      }
   }

   public void insertRecords(StoreRecordsIF recs) throws StoreException {
      try {
//      t.reset();
         recs.beforeFirst();

         Object[] sourceObjs = new Object[colsCount];
         Object[] objsToInsert = new Object[colsCount];

         while (recs.hasNext()) {
            StoreRecordIF rec = recs.nextRecord();
            for (int i = 0; i < colsCount; i++) {
               Column mdbCol = mdbCols.get(i);
               sourceObjs[i] = rec.getObject(i);
               objsToInsert[i] = mdbTable.schema.getMDBObject(mdbCol, sourceObjs[i]);
            }

            t.addRow(objsToInsert);

            processBinaryColumns(sourceObjs, objsToInsert);
         }
      } catch (Exception ex) {
         ex.printStackTrace();
         throw new StoreException("Can't insert a record in the table '" +
                 mdbTable.getName() + "' [Jackcess] " +
                 ex.getMessage(), ex);
      }
   }

   @Deprecated
   public void updateRecords(StoreRecordsIF recs) throws StoreException {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   public void deleteRecords(StoreRecordsIF recs) throws StoreException {
      throw new UnsupportedOperationException();
   }

   public void close() {
      try {
         if (!mdbTable.schema.autoSync)
            mdbTable.schema.getDatabase().flush();
         mdbTable.schema.fm.flush();
      } catch (Exception ex) {
         throw new RuntimeException("Error while flushing an MDB file: " + ex.getMessage());
      }

      mdbTable = null;
      mdbCols = null;
      t = null;
   }

}
