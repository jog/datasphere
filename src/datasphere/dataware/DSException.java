package datasphere.dataware;

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

/**
 * Generic Datasphere exception class, indicating that an error has
 * been thrown somewhere within the framework. 
 * 
 * @author James Goulding
 * @version 2010-03-11
 */
public class DSException 
extends Exception
{
	private static final long serialVersionUID = 1014530909804487992L;

	public DSException( String s ) {
		super( s );
	}
	
	public DSException( Exception e ) {
		super( e );
	}
}

// End ///////////////////////////////////////////////////////////////