package com.relationaljunction.database;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 * @author not attributable
 * @version 2.2
 */

public interface StoreRecordIF {

  int getSize();

  int getID();

  boolean isInserted();

  boolean isUpdated();

  boolean isDeleted();

  boolean isNotChanged();

  int getChangeType();

  Object getObject(int i);

   StoreFieldIF getField(int i);

//  public Object getObject(String fieldName);

  Object[] getObjects();

  StoreFieldIF[] getFields();

}


