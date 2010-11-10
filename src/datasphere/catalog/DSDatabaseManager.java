package datasphere.catalog;

/*
Copyright (C) 2010 J.Goulding, R. Mortier 

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
import datasphere.dataware.DSUpdate;

/**
 * DSDatabaseManager objects handle interaction with the catalog server's persistence
 * layer - all {@link DSCatalog} instances require an DSDatabaseManager object to manage 
 * service state. The class' interface provides both standard database functionality
 * such as connectivity management and query statement access, as well as providing 
 * utility functions such as checking the integrity of required datasphere system tables, 
 * detecting jid clashes and simplifying common queries. <br/>
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
	
	private static final String CONNECTIONS_TABLE 	= "ds_sys_connections";
	private static final String USERS_TABLE			= "ds_sys_users";
	private static final String UPDATES_TABLE		= "ds_sys_updates";
	private static final String DATAWARE_TABLE		= "ds_sys_dataware";
	
	private static final String DEFAULT_LOGIN = "dsadmin";
	private static final String DEFAULT_SYS_DB = "datasphere"; 
	private static final String DEFAULT_PASSWORD = "YOUR_PASSWORD_HERE";
	
	//////////////////////////////////
	
	/**
	 * @param address 		The URL address of the database to be used for persistence.
	 * @param login			The login name used to access that database.
	 * @param password 		The password associated with the account.
	 * @param driver		The JDBC driver for the flavour of DBMS being used.
	 */
	public DSDatabaseManager( 
		String address, 
		Driver driver ) {
		
		this.address = address;
		this.driver = driver;
		this.login = DEFAULT_LOGIN;
		this.password = DEFAULT_PASSWORD;
	}

	//////////////////////////////////
	public void setPassword( String password ) {
		this.password = password;
		logger.info( "admin password has been specified as " + password );
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
			
			res = meta.getTables( null, null, USERS_TABLE, null );
			if ( !res.next() ) missing.add( USERS_TABLE );
			
			res = meta.getTables( null, null, CONNECTIONS_TABLE, null );
			if ( !res.next() ) missing.add( CONNECTIONS_TABLE );

			res = meta.getTables( null, null, DATAWARE_TABLE, null );
			if ( !res.next() ) missing.add( DATAWARE_TABLE );
					
			res = meta.getTables(null, null, UPDATES_TABLE, null );
			if ( !res.next() ) missing.add( UPDATES_TABLE );
					
			if ( missing.isEmpty() ) {
				logger.info( "Checking System Table integrity... [SUCCESS]" );
			} else {
				logger.info( "Checking System Table integrity... [FAILED]" );
				for ( String s : missing ) { 
					logger.info( ">>> [" + s + "] table missing" );
				}
				throw new DSException( "Required System tables are missing. " +
						"You might want to try the '--create' flag if this is " +
						"your first server run. Please see --help for more details." );
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
				"CREATE TABLE  `datasphere`.`" + CONNECTIONS_TABLE + "` (" +
				"`jid` varchar(256) NOT NULL," +
				"`ctime` bigint(20) unsigned NOT NULL," +
				"`atime` bigint(20) unsigned NOT NULL," +
				" PRIMARY KEY (`jid`) " +
				")";
			
			String updatesTableQuery = 
				"CREATE TABLE  `datasphere`.`" + USERS_TABLE + "` (" +
				"`jid` varchar(256) NOT NULL," +
				"`source` varchar(256) NOT NULL," +
				"`loc` varchar(45) NOT NULL," +
				"`description` varchar(1024) NOT NULL," +
				"`crud` varchar(8) NOT NULL," +
				"`total` bigint(20) unsigned NOT NULL," +
				"`meta` text NOT NULL," +
				"`tags` text NOT NULL," +
				"`ctime` bigint(20) unsigned NOT NULL," +
				"`rtime` bigint(20) unsigned NOT NULL," +
				"`sender` varchar(256) NOT NULL," +
				"PRIMARY KEY (`jid`)" +
				")";
			
			String usersTableQuery = 
				"CREATE TABLE  `datasphere`.`" + UPDATES_TABLE + "` (" +
				"`jid` varchar(256) NOT NULL," +
				"`firstname` varchar(256) NOT NULL," +
				"`lastname` varchar(256) NOT NULL," +
				"`email` varchar(256) NOT NULL," +
				"`ctime` bigint(20) unsigned NOT NULL," +
				"`atime` bigint(20) unsigned NOT NULL," +
				"`host` varchar(256) NOT NULL," +
				"`service` varchar(256) NOT NULL," +
				"`user` varchar(256) NOT NULL," +
				"`pass` varchar(256) NOT NULL," +
				"PRIMARY KEY (`jid`)" +
				")";
			
			String datawareTableQuery = 
				"CREATE TABLE `datasphere`.`" + DATAWARE_TABLE + "` (" +
				"`dwid` varchar(256) NOT NULL," +
				"`jid` varchar(256) NOT NULL," +
				"`subscriptionStatus` varchar(16) NOT NULL," +
				"`ctime` bigint(20) unsigned NOT NULL," +
				"`mtime` bigint(20) unsigned NOT NULL," +
				"PRIMARY KEY (`dwid`,`jid`)" +
				") ENGINE=InnoDB DEFAULT CHARSET=latin1";
			
			stmt.addBatch( connectionsTableQuery );
			stmt.addBatch( usersTableQuery );
			stmt.addBatch( updatesTableQuery );
			stmt.addBatch( datawareTableQuery );
			stmt.executeBatch();
			logger.info( "Creating System Tables... [SUCCESS]" );
			
		} catch ( SQLException e ) {
			logger.severe( "Creating System Tables... [FAILED]" );
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
			stmt.addBatch( "DELETE FROM " + DATAWARE_TABLE );
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
	 * @throws SQLException 
	 */
	public Map< String, DSClientBot > fetchClients() 
	throws SQLException {
		
		Map< String, DSClientBot > clients = new HashMap< String, DSClientBot >();
				
		Statement stmt = createStatement();
		String query = "SELECT jid, user, host, service, pass FROM datasphere." + USERS_TABLE;
		ResultSet rs = stmt.executeQuery( query );
		
		while ( rs.next() ) {
			
			DSClient c = new DSClient( 
				rs.getString( "jid" ),
				rs.getString( "user" ),
				rs.getString( "host" ), 
				rs.getString( "service" ),
				rs.getString( "pass" ) );
			 
			clients.put( 
				rs.getString( "jid" ),
				new DSClientBot( c )	
			);
		}
		
		return clients;
	}
	
	//////////////////////////////////
	
	public void insertUpdate( String jid, String from, DSUpdate d ) 
	throws DSException {

		try {
			Statement stmt = createStatement();
			
			String insert = "insert into datasphere." + UPDATES_TABLE + " " +
				"( jid, source, description, crud, total, ctime, rtime, meta, tags, loc, sender ) " +
				"values ( " +
				"'" + jid + "'," +
				"'" + d.getSource() + "'," +
				"'" + d.getDesc().replace( "\'", "\\\'" ) + "'," +
				"'" + d.getCrud() + "'," +
				""  + d.getTotal() + "," +
				""  + d.getMtime() + "," +
				""  + System.currentTimeMillis() + ","; 
		
			if ( d.getMetaJSON() == null ) insert += "null,";
			else insert += "'"  + d.getMetaJSON() + "',";
						
			if ( d.getTagsJSON() == null ) insert += "null,";
			else insert += "'"  + d.getTagsJSON() + "',";
		
			if ( d.getLocationJSON() == null ) insert += "null,";
			else insert += "'" + d.getLocationJSON() + "',";	
			
			insert += "'" + from + "')";
 
			stmt.addBatch( insert );
			stmt.executeBatch();
			
		} catch ( SQLException e ) {
			logger.severe( "--- DSDatabaseManager: attempt to update failed..." );
			throw new DSException( e );
		}
	}

	//////////////////////////////////
	
	public String getSubscriptionStatus( String dwid ) {

		String subscriptionStatus = null;
		try {
			Statement stmt = createStatement();
			String query = "SELECT subscriptionStatus FROM " 
				+ DATAWARE_TABLE + " WHERE dwid='" + dwid + "'";

			ResultSet rs = stmt.executeQuery( query );
			
			if ( rs.next() ) 
				subscriptionStatus = rs.getString( "subscriptionStatus" );

		} catch ( SQLException e ) {
			logger.log( Level.SEVERE, "+++ Database Manager: SQL Meltdown - ", e );
		}
		
		return subscriptionStatus; 
		
	}
	
	//////////////////////////////////
	
	public void setSubscriptionStatus( 
		String jid, 
		String dwid, 
		String subscriptionStatus ) 
	{
		try {
			Statement stmt = createStatement();
			String query = 
					"UPDATE " + DATAWARE_TABLE + " " + 
					"SET subscriptionStatus='" + subscriptionStatus + "' " +
					"WHERE dwid='" + dwid + "' AND jid='" + jid + "'";
			stmt.executeUpdate( query );

		} catch ( SQLException e ) {
			logger.log( Level.SEVERE, "+++ Database Manager: SQL Meltdown - ", e );
		} 
	}
	
	//////////////////////////////////
	
	public void insertDataware( 
		String jid, 
		String dwid, 
		String subscriptionStatus) 
	{
		try {
			Statement stmt = createStatement();
			String query = 
					"INSERT INTO " + DATAWARE_TABLE + " VALUES (" + 
					"'" + dwid + "'," + 
					"'" + jid  + "'," + 
					"'" + subscriptionStatus + "'," +
					System.currentTimeMillis() + "," +
					System.currentTimeMillis() + ")";
			stmt.executeUpdate( query );

		} catch ( SQLException e ) {
			logger.log( Level.SEVERE, "+++ Database Manager: SQL Meltdown - ", e );
		} 
	}
}
