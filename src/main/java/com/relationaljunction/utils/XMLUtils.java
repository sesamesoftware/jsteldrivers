package com.relationaljunction.utils;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

public class XMLUtils {
   public static String getOptionalAttributeValue(Node node, String attrName) {
      NamedNodeMap attrMap = node.getAttributes();
      Node attrNode = attrMap.getNamedItem(attrName);

      if (attrNode == null)
         return null;

      if (attrNode.getNodeType() != Node.ATTRIBUTE_NODE) {
         throw new IllegalArgumentException("Object named '" + attrName +
                 "' is not attribute for element '" +
                 node.getNodeName() + "'");
      }

      return attrNode.getNodeValue();
   }

   public static String getRequiredAttributeValue(Node node, String attrName) {
      String attrValue = getOptionalAttributeValue(node, attrName);

      if (attrValue == null)
         throw new IllegalArgumentException("Attribute '" + attrName +
                 "' must be defined for element <" + node.getNodeName() + ">");

      return attrValue;
   }

   public static String nodeToString(Node node) {
      DOMImplementationLS lsImpl = (DOMImplementationLS)
              node.getOwnerDocument().getImplementation().
                      getFeature("LS", "3.0");
      LSSerializer serializer = lsImpl.createLSSerializer();
      // by default its true, so set it to false to get String without xml-declaration
      serializer.getDomConfig().setParameter("xml-declaration", false);
      return serializer.writeToString(node);
   }

}
