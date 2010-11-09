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

//-- Handles invites to communicate from dataware sources
import java.util.logging.Logger;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket;

public class DSPresenceListener 
implements PacketListener {

	private static final String EXPECTED  = "EXPECTED";
	private static final String REJECTED  = "REJECTED";
	private static final String RECEIVED  = "RECEIVED";
	private static final String ACCEPTED  = "ACCEPTED";
	private static final String RESPONDED = "RESPONDED";
	private static final String COMPLETED = "COMPLETED";
	
	private static final Logger logger = Logger.getLogger( DSCatalog.class.getName() );
	private DSClientBot parent = null;

    ///////////////////////////////

	public DSPresenceListener( DSClientBot parent ) {
		this.parent  = parent;
	}

    ///////////////////////////////

	public void processPacket( Packet packet ) {
		Presence p = (Presence) packet;
		
		if ( p.getType() == Presence.Type.unavailable ) 
			datawareUnavailable( p );
		else if ( p.getType() == Presence.Type.available ) 
			datawareAvailable( p );
		else if ( p.getType() == Presence.Type.subscribe ) 
			datawareSubscribe( p );
		else if ( p.getType() == Presence.Type.unsubscribe ) 
			datawareUnsubscribe( p );
		else if ( p.getType() == Presence.Type.error ) 
			datawarePresenceError( p );
		else if ( p.getType() == Presence.Type.subscribed ) 
			completeSubscription( p );
		else if ( p.getType() == Presence.Type.unsubscribed ) 
			completeUnsubscription( p );
	}

	///////////////////////////////


	public void datawareAvailable( Presence p ) {
		logger.finer( "--- DSPresenceListener: [" + parent.getJid() + "] " +
			"Dataware available for (" + p.getFrom() + ")" );
	}
	
    ///////////////////////////////

	public void datawareUnavailable( Presence p ) {
		logger.finer( "--- DSPresenceListener: [" + parent.getJid() + "] " +
			"Dataware unavailable for (" + p.getFrom() + ")" );
	}
	
	
    ///////////////////////////////

	public void datawareSubscribe( Presence p ) {
		
		logger.finer( "--- DSPresenceListener: [" + parent.getJid() + "] " +
			"Dataware subscription request (" + p.getFrom() + ") ");
		String sub = DSCatalog.db.getSubscriptionStatus( p.getFrom() );

		//-- new dataware - create it and wait for user confirmation.
		if ( sub == null )
			createSubscription( p );
		
		//-- request is expected so accept it
		else if ( sub.equals( EXPECTED ) )
			acceptSubscription( p );
		
		//-- request is unwelcome so reject it.
		else if ( sub.equals( REJECTED ) )
			rejectSubscription( p );
		
		//-- source is being beligerent - it will have to wait for the user.
		else if ( sub.equals( RECEIVED ) );

		//-- already dealt with - we should do some roster error checking.
		else if ( 
			sub.equals( ACCEPTED ) || 
			sub.equals( RESPONDED ) ||
			sub.equals( COMPLETED )	);
	}
	
	///////////////////////////////
	
	private void createSubscription( Presence p ) {
		DSCatalog.db.insertDataware( parent.getJid() , p.getFrom(), RECEIVED );
	}
	
	///////////////////////////////
	
	private void acceptSubscription( Presence p ) {
		
		//-- the following is required to accept the dataware's subscription request.
		Presence response = new Presence( Presence.Type.subscribed );
		response.setTo( response.getFrom() );
		parent.sendPacket( response );

		//-- and then we need to make sure the subscription is two way...
		Presence request = new Presence( Presence.Type.subscribe );
		request.setTo( p.getFrom() );
		parent.sendPacket( request );
		
		DSCatalog.db.setSubscriptionStatus( parent.getJid() , p.getFrom(), RESPONDED );
		
		logger.finer( "--- DSPresenceListener: accepted subscription request from (" + p.getFrom() + ") ");
	}
	
	///////////////////////////////
	
	private void rejectSubscription( Presence p ) {
		
		//-- the following is required to accept the dataware's subscription request.
		Presence response = new Presence( Presence.Type.unsubscribed );
		response.setTo( p.getFrom() );
		parent.sendPacket( response );
		DSCatalog.db.setSubscriptionStatus( parent.getJid() , p.getFrom(), REJECTED );
		
		logger.finer( "--- DSPresenceListener: rejected subscription request from (" + p.getFrom() + ") ");
		
		RosterPacket rp = new RosterPacket();
		rp.setType( IQ.Type.SET );
		RosterPacket.Item item = new RosterPacket.Item( p.getFrom(), null );
		item.setItemType( RosterPacket.ItemType.remove );
		rp.addRosterItem( item );
		parent.sendPacket( rp );
		logger.finer( "--- DSPresenceListener: deleted (" + p.getFrom() + ") from roster.");
	}
	
	
	///////////////////////////////
	
	
	private void completeSubscription( Presence p ) {
		//-- check that we have a subscription "RESPONDED"...
		logger.finer( "--- DSPresenceListener: accepted subscription request from (" + p.getFrom() + ") ");
	}
	
    ///////////////////////////////

	public void datawareUnsubscribe( Presence p ) {
		logger.finer( "--- DSPresenceListener: Dataware subscription removal (" + p.getFrom() + ") " + p );
	}

	///////////////////////////////
		
    private void completeUnsubscription(Presence p) {
		// TODO Auto-generated method stub
		
	}

    ///////////////////////////////
	
	private void datawarePresenceError( Presence p ) {
		logger.finer( "--- DSPresenceListener: Error in the server processing our presence requests" );
	}
	
};
