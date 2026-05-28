package com.relationaljunction.jdbc.mdb.h2;

import org.h2.command.ddl.CreateTableData;

import com.relationaljunction.database.StoreTableIF;
import com.relationaljunction.database.index.IndexTableIF;
import com.relationaljunction.jdbc.common.h2.engine.AbstractIndex;
import com.relationaljunction.jdbc.common.h2.engine.StoreTableWrapper;

public class MDBTableWrapper extends StoreTableWrapper {
   public MDBTableWrapper(CreateTableData data, StoreTableIF storeTable, IndexTableIF[] storeTableIndexes) {
      super(data, storeTable, storeTableIndexes);
   }

   @Override
   protected AbstractIndex getIndex(IndexTableIF storeTableIndex, boolean persistence) {
      return new MDBFieldIndex(this, storeTableIndex, persistence);
   }
}
