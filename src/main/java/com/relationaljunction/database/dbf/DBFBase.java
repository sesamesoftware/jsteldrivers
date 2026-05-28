package com.relationaljunction.database.dbf;

public abstract class DBFBase {

   public final static int IGNORE_MEMO_FILE_NEVER = 0;
   public final static int IGNORE_MEMO_FILE_ALWAYS = 1;
   public final static int IGNORE_MEMO_FILE_IF_NOT_EXIST = 2;
   public final static byte DELETED_BYTE = '*';
   public final static byte NON_DELETED_BYTE = ' ';

   public static int ignoreMemoFile = IGNORE_MEMO_FILE_NEVER;
   public static String DEFAULT_CHARSET = "8859_1";
   protected String charset = DEFAULT_CHARSET;
   protected final int END_OF_DATA = 0x1A;
   final static int LOCK_CHECK_PERIOD = 500;
   final static int LOCK_TIME_OUT = 100000;
   public static final String WRITE_EOF_NO = "no";
   public static final String WRITE_EOF_ON_RECORDS_NUMBER = "onRecordsNumber";
   public static final String WRITE_EOF_AT_END = "atEnd";


   public String getCharset() {
      return this.charset;
   }

   public void setCharset(String charset) {
      this.charset = charset;
   }

}
