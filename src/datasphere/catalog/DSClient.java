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

public class DSClient {

	private String JID;
	private String user;
	private String service;
	private String pass;
	private String host;
	
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
	
	public String getService() {
		return service;
	}
	
	public String getUser() {
		return user;
	}
	
	public String getJID() {
		return JID;
	}

	public String getPass() {
		return pass;
	}

	public String getHost() {
		return host;
	}
}
