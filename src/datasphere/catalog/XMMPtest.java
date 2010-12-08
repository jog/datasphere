package datasphere.catalog;

import java.io.IOException;
import java.util.Iterator;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.VCard;


public class XMMPtest {

	public static class DSPacketListener 
    implements PacketListener {
		@Override
		public void processPacket( Packet p ) {
			System.out.println( "packet2: " + p.toXML() );
	        Thread t = Thread.currentThread();
	        String name = t.getName();
	        System.out.println("main thread=" + name);
		}
	};
	
	 
	/**
	 * @param args 
	 * @throws XMPPException 
	 * @throws IOException 
	 */
	public static void main( String[] args ) 
	throws XMPPException, IOException {
		
		XMPPConnection.DEBUG_ENABLED = true;
        //ConnectionConfiguration connConfig = new ConnectionConfiguration( "jabber.org", 5222, "jabber.org" );
		ConnectionConfiguration connConfig = new ConnectionConfiguration( "talk.google.com", 5222, "gmail.com" );
        XMPPConnection connection = new XMPPConnection( connConfig );
        
        connection.connect();
        //connection.login( "mydatasphere", "mydatasphere", "datasphere" );
        connection.login( "james.goulding@gmail.com", "jim1nez", "datasphere" );
        
        // Obtain the ServiceDiscoveryManager associated with my XMPPConnection
        ServiceDiscoveryManager  discoManager = ServiceDiscoveryManager.getInstanceFor( connection );
        discoManager.addFeature( "jabber:iq:version" );
        
        Iterator<String> g = discoManager.getFeatures();
        while ( g.hasNext() ) {
        	System.out.println( g.next() );
        }
   
        VCard vCard = new VCard();
        
        vCard.getOrganization();
        
        
        //vCard.load( connection ); // load own VCard
        vCard.load( connection, "testreceiver@jabber.org");
        System.out.println( vCard ); 
        System.out.println( vCard.getLastName() );
        System.out.println( vCard.getFirstName() );

        System.in.read();
        connection.disconnect();
   }

}
