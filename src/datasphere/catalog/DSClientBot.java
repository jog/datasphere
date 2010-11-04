package datasphere.catalog;

/*
Copyright (c) 2010 J.Goulding 

Licensed under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and 
limitations under the License. 
*/

import java.util.logging.Logger;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
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
      		
			//-- registers that we can accept dataware iq communications
			ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor( connection );
			sdm.addFeature( "jabber:iq:version" );
	        
	        //-- registers update listener
	        connection.addPacketListener( 
	            	new DSUpdateListener( this ), 
	            	new MessageTypeFilter( Message.Type.chat ) );
	         
	        //-- registers iq listener.
	        connection.addPacketListener( 
	            	new DSPacketListener( this ), 
	            	new PacketTypeFilter( IQ.class ) );
	        
			logger.finer( "--- DSClientBot: connecting bot for client (" + clientJID.getJID() + ")... [SUCCESS]" );
			
		} catch ( XMPPException e ) {
			logger.severe( "--- DSClientBot: connecting bot for client (" + clientJID.getJID() + ")... [FAILED]" );
		}
    }

	
	///////////////////////////////
	
	public static void setPort( Integer serverPort ) {
		port = serverPort;		
	}
}
