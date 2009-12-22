package sun.awt.haiku;

import java.util.Vector;
import java.awt.*;
import java.awt.peer.*;
import java.awt.event.*;
import sun.awt.HaikuGraphicsDevice;
import sun.awt.HaikuGraphicsConfig;
import java.lang.ref.WeakReference;
import sun.awt.DebugHelper;
import sun.awt.SunToolkit;

public class BWindowPeer extends BContainerPeer implements WindowPeer {
	//
	// static initialization
	//
	private static final DebugHelper dbg = 
		DebugHelper.create(BWindowPeer.class);

	static {
		initIDs();
	}
	private static native void initIDs();
	
	//
	// initialization and native creation
	//

	BWindowPeer(Window target) {
		super(target);
	}
	
	void create(BComponentPeer parent) {
		fInsets = new Insets(0, 0, 0, 0);
		createAwtWindow(parent);
	}
	native void createAwtWindow(BComponentPeer parent);
	
	void initialize() {
		super.initialize();
		
		allWindows.addElement(this);
		
		Font f = ((Window)target).getFont();
		if (f == null) {
			f = defaultFont;
			((Window)target).setFont(f);
			setFont(f);
		}
	}
	
	//	
	// WindowPeer
	//
	public void toFront() {
		focusableWindow = ((Window)target).isFocusableWindow();
		_toFront();
	}
	private native void _toFront();

	public void toBack() {
		_toBack();
	}
	private native void _toBack();

	//	
	// FramePeer & DialogPeer partial shared implementation
	//

	protected void setResizable(boolean resizable) {
		_setResizable(resizable);
	}
	private native void _setResizable(boolean resizable);

	protected void setTitle(String title) {
		// allow a null title to pass as an empty string.
		if (title == null) {
			title = new String("");
		}
		_setTitle(title);
	}
	private native void _setTitle(String title);
	
	protected void setUndecorated(boolean undecorated) {
		_setUndecorated(undecorated);
	}
	private native void _setUndecorated(boolean undecorated);

	//	
	// Toolkit & peer internals
	//

	static Vector allWindows = new Vector(); // !CQ for anchoring windows, frames, dialogs
	
	public void show() {
		focusableWindow = ((Window)target).isFocusableWindow();
		super.show();
	}
	protected boolean focusableWindow; // value queried from native code.
	
	static native int getSysMinWidth();
	static native int getSysMinHeight();
	
	public synchronized void reshape(int x, int y, int width, int height) {
		reshapeFrame(x, y, width, height);
	}
	
	protected synchronized native void reshapeFrame(int x, int y, int width, int height);
	
	private native int getScreenImOn();

	//
	// BComponentPeer
	//
	
	protected void disposeImpl() {
		allWindows.removeElement(this);
		super.disposeImpl();
	}
}
	
