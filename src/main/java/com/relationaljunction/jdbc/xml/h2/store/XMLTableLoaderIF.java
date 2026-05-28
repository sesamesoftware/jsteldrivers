package com.relationaljunction.jdbc.xml.h2.store;

import java.sql.*;

public interface XMLTableLoaderIF {

  void setInsertPreparedStatement(PreparedStatement pstInsert);

  void loadTable() throws Exception;

  void clear();
}
