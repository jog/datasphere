package datasphere.catalog.xmpp;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import datasphere.catalog.DSCatalog;
import datasphere.dataware.DSException;

public class DSChatServer {

	private static Logger logger = Logger.getLogger( DSCatalog.class.getName() );
	private Integer serverPort;
	public final static int DEFAULT_SERVER_PORT = 5222;
	private static Map< String, DSClientBot > clients = new HashMap< String, DSClientBot >();
	
	///////////////////////////////
	
	public DSChatServer() {}
	
	///////////////////////////////

	public DSChatServer( Integer serverPort ) {
		this.serverPort = 
			( serverPort == null ) 
			? DEFAULT_SERVER_PORT
			: serverPort;
	}
	
	///////////////////////////////
	
	/**
	 * @throws DSException 
	 */
	public void start() 
	throws DSException {
		initialize();
		setupClients();
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
		logger.info( "--- DSChatServer: Starting the internal XMPP server on port " + this.serverPort + "... [SUCCESS]" );
	}
	
	
	///////////////////////////////
	
	/**
	 * 
	 * @throws DSException 
	 * @throws DSException
	 */
	private void setupClients() 
	throws DSException {
		
		try {
			clients = DSCatalog.db.fetchClientBots();
			
			logger.info( "--- DSChatServer: Creating XMPP bots for " + clients.size() + " clients..." );
			int connects = 0;
			for( Entry< String, DSClientBot > e : clients.entrySet() ) {
				if ( e.getValue().connect() )
					connects++;
			}
			logger.fine( "--- DSChatServer: " + connects + " XMPP bots have been connected... [SUCCESS]" );
		}
		catch ( SQLException e ) {
			logger.info( "--- DSChatServer: Attempting to retrieve data for bot creation... [FAILED]" );
			throw new DSException( e );
		}
	}
	
	///////////////////////////////
	
	/**
	 * 
	 */
	public static DSClientBot getClient( String sid ) {
		return clients.get( sid );
	}
}
