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

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import datasphere.dataware.DSException;
import datasphere.dataware.DSLogFormatter;


public final class DSCatalog {

	
	private static Logger logger = Logger.getLogger( DSCatalog.class.getName() );
	private static Handler handler = new ConsoleHandler();   
	public static DSDatabaseManager db = null;
	
	private Properties config = null;
	private Integer serverPort = null;    
    private boolean systemWipe = false;
    private boolean systemCreate = false;
    private boolean startable = true;
    
    private Map< String, DSClientBot > clients = 
    	new HashMap< String, DSClientBot >();

	
	private int DEFAULT_SERVER_PORT = 5222;
	
	///////////////////////////////
	
	/**
	 * @param port The port that the server will be listening on (default is 7474).
	 * @param databaseManager The database object which manages persistence for the game.
	 * @exception BadPortAddress thrown if the arguments are of an incorrect format.		 
	 */
	public DSCatalog( 
		Integer port, 
		DSDatabaseManager databaseManager ) 
	{
		//-- setup log formatting
	    handler.setFormatter( new DSLogFormatter() );
	    handler.setLevel( Level.FINER );
	    logger.addHandler( handler );
	    logger.setUseParentHandlers( false );
	    logger.setLevel( Level.ALL );
	    
	    //-- initialization
	    db = databaseManager;
	    this.serverPort = port;
	    
	    /*
	    //-- load config file for the server
		config = new Properties();
		InputStream configStream = getClass()
								   .getClassLoader()
								   .getResourceAsStream( "ramp/ramp.cfg" );
			
	    //-- if it exists extract the properties contained within		
		if ( configStream != null ) {
			try {
				config.load( configStream );
		 		configStream.close();	
	            RAMPMain.logger.config( "RAMP Config File - successfully loaded"); 		 		
			} catch ( IOException e ) {
				RAMPMain.logger.warning( "RAMP Config File - failure: INVALID SYNTAX" ); 					
			}
		} else {
			RAMPMain.logger.warning( "RAMP Config File - failure: MISSING FILE" );
		}*/
	}
	
	///////////////////////////////
	
	/**
	 * The server will do nothing until start() is called, at which point it will
	 * initialize system resources and initialise connections to necessary XMPP servers
	 * N.b. This method will block indefinitely unless a <i>non-startable</i> command 
	 * line argument has been supplied (such as -help), in which case it will return false. 
	 * N.b. that an exception will be thrown if sufficient parameters have not been 
	 * supplied either via the constructor or {@link #setArgs(String[])}.
	 * @exception DSException thrown if the specified port number is illegal or
	 * if the ruleset or a database manager has not been supplied.		 
	 */
	public void start() 
	throws DSException {
		
		if ( startable ) {
		
			try {
				initialize();
				setupConnections();
				
				//-- main loop
				logger.info( "Datasphere setup and ready for service..." );
				for(;;);
				
			} catch ( DSException e ) {
				logger.severe( "Datasphere System Initialization... [FAILED]" );
				throw e;
			}
		}
	}

	///////////////////////////////
	
	/**
	 * 
	 * @throws DSException 
	 * @throws DSException
	 */
	private void setupConnections() 
	throws DSException {
		
		try {
			clients = db.fetchClients();
			
			logger.info( "Attempting to create bots for " + clients.size() + " clients..." );
			for( Entry< String, DSClientBot > e : clients.entrySet() ) {
				e.getValue().connect();
			}
		}
		catch ( SQLException e ) {
			logger.info( "Attempting to retrieve data for bot creation... [FAILED]" );
			throw new DSException( e );
		}
	}
	
	///////////////////////////////
	
	/**
	 * 
	 * @throws DSException
	 */
	private void initialize() 
	throws DSException {
		
		//-- if no port has been supplied use the default
		if ( this.serverPort == null ) 	
			this.serverPort = DEFAULT_SERVER_PORT;
		DSClientBot.setPort( serverPort );
		logger.info( "Setting Datasphere to communicate on port " + this.serverPort + "... [SUCCESS]" );
		
		//-- check that the server has been supplied with a persistence layer
		if ( db == null ) {
			logger.severe( "Checking persistence layer... [FAILED]" );
			throw new DSException("Insufficient parameters - " +
				"A DSDatabaseManager object must be supplied" );
		}
				
		//-- check that the database exists
		logger.info( "Attempting to establish database connection..." );
		db.connect();
		
		//-- attempt to clear the system tables if requested
		if ( systemWipe == true ) db.clearSystemTables();
		
		//-- attempt system table creation if requested
		if  ( systemCreate == true ) {
			db.createSystemTables();
		} 
		
		//-- check to see that table integrity is ok 
		db.checkSystemTables();
	}

	///////////////////////////////
	
