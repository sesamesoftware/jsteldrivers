package com.relationaljunction.jdbc.xml.h2.store;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import com.relationaljunction.database.StoreFieldIF;
import com.relationaljunction.utils.OtherUtils;
import com.relationaljunction.utils.UnexpectedException;

import jlibs.xml.sax.dog.NodeItem;
import jlibs.xml.sax.dog.expr.Expression;
import jlibs.xml.sax.dog.expr.InstantEvaluationListener;
import jlibs.xml.sax.dog.sniff.DOMBuilder;
import jlibs.xml.sax.dog.sniff.Event;

public class XMLDogXMLTableLoader extends GenericXMLTableLoader implements XMLTableLoaderIF {
	
   private final Logger log = LoggerFactory.getLogger("XMLDogXMLTableLoader");

   private XMLDogXPathManager xmlDogXPathManager;
   private PreparedStatement pstInsert;
   private int currentPosition = 0;

   public XMLDogXMLTableLoader(XMLDogXPathManager xmlDogXPathManager) {
      super(xmlDogXPathManager.xmlTable);
      this.xmlDogXPathManager = xmlDogXPathManager;
   }

   public void clear() {
      xmlDogXPathManager = null;
      pstInsert = null;
   }

   public void setInsertPreparedStatement(PreparedStatement pstInsert) {
      this.pstInsert = pstInsert;
   }

   public void loadTable() throws Exception {
      OtherUtils.writeLogInfo(log, "using XMLDogXMLTableLoader to parse data from '" +
              xmlDogXPathManager.xmlTable.getName() + "'");

      InputStream is = xmlDogXPathManager.xmlTable.getInputStream();
      final StoreFieldIF[] storeFields = xmlDogXPathManager.xmlTable.getFields();

      final RelationOnetoOneMerger<String> merger = new RelationOnetoOneMerger<String>();
      merger.setRepeatLastValue(xmlDogXPathManager.xmlTable.repeatLastValues);
      merger.setReturnLastValueOnly(xmlDogXPathManager.xmlTable.returnLastValues);
      merger.setNullElement("");

      Event event = xmlDogXPathManager.dog.createEvent();
      event.setXMLBuilder(new DOMBuilder());

      // will contain evaluated array of string values for a particular column
      final LinkedHashMap<Expression, List<String>> relativeXQueriesResultMap =
              new LinkedHashMap<Expression, List<String>>(xmlDogXPathManager.relativeXQueries.length);

      for (Expression relativeXQuery : xmlDogXPathManager.relativeXQueries) {
         relativeXQueriesResultMap.put(relativeXQuery, new LinkedList<String>());
      }

      event.setListener(new InstantEvaluationListener() {
         @Override
         public void onNodeHit(Expression expression, NodeItem nodeItem) {
            org.w3c.dom.Node node = (org.w3c.dom.Node) nodeItem.xml;

            if (expression == xmlDogXPathManager.baseXQuery) {
               // found a base XPath element
               currentPosition++;

               // check if the row should be ignored
               boolean isRowIgnored = xmlDogXPathManager.xmlTable.ignoreRowsExpression != null &&
                       xmlDogXPathManager.xmlTable.ignoreRowsExpression.matches(currentPosition);

               if (isRowIgnored) {
                  // check if all records below are ignored too
                  if (xmlDogXPathManager.xmlTable.ignoreRowsExpression.
                          isNumberInRangeToMaximum(currentPosition)) {
                     // all records below should be ignored, so it can be treated as EOF
                     // forcedly terminate parsing
                     throw new RuntimeException("parsing is terminated");
                  }

                  // just ignore the current row
                  clearResultMap(relativeXQueriesResultMap);
                  return;
               }

               Object[] objs = new Object[storeFields.length];

               // merge relative XQuery values found within the base XPath element
               for (List<String> relativeArray : relativeXQueriesResultMap.values()) {
                  merger.addArray(relativeArray);
               }

               try {
                  for (List<String> row : merger) {
                     //        System.out.println("\nRecord: " + row);

                     // insert a record to H2
                     for (int i = 0; i < storeFields.length; i++) {
                        objs[i] = xmlDogXPathManager.xmlTable.getSQLObject(storeFields[i],
                                row.get(i), dateFormatters[i], decimalFormatters[i]);
                        pstInsert.setObject(i + 1, objs[i]);
                     }

                     pstInsert.execute();
                  }

                  merger.clear();

                  clearResultMap(relativeXQueriesResultMap);
               } catch (Exception e) {
                  throw new UnexpectedException("Can't load the record for the table '" +
                          xmlDogXPathManager.xmlTable.getName() + "'", e);
               }
            } else {
               List<String> relativeArray = relativeXQueriesResultMap.get(expression);

               if (xmlDogXPathManager.xQueriesTypeMap.get(expression).
                       equals(XMLDogXPathManager.ABSOLUTE_XPATH)) {
                  // clear values for an absolute XPath
                  relativeArray.clear();
               }

               // add the next value for this array (column)
               relativeArray.add(node.getTextContent());
            }

            OtherUtils.writeTraceInfo(log, expression.getXPath(), " value = " + node.getTextContent());
         }

         @Override
         public void finishedNodeSet(Expression expression) {
         }

         @Override
         public void onResult(Expression expression, Object result) {
            // the method is called only once while evaluating constants
            List<String> relativeArray = relativeXQueriesResultMap.get(expression);
            relativeArray.add(result.toString());
         }
      }

      );

      try {
          xmlDogXPathManager.dog.sniff(event, new InputSource(is), xmlDogXPathManager.xmlTable.disableDTD);
      } catch (Exception e) {
         // other unexpected exception
         if (!e.getCause().getCause().getMessage().equals("parsing is terminated"))
            throw new UnexpectedException("Error while parsing an XML for '" +
                    xmlDogXPathManager.xmlTable.getName() + "'", e);
      }

      is.close();
   }

   private void clearResultMap(LinkedHashMap<Expression, List<String>> relativeXQueriesResultMap) {
      // clear only relative XQuery values, absolute XQueries must be stay the same
      Set<Expression> keySet = relativeXQueriesResultMap.keySet();
      for (Expression mapExpression : keySet) {
         // if a path is absolute retain its value
         if (xmlDogXPathManager.xQueriesTypeMap.get(mapExpression).
                 equals(XMLDogXPathManager.RELATIVE_XPATH)) {
            // if a path is relative clear values
            List<String> relativeArray = relativeXQueriesResultMap.get(mapExpression);
            relativeArray.clear();
         }
      }
   }
}
