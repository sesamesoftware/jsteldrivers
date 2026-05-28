package com.relationaljunction.database.dbf;

import java.io.IOException;

public class DBFException
        extends IOException {

   public DBFException() {
      super();
   }

   public DBFException(String msg) {
      super(msg);
   }

   public DBFException(Exception ex) {
      this(ex.getMessage());
      initCause(ex);
   }

   public DBFException(String message, Exception ex) {
      this(message);
      initCause(ex);
   }

}
