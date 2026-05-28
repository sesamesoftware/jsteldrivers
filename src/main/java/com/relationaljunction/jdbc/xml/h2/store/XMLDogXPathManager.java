package com.relationaljunction.jdbc.xml.h2.store;

import java.util.HashMap;
import java.util.Set;
import java.util.regex.Pattern;

import org.jaxen.saxpath.SAXPathException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.database.StoreException;
import com.relationaljunction.database.StoreFieldIF;
import com.relationaljunction.utils.OtherUtils;
import com.relationaljunction.utils.UnexpectedException;

import jlibs.xml.DefaultNamespaceContext;
import jlibs.xml.sax.dog.XMLDog;
import jlibs.xml.sax.dog.expr.Expression;

public class XMLDogXPathManager extends XPathManager {
   static final int RELATIVE_XPATH = 0;
   static final int ABSOLUTE_XPATH = 1;

   private final Logger log = LoggerFactory.getLogger("XMLDogXPathManager");

   XMLDog dog = null;
   Expression baseXQuery = null;
   Expression[] relativeXQueries = null;
   HashMap<Expression, Integer> xQueriesTypeMap = null;
   private static final Pattern absoluteXpathPattern = Pattern.compile("/|'|\\d|-");

   public XMLDogXPathManager(XMLTable xmlTable) {
      this.xmlTable = xmlTable;

      DefaultNamespaceContext nsContext = new DefaultNamespaceContext();

      if (!xmlTable.namespacesMap.isEmpty()) {
         Set<String> prefixSet = xmlTable.namespacesMap.keySet();
         for (String prefix : prefixSet) {
            String uri = xmlTable.namespacesMap.get(prefix);
            nsContext.declarePrefix(prefix, uri);
         }
      }

      dog = new XMLDog(nsContext);

      // base XPath with ending "/"
      String baseXQueryWithSeparator;

      try {
         if (xmlTable.tableDescr.getXPath().endsWith("/")) {
            baseXQuery = dog.addXPath(xmlTable.tableDescr.getXPath().
                    substring(0, xmlTable.tableDescr.getXPath().length() - 1));
            baseXQueryWithSeparator = xmlTable.tableDescr.getXPath();
         } else {
            baseXQuery = dog.addXPath(xmlTable.tableDescr.getXPath());
            baseXQueryWithSeparator = xmlTable.tableDescr.getXPath() + "/";
         }
      } catch (SAXPathException e) {
         throw new UnexpectedException("error while compiling an XPath expression '" +
                 xmlTable.tableDescr.getXPath() +
                 "'", e);
      }

      relativeXQueries = new Expression[xmlTable.columnSpec.length];
      xQueriesTypeMap = new HashMap<Expression, Integer>(xmlTable.columnSpec.length);

      for (int i = 0; i < xmlTable.columnSpec.length; i++) {
         try {
//            Expression expression = new XPathParser(nsContext, null, null).parse(
//                    xmlTable.columnSpec[i], true);
            if (absoluteXpathPattern.matcher(xmlTable.columnSpec[i].substring(0, 1)).matches()) {
               // absolute or constant path to an element
               relativeXQueries[i] = dog.addXPath(xmlTable.columnSpec[i]);
               xQueriesTypeMap.put(relativeXQueries[i], ABSOLUTE_XPATH);
            } else if (xmlTable.columnSpec[i].equals(".")) {
               // self element "." should be changed on text() to work properly
               relativeXQueries[i] = dog.addXPath(baseXQueryWithSeparator + "text()");
               xQueriesTypeMap.put(relativeXQueries[i], RELATIVE_XPATH);
            } else {
               // relative path to an element
               relativeXQueries[i] = dog.addXPath(baseXQueryWithSeparator + xmlTable.columnSpec[i]);
               xQueriesTypeMap.put(relativeXQueries[i], RELATIVE_XPATH);
            }

            OtherUtils.writeTraceInfo(log, "Compiled '" + relativeXQueries[i].getXPath() + "' xpath");
         } catch (Exception ex) {
            throw new UnexpectedException("error while compiling an XPath expression '" +
                    xmlTable.columnSpec[i] + "'", ex);
         }
      }

   }

   @Override
   public void create(StoreFieldIF[] fields) throws StoreException {
      try {
         new XMLTableSaver(this).createXML();
      } catch (Exception e) {
         throw new StoreException("Can't create a file '" + xmlTable.getName() + "'");
      }
   }

   @Override
   public XMLTableLoaderIF getLoader() throws Exception {
      return new XMLDogXMLTableLoader(this);
   }

   @Override
   public XMLTableSaverIF getSaver() throws Exception {
      return new XMLTableSaver(this);
   }
}
