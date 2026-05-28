package com.relationaljunction.jdbc.mdb;

import java.util.Hashtable;

import javax.naming.*;
import javax.naming.spi.ObjectFactory;

import com.relationaljunction.jdbc.common.h2.CommonConnection2;
import com.relationaljunction.jdbc.common.h2.CommonDriver2;
import com.relationaljunction.jdbc.mdb.h2.MDBSchema2;

public class MDBObjectFactory2 implements ObjectFactory {

   public Object getObjectInstance(Object refObj, Name name, Context nameCtx,
                                   Hashtable<?, ?> env) throws Exception {
      Reference ref = (Reference) refObj;

      MDBDataSource2 mdbDs = null;

      if (ref.getClassName().equals("com.relationaljunction.jdbc.mdb.MDBDataSource2"))
         mdbDs = new MDBDataSource2();
      else if (ref.getClassName().equals(
              "com.relationaljunction.jdbc.mdb.MDBConnectionPoolDataSource2"))
         mdbDs = new MDBConnectionPoolDataSource2();
      else return null;

      mdbDs.setPath((String) ref.get(CommonDriver2.PATH).getContent());
      mdbDs.setCharset((String) ref.get(CommonDriver2.CHARSET).getContent());
      mdbDs.setCreate(Boolean.valueOf((String) ref.get(MDBSchema2.CREATE).
              getContent()));
      mdbDs.setFormat((String) ref.get(MDBSchema2.FORMAT_STRING).getContent());

      mdbDs.setIgnoreCase(Boolean.valueOf((String) ref.get(CommonConnection2.
              IGNORE_CASE).getContent()));
      mdbDs.setLogPath((String) ref.get(CommonDriver2.LOG_PATH).getContent());
      mdbDs.setWebParameterName((String) ref.get(CommonDriver2.USE_WEB_PARAM).getContent());
      mdbDs.setDbInMemory(Boolean.valueOf((String) ref.get(CommonConnection2.
              DB_IN_MEMORY).getContent()));
      mdbDs.setTempPath((String) ref.get(CommonDriver2.TEMP_PATH).getContent());
      return mdbDs;
   }

}
