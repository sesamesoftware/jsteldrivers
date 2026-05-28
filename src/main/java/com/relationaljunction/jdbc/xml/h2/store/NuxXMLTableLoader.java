package com.relationaljunction.jdbc.xml.h2.store;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.database.StoreFieldIF;
import com.relationaljunction.utils.OtherUtils;
import com.relationaljunction.utils.UnexpectedException;

import nu.xom.Document;
import nu.xom.Node;
import nux.xom.xquery.ResultSequence;
import nux.xom.xquery.XQueryException;

public class NuxXMLTableLoader extends GenericXMLTableLoader implements XMLTableLoaderIF {
	
   private final Logger log = LoggerFactory.getLogger("NuxXMLTableLoader");

   private NuxXPathManager xPathManager = null;
   private PreparedStatement pstInsert = null;
   private int currentPosition = 0;

   public NuxXMLTableLoader(NuxXPathManager xPathManager) {
      super(xPathManager.xmlTable);
      this.xPathManager = xPathManager;
   }

   public void setInsertPreparedStatement(PreparedStatement pstInsert) {
      this.pstInsert = pstInsert;
   }

   public void loadTable() throws Exception {
      OtherUtils.writeLogInfo(log, "using NuxXMLTableLoader to parse data from '" +
              xPathManager.xmlTable.getName() + "'");

      InputStream is = xPathManager.xmlTable.getInputStream();
      Document doc = xPathManager.builder.build(is);
      StoreFieldIF[] storeFields = xPathManager.xmlTable.getFields();

      ResultSequence baseResultSequence;
      try {
         baseResultSequence = xPathManager.baseXQuery.execute(doc);
      } catch (XQueryException ex) {
         throw new UnexpectedException("Can't resolve an XPath expression '" +
                 xPathManager.xmlTable.tableDescr.getXPath() + "'", ex);
      }

      Node baseNode;

      // loop on base elements
      while ((baseNode = baseResultSequence.next()) != null) {
         currentPosition++;

         // check if the row should be ignored
         boolean isRowIgnored = xPathManager.xmlTable.ignoreRowsExpression != null &&
                 xPathManager.xmlTable.ignoreRowsExpression.matches(currentPosition);

         if (isRowIgnored) {
            // check if all records below are ignored too
            if (xPathManager.xmlTable.ignoreRowsExpression.
                    isNumberInRangeToMaximum(currentPosition)) {
               // all records below should be ignored, so it can be treated as EOF
               // forcedly terminate parsing
               break;
            }

            // just ignore the current row
            continue;
         }

         RelationOnetoOneMerger<String> merger = new RelationOnetoOneMerger<String>();
         merger.setRepeatLastValue(xPathManager.xmlTable.repeatLastValues);
         merger.setReturnLastValueOnly(xPathManager.xmlTable.returnLastValues);
         merger.setNullElement("");

         List<List<String>> relativeArrays = new ArrayList<List<String>>(storeFields.length);

         // loop on relative elements (i.e. columns)
         for (int i = 0; i < xPathManager.relativeXQueries.length; i++) {
            ResultSequence relativeResultSequence;

            try {
               relativeResultSequence = xPathManager.relativeXQueries[i].execute(baseNode);
            } catch (XQueryException ex) {
               throw new UnexpectedException("Can't resolve an XPath expression '" +
                       xPathManager.xmlTable.columnSpec[i] + "'", ex);
            }

            // relative array (values for the current column)
            List<String> relativeArray = new LinkedList<String>();

            Node relativeNode;
            while ((relativeNode = relativeResultSequence.next()) != null) {
               relativeArray.add(relativeNode.getValue());
            }

            relativeArrays.add(relativeArray);
         }

         merger.setArrays(relativeArrays);

         for (List<String> row : merger) {
//        System.out.println("\nRecord: " + row);

            // insert a record to H2
            Object[] objs = new Object[storeFields.length];
            try {
               for (int j = 0; j < storeFields.length; j++) {
                  objs[j] = xPathManager.xmlTable.getSQLObject(storeFields[j],
                          row.get(j), dateFormatters[j], decimalFormatters[j]);
                  pstInsert.setObject(j + 1, objs[j]);
               }

               pstInsert.execute();
            } catch (Exception ex) {
               throw new UnexpectedException("Can't load the record for the table '" +
                       xPathManager.xmlTable.getName() + "'", ex);
            }
         }
      } // end of loop on base elements

      is.close();
   }

   public void clear() {
      xPathManager = null;
      pstInsert = null;
   }

}
