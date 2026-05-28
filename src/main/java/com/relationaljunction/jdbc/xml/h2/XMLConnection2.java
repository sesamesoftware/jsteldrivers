package com.relationaljunction.jdbc.xml.h2;

import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.sql.Statement;

import com.relationaljunction.database.h2.StoreSchemaIF2;
import com.relationaljunction.jdbc.common.h2.*;
import com.relationaljunction.jdbc.xml.h2.store.XMLStoreSchema;

public class XMLConnection2 extends CommonConnection2 {

   public XMLConnection2(CommonDriver2 driver, Properties props) throws
           SQLException {
      super(driver, props);
   }

   protected StoreSchemaIF2 createSchema(Properties props) throws
           SQLException {
      StoreSchemaIF2 schema;

      try {
         schema = new XMLStoreSchema(props);
      } catch (Exception ex) {
         ex.printStackTrace();
         throw driver.createException("Can't load the schema. Error was: " +
                 ex.getMessage());
      }

      return schema;
   }

   protected void initCacheTableManager() {
      this.cacheTableManager = new XMLCacheTableManager(this);
      if (isPersistentMode())
         this.cacheTableManager.initTablesInPersistentMode();
   }

   public Statement createStatement() throws SQLException {
      checkClosed();
      Statement st = new XMLStatement2(this, h2Conn.createStatement());
      driver.writeLog("Connection(" + this +
              ") -> createStatement() -> Statement(" + st + ")");
      return st;
   }

   public Statement createStatement(int resultSetType, int resultSetConcurrency) throws
           SQLException {
      checkClosed();
      Statement st = new XMLStatement2(this,
              h2Conn.createStatement(resultSetType,
                      resultSetConcurrency));
      driver.writeLog("Connection(" + this +
              ") -> createStatement() -> Statement(" + st + ")");
      return st;
   }

   public Statement createStatement(int resultSetType, int resultSetConcurrency,
                                    int resultSetHoldability) throws
           SQLException {
      checkClosed();
      Statement st = new XMLStatement2(this,
              h2Conn.createStatement(resultSetType,
                      resultSetConcurrency, resultSetHoldability));
      driver.writeLog("Connection(" + this +
              ") -> createStatement() -> Statement(" + st + ")");
      return st;
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
