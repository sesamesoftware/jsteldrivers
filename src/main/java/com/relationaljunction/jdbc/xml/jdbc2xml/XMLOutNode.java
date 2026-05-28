package com.relationaljunction.jdbc.xml.jdbc2xml;

import java.io.IOException;
import java.io.Writer;

import com.relationaljunction.utils.StringUtils;

public class XMLOutNode implements XMLOutputNodeIF {
   private boolean containsPattern = false;
   private final String nodeText;

   public XMLOutNode(String nodeText) {
      this.nodeText = nodeText;
      this.containsPattern = StringUtils.containsPattern(nodeText);
   }

   public void execute(XMLOutputContext context, Writer writer) throws IOException {
      String text;

      try {
         if (containsPattern) {
            // resolve parameters
            text = StringUtils.replaceParameters(nodeText, context.getParameterHandler());
         } else {
            text = nodeText;
         }

         writer.write(text);
      } catch (Exception e) {
         throw new RuntimeException("Error while processing <out> element: '" + nodeText + "'", e);
      }
   }

   public void reset() {
   }
}
