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
import datasphere.catalog.xmpp.DSChatServer;
import datasphere.catalog.xmpp.DSClientBot;
import datasphere.dataware.DSException;
import datasphere.dataware.DSFormatException;

public class SetSourceSubscription 
extends DSServerResource { 
	
	private static Logger logger = Logger.getLogger( DSCatalog.class.getName() );
	
	private enum Action {
		RESET, ACCEPT, REJECT, RETRY, EXPECT;
		
		public static Action match( String action ) {
			for( Action a : EnumSet.allOf( Action.class ) ) 
				if ( action.equalsIgnoreCase( a.toString() ) )
					return a;
			return null;
		}
	}

	private DSClient user;  
	private String ns;
	private String sid;
	private Action action;
	
	///////////////////////////////////
	
	@Get()
	public JsonRepresentation doGet() {
		
		try {
			//-- fetch client information
			Form form = getRequest().getResourceRef().getQueryAsForm();
			this.user = DSCatalog.db.fetchClient( form.getFirstValue( "jid" ) );
			this.ns = form.getFirstValue( "ns" );
			this.sid = form.getFirstValue( "sid" );
			this.action = Action.match( form.getFirstValue( "action" ) );
			
			
			//-- check we have everything we need
			assertInvariance();

			//-- if we have no xmmp connection we have to fail
			DSClientBot client = DSChatServer.getClient( user.getJid() );
			if ( client == null ) 
				return setResult( false, "lack of xmmp connection prevented completion." );

			//-- accept the specified subscription
			if ( action == Action.ACCEPT ) {
				client.acceptSubscription( sid );
				return setResult( true, "subscription has been accepted." );
			}
			
			//-- reset (remove) the specified subscription
			else if ( action == Action.RESET ) {
				client.resetSubscription( sid );
				return setResult( true, "subscription has been reset." );
			}
			
			//-- reset (remove) the specified subscription
			else if ( action == Action.REJECT ) {
				client.rejectSubscription( sid );
				return setResult( true, "subscription has been rejected." );
			}
			
			//-- set the specified subscription to be auto accepted
			else if ( action == Action.EXPECT ) {
				client.expectSubscription( sid );
				return setResult( true, "subscription has been set as expected." );
			}
			
			//-- reset (remove) the specified subscription
			else if ( action == Action.RETRY ) {
				client.acceptSubscription( sid );
				return setResult( true, "subscription acceptance has been resent" );
			}
		} 
		catch ( SQLException e ) {
			return setResult( false, "database error prevented completion." );
		}
		catch ( DSException e ) {
			return setResult( false, "incorrect parameters or null subscription." );
		} 
		
		return setResult( false, "unknown cause of failure." );
	}


	///////////////////////////////////
	
	public JsonRepresentation setResult( boolean result, String message ) {
		
		logger.fine( "--- SetSourceSubscription: [" +user.getJid() + "] " +
				"subscription " + action + " for \"" + ns + "\" " + 
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
	
	//////////////////////////////////

	private void assertInvariance()
	throws DSFormatException {

		if ( user == null || sid == null || ns == null || action == null )
			throw new DSFormatException();
	}
}