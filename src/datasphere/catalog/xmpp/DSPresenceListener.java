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
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

import datasphere.catalog.DSCatalog;
import datasphere.catalog.DSDataManager;
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
			//-- otherwise we are good to go...
			logger.fine( "--- DSPresenceListener: [" + parent.getJid() + "] " +
				"Dataware subscription request from " + p.getFrom() );
			
			//-- determine the current status, if any, of the subscription
			String sub = DSCatalog.db.getSubStatus( 
					parent.getJid(), 
					DSDataManager.SourceField.SID, 
					p.getFrom()
			);
						
			//-- if we aren't subscribed, steup the request and wait for user confirmation.
			if ( sub == null ) {
				parent.createSubscription( p.getFrom() );
			}
			//-- request is expected so accept it
			else if ( sub.equals( DSSub.Status.EXPECTED ) ) {
				parent.acceptSubscription( p.getFrom() );
			}
			//-- request is unwelcome so reject it.
			else if ( sub.equals( DSSub.Status.REJECTED ) ) {
				parent.rejectSubscription( p.getFrom() );
			}
			//-- source is being beligerent - it will have to wait for the user.
			else if ( sub.equals( DSSub.Status.RECEIVED ) ) {
			}
			//-- already dealt with - we should do some roster error checking.
			else if ( 
				sub.equals( DSSub.Status.ACCEPTED ) || 
				sub.equals( DSSub.Status.RESPONDED ) ||
				sub.equals( DSSub.Status.COMPLETED ) 
			);
			
		}
		catch ( SQLException e ) {
			logger.fine( "--- DSPresenceListener: " +
					"[" + parent.getJid() + "] " +
					"Rejecting dataware subscription due to database issues. " +
					"(" + p.getFrom() + ") ");
			parent.rejectSubscription( p.getFrom() );
			
		} catch ( DSFormatException e ) {
			logger.fine( "--- DSPresenceListener: " +
				"[" + parent.getJid() + "] " +
				"Rejecting dataware subscription due to missing namespace. " +
				"(" + p.getFrom() + ") ");
			parent.rejectSubscription( p.getFrom() );
		}
	}
	
	
    ///////////////////////////////

	public void datawareUnsubscribe( String sid ) {
		// TODO 
		logger.finer( "--- DSPresenceListener: [" + parent.getJid() + "] " +
				"Dataware subscription removal initiated (" + sid + ") " );
	}

	///////////////////////////////
		
    private void completeUnsubscription( String sid ) {
    	// TODO 
    	logger.finer( "--- DSPresenceListener: [" + parent.getJid() + "]" +
				"Dataware subscription removal complete (" + sid + ") " );
	}

    ///////////////////////////////
	
	private void datawarePresenceError( String sid ) {
    	// TODO 
		logger.finer( "--- DSPresenceListener: [" + parent.getJid() + "]" +
			"Error in the server processing our presence requests" );
	}
	
};
