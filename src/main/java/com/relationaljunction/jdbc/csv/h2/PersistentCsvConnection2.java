package com.relationaljunction.jdbc.csv.h2;

import java.sql.SQLException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.jdbc.common.h2.CommonDriver2;

/* the class is intended for Java servers like Tomcat, Glassfish, etc*/
public class PersistentCsvConnection2 extends CsvConnection2 {
   private final Logger log = LoggerFactory.getLogger("PersistentCsvConnection2");

   public PersistentCsvConnection2(CommonDriver2 driver, Properties props) throws
           SQLException {
      super(driver, props);
   }

   public void close() throws SQLException {
      // connection will not be closed until it has been collected by GC
      com.relationaljunction.utils.OtherUtils.writeLogInfo(log, this +
              " -> singleton connection will not be closed");
   }

//   @Override
//   protected void finalize() throws Throwable {
//      super.finalize();
//
//      try {
//         if (!physicallyClosed) {
//            com.relationaljunction.utils.OtherUtils.writeLogInfo(log, this.toString() +
//                    " -> finalize()", true);
//
//            closePhysically();
//         }
//      } catch (Exception e) {
//         log.warn("Error in PersistentCsvConnection2.finalize()", e);
//      }
//   }
}
