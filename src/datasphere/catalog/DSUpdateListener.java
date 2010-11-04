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

import java.io.IOException;
import java.util.logging.Logger;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.json.JSONException;
import org.json.JSONObject;

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
			proccessUpdate( ds );
			
		} catch (JSONException e) {
			e.printStackTrace();
		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	///////////////////////////////

	private void proccessUpdate( DSUpdate ds ) {
		logger.finer( "DSUPDATE: " + ds.toXML()  );
	}
	
};
