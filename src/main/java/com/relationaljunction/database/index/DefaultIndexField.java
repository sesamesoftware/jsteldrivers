package com.relationaljunction.database.index;

import com.relationaljunction.database.StoreFieldIF;

public class DefaultIndexField implements IndexFieldIF {
   private StoreFieldIF storeField = null;
   private StoreFieldIF referencedStoreField = null;
   private boolean ascending = true;
   private final String stringRepresentation;

   public DefaultIndexField(StoreFieldIF storeField) {
      this(storeField, true);
   }

   public DefaultIndexField(StoreFieldIF storeField, boolean ascending) {
      this.storeField = storeField;
      this.ascending = ascending;
      this.stringRepresentation = storeField.toString() + (ascending ? " ASC" : " DESC");
   }

   public StoreFieldIF getStoreField() {
      return storeField;
   }

   public boolean isAscending() {
      return ascending;
   }

   public StoreFieldIF getReferencedStoreField() {
      return referencedStoreField;
   }

   public void setReferencedStoreField(StoreFieldIF referencedStoreField) {
      this.referencedStoreField = referencedStoreField;
   }

   @Override
   public String toString() {
      return stringRepresentation;
   }
}
