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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import datasphere.catalog.DSSub.Status;
import datasphere.catalog.xmpp.DSClientBot;
import datasphere.dataware.DSException;
import datasphere.dataware.DSFormatException;
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

public class DSDataManager {

	private static Logger logger = Logger.getLogger( DSCatalog.class.getName() );
	private static final String DEFAULT_SOURCE_URL = 
		"http://www.cs.nott.ac.uk/~jog/mydatasphere.org/namespaces.html";
	private static final String DEFAULT_SOURCE_ICON = "blank.jpg";
	
	public enum SourceField {
	    SID, NAMESPACE 
	}
	
	private Connection conn = null;
	private String address;
	private String login;
	private String password;
	private Driver driver;
	
	private static final String CONNECTIONS_TABLE 	= "ds_sys_connections";
	private static final String USERS_TABLE			= "ds_sys_users";
	private static final String UPDATES_TABLE		= "ds_sys_updates";
	private static final String SUBSCRIPTIONS_TABLE	= "ds_sys_subscriptions";
	private static final String SOURCES_TABLE 		= "ds_sys_sources";
	
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
	public DSDataManager( 
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
		logger.fine( "--- DSDataManager: admin password has been " +
			"specified via the command line... [SUCCESS]" );
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
			logger.info( "--- DSDatabaseManager: Connecting to database for persistence... [SUCCESS]" );
		} catch ( SQLException e ) {
			logger.info( "--- DSDatabaseManager: Connecting to database for persistence... [FAILED]" );
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

			res = meta.getTables( null, null, SUBSCRIPTIONS_TABLE, null );
			if ( !res.next() ) missing.add( SUBSCRIPTIONS_TABLE );
					
			res = meta.getTables(null, null, UPDATES_TABLE, null );
			if ( !res.next() ) missing.add( UPDATES_TABLE );
					
			if ( missing.isEmpty() ) {
				logger.info( "--- DSDataManager: Checking System Table integrity... [SUCCESS]" );
			} else {
				logger.severe( "--- DSDataManager: Checking System Table integrity... [FAILED]" );
				for ( String s : missing ) { 
					logger.severe( ">>> [" + s + "] table missing" );
				}
				throw new DSException( "Required System tables are missing. " +
						"You might want to try the '--create' flag if this is " +
						"your first server run. Please see --help for more details." );
			}
			
		} catch ( SQLException e ) {
			logger.info( "--- DSDataManager: Checking System Table integrity... [FAILED]" );
			throw new DSException( e.getMessage() );
		} finally {
			try {
				res.close();
			} catch ( SQLException e ) {
				logger.log( Level.SEVERE, "+++ DSDataManager:: SQL Meltdown - ", e );
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
				"CREATE TABLE  `" + DEFAULT_SYS_DB + "`.`" + CONNECTIONS_TABLE + "` (" +
				"`jid` varchar(256) NOT NULL," +
				"`ctime` bigint(20) unsigned NOT NULL," +
				"`atime` bigint(20) unsigned NOT NULL," +
				" PRIMARY KEY (`jid`) " +
				")";
			
			String updatesTableQuery = 
				"CREATE TABLE  `" + DEFAULT_SYS_DB + "`.`" + USERS_TABLE + "` (" +
				"`jid` varchar(256) NOT NULL," +
				"`namespace` varchar(256) NOT NULL," +
				"`loc` varchar(45) NOT NULL," +
				"`description` varchar(1024) NOT NULL," +
				"`crud` varchar(8) NOT NULL," +
				"`total` bigint(20) unsigned NOT NULL," +
				"`meta` text NOT NULL," +
				"`tags` text NOT NULL," +
				"`ctime` bigint(20) unsigned NOT NULL," +
				"`rtime` bigint(20) unsigned NOT NULL," +
				"`sid` varchar(256) NOT NULL," +
				"PRIMARY KEY (`jid`)" +
				")";
			
			String usersTableQuery = 
				"CREATE TABLE  `" + DEFAULT_SYS_DB + "`.`" + UPDATES_TABLE + "` (" +
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
			
			String subscriptionsTableQuery = 
				"CREATE TABLE `" + DEFAULT_SYS_DB + "`.`" + SUBSCRIPTIONS_TABLE + "` (" +
				"`sid` varchar(256) NOT NULL," +
				"`jid` varchar(256) NOT NULL," +
				"`subscriptionStatus` varchar(16) NOT NULL," +
				"`ctime` bigint(20) unsigned NOT NULL," +
				"`mtime` bigint(20) unsigned NOT NULL," +
				"PRIMARY KEY (`sid`,`jid`)" +
				") ENGINE=InnoDB DEFAULT CHARSET=latin1";
			
			stmt.addBatch( connectionsTableQuery );
			stmt.addBatch( usersTableQuery );
			stmt.addBatch( updatesTableQuery );
			stmt.addBatch( subscriptionsTableQuery );
			stmt.executeBatch();
			logger.info( "--- DSDataManager: Creating System Tables... [SUCCESS]" );
			
		} catch ( SQLException e ) {
			logger.severe( "--- DSDataManager: Creating System Tables... [FAILED]" );
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
			stmt.addBatch( "DELETE FROM " + SUBSCRIPTIONS_TABLE );
			stmt.executeBatch();
			logger.info( "--- DSDataManager: Wiping System Tables of old data... [SUCCESS]" );
			
		} catch ( SQLException e ) {
			logger.severe( "--- DSDataManager: Wiping System Tables of old data... [FAILED]" );
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
			logger.info( "--- DSDataManager: Wiping Connections Table of old data... [SUCCESS]" );
			
		} catch ( SQLException e ) {
			logger.severe( "--- DSDataManager: Wiping Connections Table of old data... [FAILED]" );
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
			logger.info( "--- DSDataManager: Wiping Updates Table of old data... [SUCCESS]" );
			
		} catch ( SQLException e ) {
			logger.severe( "--- DSDataManager: Wiping Updates Table of old data... [FAILED]" );
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
	public Map< String, DSClientBot > fetchClientBots() 
	throws SQLException {
		
		Map< String, DSClientBot > clients = new HashMap< String, DSClientBot >();
				
		Statement stmt = createStatement();
		String query = "SELECT * FROM " + DEFAULT_SYS_DB + "." + USERS_TABLE;
		ResultSet rs = stmt.executeQuery( query );
		while ( rs.next() ) {
			
			DSClient c = new DSClient( 
				rs.getString( "jid" ),
				rs.getString( "firstname" ),
				rs.getString( "lastname" ),
				rs.getString( "email" ),
				rs.getLong( "ctime" ),
				rs.getLong( "atime" ),
				rs.getString( "host" ), 
				rs.getString( "service" ),
				rs.getString( "user" ),
				rs.getString( "pass" ) 
			);
			 
			clients.put( 
				rs.getString( "jid" ),
				new DSClientBot( c )	
			);
		}
		
		return clients;
	}
	
	//////////////////////////////////

	public int fetchUpdateTotal( String jid, String namespace )
	throws SQLException {		
		Statement stmt = createStatement();
		String query = "SELECT COUNT(1) total FROM " + DEFAULT_SYS_DB + "." + 
			UPDATES_TABLE + " WHERE jid='" + jid + "' ";
		if ( namespace != null )
			query += "AND namespace = '" + namespace + "'"; 
		ResultSet rs = stmt.executeQuery( query );
		
		if ( rs.next() ) {
			return rs.getInt( "total" );
		}
		else {
			return 0;
		}
	}
	
	//////////////////////////////////

	public int fetchUpdateTotal( String jid )
	throws SQLException {		
		return fetchUpdateTotal( jid, null );
	}
	
	//////////////////////////////////

	/**
	 * 
	 * @return
	 * @throws SQLException 
	 * @throws JSONException 
	 */
	public ArrayList< DSUpdate > fetchUpdates( String jid, String namespace ) 
	throws SQLException, JSONException {
		return fetchUpdates( jid, namespace, null, null );
	}
	
	//////////////////////////////////

	/**
	 * 
	 * @return
	 * @throws SQLException 
	 * @throws JSONException 
	 */
	public ArrayList< DSUpdate > fetchUpdates( String jid, String namespace, Integer limit, Integer offset  ) 
	throws SQLException, JSONException {
		
		ArrayList< DSUpdate > updates = new ArrayList< DSUpdate >();
				
		Statement stmt = createStatement();
		String query = "";
		query += "SELECT * FROM " + DEFAULT_SYS_DB + "." + UPDATES_TABLE + " "; 
		query += "WHERE jid='" + jid + "' ";
		if ( namespace != null ) 
			query += " AND namespace='" + namespace + "' ";
		query += "ORDER BY ctime DESC ";

		if ( limit != null ) query += " LIMIT " + limit;
		if ( offset != null ) query += " OFFSET " + offset;
		
		ResultSet rs = stmt.executeQuery( query );
		
		while ( rs.next() ) {
			
			DSUpdate u;
			try {
				u = new DSUpdate( 
					rs.getString( "namespace" ),
					rs.getString( "primaryTag" ),
					rs.getString( "crud" )
				)
				.setDescription( rs.getString( "description" ) )
				.setTotal( rs.getLong( "total" ) )
				.setCtime( rs.getLong( "ctime" ) )
				.setFtime( rs.getLong( "ftime" ) )
				.setRtime( rs.getLong( "rtime" ) )
				.setSid( rs.getString( "sid" ) );
				
				try {
					u.setTags( new JSONArray ( rs.getString( "tags" ) ) );
				} catch ( Exception e ) {}
				
				try {
					u.setMeta( new JSONObject( rs.getString( "meta" ) ) );
				} catch ( Exception e ) {}
				
				updates.add( u );
				
			} catch ( DSFormatException e ) {
				e.printStackTrace();
			}
		}
		
		return updates;
	}

	//////////////////////////////////

	/**
	 * 
	 * @return
	 * @throws SQLException 
	 */
	public DSClient fetchClient( String jid ) 
	throws SQLException {
		
		Statement stmt = createStatement();
		String query = "SELECT * FROM " + DEFAULT_SYS_DB + "." + 
			USERS_TABLE + " " +	"WHERE jid='" + jid + "'";
		ResultSet rs = stmt.executeQuery( query );
		
		if ( rs.next() ) {
			
			return new DSClient( 
				rs.getString( "jid" ),
				rs.getString( "firstname" ),
				rs.getString( "lastname" ),
				rs.getString( "email" ),
				rs.getLong( "ctime" ),
				rs.getLong( "atime" ),
				rs.getString( "host" ), 
				rs.getString( "service" ),
				rs.getString( "user" ),
				rs.getString( "pass" ) );
		}
		
		return null;
	}
	
	//////////////////////////////////
	
	public void insertUpdate( String jid, String from, DSUpdate d ) {

		try {
			Statement stmt = createStatement();
			
			String insert = "insert into " + DEFAULT_SYS_DB + "." + UPDATES_TABLE + " " +
				"( jid, namespace, primaryTag, description, crud, total, ctime, ftime, rtime, meta, tags, loc, sid ) " +
				"values ( " +
				"'" + jid + "'," +
				"'" + d.getNamespace() + "'," +
				"'" + d.getPrimaryTag() + "'," +
				"'" + d.getDescription().replace( "\'", "\\\'" ) + "'," +
				"'" + d.getCrud() + "'," +
				""  + d.getTotal() + "," +
				""  + d.getCtime() + "," +
				""  + d.getFtime() + "," +
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
			logger.severe( "--- DSDataManager: [" + jid + "] UPDATE FAILURE: " + e.getMessage() );
		}
	}

	//////////////////////////////////
	
	public String getSubscriptionStatus( String ns, String jid ) {

		String subscriptionStatus = null;
		try {
			Statement stmt = createStatement();
			String query = "SELECT subscriptionStatus FROM " 
				+ SUBSCRIPTIONS_TABLE + " WHERE namespace='" + ns + "' AND jid='" + jid + "'";

			ResultSet rs = stmt.executeQuery( query );
			
			if ( rs.next() ) 
				subscriptionStatus = rs.getString( "subscriptionStatus" );

		} catch ( SQLException e ) {
			
			logger.log( Level.SEVERE, "+++ DSDataManager: SQL Meltdown in getSubscriptionStatus - ", e.getMessage() );
		}
		
		return subscriptionStatus; 
	}
	
	//////////////////////////////////
	
	public void setSubStatus( 
		String ns,
		String jid, 
		Status status ) 
	{
		try {
			Statement stmt = createStatement();
			String query = 
				"UPDATE " + SUBSCRIPTIONS_TABLE + " " + 
				"SET subscriptionStatus='" + status + "' " +
				"WHERE namespace='" + ns + "' AND jid='" + jid + "'";
			
			stmt.executeUpdate( query );
			logger.finer( "--- DSDataManager: [" + jid + "] Changed Subscription \"" + ns + "\" to " + status );
			
		} catch ( SQLException e ) {
			logger.log( Level.SEVERE, "+++ DSDataManager: SQL Meltdown in setSubStatus - ", e.getMessage() );
		} 
	}
	
	//////////////////////////////////
	
	public void setSubscriptionStatusFromSid( 
		String jid, 
		String sid, 
		Status status ) 
	{
		try {
			Statement stmt = createStatement();
			String query = 
				"UPDATE " + SUBSCRIPTIONS_TABLE + " " + 
				"SET subscriptionStatus='" + status + "' " +
				"WHERE sid='" + sid + "' AND jid='" + jid + "'";
			
			stmt.executeUpdate( query );

		} catch ( SQLException e ) {
			logger.log( Level.SEVERE, "+++ DSDataManager: SQL Meltdown in setSubscriptionStatus- ", e.getMessage() );
		} 
	}
	
	//////////////////////////////////
	
	public void insertSubscription( 
		String namespace, 
		String jid, 
		Status status
	) throws SQLException, DSFormatException {		
		insertSubscription( namespace, jid, status, null );
	}

	//////////////////////////////////
	
	public void insertSubscription( 
		String namespace, 
		String jid, 
		Status status,
		String sid ) 
	throws SQLException, DSFormatException {
		
		//-- we require that the namespace is non-null
		if ( namespace == null )
			throw new DSFormatException();

		//-- if the source doesn't exist in our records fetch
		//-- its information (capabilities, icons, etc)
		DSSource s = DSCatalog.db.fetchSource( namespace );
		
		if ( s == null ) {
			logger.fine( "--- DSDataManager: new source identified - attempting to add \"" + namespace + "\" to registry.");
			registerSource( namespace );
		}
		else 
			logger.fine( "--- DSDataManager: source recognized.");
		
		//-- that done insert the subscription proper
		try {
			Statement stmt = createStatement();
			String query = 
				"INSERT INTO " + DEFAULT_SYS_DB + "." + SUBSCRIPTIONS_TABLE + " VALUES (" + 
				"'" + namespace + "'," + 
				"'" + jid + "'," + 
				"'" + status + "',";
		
			query += ( sid == null ) 
					 ? "null," 
					 : "'" + sid + "',"; 
			
			query +=
				System.currentTimeMillis() + "," +
				System.currentTimeMillis() + ")";
			
			stmt.executeUpdate( query );

		} catch ( SQLException e ) {
			logger.log( Level.SEVERE, "+++ DSDataManager: SQL Meltdown in insertSubscription - ", e.getMessage() );
		} 
		
		logger.fine( "--- DSDataManager: [" + jid + "] new subscription added for " + namespace);
	}
	
	//////////////////////////////////
	
	public void registerSource( String namespace ) 
	throws DSFormatException {
		
		if ( namespace == null )
			throw new DSFormatException();
		
		//-- TODO: fetch the source information 
		//-- TODO: unpack it 
		//-- TODO:  insert it into the namespaces
		insertSource( namespace, null, null, null );
	}

	//////////////////////////////////
	
	public void insertSource( 
		String namespace,
		String commonName,
		String url,
		String icon ) 
	throws DSFormatException {
		
		if ( namespace == null )
			throw new DSFormatException();
		
		commonName = ( commonName == null ) ? namespace : commonName;
		url = ( url == null ) ? DEFAULT_SOURCE_URL : url;
		icon = ( icon == null ) ? DEFAULT_SOURCE_ICON : icon;
		
		try {
			Statement stmt = createStatement();
			String query = 
				"INSERT INTO " + DEFAULT_SYS_DB + "." + SOURCES_TABLE + " VALUES (" + 
				"'" + namespace + "'," + 
				"'" + commonName + "'," + 
				"'" + url + "'," + 
				"'" + icon + "')";

			stmt.executeUpdate( query );

		} catch ( SQLException e ) {
			logger.log( Level.SEVERE, "+++ DSDataManager: SQL Meltdown in insertSource - ", e );
		} 
	}
	
	//////////////////////////////////
	
	public DSSub fetchSub( 
		String jid, 
		String ns
		)
	throws SQLException {
		
		Statement stmt = createStatement();
		String query = 
			"SELECT * FROM " + DEFAULT_SYS_DB + "." + SUBSCRIPTIONS_TABLE + " " +
			"WHERE jid='" + jid + "' " +
			"AND namespace='" + ns + "'";
		
		ResultSet rs = stmt.executeQuery( query );
				
		if ( rs.next() ) {
			
			return new DSSub( 
				rs.getString( "sid" ),
				rs.getString( "jid" ),
				rs.getString( "subscriptionStatus" ),
				rs.getLong( "ctime" ),
				rs.getLong( "mtime" ),
				rs.getString( "namespace" )
			);
		}
		
		return null;
	}

	//////////////////////////////////
	

	public ArrayList< DSSource > fetchSources( 
			String jid, 
			DSSub.Status ... statuses ) 
	throws SQLException {
		
		Statement stmt = createStatement();
		String query = 
			"SELECT * FROM " + 
			DEFAULT_SYS_DB + "." + SUBSCRIPTIONS_TABLE + " b, " +
			DEFAULT_SYS_DB + "." + SOURCES_TABLE + " s " +
			"WHERE b.jid='" + jid + "' " +
			"AND b.namespace=s.namespace ";

		if ( statuses.length > 0 ) {
			String str = "";
			for ( DSSub.Status status : statuses )  
				str += "'" + status.toString() + "',";
			str = str.substring( 0, str.length() - 1 );
			query += "AND b.subscriptionStatus IN (" + str + ")";
		}
		
		ArrayList< DSSource > sources = new ArrayList< DSSource >();
		ResultSet rs = stmt.executeQuery( query );
		
		while ( rs.next() ) {
			sources.add( 
				new DSSource( 
					rs.getString( "namespace" ),
					rs.getString( "commonName" ),
					rs.getString( "url" ),
					rs.getString( "icon" )
				)
			);
		}
		
		return sources;
	}
	
	//////////////////////////////////
	
	public DSSource fetchSource( String namespace )
	throws SQLException {

		Statement stmt = createStatement();
		String query = 
			"SELECT * FROM " + DEFAULT_SYS_DB + "." + SOURCES_TABLE + " " +
			"WHERE namespace='" + namespace + "'";
		ResultSet rs = stmt.executeQuery( query );

		if ( rs.next() ) {
			
			return new DSSource( 
				rs.getString( "namespace" ),
				rs.getString( "commonName" ),
				rs.getString( "url" ),
				rs.getString( "icon" )
			);
		}

		return null;
	}

	//////////////////////////////////
	
	public String fetchNamespace( String sid ) 
	throws SQLException {
		
		Statement stmt = createStatement();
		String query = 
			"SELECT namespace FROM " + DEFAULT_SYS_DB + "." + SUBSCRIPTIONS_TABLE + " " +
			"WHERE sid='" + sid + "'";
		ResultSet rs = stmt.executeQuery( query );
		return 	( rs.next() ) ? rs.getString( "namespace" ) :  null;
	}



}
