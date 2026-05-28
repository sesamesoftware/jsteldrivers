package com.relationaljunction.jdbc.dbf.h2;

import java.util.Hashtable;

import javax.naming.*;
import javax.naming.spi.ObjectFactory;

import com.relationaljunction.jdbc.common.h2.CommonConnection2;
import com.relationaljunction.jdbc.common.h2.CommonDriver2;

public class DBFObjectFactory2  implements ObjectFactory{

  public Object getObjectInstance(Object refObj, Name name, Context nameCtx,
				  Hashtable<?,?> env) throws Exception {
    Reference ref = (Reference) refObj;

    DBFDataSource2 dbfDs = null;

    if (ref.getClassName().equals("com.relationaljunction.jdbc.dbf.h2.DBFDataSource2"))
      dbfDs = new DBFDataSource2();
    else if (ref.getClassName().equals(
        "com.relationaljunction.jdbc.dbf.h2.DBFConnectionPoolDataSource2"))
      dbfDs = new DBFConnectionPoolDataSource2();
    else return null;

    dbfDs.setPath( (String) ref.get(CommonDriver2.PATH).getContent());
    dbfDs.setCharset( (String) ref.get(CommonDriver2.CHARSET).getContent());
    dbfDs.setEmptyStringIsNull(Boolean.valueOf( (String) ref.get(
	DBFSchema2.EMPTY_STRING_AS_NULL).getContent()).
			       booleanValue());
    dbfDs.setFormat( (String) ref.get(DBFSchema2.FORMAT).getContent());
    dbfDs.setFileExtension( (String) ref.get(CommonDriver2.FILE_EXTENSION).getContent());

    dbfDs.setIgnoreCase(Boolean.valueOf((String) ref.get(CommonConnection2.
        IGNORE_CASE).getContent()));
    dbfDs.setLogPath( (String) ref.get(CommonDriver2.LOG_PATH).getContent());
    dbfDs.setTrimBlanks(Boolean.valueOf( (String) ref.get(DBFSchema2.TRIM_BLANKS).
					getContent()).
			booleanValue());
    dbfDs.setWebParameterName( (String) ref.get(CommonDriver2.USE_WEB_PARAM).getContent());
    dbfDs.setDbInMemory(Boolean.valueOf((String) ref.get(CommonConnection2.
        DB_IN_MEMORY).getContent()).
                        booleanValue());
    dbfDs.setTempPath( (String) ref.get(CommonDriver2.TEMP_PATH).getContent());
    return dbfDs;
  }


}
