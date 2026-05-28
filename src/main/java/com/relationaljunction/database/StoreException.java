package com.relationaljunction.database;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 * @author not attributable
 * @version 2.2
 */

public class StoreException extends Exception {
  public StoreException(String message){
    super(message);
  }

  public StoreException(Exception ex){
    super(ex);
  }

  public StoreException(String message, Exception ex) {
    super(message + ": " + ex.getMessage(), ex);
  }
}
