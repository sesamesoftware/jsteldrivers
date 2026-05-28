package com.relationaljunction.jdbc.xml;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.Test;

public class XMLDriverTest {
	
	   private static final String URL_PREFIX = "jdbc:relationaljunction:xml:";

	@Test
	public void testXMLConnection() {
		try {
			// load the driver into memory
			Class.forName("com.relationaljunction.jdbc.xml.XMLDriver2");
			
			Connection conn = DriverManager.getConnection(URL_PREFIX + "C:/xml/19857/formschema.xml");

			// create a Statement object to execute the query with
			Statement stmt = conn.createStatement();
			// execute a query
			ResultSet rs = stmt.executeQuery("SELECT * FROM form");

			// read the data and put it to the console
			for (int j = 1; j <= rs.getMetaData().getColumnCount(); j++) {
				System.out.print(rs.getMetaData().getColumnName(j) + "\t");
			}
			System.out.println();

			while (rs.next()) {
				System.out.println("Column Count: " + rs.getMetaData().getColumnCount());
				System.out.println("START RECORD");
				for (int j = 1; j <= rs.getMetaData().getColumnCount(); j++) {
					System.out.print(rs.getMetaData().getColumnLabel(j) + " : " + rs.getObject(j) + "\n");
				}
				System.out.println("");
				System.out.println("END RECORD");
			}
			// close the objects
			rs.close();
			stmt.close();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
