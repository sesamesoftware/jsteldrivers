package com.relationaljunction.jdbc.mdb;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class MDBDriver2Test {

	private final String TEST_DATA_PATH =
			Paths.get("src", "test", "resources", "mdb", "access")
					.toAbsolutePath()
					.toString();

	private static final String URL_PREFIX = "jdbc:relationaljunction:mdb:";
	public static final String DEFAULT_EXTENSION = "";
	public static final String H2_TRIGGER_CLASS_NAME = "com.relationaljunction.jdbc.common.h2.sql.DefaultH2Trigger";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// load the driver into memory
		Class.forName("com.relationaljunction.jdbc.mdb.MDBDriver2");
	}

	@Test
	public void testMDBConnection() {
		try {
			Properties props = new java.util.Properties();
			props.put("format", "access2007");
			props.put("ignoreCase", "false");

			Connection conn = DriverManager
					.getConnection(URL_PREFIX + TEST_DATA_PATH + "\\ListBox_Search_2000.mdb", props);
			DatabaseMetaData dbmd = conn.getMetaData();
			
			dbmd.getTables("*", "", "", null);
			
			ResultSet rs = dbmd.getTables("*", "", "", null);

			// read the data and print it on the console
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
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testMDBConnection2() {
		try {
			Properties props = new java.util.Properties();
			props.put("format", "access2007");
			props.put("ignoreCase", "false");

			Connection conn = DriverManager
					.getConnection(URL_PREFIX + TEST_DATA_PATH + "/ListBox_Search_2000.mdb", props);

			// create a Statement object to execute the query with
			Statement stmt = conn.createStatement();
			// execute a query
			ResultSet rs = stmt.executeQuery("SELECT * FROM tblSalespersonContact");

			// read the data and print it on the console
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
			Assert.fail(e.getMessage());
		}
	}
}
