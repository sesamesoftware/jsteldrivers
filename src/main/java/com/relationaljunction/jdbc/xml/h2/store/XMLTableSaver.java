package com.relationaljunction.jdbc.xml.h2.store;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.relationaljunction.database.StoreFieldIF;
import com.relationaljunction.database.io.FileManager;
import com.relationaljunction.utils.OtherUtils;
import com.relationaljunction.utils.StringUtils;
import com.relationaljunction.utils.UnexpectedException;
import com.relationaljunction.utils.XMLUtils;

import jlibs.xml.sax.dog.NodeItem;
import jlibs.xml.sax.dog.expr.Expression;
import jlibs.xml.sax.dog.expr.InstantEvaluationListener;
import jlibs.xml.sax.dog.sniff.DOMBuilder;
import jlibs.xml.sax.dog.sniff.Event;

public class XMLTableSaver extends GenericXMLTableLoader implements XMLTableSaverIF {
   private static class UserNamespaceContext implements NamespaceContext {
      private Map<String, String> hashNamespaces = null;

      UserNamespaceContext(Map<String, String> hashNamespaces) {
         this.hashNamespaces = hashNamespaces;
      }

      public String getNamespaceURI(String prefix) {
         if (prefix == null)
            throw new NullPointerException("Null prefix");
         else if (prefix.equals(javax.xml.XMLConstants.XML_NS_PREFIX))
            return javax.xml.XMLConstants.XML_NS_URI;
         else
            return hashNamespaces.get(prefix);
      }

      public String getPrefix(String uri) {
         throw new UnsupportedOperationException();
      }

      public Iterator getPrefixes(String uri) {
         throw new UnsupportedOperationException();
      }
   }

   private final Logger log = LoggerFactory.getLogger("XMLTableSaver");

   public static int TAB_INDENT = 4;

   //   private int[] colPositions = null;
   private StoreFieldIF[] storeFields = null;
   private XMLDogXPathManager xmlDogXPathManager = null;

   public XMLTableSaver(XMLDogXPathManager xmlDogXPathManager) {
      super(xmlDogXPathManager.xmlTable);

      this.xmlDogXPathManager = xmlDogXPathManager;
      this.storeFields = xmlDogXPathManager.xmlTable.getFields();
//      this.colPositions = new int[storeFields.length];
   }

   /*  full rewrite XML file via "rowTemplateString" element.
      If it is not defined the driver try to get it on its own.
   */
   public void saveTable(ResultSet rsToSave, FileManager fm) throws Exception {
      if (xmlDogXPathManager.xmlTable.rowTemplateString == null) {
         // if a row template is not specified explicitly in schema.xml,
         // try to extract it for the XML file itself
         extractTemplateNode();
         setRowTemplateString();
      }

      writeXML(rsToSave, fm, true);

      fm.upload();
   }

   void createXML() throws Exception {
      writeXML(null, xmlDogXPathManager.xmlTable.getFileManager(), false);
   }

   private void writeXML(ResultSet rsToSave, FileManager fm, boolean hasData) throws Exception {
      String lineSeparator = System.getProperty("line.separator");

      Writer writer = new BufferedWriter(
              new OutputStreamWriter(fm.getOutputStream(false),
                      xmlDogXPathManager.xmlTable.charset));

      writer.write(XMLTable.XML_DECLARATION_PREFIX);
      writer.write(xmlDogXPathManager.xmlTable.charset);
      writer.write(XMLTable.XML_DECLARATION_POSTFIX + lineSeparator);

      if (xmlDogXPathManager.xmlTable.tableDescr.getXPath().contains("//")) {
         throw new Exception("Error while writing an XML: " +
                 "descendant axis should not specified in a base XPath '" +
                 xmlDogXPathManager.xmlTable.tableDescr.getXPath() + "'");
      } else if (!xmlDogXPathManager.xmlTable.tableDescr.getXPath().startsWith("/")) {
         throw new Exception("Error while writing an XML: " +
                 "a base XPath should be start with a root element '/'");
      }

      StringTokenizer st = new StringTokenizer(xmlDogXPathManager.xmlTable.tableDescr.getXPath(), "/");
      LinkedList<String> lifoElements = new LinkedList<String>();

//      StringBuilder formattingIndent = new StringBuilder();
//      for (int i = 0; i < TAB_INDENT; i++) {
//         formattingIndent.append(" ");
//      }

      int baseIndent = 0;

      // create a base path to write records inside there
      while (st.hasMoreElements()) {
         String elem = st.nextToken();

         // the last element should be ignored
         if (st.hasMoreElements()) {
            lifoElements.add(elem);

//         for (int i = 0; i < baseIndent; i++) {
//            bow.write(formattingIndent.toString());
//         }

            // write opening tags
            writer.write("<" + elem);

            // check if a namespace is specified for a element
            if (elem.contains(":")) {
               // element is specified with a namespace
               String prefix = elem.substring(0, elem.indexOf(":")).trim();
               String namespaceURI = xmlDogXPathManager.xmlTable.namespacesMap.get(prefix);
               if (namespaceURI == null)
                  throw new UnexpectedException("Error while writing an XML. " +
                          "Can't find the namespace '" + namespaceURI + "' defined");
               // write "xmlns" attribute for such an element
               writer.write(" xmlns:" + prefix + "=\"" + namespaceURI + "\"");
            }

            writer.write(">" + lineSeparator);

            baseIndent++;
         }
      }

      // write rows
      if (hasData) {
         writeRows(rsToSave, writer, lineSeparator);
      }

      // write closing tags for a base path
      while (!lifoElements.isEmpty()) {
         writer.write("</" + lifoElements.removeLast() + ">" + lineSeparator);
      }

      writer.close();
   }

