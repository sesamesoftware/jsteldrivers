package com.relationaljunction.jdbc.xml.h2.store;

import java.util.List;

import org.h2.index.Cursor;

import com.relationaljunction.database.*;

public class XMLTableReader2 implements StoreTableReaderIF {
  private XMLTable xmlTable = null;

  public XMLTableReader2(XMLTable xmlTable) throws StoreException{
    this.xmlTable = xmlTable;
  }

  public StoreTableIF getStoreTable(){
    return xmlTable;
  }

   public Cursor getIndexCursor(List<String> columnNames) {
      return null;
   }

  public StoreRecordIF nextRecord() throws StoreException {
    /**@todo Implement this com.relationaljunction.database.StoreTableReaderIF method*/
    throw new UnsupportedOperationException(
	"Method nextRecord() not yet implemented.");
  }

  public StoreFieldIF[] getFields() {
    return xmlTable.fields;
  }

  public int getRecordCount(){
    /**@todo Implement this com.relationaljunction.database.StoreTableReaderIF method*/
    throw new UnsupportedOperationException(
	"Method getRecordCount() not yet implemented.");
  }

  public void close(){
    xmlTable = null;
  }

}
