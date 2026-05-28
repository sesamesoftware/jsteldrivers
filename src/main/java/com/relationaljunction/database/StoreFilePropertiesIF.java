package com.relationaljunction.database;

public interface StoreFilePropertiesIF {
  String getPath();

  String setType(String type);

  String getType();

  void setPath(String path);

  void setCharset(String charset);

  String getCharset();

  void setAppending(boolean appending);

  boolean isAppending();
}
