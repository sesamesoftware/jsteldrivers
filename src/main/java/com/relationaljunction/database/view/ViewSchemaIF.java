package com.relationaljunction.database.view;

public interface ViewSchemaIF {

  ViewTableIF[] getStoreViews();

  ViewTableIF getViewByName(String viewName);

}
