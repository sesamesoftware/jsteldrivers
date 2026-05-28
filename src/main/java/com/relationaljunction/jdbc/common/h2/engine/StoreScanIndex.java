package com.relationaljunction.jdbc.common.h2.engine;

import org.h2.command.query.AllColumnsForPlan;
import org.h2.engine.Constants;
import org.h2.engine.SessionLocal;
import org.h2.index.Cursor;
import org.h2.index.IndexType;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.table.IndexColumn;
import org.h2.table.TableFilter;

/**
 * full scan index that returns full scan cursor
 */
public class StoreScanIndex extends AbstractIndex {

   public StoreScanIndex(StoreTableWrapper storeTableWrapper, boolean persistent) {
      super(storeTableWrapper, null);
//      initBaseIndex(storeTableWrapper, storeTableWrapper.getId(), storeTableWrapper.getName() + "_SCAN",
//              IndexColumn.wrap(storeTableWrapper.getColumns()), IndexType.createScan(persistent));
   }

   @Override
   public Cursor getIndexCursor(SearchRow first, SearchRow last) {
      // init a full scan cursor
      AbstractCursor cursor = new StoreTableScanCursor(this, first, last);
      storeTableWrapper.setRowCountApproximation(cursor.getRecordCount());
      return cursor;
   }

   @Override
   public long getCostRangeIndex(int[] masks, long rowCount) {
      return rowCount + Constants.COST_ROW_OFFSET;
   }

   @Override
   public double getCost(SessionLocal session, int[] masks, TableFilter[] filters, int filter, SortOrder sortOrder,
           AllColumnsForPlan allColumnsSet) {
       return getCostRangeIndex(masks, storeTableWrapper.getRowCountApproximation(session));
   }

}
