package datasphere.catalog.http;

public class Paginator {
	
	public int currentPage;
	public int totalPages;
	public int totalUpdates;
	public int offset;
	public int finalPage;
	public int limit;
	public String parameters;
	
	/**
	 * 
	 * @param currentPage
	 * @param totalUpdates
	 * @param limit
	 */
	public Paginator( 
		int currentPage, 
		int totalUpdates, 
		int limit,
		String jid 
		) {
		this.currentPage = currentPage;
		this.totalUpdates = totalUpdates;
		this.limit = limit;
		this.parameters = jid;
		
		this.offset = currentPage * limit;
		this.finalPage = totalUpdates / limit;

		this.totalPages =
			(totalUpdates % limit == 0)
			? finalPage 
			: finalPage + 1;
		
		assertInvariance();
	}


	/**
	 * 	confirm that all is in order with our data types
	 */
	public void assertInvariance() {
		if ( offset > totalUpdates ) {
			currentPage = finalPage;
			offset = finalPage * limit;
		}
	}  
	
	/**
	 * 
	 */
	public String generateHTML() {
		String str = "<ul class='pagination'>";
		if  ( totalPages == 0 ) return "";
		else {
			if  ( currentPage > 0 )
				str += "<li class='pageDir'><a href='?page=" + (currentPage - 1) + "&" + parameters + "'>&#171; prev </a></li>";
			
			str += numberHTML(0);
				
			if ( currentPage > 4 )
				str+="<li class='dots'>...</li>";

			int start = currentPage - 3;
			if ( start < 1 ) start = 1;
			
			int end = start + 6;
			if ( end >= finalPage ) 
				end = finalPage - 1;
			
			for( int i = start; i <= end; i++ ) 
				str += numberHTML( i );
		
		 	if ( finalPage - currentPage > 4 ) 
		 		str+="<li class='dots'>...</li>";
		
		 	if ( finalPage > 0 )
		 		str += numberHTML( finalPage );
		
		 	if ( currentPage < finalPage )
				str += "<li class='pageDir'><a href='?page=" + (currentPage + 1) + "&" + parameters + "'>next &#187;</a></li>";
		}
		
		str += "</ul>";
		return str;
	}
	
	private String numberHTML( int i ) {
		if ( i == currentPage )
			return "<li class='currentNum'>" + ( i + 1 ) + "</li>";
		else
			return "<li class='pageNum'><a href='?page=" + i + "&" + parameters + "'>" + (i + 1) + "</a></li>";
	}
}