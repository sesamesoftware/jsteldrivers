package com.relationaljunction.database;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 * @author not attributable
 * @version 2.2
 */

public interface StoreFactoryIF {

  StoreSchemaIF createSchemaIF(java.util.Properties schemaProps) throws
      StoreException;

}

