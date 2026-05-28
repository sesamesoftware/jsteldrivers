package com.relationaljunction.jdbc.common.h2.engine;

import org.h2.engine.Session;
import org.h2.engine.SessionLocal;
import org.h2.index.Cursor;
import org.h2.index.VirtualTableIndex;
import org.h2.result.Row;
import org.h2.result.SearchRow;

import com.relationaljunction.database.index.IndexTableIF;

abstract public class AbstractIndex extends VirtualTableIndex {
   protected StoreTableWrapper storeTableWrapper;
   protected IndexTableIF indexTable;

   public AbstractIndex(StoreTableWrapper storeTableWrapper, IndexTableIF indexTable) {
      super(null, null, null);
      this.storeTableWrapper = storeTableWrapper;
      this.indexTable = indexTable;
   }

   @Override
   public void add(SessionLocal session, Row row) {
   }

   @Override
   public void close(SessionLocal session) {
      storeTableWrapper = null;
      indexTable = null;
   }

   @Override
   public void remove(SessionLocal session, Row row) {
   }

   public Cursor find(SessionLocal session, SearchRow first, SearchRow last) {
      return getIndexCursor(first, last);
   }

   abstract public Cursor getIndexCursor(SearchRow first, SearchRow last);

   public double getCost(SessionLocal session, int[] masks) {
      return getCostRangeIndex(masks, getRowCountApproximation(session));
   }

   abstract public long getCostRangeIndex(int[] masks, long rowCount);

   @Override
   public void remove(SessionLocal session) {
   }

   @Override
   public void truncate(SessionLocal session) {
   }

   @Override
   public boolean canGetFirstOrLast() {
      return false;
   }

   @Override
   public Cursor findFirstOrLast(SessionLocal session, boolean first) {
      return null;
   }

   @Override
   public boolean needRebuild() {
      return false;
   }

   @Override
   public void checkRename() {
   }

   public StoreTableWrapper getStoreTableWrapper() {
      return storeTableWrapper;
   }

   public IndexTableIF getIndexTable() {
      return indexTable;
   }

   public long getRowCount(SessionLocal session) {
      return storeTableWrapper.getRowCount(session);
   }

   public long getRowCountApproximation(SessionLocal session) {
      return storeTableWrapper.getRowCountApproximation(session);
   }

}
