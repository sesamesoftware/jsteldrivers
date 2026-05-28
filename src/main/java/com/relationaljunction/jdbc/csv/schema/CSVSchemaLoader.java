package com.relationaljunction.jdbc.csv.schema;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.relationaljunction.database.StoreDataType;
import com.relationaljunction.database.StoreFieldIF;
import com.relationaljunction.database.io.FileManager;
import com.relationaljunction.utils.StringUtils;

/**
 * <p>Title: StelsCSV JDBC driver</p>
 * <p>Copyright: Copyright (c) J-Stels Software 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author J-Stels Software
 * @version 2.0
 */

/*
 This class loads the schema file in CSVSchema structure
*/

public class CSVSchemaLoader {
   private final Logger log = LoggerFactory.getLogger("CSVSchemaLoader");

   private final CSVSchemaData schema = new CSVSchemaData();
   private FileManager cfSchema = null;
   private Document document = null;
   private final Properties parameterProps;

   public CSVSchemaLoader(FileManager cfSchema, Properties parameterProps) throws Exception {
      this.cfSchema = cfSchema;
      this.parameterProps = parameterProps;
      // load schema from the file
      loadSchemaFromCommonFile(cfSchema);
   }

   public CSVSchemaData getSchemaData() {
      return this.schema;
   }

   public void loadSchemaFromCommonFile(FileManager cf) throws Exception {
//    if (!cf.exists())
//      return;
      com.relationaljunction.utils.OtherUtils.writeLogInfo(log, "schema file = '", cf.getPath(), "'");

      InputStream is;
      try {
         is = cf.getInputStream();
      } catch (Exception ex) {
         com.relationaljunction.utils.OtherUtils.writeWarnInfo(log,
                 "schema file '" + cf.getPath() + "' is not found. " +
                         "The driver will use default settings for columns in CSV/Text files", true);
         return;
      }

      if (is == null)
//      throw new Exception("Can't find the schema file '" +
//                                       cf.getPath() + "'");
         return;

      try {
         loadSchemaFromStream(is);
      } catch (Exception ex) {
         throw new Exception("Can't parse the schema file '" + cf.getPath() + "': " +
                 ex.getMessage());
      }
      is.close();
   }

   private void loadSchemaFromStream(InputStream is) throws Exception,
           FactoryConfigurationError {
      // create XML document from input stream
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setIgnoringElementContentWhitespace(true);
      DocumentBuilder builder = dbf.newDocumentBuilder();
      // create schema structure from XML document
      loadSchemaFromDocument(builder.parse(is));
   }

