package com.relationaljunction.jdbc.common.h2.engine;

import org.h2.command.query.AllColumnsForPlan;
import org.h2.engine.Constants;
import org.h2.engine.Session;
import org.h2.engine.SessionLocal;
import org.h2.index.Cursor;
import org.h2.index.IndexCondition;
import org.h2.index.IndexType;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.TableFilter;

import com.relationaljunction.database.index.IndexTableIF;
import com.relationaljunction.utils.StringUtils;

/**
 * external index supported by StelsMDB
 */
public class StoreFieldIndex extends AbstractIndex {
   public StoreFieldIndex(StoreTableWrapper storeTableWrapper, IndexTableIF indexTable,
                          boolean persistent) {
      super(storeTableWrapper, indexTable);

      // define an index type
      IndexType indexType;
      if (indexTable.isPrimaryKey())
         indexType = IndexType.createPrimaryKey(persistent, persistent);
      else if (indexTable.isUnique())
         indexType = IndexType.createUnique(persistent, persistent);
      else
         indexType = IndexType.createNonUnique(persistent);

      // init indexed columns
      Column[] columns = new Column[indexTable.getIndexFields().length];

      for (int i = 0; i < indexTable.getIndexFields().length; i++) {
         columns[i] = storeTableWrapper.getColumn(StringUtils.toUpperCaseIfNotReserved(
                 indexTable.getIndexFields()[i].getStoreField().getName()));
      }

//      initBaseIndex(storeTableWrapper, storeTableWrapper.getId(), indexTable.getIndexName(),
//              IndexColumn.wrap(columns), indexType);
   }

   public Cursor getIndexCursor(SearchRow first, SearchRow last) {
      throw new UnsupportedOperationException("StoreFieldIndex.getIndexCursor()");
   }

   @Override
   public double getCost(SessionLocal session, int[] masks, TableFilter[] filters, int filter, SortOrder sortOrder,
           AllColumnsForPlan allColumnsSet) {
       return getCostRangeIndex(masks, getRowCountApproximation(session));
   }

   @Override
   public long getCostRangeIndex(int[] masks, long rowCount) {
      rowCount += Constants.COST_ROW_OFFSET;
      long cost = rowCount;
      long rows = rowCount;
      int totalSelectivity = 0;
      if (masks == null) {
         return cost;
      }
      for (int i = 0, len = columns.length; i < len; i++) {
         Column column = columns[i];
         int index = column.getColumnId();
         int mask = masks[index];
         if ((mask & IndexCondition.EQUALITY) == IndexCondition.EQUALITY) {
            if (i == columns.length - 1 && getIndexType().isUnique()) {
               cost = getLookupCost(rowCount) + 1;
               break;
            }
            totalSelectivity = 100 - ((100 - totalSelectivity) * (100 - column.getSelectivity()) / 100);
            long distinctRows = rowCount * totalSelectivity / 100;
            if (distinctRows <= 0) {
               distinctRows = 1;
            }
            rows = Math.max(rowCount / distinctRows, 1);
            cost = getLookupCost(rowCount) + rows;
         } else if ((mask & IndexCondition.RANGE) == IndexCondition.RANGE) {
            cost = getLookupCost(rowCount) + rows / 4;
            break;
         } else if ((mask & IndexCondition.START) == IndexCondition.START) {
            cost = getLookupCost(rowCount) + rows / 3;
            break;
         } else if ((mask & IndexCondition.END) == IndexCondition.END) {
            cost = rows / 3;
            break;
         } else {
            break;
         }
      }
      return cost;
   }

   private int getLookupCost(long rowCount) {
       return 2;
   }
}
