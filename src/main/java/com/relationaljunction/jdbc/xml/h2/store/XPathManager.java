package com.relationaljunction.jdbc.xml.h2.store;

import com.relationaljunction.database.StoreException;
import com.relationaljunction.database.StoreFieldIF;


abstract class XPathManager {
  protected XMLTable xmlTable = null;

  abstract public XMLTableLoaderIF getLoader() throws Exception;

  abstract public XMLTableSaverIF getSaver() throws Exception;

  abstract public void create(StoreFieldIF[] fields) throws StoreException;
}