   private void loadSchemaFromDocument(Document schemaDoc) throws Exception {
      document = schemaDoc;
      Element mainNode = document.getDocumentElement();
      if (!mainNode.getNodeName().equals("schema")) {
         throw new Exception(
                 "[Relational Junction CSV Driver] The schema file must have the root tag 'schema'");
      }
      NodeList tblList = mainNode.getChildNodes();

      for (int i = 0; i < tblList.getLength(); i++) {
         Node tblNode = tblList.item(i);

         if (tblNode.getNodeType() != Node.ELEMENT_NODE)
            continue;

         checkTagName(tblNode, "table");

         // create TableDescr instance
         String tblName = getRequiredAttributeValue(tblNode, "name");
         CSVTableDescr tblDescr = new CSVTableDescr(tblName);

         // load local properties
         Properties tableProps = new Properties();
         NamedNodeMap attrMap = tblNode.getAttributes();
         for (int j = 0; j < attrMap.getLength(); j++) {
            Attr attr = (Attr) attrMap.item(j);
            if (!attr.getName().equalsIgnoreCase("name"))
               tableProps.setProperty(attr.getName(), attr.getValue());
         }
         tblDescr.setLocalProps(tableProps);

         // process table columns
         NodeList colList = tblNode.getChildNodes();
         for (int j = 0; j < colList.getLength(); j++) {
            Node colNode = colList.item(j);
            if (colNode.getNodeType() != Node.ELEMENT_NODE)
               continue;
            checkTagName(colNode, "column");
            String colNameAttr = getOptionalAttributeValue(colNode, "name");
            String colPosAttr = getOptionalAttributeValue(colNode, "pos");
            String colTypeAttr = getRequiredAttributeValue(colNode, "type");
            String beginPosAttr = getOptionalAttributeValue(colNode, "begin");
            String endPosAttr = getOptionalAttributeValue(colNode, "end");
            String aliasAttr = getOptionalAttributeValue(colNode, "alias");
            String sizeAttr = getOptionalAttributeValue(colNode, "size");
            String decimalCountAttr = getOptionalAttributeValue(colNode, "decimalCount");
//        int colType = this.getJDBCType(colTypeAttr);

            CSVColumnDescr colDescr;

            if (colNameAttr == null && colPosAttr == null)
               throw new Exception("[Relational Junction CSV Driver] Tag 'column' in the schema file " +
                       "must be defined with either 'name' or 'pos' attribute");
               // columns defined by its position only
            else if (colNameAttr == null) {
               int colPos = Integer.parseInt(colPosAttr);
               colDescr = new CSVColumnDescr(colPos, StoreDataType.getDataTypeByName(colTypeAttr));
            }
            // columns defined by its name only
            else if (colPosAttr == null)
               colDescr = new CSVColumnDescr(colNameAttr, StoreDataType.getDataTypeByName(colTypeAttr));
               // columns defined by its name and position
            else {
               int colPos = Integer.parseInt(colPosAttr);
               colDescr = new CSVColumnDescr(colNameAttr, colPos, StoreDataType.getDataTypeByName(colTypeAttr));
            }
            if (beginPosAttr != null)
               colDescr.setBeginPos(Integer.parseInt(beginPosAttr));
            if (endPosAttr != null)
               colDescr.setEndPos(Integer.parseInt(endPosAttr));

            // ####added 6-Nov-2006
            // set size for the column
            if (sizeAttr == null && endPosAttr != null && beginPosAttr != null) {
               colDescr.setSize(colDescr.getEndPos() - colDescr.getBeginPos() + 1);
            } else if (sizeAttr != null) {
               colDescr.setSize(Integer.parseInt(sizeAttr));
            }

            // ####added 3-Sep-2007
            // set actual type name
            colDescr.setSourceTypeName(colTypeAttr.toUpperCase());

            // ####added 10-Dec-2009
            // set decimal count
            if (decimalCountAttr != null)
               colDescr.setDecimalCount(Integer.parseInt(decimalCountAttr));

            // ####added 28-Sep-2013
            colDescr.setDateFormatString(getOptionalAttributeValue(colNode, "dateFormat"));
            colDescr.setDecimalFormatInput(getOptionalAttributeValue(colNode, "decimalFormatInput"));
            colDescr.setDecimalFormatOutput(getOptionalAttributeValue(colNode, "decimalFormatOutput"));

            // ####added 18-02-2015
            colDescr.setAlias(aliasAttr);

            tblDescr.addColumn(colDescr);
         }
         schema.addTableDescription(tblDescr);
      }
   }

   private void createEmptySchemaDocument() throws Exception {
      try {
         DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
         dbf.setIgnoringElementContentWhitespace(true);
         DocumentBuilder builder = dbf.newDocumentBuilder();
         document = builder.newDocument();
         Element elemRoot = document.createElement("schema");
         document.appendChild(elemRoot);
      } catch (Exception e) {
         throw new Exception("Can't create empty schema file");
      }
   }

