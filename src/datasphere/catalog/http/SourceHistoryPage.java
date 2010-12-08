package datasphere.catalog.http;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.restlet.data.Form;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

import datasphere.catalog.DSCatalog;
import datasphere.catalog.DSClient;
import datasphere.catalog.DSSource;
import datasphere.catalog.DSSub;
import datasphere.dataware.DSUpdate;
import freemarker.template.TemplateException;

public class SourceHistoryPage 
extends DSServerResource { 
	
	private DSClient user;
	private DSSource source;
	private DSSub subscription;  
	private Map< String, Object > data;
	
	///////////////////////////////////
	
	@Get()
	public Representation doGet() {
	
		try {
			//-- setup a data object ready for HTML processing
			data = new HashMap< String, Object >();
			
			//-- fetch client information
			Form form = getRequest().getResourceRef().getQueryAsForm();
			this.user = DSCatalog.db.fetchClient( form.getFirstValue( "jid" ) );
			this.source = DSCatalog.db.fetchSource( form.getFirstValue( "ns" ) );
			
			//-- if we have found the source examine the user's subscription status with it?
			if ( user != null && source != null ) { 

				
				//-- fetch information about active subscriptions for the side panel
				data.put( "activeSubs",  
					DSCatalog.db.fetchSources( 
						user.getJid(), 
						DSSub.Status.COMPLETED,
						DSSub.Status.RESPONDED
					)
				);
				
				//-- fetch information about pending subscriptions for the side panel
				data.put( "pendingSubs", 
					DSCatalog.db.fetchSources( 
						user.getJid(), 
						DSSub.Status.RECEIVED,
						DSSub.Status.ACCEPTED
					) 
				);
				
				//-- fetch the subscription info for the namespace currently being considered
				this.subscription = DSCatalog.db.fetchSub( 
					user.getJid(), 
					source.getNamespace() 
				);
				
				return presentSource( form );
			}
		} 
		catch ( SQLException e ) {} 
		catch ( JSONException e ) {}
		catch ( IOException e) {} 
		catch ( TemplateException e ) {}
		
		try {
			return processHTML( "error.ftl", data );
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} 
	}
	
	///////////////////////////////////
	
	private Representation presentSource( Form form ) 
	throws SQLException, JSONException, TemplateException, IOException {

		//-- pack all the data together ready for html parsing
		data.put( "user", user );
		data.put( "source", source );
		data.put( "subscription", subscription );
		
		if ( 
			subscription != null && 
			( subscription.hasStatus( DSSub.Status.COMPLETED ) ||
			  subscription.hasStatus( DSSub.Status.RESPONDED ) ) 
			) 
		{
			int page = 0;
			try { page = Integer.parseInt( form.getFirstValue( "page" ) );
			} catch ( NumberFormatException  e ) {}

			//-- calculate pagination stats
			Paginator p = new Paginator( 
				page, 
				DSCatalog.db.fetchUpdateTotal( user.getJid(), subscription.getNamespace() ),
				10,
				"jid=" + user.getJid() + "&namespace=" + subscription.getNamespace()
			);
			//-- fetch the appropriate updates to display
			ArrayList< DSUpdate > updates = DSCatalog.db.fetchUpdates(
				user.getJid(),
				subscription.getNamespace(),
				p.limit, 
				p.offset 
			);
			data.put( "updates", updates );
			data.put( "paginator", p );
		}

		return processHTML( "source_history.ftl", data );
	}

}