package com.relationaljunction.jdbc.xml.jdbc2xml;

import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class XMLOutputNode implements XMLOutputNodeIF {
   private final List<XMLOutputNodeIF> childNodes;

   public XMLOutputNode(List<XMLOutputNodeIF> childNodes) {
      this.childNodes = childNodes;
   }

   public void execute(Connection conn, Writer writer)
           throws SQLException, IOException {
      execute(new XMLOutputContext(conn), writer);
   }

   public void execute(XMLOutputContext context, Writer writer) throws SQLException, IOException {
      for (XMLOutputNodeIF node : childNodes) {
         node.execute(context, writer);
      }

      for (XMLOutputNodeIF node : childNodes) {
         node.reset();
      }

      context.close();
   }

   public void reset() {
   }

}
