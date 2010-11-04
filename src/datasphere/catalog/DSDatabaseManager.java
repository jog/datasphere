package datasphere.catalog;

/*
Copyright (c) 2010 J.Goulding 

Licensed under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and 
limitations under the License. 
*/

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import datasphere.dataware.DSException;

/**
 * DSDatabaseManager objects handle interaction with the catalog server's persistence
 * layer - all {@link DSCatalog} instances require an DSDatabaseManager object to manage 
 * service state. The class' interface provides both standard database functionality
 * such as connectivity management and query statement access, as well as providing 
 * utility functions such as checking the integrity of required datasphere system tables, 
 * detecting jid clashes and simplifying common gqueries. <br/>
 * <br/>
 * Importantly the class also provides three utility methods that return the names of
 * essential system tables: {@link #getConnectionsTable()}, {@link #getUsersTable()}
 * and {@link #getUpdatesTable()}. Any reference to these tables in an external classes
 * should not be hard coded, but refer these methods. 
 * 
 * @author James Goulding
 * @version 2010-11-03
 */

public class DSDatabaseManager {

	private static Logger logger = Logger.getLogger( DSCatalog.class.getName() );
	
	private Connection conn = null;
	private String address;
	private String login;
	private String password;
	private Driver driver;
	
	private static final String CONNECTIONS_TABLE 	= "DS_SYS_CONNECTIONS";
	private static final String USERS_TABLE			= "DS_SYS_USERS";
	private static final String UPDATES_TABLE		= "DS_SYS_UPDATES";
	
	//////////////////////////////////
	
	/**
	 * @param address 		The URL address of the database to be used for persistence.
	 * @param login			The login name used to access that database.
	 * @param password 		The password associated with the account.
	 * @param driver		The JDBC driver for the flavour of DBMS being used.
	 */
	public DSDatabaseManager( 
		String address, 
		String login, 
		String password,
		Driver driver) {
		
		this.address = address;
		this.login = login;
		this.password = password;
		this.driver = driver;
	}
	
	//////////////////////////////////
		
	public String getConnectionsTable() {
		return CONNECTIONS_TABLE;
	}
	
	//////////////////////////////////
	
	public String getUsersTable() {
		return USERS_TABLE;
	}
	
	//////////////////////////////////
	
	public String getUpdatesTable() {
		return UPDATES_TABLE;
	}
	
	//////////////////////////////////

	/**
	 * Used to obtain a JDBC {@link Statement} object through which interaction
	 * with the database, such as querying or updating of game state, can be performed.
	 * @return Statement The statement object to be used.
	 * @throws SQLException Thrown if the database cannot supply a statment.
	 */
	public final Statement createStatement() 
	throws SQLException	{
		return conn.createStatement();
	}
	
	//////////////////////////////////
	
	/**
	 * Attempts to establish a connection to the specified database. 
	 * @throws SQLException Thrown if connection cannot be made to the specified
	 * database address, via the supplied drivers.
	 */
	public void connect() 
	throws DSException {
		
		try {
			DriverManager.registerDriver ( driver );
			conn = DriverManager.getConnection( address, login , password );
			logger.info( "Connecting to database for persistence... [SUCCESS]" );
		} catch ( SQLException e ) {
			logger.info( "Connecting to database for persistence... [FAILED]" );
			throw new DSException( e );
		}
	}
	
	//////////////////////////////////
	
	/**
	 * Returns the URL database address registered to the manager.
	 * @return String The URL of the database being used. 
	 */
	public final String getAddress() {
		return address;
	}
	
	//////////////////////////////////
		
	/**
	 * Method that checks the integrity of required system tables. Currently
	 * this just checks the existence of the connections, users and updates
	 * tables. N.b. that if tables do not exist they can be automatically 
	 * created via the {@link #createSystemTables()} method.
	 * @throws DSException Thrown if there is a problem with table integrity.
	 */
	public final void checkSystemTables() 
	throws DSException {
		
		ResultSet res = null;
		
		try {
			DatabaseMetaData meta = conn.getMetaData();
			ArrayList< String > missing = new ArrayList< String >();
			
			res = meta.getTables( null, null, CONNECTIONS_TABLE, null );
			if ( !res.next() ) missing.add( CONNECTIONS_TABLE );

			res = meta.getTables(null, null, USERS_TABLE, null );
			if ( !res.next() ) missing.add( USERS_TABLE );
					
			res = meta.getTables(null, null, UPDATES_TABLE, null );
			if ( !res.next() ) missing.add( UPDATES_TABLE );
					
			if ( missing.isEmpty() ) {
				logger.info( "Checking System Table integrity... [SUCCESS]" );
			} else {
				logger.info( "Checking System Table integrity... [FAILED]" );
				for ( String s : missing ) { 
					logger.info( ">>> [" + s + "] table missing" );
				}
				throw new DSException( "Required System tables are missing.\n" +
						"You might want to try the '--create' flag if this is " +
						"your first server run.\n" +
						"Please see --help for more details." );
			}
			
		} catch ( SQLException e ) {
			logger.info( "Checking System Table integrity... [FAILED]" );
			throw new DSException( e.getMessage() );
		} finally {
			try {
				res.close();
			} catch ( SQLException e ) {
				logger.log( Level.SEVERE, "+++ Database Manager: SQL Meltdown - ", e );
			}
		}
	}

	//////////////////////////////////
	