   public void createTable(String fullTableName, StoreFieldIF[] fields,
                           boolean suppressHeaders) throws Exception {
      if (document == null)
         createEmptySchemaDocument();

      //seek the table in the CSVSchema
      if (getSchemaData().getTableDescription(fullTableName) != null)
         throw new Exception("Table '" + fullTableName +
                 "' is already described in the schema file.");

      Element mainNode = document.getDocumentElement();
      Element elemTable = document.createElement("table");
      elemTable.setAttribute("name", fullTableName);
      mainNode.appendChild(elemTable);

      CSVTableDescr descr = new CSVTableDescr(fullTableName);

      for (int i = 0; i < fields.length; i++) {
         CSVColumnDescr colDescr = new CSVColumnDescr(fields[i].getName(), fields[i].getType());
         colDescr.setSize(fields[i].getLength());
         colDescr.setDecimalCount(fields[i].getDecimalCount());

         Element elemCol = document.createElement("column");
         elemCol.setAttribute("name", fields[i].getName());
         elemCol.setAttribute("type", fields[i].getType().getName());

         if (suppressHeaders) {
            elemCol.setAttribute("pos", String.valueOf(i + 1));
            colDescr.setPos(i + 1);
         }

         if (colDescr.getSize() > 0)
            elemCol.setAttribute("size", "" + colDescr.getSize());

         if (colDescr.getDecimalCount() > 0)
            elemCol.setAttribute("decimalCount", "" + colDescr.getDecimalCount());

         elemTable.appendChild(elemCol);
         descr.addColumn(colDescr);
      }

      schema.addTableDescription(descr);
      saveSchemaInFile();
   }

   public void dropTable(String fullTableName) throws Exception {
      if (document == null)
         return;

      NodeList nl = document.getElementsByTagName("table");
      for (int i = 0; i < nl.getLength(); i++) {
         Node node = nl.item(i);
         String name = getRequiredAttributeValue(node, "name");
         if (name.trim().equalsIgnoreCase(fullTableName)) {
            schema.removeTableDescription(new CSVTableDescr(fullTableName));
            document.getDocumentElement().removeChild(node);
            saveSchemaInFile();
            return;
         }
      }
   }

   private void saveSchemaInFile() throws Exception {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount",
              "2");
      if (cfSchema.exists() && cfSchema.isReadOnly())
         throw new Exception("Schema file '" + cfSchema.getPath() +
                 "' is read-only!");

      BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(cfSchema.
              getOutputStream(false)));
      StreamResult result = new StreamResult(bw);
      transformer.transform(new DOMSource(document), result);
      bw.close();
   }

//  private int getJDBCType(String sqlType) throws Exception {
//    int jdbcType;
//    try {
//      jdbcType = DefaultStoreField.getSQLTypeCodeByName(sqlType);
//    }
//    catch (Exception ex) {
//      throw new Exception("Unknown SQL type '" + sqlType +
//                          "' in the schema file");
//    }
//    return jdbcType;
//  }

   private static void checkTagName(Node nd, String name) throws Exception {
      if (nd == null)
         throw new Exception("[Relational Junction CSV Driver] No tag named '" + name +
                 "' in the schema file");
      String real_name = nd.getNodeName();
      if (!real_name.equals(name))
         throw new Exception("[Relational Junction CSV Driver] Wrong tag name '" + real_name +
                 "' (must be '" + name + "') in the schema file");
   }

   private String getOptionalAttributeValue(Node par, String attr_name) throws
           Exception {
      String par_name = par.getNodeName();
      NamedNodeMap attr_map = par.getAttributes();
      Node node = attr_map.getNamedItem(attr_name);
      if (node == null)
         return null;
      if (node.getNodeType() != Node.ATTRIBUTE_NODE) {
         throw new Exception("[Relational Junction CSV Driver] Object named '" + attr_name +
                 "' is not attribute for object '" + par_name +
                 "' in the schema file");
      }

      // replace a named parameter if it is used
      return StringUtils.replaceParameters(node.getNodeValue(), parameterProps);
   }

   private String getRequiredAttributeValue(Node par, String attr_name) throws
           Exception {
      String attr_val = getOptionalAttributeValue(par, attr_name);
      String par_name = par.getNodeName();
      if (attr_val == null)
         throw new Exception("[Relational Junction CSV Driver] Attribute '" + attr_name +
                 "' in the schema file must be defined for object '" +
                 par_name + "'");
      return attr_val;
   }

   public static void main(String[] args) throws Exception {
   }
}
