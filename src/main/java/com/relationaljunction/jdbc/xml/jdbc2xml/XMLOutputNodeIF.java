package com.relationaljunction.jdbc.xml.jdbc2xml;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;

public interface XMLOutputNodeIF {
   void execute(XMLOutputContext context, Writer writer) throws SQLException, IOException;

   void reset();
}
