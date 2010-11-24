package datasphere.catalog.xmpp;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

public class DSNickname implements PacketExtension {

	private String nick;
	public static String namespace = "http://jabber.org/protocol/nick";
	public static String elementname = "nick";

	/////////////////////////////////////
	
	public DSNickname( ) {
		super();
	}
	
	/////////////////////////////////////
	
	@Override
	public String getElementName() {
		return DSNickname.elementname;
	}

	/////////////////////////////////////
	
	@Override
	public String getNamespace() {
		return DSNickname.namespace;
	}

	/////////////////////////////////////
	
	@Override
	public String toXML() {
		
		StringBuffer buf = new StringBuffer();
		
		buf .append( "<" )
			.append( getElementName() )
			.append( " xmlns=\"" )
			.append( getNamespace() )
			.append( "\">" );
		
		if ( nick != null )
			buf.append( this.nick );
		
		buf .append( "</" )
			.append( getElementName() )
			.append( ">" );
		
		return buf.toString();
	}
	
	/////////////////////////////////////
	
	public void setName( String nick ) {
		this.nick = nick;
	}
	
	/////////////////////////////////////
	
	public String getNick() {
		return nick;
	}

	/////////////////////////////////////
	
	public static class Provider implements PacketExtensionProvider{

		@Override
		public PacketExtension parseExtension( XmlPullParser parser ) 
		throws Exception {
			
			DSNickname nick = new DSNickname();
			
			int eventType = parser.next();
			if ( eventType == XmlPullParser.TEXT )
				nick.setName( parser.getText() );
			else 
				nick.setName( null );
			return nick;
		}

	}

}
