package datasphere.catalog;

public class DSSource {

	private String namespace;
	private String commonName;
	private String url;
	private String icon;
	private static String DEFAULT_SOURCE_ICON = "blank.jpg";
	
	//////////////////////////////////
	
	public DSSource( String namespace, String commonName, String url, String icon ) {
		this.namespace = namespace;
		this.commonName = commonName;
		this.url = url;
		
		this.icon = ( icon == null ) ? DEFAULT_SOURCE_ICON : icon;
	}
	
	//////////////////////////////////
	
	public String getNamespace() {
		return namespace;
	}
	
	//////////////////////////////////
	
	public void setNamespace( String namespace ) {
		this.namespace = namespace;
	}
	
	//////////////////////////////////
	
	public String getCommonName() {
		return commonName;
	}
	
	//////////////////////////////////
	
	public void setCommonName( String commonName ) {
		this.commonName = commonName;
	}
	
	//////////////////////////////////
	
	public String getUrl() {
		return url;
	}
	
	//////////////////////////////////
	
	public void setUrl( String url ) {
		this.url = url;
	}
	
	//////////////////////////////////
	
	public String getIcon() {
		return icon;
	}
	
	//////////////////////////////////
	
	public void setIcon( String icon ) {
		this.icon = icon;
	}
	
	//////////////////////////////////

	@Override
	public String toString() {
		return "DSSource [commonName=" + commonName + ", icon=" + icon
				+ ", namespace=" + namespace + ", url=" + url + "]";
	}	
}
