package com.relationaljunction.jdbc.xml.h2.store;

import java.sql.*;

import com.relationaljunction.database.io.FileManager;

public interface XMLTableSaverIF {

   void saveTable(ResultSet rs, FileManager fm) throws Exception;

   void clear();

}