	/**
	 * The Catalog class also handles management of the datasphere server's logging 
	 * facilities - as such the logging level can be set here (the level's
	 * are as specified in java.util.logging).
	 * @param level The minimum level for which log messages will be displayed. 
	 */
	public void setLoggingLevel( Level level ) {
		
		handler.setLevel( level );
	    logger.setLevel( level );
	}
	
	
	///////////////////////////////
	
	
	/**
	 * Allows command line arguments to be passed into the framework - any parameters 
	 * supplied in this fashion will override any that were previously being held.
	 * @param args	The command line arguments supplied on running of the server instance.
	 * @exception DSException Thrown if the supplied port number is illegal or already in use.		 
	 */
	public void setArgs( String[] args )
	throws DSException {
		
		try {
			//-- create Options object
			Options options = new Options();
			options.addOption( "w", "wipe", false, "clears the system databases on startup" );
			options.addOption( "d", "debug", false, "pulls up a debugger window for each XMPP connection" );
			options.addOption( "t", "port", true, "specify the port the server listens on");
			options.addOption( "p", "password", true, "specify the admin password for the system");
			options.addOption( "l", "log-level", true, "specify the log level (0-1000) to display");
			options.addOption( "c", "create", false, "Automatically generates required system tables");			
			options.addOption( "h", "help", false, "prints this message");
			options.addOption( "v", "version", false, "returns version information");
			options.addOption( "vf", "version-full", false, "returns full version, build and authorship information.");	
			
			CommandLineParser parser = new PosixParser();
			CommandLine cmd = parser.parse( options, args );
			
			//-- automatically generate the help statement
			if ( cmd.hasOption( "help" ) ) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "hyperplace.server", options );
				startable = false;
			}
			
			//-- determine if system databases should be cleaned
			if ( cmd.hasOption( "wipe" ) ) {
				systemWipe = true;
			}

			//-- determine if user is attempting to create the system database
			if ( cmd.hasOption( "create" ) ) {
				systemCreate = true;
			}
			
			//-- determine if user is attempting to create the system database
			//if ( cmd.hasOption( "debug" ) ) {
				XMPPConnection.DEBUG_ENABLED = true;
			//}
			
			//-- check the validity of any log-level supplied
			if ( cmd.hasOption( "log-level" ) ) {
				
				try {
					setLoggingLevel( 
						Level.parse( 
							cmd.getOptionValue( "log-level" )
							.toUpperCase() 
						)
					);	
				}
				catch ( IllegalArgumentException e ) {
					throw new ParseException( "Logging error: " + e.getMessage() );
				}
			}
			
			//-- determine if a port number is being specified
			if ( cmd.hasOption( "password" ) ) {
				try {
					String password = cmd.getOptionValue( "p" );
					db.setPassword( password );
				}
				catch ( NumberFormatException e ) {
					throw new ParseException( "Invalid Port Number specified");
				}
			}
			
			//-- determine if a port number is being specified
			if ( cmd.hasOption( "port" ) ) {
				try {
					serverPort = Integer.parseInt( cmd.getOptionValue( "t" ) );
				}
				catch ( NumberFormatException e ) {
					throw new ParseException( "Invalid Port Number specified");
				}
			}
			
			//-- determine if version information is being asked for
			if ( cmd.hasOption( "version" ) || cmd.hasOption( "version-full" ) ) {
				
				//-- load config file for the server
				Properties v = new Properties();
				InputStream is = getClass().
								 getClassLoader().
								 getResourceAsStream( "version.info" );
				
				//-- if it exists extract the properties contained within		
				if ( is != null ) {
					try {
						v.load( is );
						is.close();	
						System.out.println( "version:  datasphere.catalog " + v.getProperty("version") );
						if ( cmd.hasOption( "version-full" ) ) {
							System.out.println("compiler: " + v.getProperty("compiled-by") );
							System.out.println("built:    " + v.getProperty("build-time") + "");
							System.out.println("Java: 	  " + v.getProperty("java-version") );
						}
					} catch ( IOException e ) {
						System.out.println( "version: information cannot be loaded" );						 					
					}
				} else {
					System.out.println( "version: information cannot be found" );
				}
				
				startable = false;
			}
			
		} catch ( ParseException e ) {
			
			throw new DSException( 
				e.getMessage() + "\n" +
				"Try '--help' for more information" );
		}
	}
	
	
	
	///////////////////////////////
	
	/**
	 * @param args 
	 * @throws XMPPException 
	 * @throws IOException 
	 * @throws SQLException 
	 */
	public static void main( String[] args ) 
	throws XMPPException, IOException, DSException, SQLException {

		//-- create a database connection
		DSDatabaseManager dbm = new DSDatabaseManager( 
			"jdbc:mysql://127.0.0.1:3306/datasphere", 
			new com.mysql.jdbc.Driver() );
		
		
		//-- create an instance of the catalog
		DSCatalog c = new DSCatalog( 5222, dbm );
		c.setArgs( args );
		
		//-- and initialize the catalog if required
		c.start();
		
	}

}
