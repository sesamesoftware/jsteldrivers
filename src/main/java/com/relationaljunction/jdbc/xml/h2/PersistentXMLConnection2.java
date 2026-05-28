package com.relationaljunction.jdbc.xml.h2;

import java.sql.SQLException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.jdbc.common.h2.CommonDriver2;

/* the class is intended for Java servers like Tomcat, Glassfish, etc*/
public class PersistentXMLConnection2 extends XMLConnection2 {
	
  private final Logger log = LoggerFactory.getLogger("XMLConnection2");

  public PersistentXMLConnection2(CommonDriver2 driver, Properties props) throws
      SQLException {
    super(driver, props);
  }

  public void close() throws SQLException {
    // connection will not be closed until it has been collected by GC
    com.relationaljunction.utils.OtherUtils.writeLogInfo(log, this +
					 " -> singleton connection will not be closed");
  }
}
