package datasphere.catalog.http;

import java.sql.SQLException;
import java.util.EnumSet;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.restlet.data.CharacterSet;
import org.restlet.data.Form;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Get;

import datasphere.catalog.DSCatalog;
import datasphere.catalog.DSClient;
import datasphere.catalog.DSSub;
import datasphere.catalog.xmpp.DSChatServer;
import datasphere.catalog.xmpp.DSClientBot;
import datasphere.dataware.DSFormatException;

public class SetSourceSubscription 
extends DSServerResource { 
	
	private static Logger logger = Logger.getLogger( DSCatalog.class.getName() );
	
	private enum Action {
		RESET, ACCEPT, REJECT, RETRY, UNSUBSCRIBE;
		
		public static Action match( String action ) {
			for( Action a : EnumSet.allOf( Action.class ) ) 
				if ( action.equalsIgnoreCase( a.toString() ) )
					return a;
			return null;
		}
	}

	private DSClient user;  
	private String ns;
	private Action action;
	private DSSub sub;
	
	///////////////////////////////////
	
	public JsonRepresentation setResult( boolean result, String message ) {
		
		logger.fine( "--- SetSourceSubscription: [" +user.getJid() + "] " +
				"Manual subscription acceptance for \"" + ns + "\" " + 
				( result ? "successful" : "failed" ) + 
				" (" + message + ")" );
		try {
			JSONObject json = new JSONObject();
			json.put( "success", result );
			json.put( "message", message );
			JsonRepresentation jr = new JsonRepresentation( json ) ;
			jr.setCharacterSet( CharacterSet.UTF_8 );
			return jr;
			
		} catch ( Exception e ) {
			return null;
		}
	}
	
	///////////////////////////////////
	
	@Get()
	public JsonRepresentation doGet() {
		
		try {
			//-- fetch client information
			Form form = getRequest().getResourceRef().getQueryAsForm();
			this.user = DSCatalog.db.fetchClient( form.getFirstValue( "jid" ) );
			this.ns = form.getFirstValue( "ns" );
			this.action = Action.match( form.getFirstValue( "action" ) );
			this.sub = DSCatalog.db.fetchSub( user.getJid(), ns );
			
			//-- check we have everything we need
			assertInvariance();
			
			if ( action == Action.ACCEPT ) {
				
				//-- if we have no xmmp connection we have to fail
				DSClientBot client = DSChatServer.getClient( user.getJid() );
				
				if ( client == null ) 
					return setResult( false, "lack of xmmp connection prevented completion" );
			
				//-- log the subscription as accepted in the database
				DSCatalog.db.setSubStatus( ns, user.getJid(), DSSub.Status.ACCEPTED );

				//-- notify the xmmp server so it can update its roster
				client.acceptSubscription( sub.getSid() );
				
				//-- return a sucessful response
				return setResult( true, "subscription has been accepted!" );
			}

		} 
		catch ( SQLException e ) {
			return setResult( false, "database error prevent completion." );
		}
		catch ( DSFormatException e ) {
			return setResult( false, "incorrect parameters or null subscription." );
		} 
		
		return setResult( false, "unknown cause of failure." );
	}

	//////////////////////////////////

	private void assertInvariance()
	throws DSFormatException {
		if ( user == null || ns == null || action == null || sub == null )
			throw new DSFormatException();
	}
}