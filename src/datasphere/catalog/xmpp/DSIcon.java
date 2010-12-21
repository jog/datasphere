package datasphere.catalog.xmpp;


import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
 
public class DSIcon extends JPanel {

	private static final long serialVersionUID = 3854080591614680815L;
	private BufferedImage image;
 
    ///////////////////////////////
	
    DSIcon( BufferedImage src, int width, int rounding ) throws Exception {
        image = scaleImage( src, width );
        image = roundCorners( image, rounding );
        image = buttonImage( image );
    }

    ///////////////////////////////
    
    private BufferedImage roundCorners( BufferedImage src, int r ) {
    	int w = src.getWidth();
        int h = src.getHeight();
        int type = BufferedImage.TYPE_INT_ARGB;
        BufferedImage dst = new BufferedImage( w, h, type);
        Graphics2D g2 = dst.createGraphics();
        RoundRectangle2D r2 = new RoundRectangle2D.Double( 0, 0, w, h, r, r );
        g2.setClip( r2 );
        g2.drawImage( src, 0, 0, this );
        g2.dispose();
        return dst;
    }

    ///////////////////////////////
    
    public BufferedImage scaleImage( BufferedImage image, int width )
    throws Exception {
    	 
    	Image outputImage = image.getScaledInstance(
    		width,
    		-1,
    		Image.SCALE_SMOOTH
    	);
    	
    	BufferedImage bffImg = new BufferedImage(
    		width,
    		image.getHeight( null ) * width / image.getWidth( null ),
    		BufferedImage.TYPE_3BYTE_BGR
    	);
    	
    	Graphics offg = bffImg.createGraphics();
    	offg.drawImage ( outputImage, 0, 0, null );
    	return bffImg;
    }

    ///////////////////////////////
    
    public BufferedImage buttonImage( BufferedImage image ) 
    throws IOException {
	    BufferedImage im2 = ImageIO.read( getClass().getResourceAsStream( "/resources/images/icons/button.png" ) );
	    Graphics2D g = image.createGraphics();
	    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
	    g.drawImage(im2, ( image.getWidth()-im2.getWidth())/2, ( image.getHeight()-im2.getHeight())/2, null);
	    g.dispose();
	    return image;
    }

    ///////////////////////////////
    
    public BufferedImage getImage() {
    	return image;
    }
}

