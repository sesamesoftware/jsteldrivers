package com.relationaljunction.jdbc.common.h2.sql;

public interface H2PreparedStatementListener {

  void beginOperation();

  // modified by J-Stels Software
  void endOperation(long updateCount);

}
