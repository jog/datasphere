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

//-- Handles invites to communicate from dataware sources
import java.sql.SQLException;
import java.util.logging.Logger;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket;

import datasphere.catalog.DSCatalog;
import datasphere.catalog.DSSub;
import datasphere.dataware.DSFormatException;

public class DSPresenceListener 
implements PacketListener {

	private static final Logger logger = Logger.getLogger( DSCatalog.class.getName() );
	private DSClientBot parent = null;

    ///////////////////////////////

	public DSPresenceListener( DSClientBot parent ) {
		this.parent  = parent;
	}

    ///////////////////////////////

	public void processPacket( Packet packet ) {

		Presence p = (Presence) packet;
		String sid = p.getFrom();
		
		if ( p.getType() == Presence.Type.unavailable ) 
			datawareUnavailable( sid );
		
		else if ( p.getType() == Presence.Type.available ) 
			datawareAvailable( sid );
		
		else if ( p.getType() == Presence.Type.subscribe ) 
			datawareSubscribe( p );
		
		else if ( p.getType() == Presence.Type.unsubscribe ) 
			datawareUnsubscribe( sid );
		
		else if ( p.getType() == Presence.Type.error ) 
			datawarePresenceError( sid );
		
		else if ( p.getType() == Presence.Type.subscribed ) 
			parent.completeSubscription( p.getFrom() );
		
		else if ( p.getType() == Presence.Type.unsubscribed ) 
			completeUnsubscription( p.getFrom() );
	}

	///////////////////////////////


	public void datawareAvailable( String sid ) {
		logger.finer( "--- DSPresenceListener: [" + parent.getJid() + "] " +
			"Dataware available for (" + sid + ")" );
	}
	
    ///////////////////////////////

	public void datawareUnavailable( String sid ) {
		logger.finer( "--- DSPresenceListener: [" + parent.getJid() + "] " +
			"Dataware unavailable for (" + sid + ")" );
	}
	
	
    ///////////////////////////////

	public void datawareSubscribe( Presence p ) {
		
		try {
			//-- obtain the namespace of the source from the presence "nickname"
			DSNickname ex = (DSNickname) p.getExtension( "http://jabber.org/protocol/nick" );
			String nick = ( ex != null ) ? ex.getNick() : null;
			
			//-- if none has been supplied we reject the request.
			if ( nick == null ) 
				throw new DSFormatException();
		
			//-- otherwise we are good to go...
			logger.fine( "--- DSPresenceListener: " +
				"[" + parent.getJid() + "] " +
				"Dataware subscription request from \"" + nick + "\" " +
				"(" + p.getFrom() + ") ");

			//-- obtain the subscription proper
			String sub = DSCatalog.db.getSubscriptionStatus( nick, parent.getJid() );
			
			//-- if we aren't subscribed, steup the request and wait for user confirmation.
			if ( sub == null )
				createSubscription( nick, p.getFrom() );
			
			//-- request is expected so accept it
			else if ( sub.equals( DSSub.Status.EXPECTED ) )
				parent.acceptSubscription( p.getFrom() );
			
			//-- request is unwelcome so reject it.
			else if ( sub.equals( DSSub.Status.REJECTED ) )
				rejectSubscription( p.getFrom() );
			
			//-- source is being beligerent - it will have to wait for the user.
			else if ( sub.equals( DSSub.Status.RECEIVED ) );
	
			//-- already dealt with - we should do some roster error checking.
			else if ( 
				sub.equals( DSSub.Status.ACCEPTED ) || 
				sub.equals( DSSub.Status.RESPONDED ) ||
				sub.equals( DSSub.Status.COMPLETED ) );
			
		}
		catch ( SQLException e ) {
			logger.fine( "--- DSPresenceListener: " +
					"[" + parent.getJid() + "] " +
					"Rejecting dataware subscription due to database issues. " +
					"(" + p.getFrom() + ") ");
			rejectSubscription( p.getFrom() );
			
		} catch ( DSFormatException e ) {
			logger.fine( "--- DSPresenceListener: " +
				"[" + parent.getJid() + "] " +
				"Rejecting dataware subscription due missing namespace. " +
				"(" + p.getFrom() + ") ");
			rejectSubscription( p.getFrom() );
		}
	}
	
	///////////////////////////////
	
	private void createSubscription( String namespace, String sid ) 
	throws SQLException, DSFormatException {
		
		DSCatalog.db.insertSubscription( 
			namespace,
			parent.getJid(),
			DSSub.Status.RECEIVED,
			sid
		);
	}
	
	///////////////////////////////
	
	private void rejectSubscription( String sid ) {
		
		//-- the following is required to accept the dataware's subscription request.
		Presence response = new Presence( Presence.Type.unsubscribed );
		response.setTo( sid );
		parent.sendPacket( response );
		DSCatalog.db.setSubscriptionStatusFromSid( parent.getJid() , sid, DSSub.Status.REJECTED );
		
		logger.finer( "--- DSPresenceListener: [" + parent.getJid() + "] " +
				"rejected subscription request from (" + sid + ") ");
		
		RosterPacket rp = new RosterPacket();
		rp.setType( IQ.Type.SET );
		RosterPacket.Item item = new RosterPacket.Item( sid, null );
		item.setItemType( RosterPacket.ItemType.remove );
		rp.addRosterItem( item );
		parent.sendPacket( rp );
		logger.finer( "--- DSPresenceListener: [" + parent.getJid() + "] " +
				"deleted (" + sid + ") from roster.");
	}
	
	
    ///////////////////////////////

	public void datawareUnsubscribe( String sid ) {
		// TODO Auto-generated method stub
		logger.finer( "--- DSPresenceListener: [" + parent.getJid() + "] " +
				"Dataware subscription removal (" + sid + ") " );
	}

	///////////////////////////////
		
    private void completeUnsubscription( String sid ) {
		// TODO Auto-generated method stub
	}

    ///////////////////////////////
	
	private void datawarePresenceError( String sid ) {
		logger.finer( "--- DSPresenceListener: [" + parent.getJid() + "]" +
			"Error in the server processing our presence requests" );
	}
	
};
