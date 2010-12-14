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

import java.awt.image.BufferedImage;
import java.io.File;
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
import datasphere.catalog.DSSub;
import datasphere.catalog.DSSub.Status;
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
	

	public DSVCard getVCard( String sid, int attempts ) {

		for( int i = 0; i < attempts; i++ ) {

			try {
	    		//-- get the subscriptions vcard information
				DSVCard vCard = new DSVCard( connection, sid );
	        	logger.fine( "--- DSClientBot: [" + getJid() + "] " +
						"Fetching VCard for <" + sid + "> - attempt " + i + "... [SUCCESS] " );
	        	return vCard;

			} catch ( XMPPException e ) {
				logger.fine( "--- DSClientBot: [" + getJid() + "] " +
					"Fetching VCard for <" + sid + "> - attempt " + i + "... [RETRIEVAL FAILED]" );


			} catch ( DSFormatException e ) {
				logger.fine( "--- DSClientBot: [" + getJid() + "] " +
					"Fetching VCard for <" + sid + "> - attempt " + i + "... [PARSING FAILED] " );
				return null;
			}
		}
		return null;
	}
	
	///////////////////////////////
	
	public void createSubscription( String sid ) 
	throws SQLException, DSFormatException {

		//-- obtaining vCards seems flaky so we allow 3 attempts
		DSVCard vCard = getVCard( sid, 3 );

		//-- if we've had no response then we have to stop the subscription process
		if ( vCard == null )
			throw new DSFormatException();

		//-- otherwise add the subscription into the database
		DSCatalog.db.insertSub( 
			vCard,
			getJid(),
			DSSub.Status.RECEIVED,
			sid
		);

        //-- and save the dataware's, if one exists, to the filesystem
        try {
        	File iconFile = new File( "./resources/images/icons/" + vCard.getAvatarName() );
        	BufferedImage v = vCard.getAvatar();
        	
        	if ( v != null ) {
        		DSIcon icon = new DSIcon( v, 70, 20);
        		ImageIO.write( icon.getImage(), "png", iconFile );
        		logger.fine( "--- DSClientBot: [" + getJid() + "] " +
        			"Attempting to cache avatar for <" + sid + ">... [SUCCESS]" );
        	} else {
        		logger.fine( "--- DSClientBot: [" + getJid() + "] " +
	        			"No avatar for <" + sid + "> so using default... [SUCCESS]" );
        	}

		} catch ( Exception e ) {
        	logger.fine( "--- DSClientBot: [" + getJid() + "] " +
				"Attempting to cache avatar for <" + sid + ">... [FAILED]" );
		} 
	}
	
	///////////////////////////////
	
	public void acceptSubscription( String sid ) 
	throws SQLException {

		//-- log the subscription as accepted in the database
		DSCatalog.db.setSubStatus( 
			getJid(), 
			sid,
			DSSub.Status.ACCEPTED
		);
		
		//-- the following is required to accept the dataware's subscription request.
		Presence response = new Presence( Presence.Type.subscribed );
		response.setTo( sid );
		sendPacket( response );
		
		//-- and then we need to make sure the subscription is two way...
		Presence request = new Presence( Presence.Type.subscribe );
		request.setTo( sid );
		sendPacket( request );
		
		DSCatalog.db.setSubStatus( 
			getJid(), 
			sid,
			DSSub.Status.RESPONDED 
		);
		
		logger.finer( "--- DSClientBot: [" + getJid() + "] " +
			"accepted subscription request from (" + sid + ") ");
	}

	///////////////////////////////
	
	
	public void completeSubscription( String sid ) {

			DSCatalog.db.setSubStatus( 
				getJid(), 
				sid,
				DSSub.Status.COMPLETED
			);
			
			logger.finer( "--- DSClientBot: [" + getJid() + "] " +
				"completed subscription procedure for (" + sid + ") ");
	}

	///////////////////////////////
	
	public void rejectSubscription( String sid ) 
	throws SQLException {
		
		//-- the following is required to reject the dataware's subscription request.
		Presence response = new Presence( Presence.Type.unsubscribe );
		response.setTo( sid );
		sendPacket( response );
		
		DSCatalog.db.deleteSub( getJid(), sid );
		DSCatalog.db.updatePolicy( getJid(), sid, Status.REJECTED );
		
		logger.finer( "--- DSClientBot: [" + getJid() + "] " +
				"successfully rejected subscription request from (" + sid + ") ");
		
		RosterPacket rp = new RosterPacket();
		rp.setType( IQ.Type.SET );
		RosterPacket.Item item = new RosterPacket.Item( sid, null );
		item.setItemType( RosterPacket.ItemType.remove );
		rp.addRosterItem( item );
		sendPacket( rp );
		logger.finer( "--- DSClientBot: [" + getJid() + "] " +
				"successfully deleted (" + sid + ") from roster.");
	}
	
	///////////////////////////////
	
	public void expectSubscription( String sid ) 
	throws DSException, SQLException {

		DSCatalog.db.updatePolicy( getJid(), sid, Status.EXPECTED );
	}
	
	///////////////////////////////
	
	public void resetSubscription( String sid ) 
	throws SQLException {
		
		DSCatalog.db.resetPolicy( getJid(), sid );
	}
}
