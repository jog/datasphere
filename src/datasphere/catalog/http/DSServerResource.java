package datasphere.catalog.http;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ServerResource;


import freemarker.template.TemplateException;

public class DSServerResource 
extends ServerResource {
	 
	///////////////////////////////////
	
	protected Representation processHTML( String template, Map< String, Object> data ) 
	throws TemplateException, IOException {

        Writer out = new StringWriter();
        DSWebServer.getTemplate( template ).process( data, out );
		out.flush();  
        
		Representation result = new StringRepresentation( 
			out.toString(), 
			MediaType.TEXT_HTML 
		);

		return result;
	}

}

