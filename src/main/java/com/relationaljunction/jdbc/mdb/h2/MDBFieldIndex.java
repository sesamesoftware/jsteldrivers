package com.relationaljunction.jdbc.mdb.h2;

import org.h2.index.Cursor;
import org.h2.result.SearchRow;

import com.relationaljunction.database.index.IndexTableIF;
import com.relationaljunction.jdbc.common.h2.engine.AbstractCursor;
import com.relationaljunction.jdbc.common.h2.engine.StoreFieldIndex;
import com.relationaljunction.jdbc.common.h2.engine.StoreTableWrapper;

public class MDBFieldIndex extends StoreFieldIndex {
   public MDBFieldIndex(StoreTableWrapper storeTableWrapper, IndexTableIF storeTableIndex,
                        boolean persistent) {
      super(storeTableWrapper, storeTableIndex, persistent);
   }

   @Override
   public Cursor getIndexCursor(SearchRow first, SearchRow last) {
      AbstractCursor cursor = new MDBTableIndexCursor(this, first, last);
      storeTableWrapper.setRowCountApproximation(cursor.getRecordCount());
      return cursor;
   }
}