   private void writeRows(ResultSet rsToSave, Writer writer, String lineSeparator) throws Exception {
      int rsColCount = rsToSave.getMetaData().getColumnCount();
      HashMap<String, String> rsValueMap = new HashMap<String, String>(rsColCount);

      while (rsToSave.next()) {
         // create a map based on row values in a result set
         for (int i = 0; i < storeFields.length; i++) {
            // get a value from ResultSet
            Object obj = rsToSave.getObject(storeFields[i].getName());
            String value = xmlDogXPathManager.xmlTable.getXMLString(storeFields[i], obj,
                    dateFormatters[i], decimalFormatters[i]);
            // place the value at the parameter marker
            rsValueMap.put("{@" + storeFields[i].getName() + "}", value);
         }

         // insert values to a template row
         String rowToSave;
         try {
            rowToSave = StringUtils.replaceParameters(xmlDogXPathManager.xmlTable.rowTemplateString,
                    rsValueMap);
         } catch (Exception e) {
            throw new UnexpectedException("Error while writing an XML. " +
                    "Can't replace a parameter in a template row.", e);
         }

//         System.out.println(rowToSave + lineSeparator);

         // write a row
         writer.write(rowToSave + lineSeparator);
      }
   }

   private void setRowTemplateString() throws Exception {
      XPathFactory xPathFactory = XPathFactory.newInstance(XPathFactory.
              DEFAULT_OBJECT_MODEL_URI);

      XPath xpath = xPathFactory.newXPath();

      // set namespaces if they are specified
      if (!xmlDogXPathManager.xmlTable.namespacesMap.isEmpty()) {
         NamespaceContext namespaceContext = new UserNamespaceContext(
                 xmlDogXPathManager.xmlTable.namespacesMap);
         xpath.setNamespaceContext(namespaceContext);
      }

      XPathExpression[] columnsExpressionsArray =
              new XPathExpression[xmlDogXPathManager.xmlTable.columnSpec.length];


      // compile XPath expressions
      for (int i = 0; i < xmlDogXPathManager.xmlTable.columnSpec.length; i++) {
         try {
            columnsExpressionsArray[i] = xpath.compile(xmlDogXPathManager.xmlTable.columnSpec[i]);
         } catch (XPathExpressionException ex) {
            throw new Exception(
                    "Error while compiling an XPath expression '" +
                            xmlDogXPathManager.xmlTable.columnSpec[i] +
                            "'. Error was: " + ex.getCause().getMessage(), ex);
         }
      }

      for (int i = 0; i < columnsExpressionsArray.length; i++) {
         try {
            // base context
            NodeList exprResult = (NodeList) columnsExpressionsArray[i].evaluate(
                    xmlDogXPathManager.xmlTable.templateNode, XPathConstants.NODESET);

            if (exprResult == null || exprResult.getLength() == 0) continue;

            // insert parameters markers. They will be used to place real values fro ResultSet
            // while writing an XML file
            exprResult.item(0).setNodeValue("{@" + storeFields[i].getName() + "}");
            exprResult.item(0).setTextContent("{@" + storeFields[i].getName() + "}");
         } catch (XPathExpressionException ex2) {
//            throw new Exception(
//                    "Can't resolve an XPath expression '" +
//                            xmlDogXPathManager.xmlTable.columnSpec[i] + "'. Error was: " +
//                            ex2.getCause().getMessage(), ex2);
         }
      }

      // serialize a DOM template node to String. It will be used for writing XML file.
      xmlDogXPathManager.xmlTable.rowTemplateString =
              XMLUtils.nodeToString(xmlDogXPathManager.xmlTable.templateNode);

      OtherUtils.writeTraceInfo(log, "rowTemplateString = " +
              xmlDogXPathManager.xmlTable.rowTemplateString);
   }

   /**
    * extract a DOM node that will be used for writing records as a template
    *
    * @throws Exception
    */
   private void extractTemplateNode() throws Exception {
      InputStream is = xmlDogXPathManager.xmlTable.getInputStream();
      Event event = xmlDogXPathManager.dog.createEvent();
      event.setXMLBuilder(new DOMBuilder());

      event.setListener(new InstantEvaluationListener() {
         @Override
         public void onNodeHit(Expression expression, NodeItem nodeItem) {
            Node node = (Node) nodeItem.xml;

            if (expression == xmlDogXPathManager.baseXQuery) {
               if (xmlDogXPathManager.xmlTable.templateNode == null) {
                  xmlDogXPathManager.xmlTable.templateNode = node.cloneNode(true);
                  // forcedly terminate parsing
                  throw new RuntimeException("parsing is terminated");
               }
            }
         }

         @Override
         public void finishedNodeSet(Expression expression) {
         }

         @Override
         public void onResult(Expression expression, Object result) {
         }
      });

      try {
          xmlDogXPathManager.dog.sniff(event, new InputSource(is), xmlDogXPathManager.xmlTable.disableDTD);
      } catch (Exception e) {
         // other unexpected exception
         if (!e.getCause().getCause().getMessage().equals("parsing is terminated"))
            throw new UnexpectedException("Error while writing an XML. " +
                    "Can't extract a template node.", e);
      }

      if (xmlDogXPathManager.xmlTable.templateNode == null)
         throw new UnexpectedException("Can't find the XPath '" +
                 xmlDogXPathManager.baseXQuery.getXPath() +
                 "' in an XML document. If the XML document is empty, " +
                 "you should add 'templateRow' tag to the table description in the schema file");

      is.close();
   }

   public void clear() {
      this.storeFields = null;
      this.xmlDogXPathManager = null;
   }
}


