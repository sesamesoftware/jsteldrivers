package com.relationaljunction.database.h2;

import java.util.Properties;

import com.relationaljunction.database.StoreException;
import com.relationaljunction.jdbc.common.h2.CommonConnection2;

public interface StoreSchemaFactoryIF {
    StoreSchemaIF2 createSchema(CommonConnection2 conn, Properties props) throws StoreException;
}
