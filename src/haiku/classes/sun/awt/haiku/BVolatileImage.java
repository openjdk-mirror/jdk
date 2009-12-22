package sun.awt.haiku;

import java.awt.*;
import java.awt.image.BufferedImage;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;
import sun.awt.image.BufImgSurfaceData;
import sun.awt.image.SunVolatileImage;
import sun.awt.HaikuGraphicsEnvironment;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import sun.awt.HaikuGraphicsConfig;

/**
 * Haiku platform implementation of the VolatileImage class.
 * This implementation users a software-based SurfaceData object
 * (BufImgSurfaceData).
 */
public class BVolatileImage extends SunVolatileImage {
	static {
		accelerationEnabled = false;
	}
	
	/**
	 * Called by constructors to initialize common functionality
	 */
	private void initialize() {	}
	
	/**
	 * Constructor for Haiku-based VolatileImage using Component
	 */	
	public BVolatileImage(Component c, int width, int height) {
		super(c, width, height);
		initialize();
	}
	
	/**
	 * Constructor for Haiku-based VolatileImage using Component
	 */	
	public BVolatileImage(Component c, int width, int height, Object context) {
		super(c, width, height, context);
		initialize();
	}
	
	/**
	 * Constructor for win32-based VolatileImage using GraphicsConfiguration
	 */
	public BVolatileImage(GraphicsConfiguration graphicsConfig, int width, int height) {
		super(graphicsConfig, width, height);
		initialize();
	}
}
