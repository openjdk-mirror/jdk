package sun.awt.haiku;

import java.awt.*;
import java.awt.im.InputMethodHighlight;
import java.awt.im.spi.InputMethodDescriptor;
import java.awt.image.*;
import java.awt.peer.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.datatransfer.Clipboard;
import java.io.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.Enumeration;
import java.awt.print.PageFormat;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.security.AccessController;
import java.security.PrivilegedAction;
import sun.awt.image.ByteArrayImageSource;
import sun.awt.image.FileImageSource;
import sun.awt.image.URLImageSource;
import sun.awt.image.ImageRepresentation;
import sun.awt.print.PrintControl;
import sun.awt.AppContext;
import sun.awt.AWTAutoShutdown;
import sun.awt.EmbeddedFrame;
import sun.awt.GlobalCursorManager;
import sun.awt.SunToolkit;
import sun.awt.HaikuGraphicsConfig;
import sun.awt.HaikuGraphicsDevice;
import sun.awt.HaikuGraphicsEnvironment;
import sun.awt.DebugHelper;
import sun.awt.datatransfer.DataTransferer;

import sun.print.PrintJob2D;

import java.awt.dnd.DragSource;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.MouseDragGestureRecognizer;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.dnd.peer.DragSourceContextPeer;

//import sun.awt.haiku.BDragSourceContextPeer;

import sun.misc.PerformanceLogger;
import sun.security.action.GetPropertyAction;

public class BToolkit extends SunToolkit implements Runnable {
	private static final DebugHelper dbg = DebugHelper.create(BToolkit.class);
	
	// Constants for obtaining the system font settings.
	public static final int B_PLAIN_FONT = 0;
	public static final int B_BOLD_FONT = 1;
	public static final int B_FIXED_FONT = 2;
	public static final int B_MENU_FONT = 3;
	
	static GraphicsConfiguration config;
	
	// System clipboard.
	BClipboard clipboard;
	
	// Dynamic Layout Resize client code setting
	protected static boolean dynamicLayoutSetting = false;
	protected static int dynamicLayoutBatchDelay = 0;
	
	// cache of font peers
	private Hashtable cacheFontPeer;
	
	static {
		if (GraphicsEnvironment.isHeadless()) {
			config = null;
		} else {
			config = (HaikuGraphicsConfig) (GraphicsEnvironment.
					getLocalGraphicsEnvironment().
					getDefaultScreenDevice().
					getDefaultConfiguration());
		}
		_printHaikuVersion();
    }
	private static native void _printHaikuVersion();

	/*
	 * Reset the static GraphicsConfiguration to the default.  Called on
	 * startup and when display settings have changed.
	 */
	public static void resetGC() {
		if (GraphicsEnvironment.isHeadless()) {
			config = null;
		} else {
		  config = (GraphicsEnvironment
		  .getLocalGraphicsEnvironment()
		  .getDefaultScreenDevice()
		  .getDefaultConfiguration());
		}
	}

	public static final String DATA_TRANSFER_CLASS_NAME = "sun.awt.haiku.BDataTransferer";
	
	public BToolkit() {
		super();
		
		// Startup Toolkit threads
		if (PerformanceLogger.loggingEnabled()) {
			PerformanceLogger.setTime("BToolkit construction");
		}
		
		synchronized (this) {
			// Fix for bug #4046430 -- Race condition
			// where notifyAll can be called before
			// the "AWT-Windows" thread's parent thread is 
			// waiting, resulting in a deadlock on startup.
			Thread toolkitThread = new Thread(this, "AWT-Haiku");
			toolkitThread.setPriority(Thread.NORM_PRIORITY + 1);
			toolkitThread.setDaemon(true);

			/*
			 * Fix for 4701990.
			 * AWTAutoShutdown state must be changed before the toolkit thread
			 * starts to avoid race condition.
			 */
			AWTAutoShutdown.notifyToolkitThreadBusy();

			toolkitThread.start();
												  
			try {
				wait();
			} catch (InterruptedException x) {
			}
		}
		SunToolkit.setDataTransfererClassName(DATA_TRANSFER_CLASS_NAME);
	}
	
