package com.relationaljunction.jdbc.dbf.store;

import org.h2.index.Cursor;
import org.xBaseJ.*;
import org.xBaseJ.fields.*;

import com.relationaljunction.database.*;

import java.io.*;
import java.util.List;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 * @author not attributable
 * @version 2.2
 */

public class DBFIndexReader
    implements StoreTableReaderIF {
  private DBFIndex dbfIndex = null;
  private DBF reader = null;
  private String indexKey = null;
  private boolean hasNext = false;

  public DBFIndexReader(DBFIndex dbfIndex, String indexKey) throws
      StoreException {
    this.dbfIndex = dbfIndex;
    this.indexKey = indexKey;
    try {
      reader = new DBF(dbfIndex.path + dbfIndex.indexedTable, DBF.READ_ONLY,
                        dbfIndex.charset);
    }
    catch (Exception ex) {
      throw new StoreException("Can't open the file '" + dbfIndex.path +
                               dbfIndex.indexedTable +
                               "' [XBaseJ] " + ex.getMessage());
    }
    try {
      reader.useIndex(dbfIndex.path + dbfIndex.indexName);
    }
    catch (Exception ex) {
      throw new StoreException("Can't find or use the index '" + dbfIndex.path +
                               dbfIndex.indexedTable +
                               "' [XBaseJ] " + ex.getMessage());
    }
  }

  public StoreTableIF getStoreTable(){
    return null;
  }

  public int getRecordCount(){
    return -1;
  }

   public Cursor getIndexCursor(List<String> columnNames) {
      return null;
   }

  public StoreRecordIF nextRecord() throws StoreException {
    try {
      if (!hasNext) {
        hasNext = reader.findExact(indexKey);
        if (!hasNext)
          return null;
      }
      else {
        try {reader.findNext();}
        catch (Exception ex) {return null;}
        if (!buildIndexKeyFromDBF().equals(indexKey))
          return null;
      }
    }
    catch (Exception ex) {
      throw new StoreException("Exception while using index. [XBaseJ] " +
                               ex.getMessage());
    }
    // form a index result
    Object[] objs = null;
    objs = new Object[getFields().length];
    try {
      for (int i = 0; i < dbfIndex.tableFields.length; i++) {
        Field field = reader.getField(i + 1);
        String str = field.get();
//        objs[i] = DBFTable.getJDBCObject(dbfIndex.tableFields[i], str);
      }
    }
    catch (Exception ex) {
      throw new StoreException("Can't read a record from the index. [XBaseJ] " +
                               ex.getMessage());
    }

    return new DefaultStoreRecord(dbfIndex.tableFields, objs);
  }

  private String buildIndexKeyFromDBF() throws Exception {
    StringBuffer indexKey = new StringBuffer();
    for (int i = 0; i < dbfIndex.indexedFields.length; i++) {
      indexKey.append(reader.getField(dbfIndex.indexedFields[i].getName()).get());
    }
    return indexKey.toString();
  }

  public StoreFieldIF[] getFields(){
    return dbfIndex.tableFields;
  }

  public void close(){
    try {
      reader.close();
    }
    catch (IOException ex) {
      throw new RuntimeException("Can't close the input stream. [XBaseJ] " +
                               ex.getMessage());
    }
  }

}
