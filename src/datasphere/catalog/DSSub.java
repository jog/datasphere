package datasphere.catalog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;

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

public class DSSub {

	public enum Status {
		EXPECTED,
		REJECTED,
		RECEIVED,
		ACCEPTED,
		RESPONDED,
		COMPLETED;
		
		public static Status get( String e ) {
			for( Status s : EnumSet.allOf( Status.class )) {
				if ( e.equalsIgnoreCase( s.toString() ) )  
					return s;
			}
			return null;
		}
	}
	
	private String jid; 
	private String sid;
	private String subscriptionStatus; 
	private long ctime; 
	private long mtime; 
	private String namespace;

	///////////////////////////////
	
	public DSSub( 
		String sid, 
		String jid, 
		String subscriptionStatus, 
		long ctime, 
		long mtime,
		String namespace ) 
	{
		this.sid = sid;  
		this.jid = jid; 
		this.subscriptionStatus = subscriptionStatus;
		this.ctime = ctime;
		this.mtime = mtime;
		this.namespace = namespace;
	}
	
	///////////////////////////////
	
	public String getSid() {
		return sid;
	}

	///////////////////////////////
	
	public String getJid() {
		return jid;
	}

	///////////////////////////////
	
	public String getSubscriptionStatus() {
		return subscriptionStatus;
	}

	///////////////////////////////
	
	public long getMtime() {
		return mtime;
	}
	
	///////////////////////////////
	
	public String getMtimeAsDate() {
		SimpleDateFormat fmt = new SimpleDateFormat( "HH:mm, d MMM yyyy " );
		return fmt.format( new Date( mtime ) );
	}
	
	///////////////////////////////
	
	public long getCtime() {
		return ctime;
	}

	///////////////////////////////
	
	public String getCtimeAsDate() {
		SimpleDateFormat fmt = new SimpleDateFormat( "HH:mm, d MMM yyyy" );
		return fmt.format( new Date( ctime ) );
	}
	
	///////////////////////////////
	
	public String getNamespace() {	
		return namespace;
	}
	
	///////////////////////////////
	
	public boolean hasStatus( Status status ) {
		return getSubscriptionStatus().equalsIgnoreCase( status.toString() );
	}	

}
