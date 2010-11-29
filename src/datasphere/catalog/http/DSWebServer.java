package datasphere.catalog.http;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.restlet.Component;
import org.restlet.data.Protocol;
import org.restlet.resource.Directory;

import datasphere.catalog.DSCatalog;
import datasphere.dataware.DSLogFormatter;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;

public class DSWebServer extends Component {

	private static Logger weblogger = Logger.getLogger( "org.restlet" );
	private static Logger logger = Logger.getLogger( DSCatalog.class.getName() );
	
	public static Configuration cfg = null;
	private Integer serverPort;
	private int DEFAULT_SERVER_PORT = 80;
	private Protocol protocol = Protocol.CLAP;
	
	///////////////////////////////
	
	public DSWebServer() 
	throws SecurityException, IOException {
		
		//-- start logging service for this component
	    FileHandler fileHandler;
	    fileHandler = new FileHandler( "dshttp%g.log", true );     
	    fileHandler.setFormatter( new DSLogFormatter() );
	    
	    Handler webHandler = new ConsoleHandler();
	    webHandler.setFormatter( new DSWebLogFormatter() );
	    webHandler.setLevel( Level.FINER );
	    //DEBUG: weblogger.addHandler( handler );
	    
	    weblogger.addHandler( fileHandler );
	    weblogger.setUseParentHandlers( false );
	    weblogger.setLevel( Level.INFO );
	}
	
	///////////////////////////////

	public DSWebServer( Integer serverPort ) 
	throws SecurityException, IOException {
		this();
		this.serverPort = 
			( serverPort == null ) 
			? DEFAULT_SERVER_PORT
			: serverPort;
		
	}

	///////////////////////////////

	public void useProtocol( Protocol p ) {
		this.protocol = p;
	}
	
	///////////////////////////////
	
	@Override
	/**
	 * 
	 */
	public void start()
	throws Exception {
		
		//-- if no port has been supplied use the default
		if ( this.serverPort == null ) 	
			this.serverPort = DEFAULT_SERVER_PORT;
		
		//-- add a new server connector to the component
		getServers().add( Protocol.HTTP, serverPort );
		getClients().add( protocol );
		
 
		//-- using the default virtual host, create appropriate url routing
		String ref = ( protocol == Protocol.CLAP )  
					 ? "clap://system/resources/"
					 : "file:///C:\\HyperPlace\\datasphere\\static\\resources\\" ;
		getDefaultHost().attach( "/static/", new Directory( getContext().createChildContext(), ref ) );
		getDefaultHost().attach( "/user_history", UserHistoryPage.class );
		getDefaultHost().attach( "/source_history", SourceHistoryPage.class );
		getDefaultHost().attach( "/subscription", SetSourceSubscription.class );
		
		//-- setup the freemarker configuration files
		cfg = new Configuration();
		cfg.setObjectWrapper( new DefaultObjectWrapper() );
		
		//-- depending on the protocol setup the relevent freemarker template loader
		if ( protocol == Protocol.CLAP ) { 
			cfg.setClassForTemplateLoading( getClass(), "/resources/templates" );	
		} else {
			cfg.setDirectoryForTemplateLoading(	
				new File( "C:\\HyperPlace\\datasphere\\static\\resources\\templates\\" ) 
			);
			cfg.setTemplateLoader( 
				new ClassTemplateLoader( getClass(), "datasphere/catalog/http/templates" ) 
			);
		}
		
		//-- start the component proper
		try {
			super.start();
			logger.info( "--- DSWebServer: Starting the internal HTTP server on port " + serverPort + "... [SUCCESS]");
		} catch( BindException e ) {
			logger.severe( "--- DSWebServer: Starting the internal HTTP server on port " + serverPort + "... [FAILED]");
		} catch ( Exception e ) {
			throw e;
		}
	}
	
	//////////////////////////////////
	
	public static Template getTemplate( String name ) 
	throws IOException {
		return ( cfg == null ) ? null : cfg.getTemplate( name );
	}
}



