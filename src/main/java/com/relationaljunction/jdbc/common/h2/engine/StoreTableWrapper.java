package com.relationaljunction.jdbc.common.h2.engine;

import java.util.ArrayList;

import org.h2.command.ddl.CreateTableData;
import org.h2.engine.Session;
import org.h2.engine.SessionLocal;
import org.h2.index.Index;
import org.h2.index.IndexType;
import org.h2.result.Row;
import org.h2.table.IndexColumn;
import org.h2.table.TableBase;
import org.h2.table.TableType;

import com.relationaljunction.database.StoreException;
import com.relationaljunction.database.StoreTableIF;
import com.relationaljunction.database.StoreTableReaderIF;
import com.relationaljunction.database.index.IndexTableIF;
import com.relationaljunction.utils.UnexpectedException;

/**
 * custom external table based on StoreTableIF
 */
public class StoreTableWrapper extends TableBase {
   public static final boolean PERSISTENT_INDEXES = true;

   protected StoreTableIF storeTable;
   private ArrayList<Index> indexes = new ArrayList<Index>();
   private Index primaryKey;
   private Index uniqueIndex;
   private long approximateRowCount = 0;

   public StoreTableWrapper(CreateTableData data, StoreTableIF storeTable, IndexTableIF[] storeTableIndexes) {
      super(data);
      this.storeTable = storeTable;

      approximateRowCount = getRecordCount();
      initIndexes(storeTableIndexes);
   }

   private int getRecordCount() {
      int rowCount;

      try {
         StoreTableReaderIF reader = storeTable.getReader();
         rowCount = reader.getRecordCount();
         reader.close();

      } catch (StoreException e) {
         throw new UnexpectedException(e);
      }

      return rowCount;
   }

   private void initIndexes(IndexTableIF[] storeTableIndexes) {
      // the first index in H2 should be a scan index.
      // Table.getBestPlanItem() starts counting indexes with index 1
      indexes.add(new StoreScanIndex(this, PERSISTENT_INDEXES));

      if (storeTableIndexes == null) return;

      for (IndexTableIF storeTableIndex : storeTableIndexes) {
         // ignore foreign keys to avoid duplicating keys
         if (storeTableIndex.isForeignKey()) continue;

         // ignore indexes containing columns with descending order
         if (!storeTableIndex.isAllFieldsAscending()) continue;

         Index index = getIndex(storeTableIndex, PERSISTENT_INDEXES);

         if (storeTableIndex.isPrimaryKey()) {
            primaryKey = index;
         } else if (storeTableIndex.isUnique() && uniqueIndex == null) {
            uniqueIndex = index;
         }

         indexes.add(index);
      }
   }

   protected AbstractIndex getIndex(IndexTableIF storeTableIndex, boolean persistence) {
      throw new UnsupportedOperationException("StoreTableWrapper.getIndex()");
   }

   public StoreTableIF getStoreTable() {
      return storeTable;
   }


   /**
    * The method is called while closing an H2 connection or when H2 attempts to drop a table.
    *
    * @param SessionLocal the session
    */
   @Override
   public void close(SessionLocal session) {
      storeTable = null;

      if (indexes != null && !indexes.isEmpty()) {
         for (Index index : indexes) {
            index.close(session);
         }
      }

      indexes = null;
   }

   @Override
   public void unlock(SessionLocal s) {
   }

   @Override
   public Index addIndex(SessionLocal session, String indexName, int indexId, IndexColumn[] cols, int uniqueColumnCount,
                         IndexType indexType, boolean create, String indexComment) {
      throw new UnsupportedOperationException("Indices are not supported in this mode");
   }

   @Override
   public void removeRow(SessionLocal session, Row row) {
      // should be empty. The operation itself will be processed by the corresponding trigger
   }

   @Override
   public void addRow(SessionLocal session, Row row) {
      // should be empty. The operation itself will be processed by the corresponding trigger
   }

   @Override
   public void checkSupportAlter() {
   }

   @Override
   public TableType getTableType() {
      return TableType.EXTERNAL_TABLE_ENGINE;
   }

   @Override
   public Index getScanIndex(SessionLocal session) {
      return indexes.get(0);
   }

   @Override
   public ArrayList<Index> getIndexes() {
      return indexes;
   }

   @Override
   public boolean isLockedExclusively() {
      return false;
   }

   @Override
   public long getMaxDataModificationId() {
      return 0;
   }

   @Override
   public boolean isDeterministic() {
      return false;
   }

   @Override
   public boolean canGetRowCount(SessionLocal session) {
      return true;
   }

   @Override
   /**
    * the method is called when H2 attempts to drop a table
    */
   public boolean canDrop() {
      // clear all constraints related with a table
      if (getConstraints() != null && getConstraints().size() > 0) getConstraints().clear();
      close(null);
      return true;
   }
   
   @Override
   public long getRowCount(SessionLocal session) {
      approximateRowCount = getRecordCount();
      return approximateRowCount;
   }

   @Override
   public long getRowCountApproximation(SessionLocal session) {
      return approximateRowCount;
   }

   public void setRowCountApproximation(long approximateRowCount) {
      this.approximateRowCount = approximateRowCount;
   }

   @Override
   public void checkRename() {
   }

   @Override
   public long truncate(SessionLocal session) {
       return 0;
   }


}
