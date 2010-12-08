package datasphere.catalog.xmpp;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.imageio.ImageIO;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.packet.VCard;

import datasphere.dataware.DSFormatException;

public class DSVCard {

	private VCard vCard;
	private boolean hasAvatar = false;
	
    ///////////////////////////////
	
	public DSVCard( XMPPConnection connection, String jid ) 
	throws XMPPException, DSFormatException {
		vCard = new VCard();
		vCard.load( connection, jid );
		this.hasAvatar = ( vCard.getAvatar() != null );
		assertInvariance();
	}

    ///////////////////////////////
	
	public DSVCard(	
		String namespace,
		String name 
	) 
	throws DSFormatException {
		this.vCard = new VCard();
		this.vCard.setField( "FN", namespace );
		this.vCard.setNickName( name );
		assertInvariance();
	}
	
    ///////////////////////////////
	
	public DSVCard( VCard vCard ) 
	throws DSFormatException {
		this.vCard = vCard;
		this.hasAvatar = ( vCard.getAvatar() != null );
		assertInvariance();
	}
	
	///////////////////////////////
	
    private void assertInvariance() 
    throws DSFormatException {

    	if ( vCard == null )
    		throw new DSFormatException();
    	
    	if ( vCard.getField( "FN" ) == null )
    		throw new DSFormatException();
    	
    	if ( vCard.getNickName() == null )
    		throw new DSFormatException();
    	
	}

	///////////////////////////////

 	public String getNamespace() {
        return vCard.getField( "FN" );
	}
 	
	///////////////////////////////

 	public String toString() {
        return vCard.toString();
	}

	///////////////////////////////
 	
	public BufferedImage getAvatar() 
	throws IOException {
		return ImageIO.read( 
			new ByteArrayInputStream( 
				vCard.getAvatar() 
			)
		); 
	}

	///////////////////////////////

	public String getNickName() {
		return vCard.getNickName();
	}

	///////////////////////////////
	
	public Object getUrl() {
		return vCard.getField( "URL" );
	}

	///////////////////////////////

	public void setURL( String value ) {
		if ( value != null )
			vCard.setField( "URL", value );
	}
	
	///////////////////////////////
	
	public Object getDesc() {
		return vCard.getField( "DESC" );
	}
	
	///////////////////////////////

	public void setDesc( String value ) {
		if ( value != null )
			vCard.setField( "DESC", value );
	}
	
	///////////////////////////////
	
	public Object getOrgName() {
		return vCard.getOrganization();
	}
	
	///////////////////////////////

	public void setOrgName( String value ) {
		if ( value != null )
			vCard.setOrganizationUnit( value );
	}
	
	///////////////////////////////
	
	public Object getOrgUnit() {
		return vCard.getOrganization();
	}
	
	///////////////////////////////

	public void setOrgUnit( String value ) {
		if ( value != null )
			vCard.setOrganizationUnit( value );
	}
	
	///////////////////////////////

	public Boolean hasAvatar() {
		return hasAvatar;
	}

	///////////////////////////////

	public void setAvatar( boolean value ) {
		this.hasAvatar = value;
	}
	
	///////////////////////////////

	public String getAvatarName() {
		
		try {
			if ( hasAvatar )
				return URLEncoder.encode( getNamespace(), "UTF-8" ) + ".png";
		} catch (UnsupportedEncodingException e) {}
		
		return "blank.jpg";
	}
	
}
