package com.relationaljunction.jdbc.mdb.h2;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.h2.result.SearchRow;
import org.h2.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.IndexCursor;
import com.relationaljunction.database.StoreDataType;
import com.relationaljunction.database.StoreRecordIF;
import com.relationaljunction.jdbc.common.h2.engine.AbstractCursor;
import com.relationaljunction.jdbc.common.h2.engine.AbstractIndex;
import com.relationaljunction.jdbc.common.h2.sql.DefaultH2SQLTranslator;
import com.relationaljunction.jdbc.mdb.store.MDBTableReader;
import com.relationaljunction.utils.UnexpectedException;

public class MDBTableIndexCursor extends AbstractCursor {
   private final Logger log = LoggerFactory.getLogger("MDBTableIndexCursor");

   private MDBNativeIndex mdbNativeIndex;
   private Object[] firstMDBRow = null;
   private Object[] lastMDBRow = null;

   public static class MDBNativeIndex {
      private final IndexCursor indexCursor;
      private final List<Column> indexColumns;
      private final List<Integer> indexColumnsPositions;

      public MDBNativeIndex(IndexCursor indexCursor, List<Column> indexColumns,
                            List<Integer> indexColumnsPositions) {
         this.indexCursor = indexCursor;
         this.indexColumns = indexColumns;
         this.indexColumnsPositions = indexColumnsPositions;
      }
   }

   public MDBTableIndexCursor(AbstractIndex storeFieldIndex, SearchRow first, SearchRow last) {
      super(storeFieldIndex);

      mdbNativeIndex = ((MDBTableReader) reader).getIndexCursor(storeFieldIndex.getIndexTable());

      if (first != null) {
         firstMDBRow = convertH2RowToMDB(first);
      }

      if (last != null) {
         lastMDBRow = convertH2RowToMDB(last);
      }

      com.relationaljunction.utils.OtherUtils.writeLogInfo(log, "opening a cursor for the index " +
              storeFieldIndex.getIndexTable(), ", first row = " + first, ", last row = " + last);
   }

   public boolean next() {
      try {
//         com.relationaljunction.utils.OtherUtils.writeTraceInfo(log, "accessing an index for the table '" +
//                 storeFieldIndex.getStoreTableWrapper().getStoreTable().getName() + "'");

         if (currentRow == null && firstMDBRow != null) {
            // try to find the first closest record
            mdbNativeIndex.indexCursor.findClosestRowByEntry(firstMDBRow);
            // if the entry is not found, return false
            if (mdbNativeIndex.indexCursor.isAfterLast()) {
               close();
               return false;
            }

            StoreRecordIF storeRecord = ((MDBTableReader) reader).
                    getEngineRow(mdbNativeIndex.indexCursor.getCurrentRow());

            currentRow = DefaultH2SQLTranslator.convertStoreRecordToH2(storeRecord);
         } else {
            // try to find the next records
            mdbNativeIndex.indexCursor.moveToNextRow();

            // if the entry is not found, return false
            if (mdbNativeIndex.indexCursor.isAfterLast()) {
               close();
               return false;
            }

            Map<String, Object> rowMap = mdbNativeIndex.indexCursor.getCurrentRow();

            StoreRecordIF storeRecord = ((MDBTableReader) reader).getEngineRow(rowMap);

            if (lastMDBRow != null) {
               // if the current entry is higher than the last entry to search, return false.
               // NULLs are sorted low
               if (isRecordBeyondSearchRange(storeRecord, lastMDBRow)) {
                  close();
                  return false;
               }
            }

            currentRow = DefaultH2SQLTranslator.convertStoreRecordToH2(storeRecord);
         }
      } catch (IOException e) {
         throw new UnexpectedException("Error in MDBTableIndexCursor while using an index with columns '" +
                 mdbNativeIndex.indexColumns + "' for the table " +
                 storeFieldIndex.getStoreTableWrapper().getStoreTable().getName(), e);
      }

      return true;
   }

   public boolean isRecordBeyondSearchRange(StoreRecordIF storeRecord, Object[] lastSearchKey) {
      for (int i = 0; i < mdbNativeIndex.indexColumnsPositions.size(); i++) {
         // get a key value
         Object obj = storeRecord.getObject(mdbNativeIndex.indexColumnsPositions.get(i));
         Object keyValue = lastSearchKey[i];

         if (obj == null || keyValue == null) {
            // ignore key values if a search column is NULL
            continue;
         }

         if (!obj.getClass().equals(lastSearchKey[i].getClass())) {
            // convert a key value if it has a different class
            keyValue = DefaultH2SQLTranslator.convertValue(storeRecord.getField(i).getType(), keyValue);

            // convert autonumbers to Long, because by default they are converted to Integer
            if (storeRecord.getField(i).getType() == StoreDataType.IDENTITY) {
               obj = DefaultH2SQLTranslator.convertValue(StoreDataType.BIGINT, obj);
            }
         }

         Comparable comp1 = (Comparable) obj;
         Comparable comp2 = (Comparable) keyValue;
         if (comp1.compareTo(comp2) > 0) return true;
      }

      return false;
   }

   private Object[] convertH2RowToMDB(SearchRow row) {
      Object[] objs = new Object[mdbNativeIndex.indexColumnsPositions.size()];

      for (int i = 0; i < mdbNativeIndex.indexColumnsPositions.size(); i++) {
         Value value = row.getValue(mdbNativeIndex.indexColumnsPositions.get(i));
         objs[i] = ((MDBTableReader) reader).getMDBObject(mdbNativeIndex.indexColumns.get(i),
                 value);
      }

      return objs;
   }

   @Override
   protected void close() {
      com.relationaljunction.utils.OtherUtils.writeLogInfo(log, "closing a cursor for the index " +
              storeFieldIndex.getIndexTable());

      super.close();

      mdbNativeIndex = null;
      firstMDBRow = null;
      lastMDBRow = null;
   }
}
