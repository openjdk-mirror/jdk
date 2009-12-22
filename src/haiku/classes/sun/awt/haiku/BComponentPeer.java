package sun.awt.haiku;

import java.awt.*;
import java.awt.peer.*;
import java.awt.image.MemoryImageSource;
import java.awt.image.WritableRaster;
import java.awt.image.VolatileImage;
import sun.awt.RepaintArea;
import sun.awt.AppContext;
import sun.awt.image.ImageRepresentation;
import sun.awt.image.OffScreenImage;
import sun.awt.image.SunVolatileImage;
import java.awt.image.BufferedImage;
import java.awt.image.ImageProducer;
import java.awt.image.ImageObserver;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.event.PaintEvent;
import sun.awt.HaikuGraphicsConfig;
import sun.awt.HaikuGraphicsDevice;
import sun.awt.HaikuSurfaceData;
import sun.java2d.InvalidPipeException;
import sun.java2d.SunGraphics2D;

import java.awt.dnd.DropTarget;
import java.awt.dnd.peer.DropTargetPeer;

import sun.awt.DebugHelper;

public abstract class BComponentPeer extends BObjectPeer
	implements ComponentPeer, DropTargetPeer
{
	private static final DebugHelper dbg = DebugHelper.create(BComponentPeer.class);
	
	protected HaikuSurfaceData surfaceData;
	protected HaikuGraphicsConfig haikuGraphicsConfig;
	protected RepaintArea paintArea;
	protected boolean isLayouting = false;
	protected boolean paintPending = false;
	private int oldWidth = -1;
	private int oldHeight = -1;
	private int numBackBuffers = 0;
	private BVolatileImage backBuffer = null;

	// accessors

	public HaikuSurfaceData getSurfaceData() {
		return surfaceData;
	}

	public Rectangle getBounds() {
		return ((Component)target).getBounds();
	}
	
	/*
	 *
	 * Creation and Initialization
	 *
	 */

	BComponentPeer(Component target) {
		this.target = target;
		this.paintArea = new RepaintArea();
		
		Container parent = BToolkit.getNativeContainer(target);
		BComponentPeer parentPeer = (BComponentPeer) BToolkit.targetToPeer(parent);
		create(parentPeer);
		
		this.surfaceData = HaikuSurfaceData.createData(this, numBackBuffers);
		initialize();
	}
	
	abstract void create(BComponentPeer parent);
	
	void initialize() {
		initZOrderPosition();
		
		if (!((Component)target).isVisible()) {
			hide(); // Should be constructed hidden. This affects the surface data.
		}
		
		Color fg = ((Component)target).getForeground();
		if (fg != null) {
			setForeground(fg);
		}
		
		Color bg = ((Component)target).getBackground();
		if (bg != null) {
			setBackground(bg);
		}
		
		Font f = ((Component)target).getFont();
		if (f != null) {
			setFont(f);
		}
		if (! ((Component)target).isEnabled()) {
			disable();
		}
		
		Rectangle r = ((Component)target).getBounds();
		setBounds(r.x, r.y, r.width, r.height);
		
		if (((Component)target).isVisible()) {
			show(); // if we're supposed to be visible, then by golly let's show ourselves.
		}
		
	}
	public void initZOrderPosition() {
		Container p = ((Component)target).getParent();
		BComponentPeer peerAbove = null;
		
		if (p != null) {
			Component children[] = p.getComponents();
			for (int i = 0; i < children.length; i++) {
				if (children[i] == target) {
					break;
				} else {
					Object cpeer = BToolkit.targetToPeer(children[i]);
					if (cpeer != null && 
						!(cpeer instanceof java.awt.peer.LightweightPeer))
						peerAbove = (BComponentPeer)cpeer;
				}
			}
		}
		setZOrderPosition(peerAbove);
	}
    private native void setZOrderPosition(BComponentPeer compAbove);
	
	/*
	 *
	 * ComponentPeer interface
	 *
	 */
	
	public boolean isObscured() {
		return true;
	}
	// public native boolean isObscured();

	public boolean canDetermineObscurity() {
		// Haiku Cannot determine obscurity though the default API.
		// We may be able to hack this in using BDirectWindow and 
		// Creative intersections of the clipping region with the
		// bounds of the component within the toplevel window.
		return false;
	}

	public void setVisible(boolean b) {
		if (b) {
			show();
		} else {
			hide();
		}
	}

	public void setEnabled(boolean b) {
		if (b) {
			enable();
		} else {
			disable();
		}
	}

	//
	// Component Peer: Painting
	
	public void paint(Graphics g) {
		((Component)target).paint(g);
	}
	
	public void repaint(long tm, int x, int y, int width, int height) {
	}
	
	public void print(Graphics g) {
		Component comp = (Component)target;
		
		// windows bands the image into smaller chunks... 
		// motif has something similar to the following.
		comp.print(g);
	}
	
	public void setBounds(int x, int y, int width, int height) {
		// Should set paintPending before reshape to prevent
		// thread race between paint events
		// Native components do redraw after resize
		paintPending = (width != oldWidth) || (height != oldHeight);
		
		reshape(x, y, width, height);
		if ((width != oldWidth) || (height != oldHeight)) {
			// Only recreate surfaceData if this setBounds is called
			// for a resize; a simple move should not trigger a recreation
			try {
				replaceSurfaceData();
			} catch (InvalidPipeException e) {
				// REMIND : what do we do if our surface creation failed?
				System.err.println("FAILED TO CREATE SURFACEDATA!");
			}
			oldWidth = width;
			oldHeight = height;
		}
		
		serialNum++;
	}
	public int serialNum = 0;
	
	public void handleEvent(AWTEvent e) {
		int id = e.getID();
		
		switch(id) {
			case PaintEvent.PAINT:
				// Got native painting
				paintPending = false;
				// Fallthrough to next statement
			case PaintEvent.UPDATE:
				// Skip all painting while layouting and all UPDATEs
				// while waiting for native paint
				if (!isLayouting && !paintPending) {
					paintArea.paint(target, shouldClearRectBeforePaint());
				}
				return;
			default:
				break;
		}
		
		// call the native code
		nativeHandleEvent(e);
	}
	private native void nativeHandleEvent(AWTEvent e);
	
	public void coalescePaintEvent(PaintEvent e) {
		Rectangle r = e.getUpdateRect();
		paintArea.add(r, e.getID());
		
		if (dbg.on) {
			switch(e.getID()) {
				case PaintEvent.UPDATE:
					dbg.println("BCP coalescePaintEvent : UPDATE : add : x = " +
				r.x + ", y = " + r.y + ", width = " + r.width + ",height = " + r.height);
				return;
			case PaintEvent.PAINT:
					dbg.println("BCP coalescePaintEvent : PAINT : add : x = " +
				r.x + ", y = " + r.y + ", width = " + r.width + ",height = " + r.height);
				return;
			}
		}
	}

	//
	// Component Peer: Layout and Location
	
	public native Point getLocationOnScreen();
	
	public Dimension getPreferredSize() {
		return getMinimumSize();
	}
	
	public Dimension getMinimumSize() {
		return ((Component)target).getSize();
	}
	
	//
	// Component Peer: Color Model, Toolkit, Graphics, Font Metrics
	
	// This will return null for Components not yet added to a Container
	public ColorModel getColorModel() {
		GraphicsConfiguration gc = getGraphicsConfiguration();
		if (gc != null) {
			return gc.getColorModel();
		} else {
			return null;
		}
	}
	
	// This will return null for Components not yet added to a Container
	public ColorModel getColorModel(int transparency) {
		GraphicsConfiguration gc = getGraphicsConfiguration();
		if (gc != null) {
			return gc.getColorModel(transparency);
		} else {
			return null;
		}
	}
	
	public java.awt.Toolkit getToolkit() {
		return Toolkit.getDefaultToolkit();
	}
	
	public synchronized Graphics getGraphics() {
		if (!isDisposed()) {
			Component target = (Component)this.target;
			
			/* fix for bug 4746122. Color and Font shouldn't be null */
			Color bgColor = target.getBackground();
			if (bgColor == null) {
				bgColor = SystemColor.window;
				target.setBackground(bgColor);
			}
			Color fgColor = target.getForeground();
			if (fgColor == null) {
				fgColor = SystemColor.windowText;
				target.setForeground(fgColor);
			}
			Font font = target.getFont(); 
			if (font == null) {
				font = defaultFont;
				target.setFont(font);
			}
			//return new SunGraphics2D(surfaceData, fgColor, bgColor, font);
			return new HaikuGraphics2D(surfaceData, fgColor, bgColor, font);
		}
		return null;
	}
	
	public FontMetrics getFontMetrics(Font font) {
		return BFontMetrics.getFontMetrics(font);
	}
	
	//
	// Component Peer: Disposal
	
	protected void disposeImpl() {
		HaikuSurfaceData oldData = surfaceData;
		surfaceData = null;
		oldData.invalidate();
		// remove from updater before calling targetDisposedPeer
		BToolkit.targetDisposedPeer(target, this);
		_dispose();
	}
	private synchronized native void _dispose();
	
	//
	// Component Peer: Display Settings
	
	public synchronized void setForeground(Color c) {
		_setForeground(c.getRGB());
	}
	private native void _setForeground(int rgb);

	public synchronized void setBackground(Color c) {
		_setBackground(c.getRGB());
	}
	private native void _setBackground(int rgb);
	
	public synchronized void setFont(Font f) {
		_setFont(f);
	}
	private native void _setFont(Font f);

	//
	// Component Peer: Cursor and Focus
	
	public final void updateCursorImmediately() {
		BGlobalCursorManager.getCursorManager().updateCursorImmediately();
	}
	
	public boolean requestFocus(Component lightweightChild, boolean temporary,
								boolean focusedWindowChangeAllowed, long time)
	{
		if (processSynchronousLightweightTransfer((Component)target, lightweightChild, temporary, 
													focusedWindowChangeAllowed, time))
		{
			return true;
		} else {
			return _requestFocus(lightweightChild, temporary, focusedWindowChangeAllowed, time);
		}
	}
	private native static boolean processSynchronousLightweightTransfer(Component heavyweight, Component descendant,
																boolean temporary, boolean focusedWindowChangeAllowed,
																long time);
	private native boolean _requestFocus(Component lightweightChild, boolean temporary,
	                                     boolean focusedWindowChangeAllowed, long time);
	
	public boolean isFocusable() {
		return false;
	}

	//
	// Component Peer: Image	

	public Image createImage(ImageProducer producer) {
		return new BImage(producer);
	}
	
	public Image createImage(int width, int height) {
		ColorModel model = getColorModel(Transparency.OPAQUE);
		WritableRaster wr = model.createCompatibleWritableRaster(width, height);
		return new BOffScreenImage((Component)target, model, wr,
									model.isAlphaPremultiplied());
	}
	
	public VolatileImage createVolatileImage(int width, int height) {
		return new BVolatileImage((Component)target, width, height);
	}
	
	public boolean prepareImage(Image img, int w, int h, ImageObserver o) {
		return BToolkit.prepareScrImage(img, w, h, o);
	}
	
	public int checkImage(Image img, int w, int h, ImageObserver o) {
		return BToolkit.checkScrImage(img, w, h, o);
	}
	
	// Return the GraphicsConfiguration associated with this peer, either
	// the locally stored HaikuGraphicsConfig or that of the target Component.
	public GraphicsConfiguration getGraphicsConfiguration() {
		if (haikuGraphicsConfig != null) {
			return haikuGraphicsConfig;
		} else {
			// we don't need a treelock here, since
			// Component.getGraphicsConfiguration() gets itself.
			return ((Component)target).getGraphicsConfiguration();
		}
	}

	public boolean handlesWheelScrolling() {
		// should this be cached?
		return nativeHandlesWheelScrolling();
	}
	private native boolean nativeHandlesWheelScrolling();
	
	//
	// Component Peer: back buffers

	public synchronized void createBuffers(int numBuffers, BufferCapabilities caps)
		throws AWTException
	{
		assertFullScreen();
		// Re-create the primary surface
		this.numBackBuffers = (numBuffers - 1);
		
		try {
			replaceSurfaceData();
		} catch (InvalidPipeException e) {
			throw new AWTException(e.getMessage());
		}
	}
	
    public Image getBackBuffer() {
        if (backBuffer == null) {
            throw new IllegalStateException("Buffers have not been created");
        }
        return backBuffer;
    }

    public synchronized void flip(BufferCapabilities.FlipContents flipAction) {
        if (backBuffer == null) {
            throw new IllegalStateException(
                "Buffers have not been created");
        }
        Component target = (Component)this.target;
        int width = target.getWidth();
        int height = target.getHeight();
        if (flipAction == BufferCapabilities.FlipContents.COPIED) {
            Graphics g = target.getGraphics();
            g.drawImage(backBuffer, 0, 0, width, height,
                target);
            g.dispose();
        } else {
            try {
                surfaceData.flip(backBuffer.getSurfaceData());
            } catch (sun.java2d.InvalidPipeException e) {
                return; // Flip failed
            }
            if (flipAction ==
                BufferCapabilities.FlipContents.BACKGROUND) {
                Graphics g = backBuffer.getGraphics();
                g.setColor(target.getBackground());
                g.fillRect(0, 0, width, height);
                g.dispose();
            }
        }
    }

	public synchronized void destroyBuffers() {
		disposeBackBuffer();
		numBackBuffers = 0;
	}
	
	private synchronized void disposeBackBuffer() {
		if (backBuffer == null) {
			return;
		}
		backBuffer = null;
	}
	
    /**
     * DEPRECATED:  Replaced by getPreferredSize().
     */
	public Dimension minimumSize() {
		return getMinimumSize();
	}

    /**
     * DEPRECATED:  Replaced by getMinimumSize().
     */
	public Dimension preferredSize() {
		return getPreferredSize();
	}

    /**
     * DEPRECATED:  Replaced by setVisible(boolean).
     */
	public void show() {
		Dimension s = ((Component)target).getSize();
		oldHeight = s.height;
		oldWidth = s.width;
		_show();
	}
	private native void _show();
	
    /**
     * DEPRECATED:  Replaced by setVisible(boolean).
     */
	public synchronized void hide() {
		_hide();
	}
	private native void _hide();
		
    /**
     * DEPRECATED:  Replaced by setEnabled(boolean).
     */
	public native void enable();

    /**
     * DEPRECATED:  Replaced by setEnabled(boolean).
     */
	public native void disable();

    /**
     * DEPRECATED:  Replaced by setBounds(int, int, int, int).
     */
	public synchronized void reshape(int x, int y, int width, int height) {
		_reshape(x, y, width, height);
	}
	private synchronized native void _reshape(int x, int y, int width, int height);
	
	
	/*
	 *
	 * DropTargetPeer interface
	 *
	 */
	
	// register a DropTarget with this native peer
	public synchronized void addDropTarget(DropTarget dt) {
		_addDropTarget(dt);
	}
	private native void _addDropTarget(DropTarget dt);
	
	// unregister a DropTarget with this native peer
	public synchronized void removeDropTarget(DropTarget dt) {
		_removeDropTarget(dt);
	}
	private native void _removeDropTarget(DropTarget dt);
	
	// DropTarget support
	int nDropTargets;
	long nativeDropTargetContext; // native pointer ? 
	

	/*
     *
     * Object
     *
     */
	
	public String toString() {
		return getClass().getName() + "[" + target + "]";
	}
	

	/*
	 *
	 * BComponentPeer
	 *
	 */
	
	// Called from native code (on Toolkit thread) in order to 
	// dynamically layout the Conatiner during resizing
	void dynamicallyLayoutContainer() {
		if (dbg.on) {
			Container parent = BToolkit.getNativeContainer((Component)target);
			dbg.assertion(parent == null);
		}
		final Container cont = (Container)target;
		
		BToolkit.executeOnEventHandlerThread(cont, new Runnable() {
			public void run() {
				// Discarding old paint events dosen't seem to be necessary.
				cont.invalidate();
				cont.validate();
				// Forcing a paint here dosen't seem to be necessary.
				paintDamagedAreaImmediately();
			}
		});
	}
	
	// Paints any portion of the component that needs updating
	// before the call returns (similar to the Win32 API UpdateWindow)
	void paintDamagedAreaImmediately() {
		// force sending any pending WM_PAINT events so
		// the damage area is updated on the Java side
		updateWindow();
		// make sure paint events are transferred to main event queue
		// for coalescing
		BToolkit.getBToolkit().flushPendingEvents();
		// paint the damaged area
		paintArea.paint(target, shouldClearRectBeforePaint());
	}
	private native synchronized void updateWindow();

	// override and return false on components that DO NOT require
	// a clearRect() before painting (i.e. native components)
	public boolean shouldClearRectBeforePaint() {
		return true;
	}

	/**
	 * Creates new surfaceData object and invalidates the previous
	 * surfaceData object.
	 * Replacing the surfacedata should never lock on any resources which
	 * are required by other threads which may have them and may require
	 * the tree-lock.
	 */
	public void replaceSurfaceData() {
		synchronized(((Component)target).getTreeLock()) {
			synchronized(this) {
				if (pData == 0) {
					return;
				}
				HaikuSurfaceData oldData = surfaceData;
				surfaceData = HaikuSurfaceData.createData(this, numBackBuffers);
				if (oldData != null) {
					oldData.invalidate();
				}
				createBackBuffer();
			}
		}
	}
	
	public void replaceSurfaceDataLater() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					replaceSurfaceData();
				} catch (InvalidPipeException e) {
					// REMIND : what do we do if our surface creation failed?
				}
			}
		});
	}
	
	private synchronized void createBackBuffer() {
		if (numBackBuffers > 0) {
			// create the backbuffer!
			Component ctarg = (Component)target;
			backBuffer = new BVolatileImage(ctarg, ctarg.getWidth(), ctarg.getHeight(), surfaceData);
		} else {
			backBuffer = null;
		}
	}

	// fallback default font object
	final static Font defaultFont = new Font("Dialog",
			BToolkit.getFontStyle(getDefaultFontStyle()),
			BToolkit.getFontSize(getDefaultFontStyle()));
	
	/**
	 * Gets the be_font_xxxx style defined in BToolkit to use for this component.
	 */
	public static int getDefaultFontStyle() {
		return BToolkit.B_PLAIN_FONT;
	}
	
	// Toolkit & peer internals
	private int updateX1, updateY1, updateX2, updateY2;
	
	// Callbacks for window-system events to the frame
	
	// invoke a update() method call on the target
	void handleRepaint(int x, int y, int w, int h) {
		// Repaints are posted from updateClient now...
	}
	
	// Invoke a paint() method call on the target, after clearing the damaged area.
	void handleExpose(int x, int y, int w, int h) {
		if (!((Component)target).getIgnoreRepaint()) {
			postEvent(new PaintEvent((Component)target, PaintEvent.PAINT,
								new Rectangle(x, y, w, h)));
		}
	}
	
	/* Invoke a paint() method call on the target, without clearing the
	 * damaged area.  This is normally called by a native control after
	 * it has painted itself. 
	 *
	 * NOTE: This is called on the privileged toolkit thread. Do not
	 *   call directly into user code using this thread!
	 */
	void handlePaint(int x, int y, int w, int h) {
		if (!((Component)target).getIgnoreRepaint()) {
			postEvent(new PaintEvent((Component)target, PaintEvent.PAINT,
								new Rectangle(x, y, w, h)));
		}
	}
	
	/*
	 * Post an event. Queue it for execution by the callback thread.
	 */
	void postEvent(AWTEvent event) {
		BToolkit.postEvent(BToolkit.targetToAppContext(target), event);
	}

	// Multi-buffering
	private boolean isFullScreen() {
		System.out.println("BComponentPeer.isFullScreen() need impl");
		return false;
	}
	
	private void assertFullScreen() throws AWTException {
		if (!isFullScreen()) {
			throw new AWTException(
				"The operation requestion is only supported on a full-screen" +
				" exclusive window");
		}
	}
	
}
