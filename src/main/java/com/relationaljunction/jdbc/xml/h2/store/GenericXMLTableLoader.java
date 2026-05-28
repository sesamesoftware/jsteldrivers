package com.relationaljunction.jdbc.xml.h2.store;

import java.util.Locale;
import java.util.StringTokenizer;

import com.relationaljunction.database.StoreDataType;
import com.relationaljunction.database.StoreFieldIF;
import com.relationaljunction.utils.DateFormatter;
import com.relationaljunction.utils.DecimalFormatter;

public class GenericXMLTableLoader {
   protected DateFormatter[] dateFormatters;
   protected DecimalFormatter[] decimalFormatters;

   GenericXMLTableLoader(XMLTable xmlTable) {
      prepareUtilClasses(xmlTable);
   }

   private void prepareUtilClasses(XMLTable xmlTable) {
      dateFormatters = new DateFormatter[xmlTable.getFields().length];
      decimalFormatters = new DecimalFormatter[xmlTable.getFields().length];

      for (int i = 0; i < xmlTable.getFields().length; i++) {
         StoreFieldIF storeField = xmlTable.getFields()[i];

         // init date formatters
         if (storeField.getType().isDateType()) {
            Locale localeUsed = xmlTable.locale;

            if (xmlTable.tableDescr != null) {
               XMLColumnDescr columnDescr = xmlTable.tableDescr.findColumnDescrByName(storeField.getName());

               if (columnDescr != null) {
                  if (columnDescr.getLocale() != null) {
                     StringTokenizer tokenizer = new StringTokenizer(columnDescr.getLocale(), "_");
                     localeUsed = new Locale(tokenizer.nextToken(), tokenizer.nextToken());
                  }

                  if (columnDescr.getDateFormatString() != null) {
                     // decimal format is set separately for the specific column
                     dateFormatters[i] = new DateFormatter(columnDescr.getDateFormatString(), localeUsed);
                  }
               }
            }

            // date format is inherited from a table or a schema property.
            if (dateFormatters[i] == null && xmlTable.dateFormatString != null) {
               dateFormatters[i] = new DateFormatter(xmlTable.dateFormatString, localeUsed);
            }
         }
         // init decimal formatters
         else if (storeField.getType().isNumberType()) {
            if (xmlTable.tableDescr != null) {
               XMLColumnDescr columnDescr = xmlTable.tableDescr.findColumnDescrByName(storeField.getName());

               if (columnDescr != null && columnDescr.getDecimalFormatInput() != null) {
                  // decimal format is set separately for the specific column
                  decimalFormatters[i] = new DecimalFormatter(columnDescr.getDecimalFormatInput(),
                          columnDescr.getDecimalFormatOutput(), xmlTable.locale);
               }
            }

            // decimal format is inherited from a table or a schema property.
            if (decimalFormatters[i] == null && xmlTable.decimalFormatInput != null) {
               decimalFormatters[i] = new DecimalFormatter(xmlTable.decimalFormatInput,
                       xmlTable.decimalFormatOut, xmlTable.locale);
            }

            // set BigDecimal parsing for NUMERIC (BIGDECIMAL) types
            if (decimalFormatters[i] != null && storeField.getType() == StoreDataType.NUMERIC) {
               decimalFormatters[i].setParseBigDecimal(true);
            }
         }
      }
   }
}
