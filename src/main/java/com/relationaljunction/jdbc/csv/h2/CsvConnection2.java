package com.relationaljunction.jdbc.csv.h2;

import java.util.Properties;
import java.util.concurrent.Executor;
import java.sql.SQLException;

import com.relationaljunction.database.h2.StoreSchemaIF2;
import com.relationaljunction.jdbc.common.h2.*;
import com.relationaljunction.jdbc.csv.h2.store.CsvStoreSchema2;

public class CsvConnection2 extends CommonConnection2{
  public CsvConnection2(CommonDriver2 driver, Properties props) throws
      SQLException {
    super(driver, props);
  }

  protected StoreSchemaIF2 createSchema(Properties props) throws
      SQLException {
    String filePath = props.getProperty(CommonDriver2.PATH);
    return new CsvStoreSchema2(filePath, props);
  }

  protected void loadSpecificPropertiesInVariables(Properties props) {
  }

@Override
public void setSchema(String schema) throws SQLException {
	// TODO Auto-generated method stub
	
}

@Override
public String getSchema() throws SQLException {
	// TODO Auto-generated method stub
	return null;
}

@Override
public void abort(Executor executor) throws SQLException {
	// TODO Auto-generated method stub
	
}

@Override
public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
	// TODO Auto-generated method stub
	
}

@Override
public int getNetworkTimeout() throws SQLException {
	// TODO Auto-generated method stub
	return 0;
}

}
