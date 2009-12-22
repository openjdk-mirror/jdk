package sun.awt.haiku;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.Hashtable;
import java.util.Iterator;

import sun.awt.image.AcceleratedOffScreenImage;
import sun.awt.image.OffScreenImage;
import sun.java2d.SurfaceData;
import sun.java2d.SunGraphics2D;
import sun.java2d.loops.CompositeType;
import sun.awt.HaikuGraphicsEnvironment;
import sun.awt.HaikuGraphicsConfig;

public class BOffScreenImage extends AcceleratedOffScreenImage {
	static {
		accelerationEnabled = false;
	}
	
	public BOffScreenImage(Component c, ColorModel cm, 
			WritableRaster raster, boolean isRasterPremultiplied)
	{
		super(c, cm, raster, isRasterPremultiplied);
	}
	
	protected boolean isValidHWSD(GraphicsConfiguration gc) {
		return false;
	}
	
	protected ColorModel getDeviceColorModel() {
		return c.getColorModel();
	}
	
	public void initAcceleratedBackground(GraphicsConfiguration gc,
			int width, int height)
	{
		return;
	}
	
	protected boolean operationSupported(CompositeType comp, Color bgColor, boolean scale) {
		return false;
	}
	
	protected boolean destSurfaceAccelerated(SurfaceData destSD) {
		return false;
	}
}
