package datasphere.catalog.http;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.restlet.Component;
import org.restlet.data.Protocol;
import org.restlet.resource.Directory;

import datasphere.dataware.DSLogFormatter;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;

public class DSWebServer extends Component {

	private static Logger logger = Logger.getLogger( "org.restlet" );
	public static Configuration cfg = null;
	private Integer serverPort;
	private int DEFAULT_SERVER_PORT = 80;
	
	///////////////////////////////
	
	public DSWebServer() 
	throws SecurityException, IOException {
		
		//-- start logging service for this component
	    FileHandler fileHandler;
	    fileHandler = new FileHandler( "dshttp%g.log", true );     
	    fileHandler.setFormatter( new DSLogFormatter() );
	    logger.addHandler( fileHandler );
	    
	    /** 
	    DEBUG  Handler handler = new ConsoleHandler();
	    handler.setFormatter( new DSWebLogFormatter() );
	    handler.setLevel( Level.FINER );
	    logger.addHandler( handler );
	    */
	    logger.setUseParentHandlers( false );
	    logger.setLevel( Level.INFO );
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
		getClients().add( Protocol.CLAP );
		
		//-- using the default virtual host, create url routing
		getDefaultHost().attach( "/user_history", UserHistoryPage.class );
		getDefaultHost().attach( "/source_history", SourceHistoryPage.class );
		getDefaultHost().attach( "/subscription", SetSourceSubscription.class );
		getDefaultHost().attach( "/static/", new Directory( 
			getContext().createChildContext(), "clap://system/resources/" ) );
		
		//-- setup the freemarker configuration files
		cfg = new Configuration();
		cfg.setClassForTemplateLoading( this.getClass(), "/resources/templates" );
		cfg.setObjectWrapper( new DefaultObjectWrapper() );  

		//-- DEBUG (used on local machine) 
		//-- getClients().add( Protocol.FILE ); 
		//-- getDefaultHost().attach( "/static/", new Directory( 
		//--	getContext().createChildContext(),
		//--	"file:///C:\\HyperPlace\\datasphere\\static\\resources\\" )
		//-- );
		//-- cfg.setDirectoryForTemplateLoading(	new File( TEMPLATES_DIR ) );

		//-- start the component proper
		super.start();
	}
	
	//////////////////////////////////
	
	public static Template getTemplate( String name ) 
	throws IOException {
		return ( cfg == null ) ? null : cfg.getTemplate( name );
	}
}







/*cfg.setTemplateLoader(
	new ClassTemplateLoader( 
		getClass(), 
		"datasphere/catalog/http/templates"
	)
);*/