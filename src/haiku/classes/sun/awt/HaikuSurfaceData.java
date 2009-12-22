package sun.awt;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsConfiguration;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;

import sun.awt.SunHints;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;
import sun.java2d.pipe.Region;
import sun.java2d.pipe.PixelToShapeConverter;
import sun.java2d.loops.GraphicsPrimitive;
import sun.java2d.loops.SurfaceType;
import sun.java2d.loops.CompositeType;
import sun.java2d.loops.RenderLoops;
import sun.java2d.loops.XORComposite;
import sun.awt.HaikuGraphicsConfig;
import sun.awt.HaikuGraphicsDevice;
import sun.awt.HaikuRenderer;
import sun.awt.image.PixelConverter;
import sun.awt.haiku.BComponentPeer;

/**
 * Handles setting up a SurfaceData object for a Haiku Component.
 * 
 * SurfaceData's describe a surface for the Drawing "pipes", a.k.a
 * renderers.
 *
 * HaikuRenderer is a pipe for drawing with offscreen BBitmap / BViews
 * to the SurfaceData offscreen buffer.
 */
public class HaikuSurfaceData extends SurfaceData {
	BComponentPeer peer;
	private HaikuGraphicsConfig graphicsConfig;
	private RenderLoops solidloops;
	
	public static final String 
		DESC_INT_ARGB_BE = "Integer ARGB Haiku";
	
	public static final SurfaceType IntRgbBe = 
		SurfaceType.IntArgb.deriveSubType(DESC_INT_ARGB_BE);
	
	private static native void initIDs(Class xorComp);
	
	static {
		if (!GraphicsEnvironment.isHeadless()) {
			initIDs(XORComposite.class);
			//HaikuBlitLoops.register();
		}
	}
	
	// Haiku Specific Rendering pipes.
	protected static HaikuRenderer bePipe;
	protected static PixelToShapeConverter beTxPipe;
	
	static {
		bePipe = new HaikuRenderer();
		if (GraphicsPrimitive.tracingEnabled()) {
			bePipe = bePipe.traceWrap();
		}
		beTxPipe = new PixelToShapeConverter(bePipe);
	}

	/**
	 * Constructs a new SurfaceData object for the peer with the given type.
	 */
	public HaikuSurfaceData(BComponentPeer peer, SurfaceType sType, int numBuffers) {
		super(sType, peer.getColorModel());
		ColorModel cm = peer.getColorModel();
		this.peer = peer;
		int rMask = 0, gMask = 0, bMask = 0;
		int depth;
		depth = cm.getPixelSize();
		if (cm instanceof DirectColorModel) {
			DirectColorModel dcm = (DirectColorModel)cm;
			rMask = dcm.getRedMask();
			gMask = dcm.getGreenMask();
			bMask = dcm.getBlueMask();
		}
		this.graphicsConfig = (HaikuGraphicsConfig) peer.getGraphicsConfiguration();
		this.solidloops = graphicsConfig.getSolidLoops(sType);
		
		HaikuGraphicsDevice gd = (HaikuGraphicsDevice)graphicsConfig.getDevice();
		initOps(peer, depth, rMask, gMask, bMask, numBuffers, gd.getScreen());
	}
	
	/**
	 * Creates a new SurfaceData for the given peer.
	 */
	public static HaikuSurfaceData createData(BComponentPeer peer, int numBuffers) {
		SurfaceType sType = getSurfaceType(peer.getColorModel());
		return new HaikuSurfaceData(peer, sType, numBuffers);
	}
	
	public BComponentPeer getPeer() {
		return peer;
	}
	
	/**
	 * We only support one surface type at this time.
	 */
	public static SurfaceType getSurfaceType(ColorModel cm) {
		return IntRgbBe;
	}
	
	public Raster getRaster(int x, int y, int w, int h) {
		System.out.println("HaikuSurfaceData getRaster not implemented!\n");
		throw new InternalError("not implemented yet");
	}
	
