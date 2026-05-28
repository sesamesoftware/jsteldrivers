package com.relationaljunction.jdbc.dbf;

import java.sql.*;

import org.junit.Assert;
import org.junit.Test;

public class DBFDriver2Test2 {

	private final String TEST_DATA_PATH = "C:/testData/dbf/";

	public DBFDriver2Test2() {
	}

	@Test
	public void testFileConnection() {
		try {
			// load the driver into memory
			Class.forName("com.relationaljunction.jdbc.dbf.DBFDriver2");
			String DBFFILESDIR = TEST_DATA_PATH + "databases";

			java.util.Properties props = new java.util.Properties();

			Connection conn = DriverManager.getConnection("jdbc:relationaljunction:dbf:" + DBFFILESDIR, props);

			// create a Statement object to execute the query with
			Statement stmt = conn.createStatement();
			// execute a query
			ResultSet rs = stmt.executeQuery("SELECT * FROM \"dbase_8b.dbf\"");

			// read the data and put it to the console
			for (int j = 1; j <= rs.getMetaData().getColumnCount(); j++) {
				System.out.print(rs.getMetaData().getColumnName(j) + "\t");
			}
			System.out.println();

			while (rs.next()) {
				System.out.println("Column Count: " + rs.getMetaData().getColumnCount());
				for (int j = 1; j <= rs.getMetaData().getColumnCount(); j++) {
					System.out.print("Start Record" + rs.getObject(j) + "End Record" + "\t" );
				}
			}

			// close the objects
			rs.close();
			stmt.close();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}
}
