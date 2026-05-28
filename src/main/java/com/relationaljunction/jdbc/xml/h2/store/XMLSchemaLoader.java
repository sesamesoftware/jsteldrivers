package com.relationaljunction.jdbc.xml.h2.store;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;

import org.w3c.dom.*;

import com.relationaljunction.database.StoreDataType;
import com.relationaljunction.database.io.*;
import com.relationaljunction.jdbc.xml.jdbc2xml.XMLOutputFactory;
import com.relationaljunction.jdbc.xml.jdbc2xml.XMLOutputNode;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2006</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 2.5
 */

public class XMLSchemaLoader {
   private Document schemaDocument = null;
   private final XMLSchemaData xmlData = new XMLSchemaData();
   private Properties parameters = new Properties();

   public XMLSchemaLoader(FileManager fmSchema, Properties parameters) throws Exception {
      this.parameters = parameters;

      // load schema from the url
      loadSchema(fmSchema);
   }

   public XMLTableDescr getTableDescription(String tableName) throws Exception {
      return xmlData.getTableDescription(tableName);
   }

   public Set<XMLTableDescr> getTables() {
      return xmlData.getTables();
   }

   public void loadSchema(FileManager fmSchema) throws Exception {
      InputStream is = fmSchema.getInputStream();
      try {
         loadSchema(is);
      } catch (Exception ex) {
//      ex.printStackTrace();
         throw new Exception("Can't parse the schema file '" + fmSchema.getPath() +
                 "': " + ex.getMessage(), ex);
      }
      is.close();
   }

   private void loadSchema(InputStream is) throws Exception {
      // create XML document from input stream
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setIgnoringElementContentWhitespace(true);
      DocumentBuilder builder = dbf.newDocumentBuilder();
      // create schema structure from XML document
      schemaDocument = builder.parse(is);
      loadSchema(schemaDocument);
   }

   private void loadSchema(Document schemaDoc) throws Exception {
      Element mainNode = schemaDoc.getDocumentElement();
      if (!mainNode.getNodeName().equals("schema")) {
         throw new Exception(
                 "The schema file must have the root tag 'schema'");
      }
      NodeList nodeList = mainNode.getChildNodes();

      // table description loop
      for (int i = 0; i < nodeList.getLength(); i++) {
         Node node = nodeList.item(i);
         if (node.getNodeType() != Node.ELEMENT_NODE)
            continue;


         if (node.getNodeName().equals("table")) {
            xmlData.addTable(processTableElement(node, false));
         } else if (node.getNodeName().equals("xmlfile")) {
            processXmlfileElement(node);
         }
      }
   }


   private void processXmlfileElement(Node node) throws Exception {
      LinkedList<XMLTableDescr> descrList = new LinkedList<XMLTableDescr>();
      XMLOutputNode xmlOutputNode = null;

      String filePath = this.getRequiredAttributeValue(node, "file");

      NodeList nodeList = node.getChildNodes();

      for (int i = 0; i < nodeList.getLength(); i++) {
         Node childNode = nodeList.item(i);
         if (childNode.getNodeType() != Node.ELEMENT_NODE)
            continue;


         if (childNode.getNodeName().equals("table")) {
            XMLTableDescr descr = processTableElement(childNode, true);
            xmlData.addTable(descr);
            descrList.add(descr);
         } else if (childNode.getNodeName().equals("output")) {
            xmlOutputNode = XMLOutputFactory.createXMLOutput(childNode);
         }
      }

      // set common file path and output if it exists
      for (XMLTableDescr descr : descrList) {
         descr.setXMLOutput(xmlOutputNode);
         descr.setFilePath(filePath);
      }

   }

