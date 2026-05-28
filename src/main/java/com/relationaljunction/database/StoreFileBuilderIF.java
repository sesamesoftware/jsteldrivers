package com.relationaljunction.database;

public interface StoreFileBuilderIF
    extends StoreFilePropertiesIF {

  StoreFileReaderIF getReader();

  StoreFileWriterIF getWriter();
}