	public void run() {
		boolean startPump = _init();

		if (startPump) {
			ThreadGroup mainTG = (ThreadGroup)AccessController.doPrivileged(
				new PrivilegedAction() {
					public Object run() {
						ThreadGroup currentTG = 
								Thread.currentThread().getThreadGroup();
						ThreadGroup parentTG = currentTG.getParent();
						while (parentTG != null) {
							currentTG = parentTG;
							parentTG = currentTG.getParent();
						}
						return currentTG;
					}
			});
			
			Runtime.getRuntime().addShutdownHook(
					new Thread(mainTG, new Runnable() {
							public void run() {
								_shutdown();
							}
					}, "Shutdown-Thread")
			);
		}
		
		synchronized(this) {
			notifyAll();
		}
		
		if (startPump) {
			_eventLoop(); // will Dispose Toolkit when shutdown hook executes
		}
	}
				
	/* 
	 * eventLoop() begins the native message pump (BApplication Looper) 
	 * which retrieves and processes native events.
	 *
	 * When shutdown() is called by the ShutdownHook added in run(), a
	 * _QIT message is posted to the Toolkit thread indicating that
	 * eventLoop() should Dispose the toolkit and exit.
	 */
	private native boolean _init();
	private native void _eventLoop();
	private native void _shutdown();
	/** only called if runFinalizersOnExit */
	protected void finalize() {
		_finalize();
	}
	private native void _finalize();
	
	/**
	 * Used within the Haiku AWT Implementation.
	 */
	public static BToolkit getBToolkit() {
		BToolkit toolkit = (BToolkit)Toolkit.getDefaultToolkit();
		return toolkit;
	}
	
	
	/*
	 * Create peer objects.
	 */
	
	public ButtonPeer createButton(Button target) {
		ButtonPeer peer = new BButtonPeer(target);
		targetCreatedPeer(target, peer);
		return peer;
	}
	
	public TextFieldPeer createTextField(TextField target) {
		TextFieldPeer peer = new BTextFieldPeer(target);
		targetCreatedPeer(target, peer);
		return peer;
	}

	public LabelPeer createLabel(Label target) {
		LabelPeer peer = new BLabelPeer(target);
		targetCreatedPeer(target, peer);
		return peer;
	}

	public ListPeer createList(List target) {
		ListPeer peer = new BListPeer(target);
		targetCreatedPeer(target, peer);
		return peer;
	}

	public CheckboxPeer createCheckbox(Checkbox target) {
		CheckboxPeer peer = new BCheckboxPeer(target);
		targetCreatedPeer(target, peer);
		return peer;
	}

	public ScrollbarPeer createScrollbar(Scrollbar target) {
		ScrollbarPeer peer = new BScrollbarPeer(target);
		targetCreatedPeer(target, peer);
		return peer;
	}

	public ScrollPanePeer createScrollPane(ScrollPane target) {
		ScrollPanePeer peer = new BScrollPanePeer(target);
		targetCreatedPeer(target, peer);
		return peer;
	}

	public TextAreaPeer createTextArea(TextArea target) {
		TextAreaPeer peer = new BTextAreaPeer(target);
		targetCreatedPeer(target, peer);
		return peer;
	}

	public ChoicePeer createChoice(Choice target) {
		ChoicePeer peer = new BChoicePeer(target);
		targetCreatedPeer(target, peer);
		return peer;
	}

	public FramePeer  createFrame(Frame target) {
		FramePeer peer = new BFramePeer(target);
		targetCreatedPeer(target, peer);
		return peer;
	}

	public CanvasPeer createCanvas(Canvas target) {
		CanvasPeer peer = new BCanvasPeer(target);
		targetCreatedPeer(target, peer);
		return peer;
	}

	public PanelPeer createPanel(Panel target) {
		PanelPeer peer = new BPanelPeer(target);
		targetCreatedPeer(target, peer);
		return peer;
	}

	public WindowPeer createWindow(Window target) {
		WindowPeer peer = new BWindowPeer(target);
		targetCreatedPeer(target, peer);
		return peer;
	}

	public DialogPeer createDialog(Dialog target) {
		DialogPeer peer = new BDialogPeer(target);
		targetCreatedPeer(target, peer);
		return peer;
	}

	public FileDialogPeer createFileDialog(FileDialog target) {
		FileDialogPeer peer = new BFileDialogPeer(target);
		targetCreatedPeer(target, peer);
		return peer;
	}

	public MenuBarPeer createMenuBar(MenuBar target) {
		MenuBarPeer peer = new BMenuBarPeer(target);
		targetCreatedPeer(target, peer);
		return peer;
	}

