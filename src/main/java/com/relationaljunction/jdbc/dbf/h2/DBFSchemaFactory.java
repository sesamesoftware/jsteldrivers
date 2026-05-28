package com.relationaljunction.jdbc.dbf.h2;

import java.util.Properties;

import com.relationaljunction.database.StoreException;
import com.relationaljunction.database.h2.StoreSchemaFactoryIF;
import com.relationaljunction.database.h2.StoreSchemaIF2;
import com.relationaljunction.jdbc.common.h2.CommonConnection2;
import com.relationaljunction.jdbc.common.h2.CommonDriver2;

public class DBFSchemaFactory implements StoreSchemaFactoryIF {
    public StoreSchemaIF2 createSchema(CommonConnection2 conn, Properties props) throws StoreException {
        String path = props.getProperty(CommonDriver2.PATH);
        if (path.toUpperCase().endsWith(".DBC"))
            return new DBCSchema(props);
        else
            return new DBFSchema2(props);
    }
}