	/**
	 * Automatically create the system tables necessary for the framework to run.
	 * These are composed of tables to keep track of connections, registered 
	 * users, and the updates we have received from their dataware.
	 * @throws SQLException Thrown if the tables cannot be created due to a DB error.
	 */
	public void createSystemTables() 
	throws DSException {
		
		try {
			Statement stmt = createStatement();
			
			String connectionsTableQuery = 
				"CREATE TABLE " + CONNECTIONS_TABLE + " (" +
				"	CONNECTIONID NUMBER NOT NULL ENABLE, " +
				"   IPADDRESS VARCHAR2(256 BYTE) NOT NULL ENABLE, " + 
				"   PORT NUMBER NOT NULL ENABLE, " +
				"   TIMESTAMP NUMBER " +
				")";
			
			String devicesTableQuery = 
				"CREATE TABLE " + USERS_TABLE + " (" +
				"   CONNECTIONID NUMBER NOT NULL ENABLE, " +
				"   DEVICEID VARCHAR2(256 BYTE) NOT NULL ENABLE, " +
				"   DEVICEID_TYPE VARCHAR2(128 BYTE) NOT NULL ENABLE, " + 
				"   TIMESTAMP VARCHAR2(4000 BYTE) " +
				")";
			
			String positionsTableQuery = 
				"CREATE TABLE " + UPDATES_TABLE + " (" + 
				"   DEVICEID VARCHAR2(256 BYTE) NOT NULL ENABLE, " + 
				"   LONGITUDE NUMBER NOT NULL ENABLE, " +
				"   LATITUDE NUMBER NOT NULL ENABLE, " +
				"   PROVIDER VARCHAR2(128 BYTE), " +
				"   ACCURACY NUMBER, " +
				"   SPEED NUMBER, " +
				"   CONDITION VARCHAR2(32 BYTE), " + 
				"   TIMESTAMP VARCHAR2(4000 BYTE) " +
				")";
			
			stmt.addBatch( connectionsTableQuery );
			stmt.addBatch( devicesTableQuery );
			stmt.addBatch( positionsTableQuery );
			stmt.executeBatch();
			logger.info( "Creating System Tables... [SUCCESS]" );
			
		} catch ( SQLException e ) {
			logger.info( "Creating System Tables... [FAILED]" );
			throw new DSException( e );
		}
	}

	
	//////////////////////////////////
	
	/**
	 * When called, removes old connection, user and update information from
	 * system tables, giving an instance a blank slate from which to run.
	 * @throws SQLException Thrown if deletions cannot be made from system tables.
	 */
	public void clearSystemTables() 
	throws DSException {
		
		try {
			Statement stmt = createStatement();
			stmt.addBatch( "DELETE FROM " + USERS_TABLE );
			stmt.addBatch( "DELETE FROM " + CONNECTIONS_TABLE );
			stmt.addBatch( "DELETE FROM " + UPDATES_TABLE );
			stmt.executeBatch();
			logger.info( "Wiping System Tables of old data... [SUCCESS]" );
			
		} catch ( SQLException e ) {
			logger.info( "Wiping System Tables of old data... [FAILED]" );
			throw new DSException( e );
		}
	}
	
	//////////////////////////////////
	
	/**
	 * When called, removes old connection information from the system table.
	 * @throws SQLException Thrown if deletions cannot be made from system tables.
	 */
	public void clearConnections() 
	throws SQLException {
		
		try {
			Statement stmt = createStatement();
			stmt.addBatch( "DELETE FROM " + CONNECTIONS_TABLE );
			stmt.executeBatch();
			logger.info( "Wiping Connections Table of old data... [SUCCESS]" );
			
		} catch ( SQLException e ) {
			logger.info( "Wiping Connections Table of old data... [FAILED]" );
			throw e;
		}
	}
	
	//////////////////////////////////
	
	/**
	 * When called, removes old connection information from the system table.
	 * @throws SQLException Thrown if deletions cannot be made from system tables.
	 */
	public void clearUpdates() 
	throws SQLException {
		
		try {
			Statement stmt = createStatement();
			stmt.addBatch( "DELETE FROM " + UPDATES_TABLE );
			stmt.executeBatch();
			logger.info( "Wiping Updates Table of old data... [SUCCESS]" );
			
		} catch ( SQLException e ) {
			logger.info( "Wiping Updates Table of old data... [FAILED]" );
			throw e;
		}
	}
	
	//////////////////////////////////

	/**
	 * Returns a list of the JID's currently connected to by the system 
	 * @returns List the list of currently active (i.e. connected) jids
	 */
	public ArrayList< String > getAllConnections() {
		
		ArrayList< String > a = new ArrayList< String >();
		try {

			Statement stmt = createStatement();
			String query = "SELECT JID FROM " + CONNECTIONS_TABLE; 
			ResultSet rs = stmt.executeQuery( query );
			while ( rs.next() ) 
				a.add( rs.getString( "JID" ) );
			
		} catch ( SQLException e ) {
			logger.log( Level.SEVERE, "+++ Database Manager: SQL Meltdown - ", e );
		} 
		
		return a;
	}

	//////////////////////////////////

	/**
	 * 
	 * @return
	 */
	public Map< String, DSClientBot > fetchClients() {
		
		Map< String, DSClientBot > clients = new HashMap< String, DSClientBot >();
		
		DSClientBot xc1 = new DSClientBot( 
			new DSClient( 
				"james.goulding@gmail.com",
				"james.goulding@gmail.com",
				"talk.google.com", 
				"gmail.com",
				"jim1nez" )
			 );

		DSClientBot xc2 = new DSClientBot( 
			new DSClient( 
				"testreceiver@jabber.org",
				"testreceiver",
				"jabber.org", 
				"jabber.org",
				"jim1nez" )
		);

		
		clients.put( "james.goulding@gmail.com", xc1 ); 
		clients.put( "testreceiver@jabber.org", xc2 ); 

		return clients;
	}	
}