	/**
	 * validatePipe checks the current properties of the SunGraphics2D object
	 * then sets the appropriate pipes for the given Graphics object.
	 * 
	 * This overrides the sun.java2d.SurfaceData's validatePipe which
	 * implements everything in the headless mode.
	 * 
	 * The goal of this function is to set this SurfaceData instance to use
	 * our native render pipes (to direct output to our peers) when appropriate.
	 */
	public void validatePipe(SunGraphics2D sg2d) {
		// IF antialias hinting is off 
		// AND we're painting in solid colors
		// AND EITHER compositeState is set to COPY OR XOR
		// AND we're NOT going to CLIP a SHAPE
		// THEN
		// use our pipe.
		// ELSE 
		// perform everything in software according to the superclass.
/*		if (sg2d.antialiasHint != SunHints.INTVAL_ANTIALIAS_ON &&
			sg2d.paintState == sg2d.PAINT_SOLIDCOLOR &&
			(sg2d.compositeState == sg2d.COMP_ISCOPY ||
				sg2d.compositeState == sg2d.COMP_XOR) &&
			sg2d.clipState != sg2d.CLIP_SHAPE)
		{
			sg2d.imagepipe = imagepipe;
			if (sg2d.transformState > sg2d.TRANSFORM_TRANSLATEONLY) {
				sg2d.drawpipe = beTxPipe;
				sg2d.fillpipe = beTxPipe;
			} else if (sg2d.strokeState != sg2d.STROKE_THIN) {
				sg2d.drawpipe = beTxPipe;
				sg2d.fillpipe = bePipe;
			} else {
				sg2d.drawpipe = bePipe;
				sg2d.fillpipe = bePipe;
			}
			sg2d.shapepipe = bePipe;
			// There's something in the other surfaceData's complaining about
			// no alternate text pipe. Personally, I think it's a bunch of hooey
			// especially when by default Haiku does antialiased text just fine.
			// TODO: Take a look at writing a HaikuTextRenderer.
			if (sg2d.textAntialiasHint != SunHints.INTVAL_TEXT_ANTIALIAS_ON) {
				sg2d.textpipe = solidTextRenderer;
			} else {
				sg2d.textpipe = aaTextRenderer;
			}
			// We use getRenderLoops() rather than setting solidloops
			// directly so that we get the appropriate loops in XOR mode.
			sg2d.loops = getRenderLoops(sg2d);
		} else {
*/			super.validatePipe(sg2d); // Fallback to the normal behavior.
//		}
	}
	
	public void lock() { }
	
	public void unlock() { }
	
	/** 
	 * Gets the loops to use when rendering.
	 * This is called by validatePipes to ensure the proper loops are set for XOR mode.
	 */
	public RenderLoops getRenderLoops(SunGraphics2D sg2d) {
		if (sg2d.paintState == sg2d.PAINT_SOLIDCOLOR &&
			sg2d.compositeState == sg2d.COMP_ISCOPY)
		{
			return solidloops;
		}
		return super.getRenderLoops(sg2d);
	}
	
	/**
	 * Gets the GraphicsConfiguration we're currently setup in.
	 */
	public GraphicsConfiguration getDeviceConfiguration() {
		return graphicsConfig;
	}
	
	private native void initOps(BComponentPeer peer, int depth, int redMask,
			 int greenMask, int blueMask, int numBuffers, int screen);
	
	
	public SurfaceData getReplacement() {
		return peer.getSurfaceData();
	}
	
	public Rectangle getBounds() {
		Rectangle r = peer.getBounds();
		r.x = r.y = 0;
		return r;
	}
	
    /**
     * Performs a copyarea within this surface.  Returns
     * false if there is no algorithm to perform the copyarea
     * given the current settings of the SunGraphics2D.
     */
	public boolean copyArea(SunGraphics2D sg2d,
			int x, int y, int w, int h, int dx, int dy)
	{
		return _copyArea(x, y, w, h, dx, dy);
	}
	private native boolean _copyArea(int x, int y, int w, int h, int dx, int dy);
	
	private native void _invalidate();
	
	public void invalidate() {
		if (isValid()) {
			_invalidate();
			super.invalidate();
		}
	}
	
	/**
	 * Called by HaikuGraphics2D at the end of a Graphics transaction.
	 * bounds is the smallest possible rectangle that needs repainted.
	 */ 
	public native void repaint(int x, int y, int w, int h);
	
    // This gets called when restoring the back buffer
    public native void restoreSurface();
    public native void flip(SurfaceData data);
}