	public MenuPeer createMenu(Menu target) {
		MenuPeer peer = new BMenuPeer(target);
		targetCreatedPeer(target, peer);
		return peer;
	}

	public PopupMenuPeer createPopupMenu(PopupMenu target) {
		PopupMenuPeer peer = new BPopupMenuPeer(target);
		targetCreatedPeer(target, peer);
		return peer;
	}

	public MenuItemPeer createMenuItem(MenuItem target) {
		MenuItemPeer peer = new BMenuItemPeer(target);
		targetCreatedPeer(target, peer);
		return peer;
	}

	public CheckboxMenuItemPeer createCheckboxMenuItem(CheckboxMenuItem target) {
		CheckboxMenuItemPeer peer = new BCheckboxMenuItemPeer(target);
		targetCreatedPeer(target, peer);
		return peer;
	}

	//public BEmbeddedFramePeer createEmbeddedFrame(BEmbeddedFrame target) {
	//	System.out.println("FINISH: BEmbeddedFramePeer");
	//	return null;
		//BEmbeddedFramePeer peer = new BEmbeddedFramePeer(target);
		//targetCreatedPeer(target, peer);
		//return peer;
	//}
	
	public RobotPeer createRobot(Robot target, GraphicsDevice screen) {
		// (target is unused for now)
		// Robot's don't need to go in the peer map since
		// they're not Component's
		return new BRobotPeer(screen);
	}
	
	/**
	 * Returns <code>true</code> if this frame state is supported.
	 */
	public boolean isFrameStateSupported(int state) {
		switch (state) {
		  case Frame.NORMAL:
		  case Frame.ICONIFIED:
		  case Frame.MAXIMIZED_BOTH:
			  return true;
		  default:
			  return false;
		}
	}
	
	static ColorModel screenmode1;
	
	static ColorModel getStaticColorModel() {
		if (GraphicsEnvironment.isHeadless()) {
			throw new IllegalArgumentException();
		}
		if (config == null) {
			resetGC();
		}
		return config.getColorModel();
	}
	
	public ColorModel getColorModel() {
		return getStaticColorModel();
	}
	
	public int getScreenResolution() {
		return _getScreenResolution();
	}
	private native int _getScreenResolution();
	
	protected int getScreenWidth() {
		return _getScreenWidth();
	}
	private native int _getScreenWidth();
	
	protected int getScreenHeight() {
		return _getScreenHeight();
	}
	private native int _getScreenHeight();
	
	public Insets getScreenInsets(GraphicsConfiguration gc) {
		return new Insets(0,0,0,0);
	}
	
	public FontMetrics getFontMetrics(Font font) {
		// See notes in MToolkit (motif) and WToolkit (windows)
		return super.getFontMetrics(font);
	}
	
	public FontPeer getFontPeer(String name, int style){
		return new BFontPeer(name, style);
	}
	
	/**
	 * Gets the Haiku user setting for the given font size.
	 */
	public static int getFontSize(int type) {
		return _getFontSize(type);
	}
	private static native int _getFontSize(int type);
	
	/**
	 * Gets the Haiku user setting for the given font style.
	 */
	public static int getFontStyle(int type) {
		return _getFontStyle(type);
	}
	private static native int _getFontStyle(int type);
	
	
	public void sync() {
		_sync();
	}
	private native void _sync();
	
	static boolean prepareScrImage(Image img, int w, int h, ImageObserver o) {
		if (w == 0 || h == 0) {
			return true;
		}
		
		// Must be an OffScreenImage
		if (!(img instanceof BImage)) {
			return true;
		}
		
		BImage bimg = (BImage) img;
		if (bimg.hasError()) {
			if (o != null) {
				o.imageUpdate(img, ImageObserver.ERROR|ImageObserver.ABORT,
						 -1, -1, -1, -1);
			}
			return false;	
		}
		ImageRepresentation ir = bimg.getImageRep();
		return ir.prepare(o);
	}
	
	static int checkScrImage(Image img, int w, int h, ImageObserver o) {
		if (!(img instanceof BImage)) {
			return ImageObserver.ALLBITS;
		}
		
		BImage bimg = (BImage)img;
		int repbits;
		if (w == 0 || h == 0) {
			repbits = ImageObserver.ALLBITS;
		} else {
			repbits = bimg.getImageRep().check(o);
		}
		return bimg.check(o) | repbits;
	}
	
