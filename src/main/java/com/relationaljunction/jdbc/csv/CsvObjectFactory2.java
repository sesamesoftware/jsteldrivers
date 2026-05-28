package com.relationaljunction.jdbc.csv;

import javax.naming.*;

import java.util.Hashtable;

import javax.naming.spi.*;

import com.relationaljunction.jdbc.common.h2.CommonConnection2;
import com.relationaljunction.jdbc.csv.store.CSVStoreSchema;

public class CsvObjectFactory2
    implements ObjectFactory {
  public Object getObjectInstance(Object refObj, Name name, Context nameCtx,
                                  Hashtable<? , ? > env) throws Exception {
    Reference ref = (Reference) refObj;

    CsvDataSource2 csvDs = null;

    if (ref.getClassName().equals("com.relationaljunction.jdbc.csv.CsvDataSource2"))
      csvDs = new CsvDataSource2();
    else if (ref.getClassName().equals("com.relationaljunction.jdbc.csv.CsvConnectionPoolDataSource2"))
      csvDs = new CsvConnectionPoolDataSource2();
    else return null;

    csvDs.setPath( (String) ref.get(CSVStoreSchema.PATH).getContent());
    csvDs.setCharset( (String) ref.get(CSVStoreSchema.CHARSET).getContent());
    csvDs.setCommentLine( (String) ref.get(CSVStoreSchema.COMMENT_LINE).getContent());
    csvDs.setDateFormat( (String) ref.get(CSVStoreSchema.DATE_FORMAT).getContent());
    csvDs.setDecimalFormatInput( (String) ref.get(CSVStoreSchema.DECIMAL_FORMAT_INPUT).
				getContent());
    csvDs.setDecimalFormatOutput( (String) ref.get(CSVStoreSchema.DECIMAL_FORMAT_OUTPUT).
				 getContent());
    csvDs.setSeparator( (String) ref.get(CSVStoreSchema.SEPARATOR).getContent());
    csvDs.setFileExtension( (String) ref.get(CSVStoreSchema.FILE_EXTENSION).getContent());
    csvDs.setRowDelimiter( (String) ref.get(CSVStoreSchema.ROW_DELIMITER).getContent());
    csvDs.setSchema( (String) ref.get(CSVStoreSchema.SCHEMA).getContent());
    csvDs.setEscapeEOLInQuotes(Boolean.valueOf( (String) ref.get(
	CSVStoreSchema.ESCAPE_EOL_IN_QUOTES).getContent()).booleanValue());
    csvDs.setEmptyStringIsNull(Boolean.valueOf( (String) ref.get(
	CSVStoreSchema.EMPTY_STRING_AS_NULL).getContent()).booleanValue());
    csvDs.setLocale( (String) ref.get(CSVStoreSchema.LOCALE).getContent());
    csvDs.setLogPath( (String) ref.get(CSVStoreSchema.LOG_PATH).getContent());
    csvDs.setNullString( (String) ref.get(CSVStoreSchema.NULL_STRING).getContent());
    csvDs.setPaddingChar( (String) ref.get(CSVStoreSchema.PADDING_CHAR).getContent());
    csvDs.setSuppressHeaders(Boolean.valueOf( (String) ref.get(
	CSVStoreSchema.SUPPRESS_HEADERS).getContent()).booleanValue());
    csvDs.setTrimBlanks(Boolean.valueOf( (String) ref.get(CSVStoreSchema.TRIM_BLANKS).
					getContent()).booleanValue());
    csvDs.setWebParameterName( (String) ref.get(CSVStoreSchema.USE_WEB_PARAM).getContent());
    csvDs.setQuoteString(Boolean.valueOf( (String) ref.get(CSVStoreSchema.QUOTE_STRING).
					 getContent()).booleanValue());
    csvDs.setDbInMemory(Boolean.valueOf((String) ref.get(CommonConnection2.
        DB_IN_MEMORY).getContent()).
                        booleanValue());
    csvDs.setTempPath( (String) ref.get(CSVStoreSchema.TEMP_PATH).getContent());

    return csvDs;
  }

}
