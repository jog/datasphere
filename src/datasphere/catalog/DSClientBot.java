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

import java.util.logging.Logger;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.ServiceDiscoveryManager;

public class DSClientBot {

	private static final Logger logger = Logger.getLogger( 
			DSCatalog.class.getName() );
	
	private static int port = 5222;
	private static String resource = "datasphere";
	
	private DSClient clientJID = null;
	private XMPPConnection connection = null; 

	///////////////////////////////
	
	DSClientBot( DSClient clientJID ) {
     	this.clientJID = clientJID;
	}
	
	///////////////////////////////
	
	public void connect() {

		try {

	        
			ConnectionConfiguration connConfig = new ConnectionConfiguration( 
    		clientJID.getHost(), port, clientJID.getService() );
    		connection = new XMPPConnection( connConfig );
    		connection.connect();
    		connection.login( clientJID.getUser(), clientJID.getPass(), resource );
      		
			//-- register that we manually subscribe to subscription requests
    		Roster.setDefaultSubscriptionMode( Roster.SubscriptionMode.manual );
    		Roster roster = connection.getRoster();
			roster.setSubscriptionMode( Roster.SubscriptionMode.manual );
	        
			//-- registers that we can accept dataware iq communications
			ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor( connection );
			sdm.addFeature( "jabber:iq:version" );
	        
			//-- registers update listener (for dataware update messages)
	        connection.addPacketListener( 
	            	new DSUpdateListener( this ), 
	            	new MessageTypeFilter( Message.Type.chat ) );
	         
	        //-- registers iq listener (for dataware requests).
	        connection.addPacketListener( 
	            	new DSPacketListener( this ), 
	            	new PacketTypeFilter( IQ.class ) );
	        
	        
	        //-- registers a precense listener (for dataware invites) 
	        connection.addPacketListener( 
	            	new DSPresenceListener( this ), 
	            	new PacketTypeFilter( Presence.class ) );
	        
			logger.finer( "--- DSClientBot: connecting bot for client (" + clientJID.getJID() + ")... [SUCCESS]" );
			
		} catch ( XMPPException e ) {
			logger.severe( "--- DSClientBot: connecting bot for client (" + clientJID.getJID() + ")... [FAILED]" );
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
		return clientJID.getJID();
	}

}
