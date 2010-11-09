package datasphere.dataware;

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
public final class DSLogFormatter extends Formatter 
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
