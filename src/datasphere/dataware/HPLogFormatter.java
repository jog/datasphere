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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * A simple formatter used in all Datasphere logging. 
 * 
 * @author James Goulding
 * @version 2010-11-03
 */
public final class HPLogFormatter extends Formatter 
{
	private SimpleDateFormat fmt = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
	
	public String format( LogRecord record ) {
		
		String level = String.format("%1$-9s", "[" + record.getLevel() + "]"); 
		return fmt.format( new Date() ) + 
			" " + level +
			" " + record.getMessage() + 
			"\n";
	} 
}
