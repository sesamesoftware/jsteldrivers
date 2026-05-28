package com.relationaljunction.jdbc.common.h2.engine;

import org.h2.result.SearchRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.database.StoreRecordIF;
import com.relationaljunction.jdbc.common.h2.sql.DefaultH2SQLTranslator;
import com.relationaljunction.utils.UnexpectedException;

/**
 * full scan cursor iterating records via StoreTableReaderIF
 */
public class StoreTableScanCursor extends AbstractCursor {
   private final Logger log = LoggerFactory.getLogger("StoreTableScanCursor");

   StoreTableScanCursor(AbstractIndex storeFieldIndex, SearchRow first, SearchRow last) {
      super(storeFieldIndex);

      com.relationaljunction.utils.OtherUtils.writeLogInfo(log, "opening a scan cursor for the table '" +
              storeFieldIndex.getStoreTableWrapper().getStoreTable().getName() + "'");
   }

   public boolean next() {
      try {
         StoreRecordIF rec = reader.nextRecord();

         if (rec == null) {
            close();
            return false;
         } else {
            currentRow = DefaultH2SQLTranslator.convertStoreRecordToH2(rec);
         }
      } catch (Exception e) {
         e.printStackTrace();
         throw new UnexpectedException("Error in StoreTableScanCursor: " + e.getMessage(), e);
      }

      return true;
   }

   @Override
   protected void close() {
      com.relationaljunction.utils.OtherUtils.writeLogInfo(log, "closing a scan cursor for the table '" +
              storeFieldIndex.getStoreTableWrapper().getStoreTable().getName() + "'");

      super.close();
   }
}
