package datasphere.catalog;

public class DSMain {
	
	/**
	 * @param args 
	 * @throws Exception 
	 */
	public static void main( String[] args ) 
	throws Exception {

		//-- create a database connection
		DSDataManager dbm = new DSDataManager( 
			"jdbc:mysql://127.0.0.1:3306/datasphere", 
			new com.mysql.jdbc.Driver() 
		);
		
		//-- create an instance of the catalog
		DSCatalog catalog = new DSCatalog( dbm );
		catalog.setStartable( DSCatalog.ALL_PROTOCOLS );
		catalog.setArgs( args );
		
		//-- and finally start it running
		catalog.start();
	}	
}
