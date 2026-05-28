package com.relationaljunction.jdbc.csv.h2.store;

import java.util.*;
import java.sql.*;

import com.relationaljunction.database.h2.*;
import com.relationaljunction.jdbc.csv.store.CSVStoreSchema;

public class CsvStoreSchema2 extends CSVStoreSchema implements StoreSchemaIF2 {
  public CsvStoreSchema2(String urlPath, Properties globalProps) throws
      SQLException {
    super(urlPath, globalProps);
  }
}
