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

import java.util.logging.Logger;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;

import datasphere.catalog.DSCatalog;

public class DSPacketListener 
implements PacketListener {

	private static final Logger logger = Logger.getLogger( DSCatalog.class.getName() );
	private DSClientBot parent = null;

    ///////////////////////////////

	public DSPacketListener( DSClientBot parent ) {
		this.parent  = parent;
	}

    ///////////////////////////////
	
	@Override
	public void processPacket( Packet p ) {
		logger.finest( "--- DSPacketListener: [" + parent.getJid() + "] " + p.toXML()  );
	}
};
