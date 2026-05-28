package com.relationaljunction.jdbc.xml.h2.store;

import java.util.*;

import com.relationaljunction.jdbc.xml.jdbc2xml.XMLOutputNode;
import com.relationaljunction.utils.UnexpectedException;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2006</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 2.5
 */

public class XMLTableDescr {
   private final Map<String, XMLColumnDescr> columnsHash =
           Collections.synchronizedMap(new LinkedHashMap<String, XMLColumnDescr>());
   private String name;
   private String filePath;
   private final String xPath;
   private Properties localProps = new Properties();
   private String templateRow;
   private XMLOutputNode xmlOutput;

   XMLTableDescr(String filePath, String xPath) {
      this.filePath = filePath;
      this.xPath = xPath;
   }

   XMLTableDescr(String name, String filePath, String xPath) {
      this.name = name.toLowerCase();
      this.filePath = filePath;
      this.xPath = xPath;
   }

   void addColumn(XMLColumnDescr colDescr) throws
           Exception {
      if (columnsHash.containsKey(colDescr.getName().toLowerCase()))
         throw new Exception("The column '" + colDescr.getName() +
                 "' is already described for the table '" + name +
                 "'");
      columnsHash.put(colDescr.getName().toLowerCase(), colDescr);
   }

   public List<XMLColumnDescr> getColumns() {
      return new ArrayList<XMLColumnDescr>(columnsHash.values());
   }

   public XMLColumnDescr findColumnDescrByName(String name) {
      return columnsHash.get(name.toLowerCase());
   }

   @Override
   public boolean equals(Object o) {
      if (o == null)
         return false;

      if (!(o instanceof XMLTableDescr))
         return false;

      XMLTableDescr tableDescr = (XMLTableDescr) o;

      if (getName() != null)
         return tableDescr.getName().equals(getName());
      else if (getFilePath() != null)
         return tableDescr.getFilePath().equals(getFilePath());

      return false;
   }

   @Override
   public int hashCode() {
      if (getName() != null)
         return getName().hashCode();
      else if (getFilePath() != null)
         return getFilePath().hashCode();

      throw new UnexpectedException("getName() and getFilePath() are null");
   }

   public Properties getLocalProps() {
      return localProps;
   }

   public String getFilePath() {
      return filePath;
   }

   public void setFilePath(String filePath) {
      this.filePath = filePath;
   }

   public String getName() {
      return name;
   }

   public String getXPath() {
      return xPath;
   }

   public void setLocalProps(Properties localProps) {
      this.localProps = localProps;
   }

   public String getTemplateRow() {
      return templateRow;
   }

   public void setTemplateRow(String templateRow) {
      this.templateRow = templateRow;
   }

   public XMLOutputNode getXMLOutput() {
      return xmlOutput;
   }

   public void setXMLOutput(XMLOutputNode xmlOutput) {
      this.xmlOutput = xmlOutput;
   }

}