	public int checkImage(Image img, int w, int h, ImageObserver o) {
		return checkScrImage(img, w, h, o);
	}
	
	public boolean prepareImage(Image img, int w, int h, ImageObserver o) {
		return prepareScrImage(img, w, h, o);
	}
	
	public Image createImage(ImageProducer producer) {
		return new BImage(producer);
	}
	
	public PrintJob getPrintJob(final Frame frame, final String doctitle,
				 final Properties props) 
	{
		if (GraphicsEnvironment.isHeadless()) {
			throw new IllegalArgumentException();
		}
		
		PrintJob2D printJob = new PrintJob2D(frame, doctitle, props);
		
		if (printJob.printDialog() == false) {
			printJob = null;
		}
		
		return printJob;
	}
	
	public PrintJob getPrintJob(final Frame frame, final String doctitle,
				final JobAttributes jobAttributes,
				final PageAttributes pageAttributes)
	{
		if (GraphicsEnvironment.isHeadless()) {
			throw new IllegalArgumentException();
		}
		
		PrintJob2D printJob = new PrintJob2D(frame, doctitle,
											 jobAttributes, pageAttributes);

		if (printJob.printDialog() == false) {
			printJob = null;
		}

		return printJob;
	}
	
	public void beep() {
		_beep();
	}
	private native void _beep();
	
	public Clipboard getSystemClipboard() {
		SecurityManager security = System.getSecurityManager();
		if (security != null) {
			security.checkSystemClipboardAccess();
		}
		
		synchronized (this) {
			if (clipboard == null) {
				clipboard = new BClipboard("system");
			}
		}
		
		return clipboard;
	}
	
	public void loadSystemColors(int[] systemColors) {
		_loadSystemColors(systemColors);
	}
	private native int _loadSystemColors(int[] systemColors);

    public int getMenuShortcutKeyMask() throws HeadlessException {
        return _getMenuShortcutKeyMask();
    }
	private native int _getMenuShortcutKeyMask();
	
	public boolean getLockingKeyState(int key) {
		if (! (key == KeyEvent.VK_CAPS_LOCK || key == KeyEvent.VK_NUM_LOCK ||
			   key == KeyEvent.VK_SCROLL_LOCK || key == KeyEvent.VK_KANA_LOCK)) {
			throw new IllegalArgumentException("invalid key for Toolkit.getLockingKeyState");
		}
		return _getLockingKeyState(key);
	}
	private native boolean _getLockingKeyState(int key);

	public void setLockingKeyState(int key, boolean on) {
		if (! (key == KeyEvent.VK_CAPS_LOCK || key == KeyEvent.VK_NUM_LOCK ||
			   key == KeyEvent.VK_SCROLL_LOCK || key == KeyEvent.VK_KANA_LOCK)) {
			throw new IllegalArgumentException("invalid key for Toolkit.setLockingKeyState");
		}
		_setLockingKeyState(key, on);
	}
	private native void _setLockingKeyState(int key, boolean on);
	
	public static final Object targetToPeer(Object target) {
		return SunToolkit.targetToPeer(target);
	}

	public static final void targetDisposedPeer(Object target, Object peer) {
		SunToolkit.targetDisposedPeer(target, peer);
	}

	/**
	 * Returns a new input method adapter descriptor for native input methods.
	 */
	public InputMethodDescriptor getInputMethodAdapterDescriptor() {
		return new BInputMethodDescriptor();
	}

	/**
	 * Returns a style map for the input method highlight.
	 */
	public Map mapInputMethodHighlight(InputMethodHighlight highlight) {
		System.out.println("FINISH: BInputMethod."); 
		return null;
		//return BInputMethod.mapInputMethodHighlight(highlight);
	}

	/**
	 * Returns whether enableInputMethods should be set to true for peered
	 * TextComponent instances on this platform.
	 */
	public boolean enableInputMethodsForTextComponent() {
		return true;
	}

	/**
	 * Returns the default keyboard locale of the underlying operating system
	 */
	public Locale getDefaultKeyboardLocale() {
		System.out.println("FINISH: BInputMethod. Fix BToolkit.getDefaultKeyboardLocale()"); 
		return super.getDefaultKeyboardLocale();
		/*Locale locale = BInputMethod.getNativeLocale();
	
		if (locale == null) {
			return super.getDefaultKeyboardLocale();
		} else {
			return locale;
		}
		*/
	}

