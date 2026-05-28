package com.relationaljunction.database.index;

import com.relationaljunction.database.StoreFieldIF;

public interface IndexFieldIF {

  StoreFieldIF getStoreField();

  StoreFieldIF getReferencedStoreField();

  boolean isAscending();
}
