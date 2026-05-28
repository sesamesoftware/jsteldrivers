package com.relationaljunction.jdbc.xml.h2.store;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.database.StoreException;
import com.relationaljunction.database.StoreFieldIF;
import com.relationaljunction.utils.UnexpectedException;

import net.sf.saxon.query.StaticQueryContext;
import nu.xom.Builder;
import nux.xom.io.StaxUtil;
import nux.xom.xquery.XQuery;
import nux.xom.xquery.XQueryException;

public class NuxXPathManager extends XPathManager {
   
   private final Logger log = LoggerFactory.getLogger("NuxXPathManager");

   Builder builder = null;
   XQuery baseXQuery = null;
   XQuery[] relativeXQueries = null;

   NuxXPathManager(XMLTable xmlTable) {
      this.xmlTable = xmlTable;

      boolean useSAX = true;

      builder = useSAX ? new Builder() : StaxUtil.createBuilder(null, null);

      StaticQueryContext queryContext = new StaticQueryContext(new net.sf.saxon.Configuration());

      if (!xmlTable.namespacesMap.isEmpty())
         setNamespaces(queryContext);

      try {
         baseXQuery = new XQuery(xmlTable.tableDescr.getXPath(), null,
                 queryContext, null);
      } catch (XQueryException ex) {
         ex.printStackTrace();
         throw new UnexpectedException("error while compiling an XPath expression '" +
                 xmlTable.tableDescr.getXPath() +
                 "'", ex);
      }

      relativeXQueries = new XQuery[xmlTable.columnSpec.length];

      for (int i = 0; i < xmlTable.columnSpec.length; i++) {
         try {
            relativeXQueries[i] = new XQuery(xmlTable.columnSpec[i], null,
                    queryContext, null);
         } catch (Exception ex) {
            ex.printStackTrace();
            throw new UnexpectedException("error while compiling an XPath expression '" +
                    xmlTable.columnSpec[i] + "'", ex);
         }
      }
   }

   private void setNamespaces(StaticQueryContext queryContext) {
      String prefix = null, uri = null;

      try {
         Set<String> prefixSet = xmlTable.namespacesMap.keySet();
         for (String curPrefix : prefixSet) {
            prefix = curPrefix;
            uri = xmlTable.namespacesMap.get(prefix);

            if (uri.equals(XMLStoreSchema.XML_NAMESPACE_URI)) {
               log.warn("The XML namespace (http://www.w3.org/XML/1998/namespace) is specified " +
                       "in the 'namespaces' property. It will be ignored.");
               continue;
            }

            queryContext.declarePassiveNamespace(prefix, uri, true);
         }
      } catch (Exception ex) {
         ex.printStackTrace();
         throw new UnexpectedException("Can't declare a namespace for prefix '" +
                 prefix + "' and URI '" + uri + "'", ex);
      }
   }

   public void create(StoreFieldIF[] fields) throws StoreException {
   }

   public XMLTableLoaderIF getLoader() throws Exception {
      return new NuxXMLTableLoader(this);
   }

   public XMLTableSaverIF getSaver() throws Exception {
      return null;
   }

}
