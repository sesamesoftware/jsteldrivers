package com.relationaljunction.jdbc.xml.jdbc2xml;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.relationaljunction.utils.XMLUtils;

import java.util.LinkedList;
import java.util.List;

public class XMLOutputFactory {
   public static XMLOutputNode createXMLOutput(Node xmlOutputNode) {
      List<XMLOutputNodeIF> childNodes = new LinkedList<XMLOutputNodeIF>();

      processChildElements(xmlOutputNode, childNodes);

      return new XMLOutputNode(childNodes);
   }

   private static void processChildElements(Node parentNode, List<XMLOutputNodeIF> childNodes) {
      NodeList nodeList = parentNode.getChildNodes();

      for (int i = 0; i < nodeList.getLength(); i++) {
         Node node = nodeList.item(i);

         if (node.getNodeType() != Node.ELEMENT_NODE)
            continue;

         if (node.getNodeName().equals("out")) {
            childNodes.add(processOutElement(node));
         } else if (node.getNodeName().equals("query")) {
            childNodes.add(processQueryElement(node));
         } else if (node.getNodeName().equals("group")) {
            childNodes.add(processGroupElement(node));
         } else if (node.getNodeName().equals("condition")) {
            childNodes.add(processConditionElement(node));
         } else if (node.getNodeName().equals("loop")) {
            childNodes.add(processLoopElement(node));
         }

//         System.out.println(node.getNodeName());
      }

   }

   private static XMLOutputNodeIF processGroupElement(Node node) {
      String groupColumns = XMLUtils.getRequiredAttributeValue(node, "columns");
      List<XMLOutputNodeIF> childNodes = new LinkedList<XMLOutputNodeIF>();
      processChildElements(node, childNodes);

      return new XMLGroupNode(childNodes, groupColumns);
   }

   private static XMLOutputNodeIF processConditionElement(Node node) {
      List<XMLOutputNodeIF> childNodes = new LinkedList<XMLOutputNodeIF>();
      processChildElements(node, childNodes);

      return new XMLConditionNode(node, childNodes);
   }

   private static XMLOutputNodeIF processLoopElement(Node node) {
      String queryAlias = XMLUtils.getRequiredAttributeValue(node, "queryAlias");
      List<XMLOutputNodeIF> childNodes = new LinkedList<XMLOutputNodeIF>();

      processChildElements(node, childNodes);

      if (childNodes.isEmpty())
         throw new IllegalArgumentException("<loop> element '" + XMLUtils.nodeToString(node) +
                 "' has no internal XML elements like <out>, <query>, etc");

      return new XMLLoopNode(childNodes, queryAlias);
   }

   private static XMLOutputNodeIF processQueryElement(Node node) {
      String sql = XMLUtils.getRequiredAttributeValue(node, "sql");
      String alias = XMLUtils.getRequiredAttributeValue(node, "alias");

      return new XMLQueryNode(alias, sql);
   }

   private static XMLOutputNodeIF processOutElement(Node node) {
      return new XMLOutNode(node.getTextContent());
   }

}
