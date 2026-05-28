package com.relationaljunction.utils;

/**
 * <p>Title: StelsMDB JDBC driver</p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2008</p>
 *
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 1.0
 */
public class UnexpectedException extends RuntimeException {
  public UnexpectedException() {
    super();
  }

  public UnexpectedException(String message) {
    super(message);
  }

  public UnexpectedException(Exception ex) {
    super(ex);
  }

  public UnexpectedException(String message, Exception ex) {
    super(message + ": " + ex.getMessage(), ex);
  }
}
