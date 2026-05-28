package com.relationaljunction.database;


/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 2.2
 */

public class DefaultStoreRecord
        implements StoreRecordIF {
   private static final int NOT_CHANGED = -1;
   private static final int INSERTED = 0;
   private static final int UPDATED = 1;
   private static final int DELETED = 2;
   private Object[] objs = null;
   private StoreFieldIF[] fields = null;
   private int id = -1;
   private int changeType = NOT_CHANGED;

//  public DefaultStoreRecord(Vector fields, Vector objects) {
//    this.objects = objects;
//    this.fields = fields;
//  }

   public DefaultStoreRecord(StoreFieldIF[] fields, Object[] objs) {
      if (fields.length != objs.length)
         throw new IllegalArgumentException(
                 "The number of fields must equals to the number of objects");
      this.fields = fields;
      this.objs = objs;
   }

   public int getSize() {
      return this.objs.length;
   }

   public int getID() {
      return this.id;
   }

   public void setID(int id) {
      this.id = id;
   }

   public Object getObject(int i) {
      if (i < 0 || i >= getSize())
         throw new ArrayIndexOutOfBoundsException("Index out of range " + i);
      return objs[i];
   }

   public StoreFieldIF getField(int i) {
      return fields[i];
   }

   public Object getObject(String fieldName) {
      for (int i = 0; i < getSize(); i++) {
         if (fields[i].getName().equalsIgnoreCase(fieldName))
            return getObject(i);
      }
      throw new IllegalArgumentException("Field '" + fieldName + "' not found");
   }

   public Object[] getObjects() {
      return objs;
   }

   public StoreFieldIF[] getFields() {
      return fields;
   }

   public int getChangeType() {
      return changeType;
   }

   // ####### record flags ######
   public boolean isDeleted() {
      return changeType == DELETED;
   }

   public boolean isInserted() {
      return changeType == INSERTED;
   }

   public boolean isUpdated() {
      return changeType == UPDATED;
   }

   public boolean isNotChanged() {
      return changeType == NOT_CHANGED;
   }

   public void setDeleted() {
      this.changeType = DELETED;
   }

   public void setInserted() {
      this.changeType = INSERTED;
   }

   public void setUpdated() {
      this.changeType = UPDATED;
   }

   public void setNotChanged() {
      changeType = NOT_CHANGED;
   }

   public void setChangeType(int changeType) {
      this.changeType = changeType;
   }
}