   private XMLTableDescr processTableElement(Node tblNode,
                                             boolean insideXmlfileElement) throws Exception {
      // a table name is an optional attribute
      String tableName = this.getOptionalAttributeValue(tblNode, "name");

      String filePath = null;
      if (!insideXmlfileElement) {
         filePath = this.getRequiredAttributeValue(tblNode, "file");
      }

      String xPath = this.getRequiredAttributeValue(tblNode, "path");

      XMLTableDescr tblDescr;

      if (tableName == null)
         tblDescr = new XMLTableDescr(filePath, xPath);
      else
         tblDescr = new XMLTableDescr(tableName, filePath, xPath);

      // load custom properties, except reserved "name", "file" and "path" properties
      Properties tableProps = new Properties();
      NamedNodeMap attrMap = tblNode.getAttributes();
      for (int j = 0; j < attrMap.getLength(); j++) {
         Attr attr = (Attr) attrMap.item(j);
         if (!attr.getName().equalsIgnoreCase("name") &&
                 !attr.getName().equalsIgnoreCase("file") &&
                 !attr.getName().equalsIgnoreCase("path"))
            tableProps.setProperty(attr.getName(), attr.getValue());
      }

      tblDescr.setLocalProps(tableProps);

      // process table columns and other info
      NodeList colList = tblNode.getChildNodes();
      for (int j = 0; j < colList.getLength(); j++) {
         Node node = colList.item(j);
         if (node.getNodeType() != Node.ELEMENT_NODE)
            continue;

         if (node.getNodeName().equals("column")) {
            processColumnElement(tblDescr, node);
         } else if (node.getNodeName().equals("templateRow")) {
            tblDescr.setTemplateRow(node.getTextContent());
         } else if (node.getNodeName().equals("output")) {
            tblDescr.setXMLOutput(XMLOutputFactory.createXMLOutput(node));
         }
      }

      return tblDescr;
   }


   private void processColumnElement(XMLTableDescr tblDescr, Node colNode) throws Exception {
      String colName = this.getRequiredAttributeValue(colNode, "name");
      String colType = this.getRequiredAttributeValue(colNode, "type");
      String colPath = this.getRequiredAttributeValue(colNode, "path");
      String colSize = this.getOptionalAttributeValue(colNode, "size");
      String decimalCount = this.getOptionalAttributeValue(colNode, "decimalCount");
      String relation = this.getOptionalAttributeValue(colNode, "relation");
      String context = this.getOptionalAttributeValue(colNode, "context");
      XMLColumnDescr colDescr = new XMLColumnDescr(colName, colPath,
              StoreDataType.getDataTypeByName(colType));

      if (relation != null)
         colDescr.setRelation(relation);
      if (colSize != null)
         colDescr.setSize(Integer.parseInt(colSize));
      if (decimalCount != null)
         colDescr.setDecimalCount(Integer.parseInt(decimalCount));
      if (context != null)
         colDescr.setContext(context);

      // since version 3.2
      colDescr.setDateFormatString(getOptionalAttributeValue(colNode, "dateFormat"));
      colDescr.setDecimalFormatInput(getOptionalAttributeValue(colNode, "decimalFormatInput"));
      colDescr.setDecimalFormatOutput(getOptionalAttributeValue(colNode, "decimalFormatOutput"));
      colDescr.setLocale(getOptionalAttributeValue(colNode, "locale"));

      tblDescr.addColumn(colDescr);
   }

   private static void checkTagName(Node nd, String name) throws Exception {
      if (nd == null)
         throw new Exception("No tag named '" + name +
                 "' in the schema file");
      String realName = nd.getNodeName();
      if (!realName.equals(name))
         throw new Exception("Wrong tag name '" + realName +
                 "' (must be '" + name + "') in the schema file");
   }

   private String getOptionalAttributeValue(Node par, String attrName) throws
           Exception {
      String parName = par.getNodeName();

      NamedNodeMap attrMap = par.getAttributes();
      Node node = attrMap.getNamedItem(attrName);
      if (node == null)
         return null;

      if (node.getNodeType() != Node.ATTRIBUTE_NODE) {
         throw new Exception("Object named '" + attrName +
                 "' is not attribute for object '" + parName +
                 "' in the schema file");
      }

      return com.relationaljunction.utils.StringUtils.replaceParameters(node.getNodeValue(), parameters);
//    return checkForParameter(node.getNodeValue());
   }


   private String getRequiredAttributeValue(Node par, String attrName) throws
           Exception {
      String attrValue = getOptionalAttributeValue(par, attrName);
      String parName = par.getNodeName();
      if (attrValue == null)
         throw new Exception("Attribute '" + attrName +
                 "' in the schema file must be defined for object '" +
                 parName + "'");
      return attrValue;
   }

//  private String checkForParameter(String valueName){
//    if (valueName.startsWith("{@") && valueName.endsWith("}")) {
//      String paramName = valueName.substring(2, valueName.length() - 1);
//
//      if (paramName == null)
//        throw new NullPointerException("NULL name for a parameter variable");
//
//      String paramValue = parameters.getProperty(paramName);
//      if (paramValue == null)
//        throw new IllegalArgumentException("Parameter variable " + paramName +
//                                           " is not specified");
//      return paramValue;
//    }
//
//    return valueName;
//  }

}
