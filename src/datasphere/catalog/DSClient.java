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

public class DSClient {

	private String JID;
	private String user;
	private String service;
	private String pass;
	private String host;
	
	///////////////////////////////
	
	public DSClient( 
		String JID, 
		String user, 
		String host, 
		String service, 
		String pass ) 
	{
		this.JID = JID;
		this.user = user;
		this.host = host;
		this.service = service;
		this.pass = pass;
	}
	
	///////////////////////////////
	
	public String getService() {
		return service;
	}
	
	///////////////////////////////
	
	public String getUser() {
		return user;
	}
	
	///////////////////////////////
	
	public String getJID() {
		return JID;
	}
	
	///////////////////////////////
	
	public String getPass() {
		return pass;
	}
	
	///////////////////////////////
	
	public String getHost() {
		return host;
	}
	
	///////////////////////////////

	@Override
	public String toString() {
		return "DSClient [JID=" + JID + ", host=" + host + ", pass=" + pass
				+ ", service=" + service + ", user=" + user + "]";
	}	
}
