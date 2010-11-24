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

import java.io.IOException;
import java.util.logging.Logger;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.json.JSONException;
import org.json.JSONObject;

import datasphere.catalog.DSCatalog;
import datasphere.dataware.DSException;
import datasphere.dataware.DSUpdate;


public class DSUpdateListener 
implements PacketListener {
	
	private static final Logger logger = Logger.getLogger( DSCatalog.class.getName() );
	private DSClientBot parent = null;

    ///////////////////////////////

	public DSUpdateListener( DSClientBot parent ) {
		this.parent  = parent;
	}

    ///////////////////////////////
	
	@Override
	public void processPacket( Packet p ) {
		Message m = (Message) p;
		JSONObject o;
		
		try {
			o = new JSONObject( m.getBody() );
			DSUpdate ds = new DSUpdate( o ); 
			processUpdate( m.getTo(), m.getFrom(), ds );
			
		} catch ( JSONException e ) {
			e.printStackTrace();
		
		} catch ( IOException e ) {
			e.printStackTrace();
			
		} catch ( DSException e ) {
			e.printStackTrace();
		}
	}

	///////////////////////////////

	private void processUpdate( String to, String from, DSUpdate ds ) 
	throws DSException {
		logger.fine( "--- DSUpdateListener: [" + parent.getJid() + "] " + ds.toXML()  );
		DSCatalog.db.insertUpdate( to, from, ds );
	}
	
};
