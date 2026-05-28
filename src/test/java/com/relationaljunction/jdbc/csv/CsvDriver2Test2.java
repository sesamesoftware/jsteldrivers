package com.relationaljunction.jdbc.csv;

import org.junit.Test;

import java.sql.*;


public class CsvDriver2Test2 {
	
  public CsvDriver2Test2() {
  }

  @Test
  public void testFileConnection(){
	    try
	    {
	      // load the driver into memory
	      Class.forName("com.relationaljunction.jdbc.csv.CsvDriver2");
	      // create a connection. CSV files are assumed to be in the current directory
	      //Connection conn = DriverManager.getConnection("jdbc:relationaljunction:csv:.?separator=,");
	      
	      String CSVFILESDIR = "C:/csv/contact/";
	      
	      java.util.Properties props = new java.util.Properties();

	      props.put("rowDelimiter", "\n");
	      props.put("escapeSeparatorInQuotes", "true");
	      props.put("separator", ","); // fields separator
	      props.setProperty("dbInMemory", "false");
	      props.put("suppressHeaders", "false"); // column headers are on the first line
	      props.put("fileExtension", ".csv"); // default file extension is .csv
	      props.put("charset", "ISO-8859-2"); // file encoding is "ISO-8859-2"
	      props.put("commentLine", "--"); // string denoting comment line is "--"

	      Connection conn = DriverManager.getConnection("jdbc:relationaljunction:csv:" + CSVFILESDIR, props);
	 
	      // create a Statement object to execute the query with
	      Statement stmt = conn.createStatement();
	      // execute a query
	      ResultSet rs = stmt.executeQuery("SELECT OTHERSTREET FROM \"Contact.csv\"");
	 
	      // read the data and put it to the console
	      for (int j = 1; j <= rs.getMetaData().getColumnCount(); j++) {
	        System.out.print(rs.getMetaData().getColumnName(j) + "\t");
	      }
	      System.out.println();
	 
	      while (rs.next())
	      {
	    	  System.out.println("Column Count: " + rs.getMetaData().getColumnCount());
	          for(int j=1; j <= rs.getMetaData().getColumnCount(); j++){
	          	System.out.print("Start Record" + rs.getObject(j) + "End Record" + "\t" );
	          }
	      }
	      // close the objects
	      rs.close();
	      stmt.close();
	      conn.close();
	    }
	    catch(Exception e)
	    {
	      e.printStackTrace();
	    }
  }
}


