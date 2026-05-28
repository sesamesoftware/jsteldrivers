package com.relationaljunction.jdbc.mdb.h2;

import java.util.Properties;
import java.util.concurrent.Executor;
import java.sql.SQLException;

import com.relationaljunction.database.h2.StoreSchemaIF2;
import com.relationaljunction.jdbc.common.h2.*;

public class MDBConnection2 extends CommonConnection2 {
  public MDBConnection2(CommonDriver2 driver, Properties props) throws
      SQLException {
    super(driver, props);
  }

   public WatchModificationsThread createWatchingModificationsThread(int checkPeriod) {
      return new WatchSchemaModificationsThread(this, checkPeriod);
   }

   protected StoreSchemaIF2 createSchema(Properties props) throws
      SQLException {
    return new MDBSchema2(props);
  }

  protected void addSpecificH2Properties(Properties h2Properties){
    h2Properties.setProperty("MODE", "RJ");
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
