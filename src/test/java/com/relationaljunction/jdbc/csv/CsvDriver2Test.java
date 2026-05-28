package com.relationaljunction.jdbc.csv;

import com.relationaljunction.jdbc.filepath;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Paths;
import java.sql.*;
import java.util.Properties;

public class CsvDriver2Test {

	private final String TEST_DATA_PATH =
			Paths.get("src", "test", "resources", "csv", "contact")
					.toAbsolutePath()
					.toString();
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Class.forName("com.relationaljunction.jdbc.csv.CsvDriver2");
	}

	public CsvDriver2Test() {
	}

	@Test
	public void testFileConnection() {
		try {
			// create a connection. CSV files are assumed to be in the current directory
			// Connection conn =
			// DriverManager.getConnection("jdbc:relationaljunction:csv:.?separator=,");
			String url = "jdbc:relationaljunction:csv:" + TEST_DATA_PATH + "?separator=,&escapeEOLInQuotes=true";
			Connection conn = DriverManager
					.getConnection(url);

			// create a Statement object to execute the query with
			Statement stmt = conn.createStatement();
			// execute a query
			ResultSet rs = stmt.executeQuery("SELECT * FROM \"Contact.csv\"");

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
			Assert.fail(e.getMessage());
		}
	}

	//mode #1
	@Test
	public void testConnectionProperties() {
		try {
			Properties props = new java.util.Properties();
			props.setProperty("dbInMemory", "true");   // use the first mode (a temporary synchrobase in RAM)
			props.setProperty("tempPath", TEST_DATA_PATH + "\\temp");
			Connection conn = DriverManager.getConnection("jdbc:relationaljunction:csv:" + TEST_DATA_PATH, props);
		} catch (SQLException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	//mode #1
	@Test
	public void testRamSynchrobase() {
		try {
			Properties props = new java.util.Properties();

			props.setProperty("dbInMemory", "true");   // use the first mode (a temporary synchrobase in RAM)
			props.setProperty("tempPath", TEST_DATA_PATH + "\\temp");
			Connection conn = DriverManager.getConnection("jdbc:relationaljunction:csv:" + TEST_DATA_PATH, props);
		} catch (SQLException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	//mode #2
	@Test
	public void testTempSynchrobase() {
		try {
			Properties props = new java.util.Properties();

			props.setProperty("dbInMemory", "false");   // switch to the second mode (a temporary synchrobase on the hard drive)
			props.setProperty("tempPath", TEST_DATA_PATH + "\\temp");
			Connection conn = DriverManager.getConnection("jdbc:relationaljunction:csv:" + TEST_DATA_PATH, props);
		} catch (SQLException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	//mode #3
	@Test
	public void testDiskSynchrobase() {
		try {
			Properties props = new java.util.Properties();

			props.setProperty("dbPath", TEST_DATA_PATH + "synchrobases/syncro_db_name");   // switch to the second mode (a temporary synchrobase on the hard drive)
			props.setProperty("tempPath", TEST_DATA_PATH + "\\temp");
			Connection conn = DriverManager.getConnection("jdbc:relationaljunction:csv:" + TEST_DATA_PATH, props);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testWatchFileModifications() {

	}

	@Test
	public void testSingleton() {

	}
	
	@Test
	public void testTimestamp() {

	}
	
	@Test
	public void testFunctions() {
		//Test LEN
		//Test IIF
	}

	@Test
	public void testLogging() {
		
	}

}
