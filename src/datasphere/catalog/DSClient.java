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

	private String jid;
	private String firstname; 
	private String lastname; 
	private String email; 
	private long ctime; 
	private long atime;
	private String host;
	private String service; 
	private String user;
	private String password;
	
	///////////////////////////////
	
	public String getJid() {
		return jid;
	}

	///////////////////////////////
	
	public String getFirstname() {
		return firstname;
	}

	///////////////////////////////
	
	public String getLastname() {
		return lastname;
	}

	///////////////////////////////
	
	public String getEmail() {
		return email;
	}

	///////////////////////////////
	
	public long getCtime() {
		return ctime;
	}

	///////////////////////////////
	
	public long getAtime() {
		return atime;
	}

	///////////////////////////////
	
	public String getHost() {
		return host;
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
	
	public String getPassword() {
		return password;
	}

	///////////////////////////////
	
	public DSClient( 
		String jid, 
		String firstname, 
		String lastname, 
		String email, 
		long ctime, 
		long atime, 
		String host, 
		String service, 
		String user, 
		String password ) 
	{
		this.jid = jid; 
		this.firstname = firstname;  
		this.lastname = lastname;  
		this.email = email;  
		this.ctime = ctime;  
		this.atime = atime;  
		this.host = host;  
		this.service = service;  
		this.user = user;  
		this.password = password; 
	}
	

}
