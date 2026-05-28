package com.relationaljunction.jdbc.common.h2.sql;

public interface H2TriggerIF extends org.h2.api.Trigger {

//  public void beginOperation();

//  public void endOperation();

//  public void commit();

//  public void rollback();

  void closeTrigger();
}
