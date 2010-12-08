package datasphere.catalog.xmpp;

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

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;

import datasphere.catalog.DSCatalog;
import datasphere.catalog.DSClient;
import datasphere.catalog.DSDataManager;
import datasphere.catalog.DSSub;
import datasphere.dataware.DSException;
import datasphere.dataware.DSFormatException;

public class DSClientBot {

	private static final Logger logger = Logger.getLogger( 
			DSCatalog.class.getName() );
	
	private static int port = 5222;
	private static String resource = "datasphere";
	
	private DSClient clientJID = null;
	XMPPConnection connection = null; 

	///////////////////////////////
	
	public DSClientBot( DSClient clientJID ) {
     	this.clientJID = clientJID;
	}
	
	///////////////////////////////
	
	public boolean connect() {

		try {
	        
			ConnectionConfiguration connConfig = new ConnectionConfiguration( 
    		clientJID.getHost(), port, clientJID.getService() );
    		connection = new XMPPConnection( connConfig );
    		connection.connect();
    		connection.login( clientJID.getUser(), clientJID.getPassword(), resource );
      		
			//-- register that we manually subscribe to subscription requests
    		Roster.setDefaultSubscriptionMode( Roster.SubscriptionMode.manual );
    		Roster roster = connection.getRoster();
			roster.setSubscriptionMode( Roster.SubscriptionMode.manual );
	        
			//-- registers that we can accept dataware iq communications
			ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor( connection );
			sdm.addFeature( "jabber:iq:version" );
			
			//-- for some reason smack doesn't seem to support nickname extensions!?
			ProviderManager pm = ProviderManager.getInstance();
			pm.addExtensionProvider( 
	        	"nick", 
	        	"http://jabber.org/protocol/nick", 
	        	new DSNickname.Provider()
	        );
	      
			//-- registers update listener (for dataware update messages)
	        connection.addPacketListener( 
	            	new DSUpdateListener( this ), 
	            	new PacketTypeFilter( Message.class ) );
	         
	        //-- registers iq listener (for dataware requests).
	        connection.addPacketListener( 
	            	new DSPacketListener( this ), 
	            	new PacketTypeFilter( IQ.class ) );
	    	
	        //-- registers a precense listener (for dataware invites) 
	        connection.addPacketListener( 
	            	new DSPresenceListener( this ), 
	            	new PacketTypeFilter( Presence.class ) );
			
	        logger.fine( "--- DSClientBot: connecting bot for client [" + clientJID.getJid() + "]... [SUCCESS]" );
			
	        return true;
			
		} catch ( XMPPException e ) {
			logger.severe( "--- DSClientBot: connecting bot for client [" + clientJID.getJid() + "]... [FAILED]" );
			return false;
		}
    }

	///////////////////////////////
	
	public static void setPort( Integer serverPort ) {
		port = serverPort;		
	}
	
	///////////////////////////////
	
	public void sendPacket( Packet p ) {
		connection.sendPacket( p );
	}

	///////////////////////////////

	public String getJid() {
		return clientJID.getJid();
	}

	///////////////////////////////
	
	public void createSubscription( String sid ) 
	throws SQLException, DSFormatException {

		//-- obtaining vCards seems flaky so we allow 3 attempts
		for( int attempt = 1; attempt < 4; ) 
		{
			try {
	    		//-- get the subscriptions vcard information
	        	DSVCard vCard = new DSVCard( connection, sid );
	
	        	logger.fine( "--- DSClientBot: [" + getJid() + "] " +
					"Fetching VCard for <" + sid + "> - attempt " + attempt + "... [SUCCESS] " );
	        	
				//-- and add the subscription into the database
		        DSCatalog.db.insertSubscription( 
		        	vCard,
					getJid(),
					DSSub.Status.RECEIVED,
					sid
				);
		        
		        //-- save the avatar if one exists
		        try {
					ImageIO.write( 
						vCard.getAvatar(), 
						"png", 
						new File( "C://" + vCard.getNamespace() + ".png" ) 
					);
					
		        	logger.finer( "--- DSClientBot: [" + getJid() + "] " +
							"Attempting to cache avatar for <" + sid + ">... [SUCCESS]" );
		        	
				} catch (IOException e) {
		        	logger.finer( "--- DSClientBot: [" + getJid() + "] " +
						"Attempting to cache avatar for <" + sid + ">... [FAILED]" );
				}
		        
		        return;

			} catch ( XMPPException e ) {
				logger.fine( "--- DSClientBot: [" + getJid() + "] " +
					"Fetching VCard for <" + sid + "> - attempt " + attempt + "... [FAILED]" );
				attempt++;

			} catch ( DSFormatException e ) {
				logger.fine( "--- DSClientBot: [" + getJid() + "] " +
					"Fetching VCard for <" + sid + "> - attempt " + attempt + "... [SUCCESS] " );
				throw e;
			}
		}
		
		throw new DSFormatException();
	}
	
	///////////////////////////////
	
	public void acceptSubscription( String sid ) 
	throws SQLException {

		//-- the following is required to accept the dataware's subscription request.
		Presence response = new Presence( Presence.Type.subscribed );
		response.setTo( sid );
		sendPacket( response );
		
		//-- and then we need to make sure the subscription is two way...
		Presence request = new Presence( Presence.Type.subscribe );
		request.setTo( sid );
		sendPacket( request );
		
		String ns = DSCatalog.db.fetchNamespace( sid );
		DSCatalog.db.setSubStatus( ns, getJid(), DSSub.Status.RESPONDED );
		
		logger.finer( "--- DSClientBot: [" + getJid() + "] " +
			"accepted subscription request from (" + sid + ") ");
	}

	///////////////////////////////
	
	
	public void completeSubscription( String sid ) {
		
		try {
		
			String ns = DSCatalog.db.fetchNamespace( sid );
			if ( ns != null )
				DSCatalog.db.getSubStatus( 
					getJid(), 
					DSDataManager.SourceField.NAMESPACE, 
					ns 
				);
			else 
				throw new DSException();
			
			DSCatalog.db.setSubStatus( ns, getJid(), DSSub.Status.COMPLETED );
			
			logger.finer( "--- DSClientBot: [" + getJid() + "] " +
				"completed subscription procedure for (" + sid+ ") ");
			
		} catch( SQLException e ) {
			logger.finer( 
				"--- DSClientBot: [" + getJid() + "] " +
				"subscription completion failed due to database errors (" + sid + ") ");
			
		} catch( DSException e ) {
			logger.finer( 
				"--- DSClientBot: [" + getJid() + "] " +
				"subscription completion failed due to lack of namespace for (" + sid + ") ");
		}
	}

	///////////////////////////////
	
	public void rejectSubscription( String sid ) {
		
		//-- the following is required to accept the dataware's subscription request.
		Presence response = new Presence( Presence.Type.unsubscribed );
		response.setTo( sid );
		sendPacket( response );
		
		DSCatalog.db.setSubscriptionStatusFromSid( getJid() , sid, DSSub.Status.REJECTED );
		
		logger.finer( "--- DSClientBot: [" + getJid() + "] " +
				"rejected subscription request from (" + sid + ") ");
		
		RosterPacket rp = new RosterPacket();
		rp.setType( IQ.Type.SET );
		RosterPacket.Item item = new RosterPacket.Item( sid, null );
		item.setItemType( RosterPacket.ItemType.remove );
		rp.addRosterItem( item );
		sendPacket( rp );
		logger.finer( "--- DSClientBot: [" + getJid() + "] " +
				"deleted (" + sid + ") from roster.");
	}
	
	
}
