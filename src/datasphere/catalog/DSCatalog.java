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
import java.util.Properties;
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

import datasphere.catalog.http.DSWebServer;
import datasphere.catalog.xmpp.DSChatServer;
import datasphere.dataware.DSException;
import datasphere.dataware.DSLogFormatter;


public final class DSCatalog {

	public static final int XMPP = 0;
	public static final int HTTP = 1;
	public static final int ALL_PROTOCOLS = 2;
	
	private static Logger logger = Logger.getLogger( DSCatalog.class.getName() );
	private static Handler handler = new ConsoleHandler();   
	
	public static DSDataManager db = null;
	private Properties config = null;
	
	private Integer httpPort = null;
	private Integer xmppPort = null;
	
	private boolean httpStartable = true;
    private boolean xmppStartable = true;
    
    private DSWebServer httpServer = null;
    private DSChatServer xmppServer = null;
	
    private boolean systemWipe = false;
    private boolean systemCreate = false;

    ///////////////////////////////
	
	/**
	 * @param databaseManager The database object which manages persistence for the game.
	 */
	public DSCatalog( DSDataManager databaseManager ) {
	    db = databaseManager;
	    setupLogging();
	    setupConfiguration();
	}
	
	///////////////////////////////
	
	/**
	 * setup log formatting
	 */
	public void setupLogging() {

	    handler.setFormatter( new DSLogFormatter() );
	    handler.setLevel( Level.FINER );
	    logger.addHandler( handler );
	    logger.setUseParentHandlers( false );
	    logger.setLevel( Level.FINER );
	}
	
	///////////////////////////////
	
	/**
	 * load config file for the server
	 */
	public void setupConfiguration() {   
		
		config = new Properties();
		InputStream configStream = getClass()
			 .getClassLoader()
			.getResourceAsStream( "datasphere/catalog/ds.cfg" );
			
	    //-- if it exists extract the properties contained within		
		
		if ( configStream != null ) {
			
			try {
				config.load( configStream );
		 		configStream.close();	
	            logger.config( "--- DSCatalog: loading configuration file... [SUCCESS]"); 		
	            
			} catch ( IOException e ) {
				logger.config( "--- DSCatalog: loading configuration file... [FAILED]");
				logger.warning( "--- DSCatalog: config file has invalid syntax. continuing with defaults." ); 		
		
			}
		} else 
			logger.config( "no configuration file detected. continuing with defaults... [SUCCESS]");
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
		
		//-- check that the server has been supplied with a persistence layer
		if ( db == null ) {
			logger.severe( "--- DSCatalog: Checking persistence layer... [FAILED]" );
			throw new DSException("Insufficient parameters - " +
				"A DSDatabaseManager object must be supplied" );
		}
				
		//-- check that the database exists
		logger.info( "--- DSCatalog: Establishing database connection and consistency..." );
		db.connect();
		
		//-- attempt to clear the system tables if requested
		if ( systemWipe == true ) 
			db.clearSystemTables();
		
		//-- attempt system table creation if requested
		if  ( systemCreate == true ) 
			db.createSystemTables();
		
		//-- check to see that table integrity is ok 
		db.checkSystemTables();

		//-- start the servers up and running
		logger.info( "--- DSCatalog: Attempting to start server components..." );
		startHTTP();
		startXMPP();
		
		//-- announce success
		if ( xmppServer != null && httpServer != null ) { 
			logger.info( "Datasphere setup and ready for FULL service..." );
		}
		else if ( xmppServer != null || httpServer != null ) {
			logger.info( "Datasphere setup and ready for \"partial\" service..." );
		}
		else if ( xmppServer != null || httpServer != null ) {
			logger.info( "Datasphere setup failed. No service is available." );
		}
	}
	
	///////////////////////////////
	
	/**
	 * create an instance of the http server
	 * @throws DSException
	 */
	public void startHTTP() 
	throws DSException {

		if ( httpStartable ) {
			try {
				httpServer = new DSWebServer( httpPort );
				httpServer.start();
				logger.info( "--- DSCatalog: HTTP server setup...[SUCCESS]" );
				
			} catch( Exception e ) {
				logger.severe( "--- DSCatalog: HTTP server setup...[FAILED]" );
				e.printStackTrace();
				httpServer = null;
			}
		}
	}
	
	///////////////////////////////
	
	/**
	 * create an instance of the xmpp server
	 * @throws DSException
	 */
	public void startXMPP() 
	throws DSException {

		if ( xmppStartable ) {
			try {
				xmppServer = new DSChatServer( xmppPort );
				xmppServer.start();
				logger.info( "--- DSCatalog: XMPP server setup...[SUCCESS]" );
				
			} catch ( DSException e ) {
				logger.severe( "--- DSCatalog: XMPP server setup...[FAILED]" );
				e.printStackTrace();
				xmppServer = null;
			}

		}
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
	 * 
	 */
	public void setStartable( int protocol ) {

		if ( protocol == XMPP ) {
			xmppStartable = true;
			httpStartable = false;
		}
		else if ( protocol == HTTP ) {
			xmppStartable = false;
			httpStartable = true;
		}
		else if ( protocol == ALL_PROTOCOLS ) {
			xmppStartable = true;
			httpStartable = true;
		}
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
			options.addOption( "x", "xmpp", true, "specify the port the xmpp server listens on. default is 5222");
			options.addOption( "t", "http", true, "specify the port the http server listens on. default is 80");
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
				formatter.printHelp( "DSCatalog.jar", options );
				xmppStartable = false;
				httpStartable = false;
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
					throw new ParseException( "Invalid password specified");
				}
			}
			
			//-- determine if an xmpp port number is being specified
			if ( cmd.hasOption( "xmpp" ) ) {
				try {
					xmppPort = Integer.parseInt( cmd.getOptionValue( "x" ) );
				}
				catch ( NumberFormatException e ) {
					throw new ParseException( "Invalid XMPP Port Number specified");
				}
			}
			
			
			//-- determine if an xmpp port number is being specified
			if ( cmd.hasOption( "http" ) ) {
				try {
					httpPort = Integer.parseInt( cmd.getOptionValue( "t" ) );
				}
				catch ( NumberFormatException e ) {
					throw new ParseException( "Invalid HTTP Port Number specified");
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
				
				xmppStartable = false;
				httpStartable = false;
			}
			
		} catch ( ParseException e ) {
			
			throw new DSException( 
				e.getMessage() + "\n" +
				"Try '--help' for more information" );
		}
	}
}
