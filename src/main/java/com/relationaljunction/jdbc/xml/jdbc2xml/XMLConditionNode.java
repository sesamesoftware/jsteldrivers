package com.relationaljunction.jdbc.xml.jdbc2xml;

import org.w3c.dom.Node;

import com.relationaljunction.utils.XMLUtils;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class XMLConditionNode implements XMLOutputNodeIF {
   private enum IterationEnum {
      FIRST(-10),
      LAST(-9),
      NOT_FIRST(-8),
      NOT_LAST(-7);

      private final int code;

      IterationEnum(int code) {
         this.code = code;
      }

      private static IterationEnum parseIteration(String iteration) {
         if (iteration.equalsIgnoreCase("first")) return FIRST;
         else if (iteration.equalsIgnoreCase("last")) return LAST;
         else if (iteration.equalsIgnoreCase("!first")) return NOT_FIRST;
         else if (iteration.equalsIgnoreCase("!last")) return NOT_LAST;
         else throw new RuntimeException("Invalid iteration: " + iteration);
      }
   }

   private interface Condition {
      boolean evaluate(XMLOutputContext context) throws SQLException;
   }

   private class IterationCondition implements Condition {

      private final IterationEnum iterationEnum;

      private IterationCondition(String iteration) {
         this.iterationEnum = IterationEnum.parseIteration(iteration);
      }

      public boolean evaluate(XMLOutputContext context) throws SQLException {
         ResultSet rs = context.getResultSet(queryAlias);

         if (iterationEnum == IterationEnum.FIRST) return rs.isFirst();
         else if (iterationEnum == IterationEnum.NOT_FIRST) return !rs.isFirst();
         else if (iterationEnum == IterationEnum.LAST) return rs.isLast();
         else if (iterationEnum == IterationEnum.NOT_LAST) return !rs.isLast();

         return false;
      }
   }

   //   private Set<Integer> ignoreIterationSet = new HashSet<Integer>();
//   private int iteration = 0;
   private final List<XMLOutputNodeIF> childNodes;
   private final String stringRepresentation;
   private IterationCondition iterationCondition;
   private final String queryAlias;

   public XMLConditionNode(Node node, List<XMLOutputNodeIF> childNodes) {
      this.childNodes = childNodes;
      this.stringRepresentation = XMLUtils.nodeToString(node);
      this.queryAlias = XMLUtils.getRequiredAttributeValue(node, "queryAlias");

      String ignoreIterationAttr = XMLUtils.getOptionalAttributeValue(node, "iteration");
      if (ignoreIterationAttr != null)
         iterationCondition = new IterationCondition(ignoreIterationAttr);
   }

//   public void setIgnoreIterations(String ignoreIterations) {
//      StringTokenizer st = new StringTokenizer(ignoreIterations, ",");
//      while (st.hasMoreElements()) {
//         ignoreIterationSet.add(Integer.parseInt(st.nextToken()));
//      }
//   }

   public void execute(XMLOutputContext context, Writer writer) throws IOException {
      // ignore iterations if the corresponding property is specified
//      if (ignoreIterationSet.size() > 0 && ignoreIterationSet.contains(iteration)) {
//         iteration++;
//         return;
//      }

      try {
         if (iterationCondition != null && !iterationCondition.evaluate(context)) {
            return;
         }

         // get child nodes executed
         for (XMLOutputNodeIF node : childNodes) {
            node.execute(context, writer);
         }
      } catch (Exception e) {
         throw new RuntimeException("Error while processing <condition> element: '" +
                 stringRepresentation + "'", e);
      }

//      iteration++;
   }

   public void reset() {
//      iteration = 0;
   }

   @Override
   public String toString() {
      return stringRepresentation;
   }
}