	/**
	 * Returns a new custom cursor.
	 */
	public Cursor createCustomCursor(Image cursor, Point hotSpot, String name)
		throws IndexOutOfBoundsException 
	{
		System.out.println("FINISH: BCustomCursor");
		return null;
		//return new BCustomCursor(cursor, hotSpot, name);
	}

	/**
	 * Returns the supported cursor size
	 * 
	 * According to the Haiku R5 BeBook:
	 *		"Cursors are always square; currently, only 16-by-16 cursors are allowed."
	 * Sure does make that easy for now, eh?
	 */
	public Dimension getBestCursorSize(int preferredWidth, int preferredHeight) {
		return new Dimension(16, 16);
	}

	/**
	 * Gets the maximum number of colors allowed for a cursor.
	 * 
	 * Again, we reference the R5 BeBook and find:
	 * 		"Currently, only one-bit monochrome is allowed."
	 */
	public int getMaximumCursorColors() {
		return 2; // black and white.
	}
	
	/**
	 * create the peer for a DragSourceContext
	 */
	public DragSourceContextPeer createDragSourceContextPeer(DragGestureEvent dge) throws InvalidDnDOperationException {
		return BDragSourceContextPeer.createDragSourceContextPeer(dge);
	}

	public DragGestureRecognizer createDragGestureRecognizer(Class abstractRecognizerClass, DragSource ds, Component c, int srcActions, DragGestureListener dgl) {
		if (MouseDragGestureRecognizer.class.equals(abstractRecognizerClass)) {
			return new BMouseDragGestureRecognizer(ds, c, srcActions, dgl);
		} else {
			return null;
		}
	}
	
	private static final String prefix  = "DnD.Cursor.";
	private static final String postfix = ".32x32";
	private static final String awtPrefix  = "awt.";

	protected Object lazilyLoadDesktopProperty(String name) {
		if (name.startsWith(prefix)) {
			String cursorName = name.substring(prefix.length(),
			  name.length()) + postfix;

			try {
				return Cursor.getSystemCustomCursor(cursorName);
			} catch (AWTException awte) {
				throw new RuntimeException("cannot load system cursor: " + cursorName);
			}
		}
		/*
		 * At this time, I'm leaving out a BDesktopProperties
		 * In the future it will probably be prudent to have one so that
		 * we can exploit it with the Haiku Pluggable Look And Feel or other
		 * Haiku specific code that we'll need to write.
		 * At this time, it's a really moot point.
		 */
		/*
		else if (BDesktopProperties.isWindowsProperty(name) ||
				   name.startsWith(awtPrefix)) {
			synchronized(this) {
				if (bprops == null) {
					bprops = new BDesktopProperties(this);
				} else {
					// Only need to do this if wprops already existed,
					// because in that case the value could be stale
					if (name.equals("awt.dynamicLayoutSupported")) {
						return lazilyLoadDynamicLayoutSupportedProperty(name);
					}
				}

				// XXX do the same for "win.text.fontSmoothingOn" ???

				Object prop = bprops.getProperty(name);
				return prop;
			}
		}
		*/
		
		/* Additionally, we have no need to lazily load dynamic layout
		 * properties, since all Haiku distros support that by nature. The only
		 * questions to that end are if we fully support dynamic layout, and 
		 * weather or not our native peer classes will filter out some of those
		 * events.
		 */
		return super.lazilyLoadDesktopProperty(name);
	}
	
	protected void initializeDesktopProperties() {
		desktopProperties.put("DnD.Autoscroll.initialDelay",	 new Integer(50));
		desktopProperties.put("DnD.Autoscroll.interval",		 new Integer(50));
		desktopProperties.put("DnD.Autoscroll.cursorHysteresis", new Integer(5));
		
		/**
		 * Wheelmouse detection?! You have got to be kidding me...
		 * Haiku leaves reading wheel info to the input_device. There 
		 * isn't a way to poll the input_server for this information AFAIK.
		 * We'll do as the Solaris / Linux folks and leave this out...
		 */
		 //desktopProperties.put("awt.wheelMousePresent", new Boolean(false));
		 
		 if (!GraphicsEnvironment.isHeadless()) {
		 	desktopProperties.put("awt.multiClickInterval", 
		 			new Integer(_getMulticlickTime()));
		 }
	}
	private native int _getMulticlickTime();
}
