package sun.awt;

import java.awt.BufferCapabilities;
import java.awt.ImageCapabilities;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.VolatileImage;
import java.awt.image.WritableRaster;
import java.awt.geom.AffineTransform;
import java.awt.Rectangle;
import java.awt.Transparency;

import sun.java2d.SurfaceData;
import sun.java2d.loops.RenderLoops;
import sun.java2d.loops.SurfaceType;
import sun.java2d.loops.CompositeType;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;

import sun.awt.haiku.BVolatileImage;

/**
 * This is an implementation of a GraphicsConfiguration object for a 
 * single Haiku Workspace. 
 *
 * @author Bryan Varner
 * @see GraphicsEnvironment
 * @see GraphicsDevice
 */
public class HaikuGraphicsConfig extends GraphicsConfiguration {
	HaikuGraphicsDevice screen;
	int visual; // workspace number
	
	ColorModel colorModel;
	
	private static BufferCapabilities bufferCaps;
	private static ImageCapabilities imageCaps;
	
	public RenderLoops solidloops;
	
	/**
	 * @deprecated as of JDK version 1.3
	 * replaced by <code>getConfig()</code>
	 */
	public HaikuGraphicsConfig(GraphicsDevice device, int visualnum) {
		this.screen = (HaikuGraphicsDevice)device;
		this.visual = visualnum;
		
		colorModel = new DirectColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), 
				32, 0x00ff0000, 0x0000ff00, 0x000000ff, 0x00, false, DataBuffer.TYPE_INT);
	}
	
	/**
	 * @param device The parent device of this configuration
	 * @param pixFormatID The workspaceID (visualnum in constructor) of this configuration
	 */
	public static HaikuGraphicsConfig getConfig(HaikuGraphicsDevice device,
												int pixFormatID)
	{
		return new HaikuGraphicsConfig(device, pixFormatID);
	}
	
	public GraphicsDevice getDevice() {
		return screen;
	}
	
	public BufferedImage createCompatibleImage(int width, int height) {
		ColorModel model = getColorModel();
		WritableRaster raster = model.createCompatibleWritableRaster(width, height);
		return new BufferedImage(model, raster, model.isAlphaPremultiplied(), null);
	}
	
	public VolatileImage createCompatibleVolatileImage(int width, int height) {
		return (VolatileImage)(new BVolatileImage(this, width, height));
	}
	
	public BufferedImage createCompatibleImage(int width, int height, 
												int transparency)
	{
/*		switch(transparency) {
			case Transparency.OPAQUE:
				return createCompatibleImage(width, height);
			case Transparency.BITMASK: {
				// Fallback to translucent transparency, and see what this breaks!
				System.out.println("HaikuGrpahicsConfig.createCompatibleImage()\n" + 
						"BITMASK TRANSPARENCY SEPECIFIED, FALLING BACK TO TRANSLUCENT.");
				ColorModel cm = getColorModel(transparency);
				WritableRaster wr = cm.createCompatibleWritableRaster(width, height);
				return new HaikuPeerlessImage(cm, wr, cm.isAlphaPremultiplied(), Transparency.BITMASK);
			}
			case Transparency.TRANSLUCENT: {
				System.out.println("CREATE COMPATIBLE TRANSLUCENT\n");
				ColorModel cm = getColorModel(transparency);
				WritableRaster wr = cm.createCompatibleWritableRaster(width, height);
				return new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);
			}
			default:
				throw new IllegalArgumentException("Unknown transparency type " +
													transparency);
		}
*/
		return createCompatibleImage(width, height);
	}
	
	public ColorModel getColorModel() {
		return colorModel;
	}
	
	public ColorModel getColorModel(int transparency) {
		return colorModel;
	}
	
	public AffineTransform getDefaultTransform() {
		return new AffineTransform();
	}
	
	public AffineTransform getNormalizingTransform() {
		double xscale = getXResolution() / 72.0;
		double yscale = getYResolution() / 72.0;
		
		System.out.println("getNormalizingTransform()!!!");
		return new AffineTransform(xscale, 0.0, 0.0, yscale, 0.0, 0.0);
	}
	
	public Rectangle getBounds() {
		return new Rectangle(0, 0, getWidth(visual), getHeight(visual));
	}
	
	/**
	 * Gets the width of the given workspace
	 */
	public native int getWidth(int workspace);
	
	/**
	 * Gets the height of the given workspace
	 */
	public native int getHeight(int workspace);
	
	/** Hook functions to get the display resolution. 
	 * Future OSBOS implementations should investigate having OS-level 
	 * support for this. This is sorta important for WYSIWYG.
	 * In that case, these would become native methods.
	 */
	private double getXResolution() {
		return 72.0;
	}
	
	/** Hook functions to get the display resolution. 
	 * Future OSBOS implementations should investigate having OS-level 
	 * support for this. This is sorta important for WYSIWYG.
	 * In that case, these would become native methods.
	 */
	private double getYResolution() {
		return 72.0;
	}
	
	public synchronized RenderLoops getSolidLoops(SurfaceType stype) {
		if (solidloops == null) {
			solidloops = SurfaceData.makeRenderLoops(SurfaceType.OpaqueColor,
								CompositeType.SrcNoEa, stype);
		}
		return solidloops;
	}
}
