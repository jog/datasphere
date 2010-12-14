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
import datasphere.catalog.DSSub.Status;
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

		try {
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
			
		} catch ( SQLException e ) {
			//-- complete database meltdown. If we got here, then it 
			//-- is likely that roster and db will now be inconsistent.
			e.printStackTrace();
		}			
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

	public void datawareSubscribe( Presence p ) 
	throws SQLException {
		
		try {
			//-- otherwise we are good to go...
			logger.fine( "--- DSPresenceListener: [" + parent.getJid() + "] " +
				"Dataware subscription request from <" + p.getFrom() + ">" );

			//-- do we have a policy for the source requesting subscription?
			Status policy = DSCatalog.db.fetchPolicy(
					parent.getJid(), 
					p.getFrom() 
				);

			//-- if we don't then let the user decide what to do manually
			if ( policy == Status.REJECTED ) {
				logger.fine( "--- DSPresenceListener: " +
						"[" + parent.getJid() + "] " +
						"Automatically rejecting dataware subscription due to policy " +
						"<" + p.getFrom() + "> ");
				parent.rejectSubscription( p.getFrom() );
			}
			
			//-- otherwise we can create the subscription
			else { 
				parent.createSubscription( p.getFrom() );
				
				//-- and if the source had been deemed as friendly we
				//-- can immediately accept it.
				if ( policy == Status.EXPECTED )  {
					logger.fine( "--- DSPresenceListener: " +
							"[" + parent.getJid() + "] " +
							"Automatically accepting dataware subscription due to policy " +
							"<" + p.getFrom() + "> ");
					parent.acceptSubscription( p.getFrom() );
				}
			}
		}
		catch ( SQLException e ) {
			logger.fine( "--- DSPresenceListener: " +
					"[" + parent.getJid() + "] " +
					"Rejecting dataware subscription due to database issues. " +
					"<" + p.getFrom() + "> ");
			parent.rejectSubscription( p.getFrom() );
			
		} catch ( DSFormatException e ) {
			logger.fine( "--- DSPresenceListener: " +
				"[" + parent.getJid() + "] " +
				"Rejecting dataware subscription due to missing namespace. " +
				"<" + p.getFrom() + "> ");
			parent.rejectSubscription( p.getFrom() );
		}
	}
	
	
    ///////////////////////////////

	public void datawareUnsubscribe( String sid ) {
		// TODO 
		logger.finer( "--- DSPresenceListener: [" + parent.getJid() + "] " +
				"Dataware subscription removal initiated <" + sid + ">" );
	}

	///////////////////////////////
		
    private void completeUnsubscription( String sid ) {
    	// TODO 
    	logger.finer( "--- DSPresenceListener: [" + parent.getJid() + "]" +
				"Dataware subscription removal complete <" + sid + ">" );
	}

    ///////////////////////////////
	
	private void datawarePresenceError( String sid ) {
    	// TODO 
		logger.finer( "--- DSPresenceListener: [" + parent.getJid() + "]" +
			"Error in the server processing our presence requests <" + sid + ">" );
	}
	
};
