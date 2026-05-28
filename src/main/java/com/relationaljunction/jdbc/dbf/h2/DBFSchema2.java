package com.relationaljunction.jdbc.dbf.h2;

import java.util.*;

import com.relationaljunction.database.StoreException;
import com.relationaljunction.database.h2.StoreSchemaIF2;
import com.relationaljunction.jdbc.dbf.store.DBFSchema;


public class DBFSchema2 extends DBFSchema implements StoreSchemaIF2 {
    public DBFSchema2(Properties globalProps) throws StoreException {
        super(globalProps);

       DBFTableEngine.setSchema(this);
    }

   @Override
   public String getExternalEngineClass() {
      return DBFTableEngine.class.getName();
   }
}
