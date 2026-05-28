package com.relationaljunction.jdbc.common.h2.engine;

import org.h2.index.Cursor;
import org.h2.result.Row;
import org.h2.result.SearchRow;

import com.relationaljunction.database.StoreException;
import com.relationaljunction.database.StoreTableReaderIF;
import com.relationaljunction.utils.UnexpectedException;

abstract public class AbstractCursor implements Cursor {
   protected AbstractIndex storeFieldIndex;
   protected StoreTableReaderIF reader;
   protected Row currentRow;


   public AbstractCursor(AbstractIndex storeFieldIndex) {
      this.storeFieldIndex = storeFieldIndex;

      try {
         this.reader = storeFieldIndex.getStoreTableWrapper().getStoreTable().getReader();
      } catch (StoreException e) {
         throw new UnexpectedException(e);
      }
   }

   public Row get() {
      return currentRow;
   }

   public SearchRow getSearchRow() {
      return currentRow;
   }

   abstract public boolean next();

   public boolean previous() {
      throw new UnsupportedOperationException("AbstractCursor.previous()");
   }

   public int getRecordCount() {
      return reader.getRecordCount();
   }

   protected void close() {
      if (reader != null) {
         reader.close();
         reader = null;
      }

      storeFieldIndex = null;
      currentRow = null;
   }
}
