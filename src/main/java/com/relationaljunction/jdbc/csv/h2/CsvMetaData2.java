package com.relationaljunction.jdbc.csv.h2;

import com.relationaljunction.jdbc.common.h2.CommonConnection2;
import com.relationaljunction.jdbc.common.h2.CommonMetaData2;

public class CsvMetaData2 extends CommonMetaData2 {

  CsvMetaData2(CommonConnection2 conn) throws java.sql.SQLException {
    super(conn);
  }
}
