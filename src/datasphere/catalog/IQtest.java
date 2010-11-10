package datasphere.catalog;

import java.io.IOException;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.packet.DiscoverItems.Item;


public class IQtest {

	public static class DSPacketListener 
    implements PacketListener {

		@Override
		public void processPacket( Packet p ) {
			System.out.println( "received " + p.toXML() );
		}
		
	}
	
	/**
	 * @param args
	 * @throws XMPPException 
	 * @throws IOException 
	 */
	public static void main(String[] args) 
	throws XMPPException, IOException {
	
        XMPPConnection.DEBUG_ENABLED = true;
        ConnectionConfiguration connConfig = new ConnectionConfiguration( "jabber.org", 5222, "jabber.org" );
        XMPPConnection connection = new XMPPConnection( connConfig );
        connection.connect();         

        connection.login( "testsender", "jim1nez", "datasphere" );
        System.out.println( "connected: " + connection.getUser() );
                 
        Message message = new Message( "testreceiver@jabber.org/datasphere", Message.Type.chat );
        message.setBody( "The eagle has landed for the final time!" );
        connection.sendPacket( message );
        System.in.read();
        
        //-- actual test
        final IQ iq = new IQ() {
        	public String getChildElementXML() {
        		return "<query xmlns=\"jabber:iq:version\"/>";
        		//return "<query xmlns=\"mynamespace:test\"/>";
        		//return "<ping xmlns=\"urn:xmpp:ping\"/>";
        		//return "<query xmlns=\"http://jabber.org/protocol/disco#info\"/>";
        	}
        };
        
        iq.setType( IQ.Type.GET );
        iq.setTo( "testreceiver@jabber.org/datasphere" );
        iq.setFrom ( "testsender@jabber.org/datasphere" );
	    connection.sendPacket( iq );
	    
	    // Obtain the ServiceDiscoveryManager associated with my XMPPConnection
	      ServiceDiscoveryManager discoManager = ServiceDiscoveryManager.getInstanceFor(connection);
	      
	      // Get the information of a given XMPP entity
	      // This example gets the information of a conference room
	      DiscoverItems discoItems = discoManager.discoverItems( "testreceiver@jabber.org" );
	      
	   // Get the discovered items of the queried XMPP entity
	       java.util.Iterator<Item> it = discoItems.getItems();
	      // Display the items of the remote XMPP entity
	      while (it.hasNext()) {
	    	  System.out.println( "IN");
	          DiscoverItems.Item item = (DiscoverItems.Item) it.next();
	          System.out.println(item.getEntityID());
	          System.out.println(item.getNode());
	          System.out.println(item.getName());
	      }	
	}
}
