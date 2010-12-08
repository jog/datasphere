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
import datasphere.catalog.DSSub;
import datasphere.dataware.DSFormatException;
import datasphere.dataware.DSUpdate;
import freemarker.template.TemplateException;

public class UserHistoryPage 
extends DSServerResource { 
	
	private DSClient user;  
	private String jid;
	
	///////////////////////////////////
	
	@Get()
	public Representation doGet() {
		
		try {
			//-- setup a data object ready for HTML processing
			Map< String, Object > data = new HashMap< String, Object >();
			
			//-- grab relevent get information from the request
			int page = 0;
			try {
				Form form = getRequest().getResourceRef().getQueryAsForm();
				this.jid = form.getFirstValue( "jid" );
				page = Integer.parseInt( form.getFirstValue( "page" ) );
			} catch ( NumberFormatException  e ) {}
			
			//-- fetch client information
			this.user = DSCatalog.db.fetchClient( jid );

			if ( user == null ) {
				return processHTML( "error.ftl", data );
			}
			
			//-- fetch information about active subscriptions for the side panel
			data.put( "activeSubs",  
				DSCatalog.db.fetchSources( 
					user.getJid(), 
					DSSub.Status.COMPLETED ,
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
			
			//-- calculate pagination stats
			Paginator p = new Paginator( 
				page, 
				DSCatalog.db.fetchUpdateTotal( jid ),
				10,
				"jid=" + user.getJid() 
			);
			
			//-- fetch the appropriate updates to display
			ArrayList< DSUpdate > updates = DSCatalog.db.fetchUpdates(
				jid,
				null,
				p.limit, 
				p.offset
			);

			//-- pack all the data ready for HTML processing
			data.put( "user", user );
			data.put( "updates", updates );
			data.put( "paginator", p );
			
			//-- and finallly process
			return processHTML( "user_history.ftl", data );
			
		} catch ( SQLException e ) {
			e.printStackTrace();
			return null;
			
		} catch ( JSONException e) {
			e.printStackTrace();
			return null;
			
		} catch ( IOException e) {
			e.printStackTrace();
			return null;
			
		} catch ( TemplateException e ) {
			e.printStackTrace();
			return null;
		
		} catch ( DSFormatException e ) {
			e.printStackTrace();
			return null;
		}

	}
}