package sun.awt.haiku;

import java.awt.*;
import java.awt.peer.*;
import sun.awt.im.*;

class BFramePeer extends BWindowPeer implements FramePeer {
	//
	// initialization and native creation
	//

	BFramePeer(Frame target) {
		super(target);
		
		InputMethodManager imm = InputMethodManager.getInstance();
		String menuString = imm.getTriggerMenuString();
		if (menuString != null) {
//			pSetIMMOption(menuString);
		}
	}
	
	void create(BComponentPeer parent) {
		fInsets = new Insets(0, 0, 0, 0);
		createAwtFrame(parent);
	}
	native void createAwtFrame(BComponentPeer parent);
	
	void initialize() {
		super.initialize();
		
		Frame target = (Frame)this.target;
		
		setResizable(target.isResizable());
		setTitle(target.getTitle());
		setState(target.getExtendedState());
		setUndecorated(target.isUndecorated());
		
		Image icon = target.getIconImage();
		if (icon != null) {
			setIconImage(icon);
		}
	}
	
	//
	// FramePeer methods
	//

	public void setTitle(String title) {
		super.setTitle(title);
	}

	public void setIconImage(Image im) {
		// Ummm, this is Haiku, we aren't going to be able to do this.
		return;
	}
	
	public void setMenuBar(MenuBar mb) {
		if (mb == null) {
			_setMenuBar(null);
		} else {
			BMenuBarPeer mbPeer = (BMenuBarPeer) BToolkit.targetToPeer(mb);
			_setMenuBar(mbPeer);
		}
	}
	private native void _setMenuBar(MenuBarPeer mbPeer);

	public void setResizable(boolean resizable) {
		super.setResizable(resizable);
	}

	public void setState(int state) {
		_setState(state);
	}
	private native void _setState(int state);

	public int getState() {
		return _getState();
	}
	private native int _getState();
	
	public void setMaximizedBounds(Rectangle r) {
		if (r == null) {
			_clearMaximizedBounds();
		} else {
			_setMaximizedBounds(r.x, r.y, r.width, r.height);
		}
	}
	
	// Convenience methods to save us from trouble of extracting
	// Rectangle fields in native code
	private native void _setMaximizedBounds(int x, int y, int w, int h);
	private native void _clearMaximizedBounds();
	
	//
	// ComponentPeer methods
	//

	public void reshape(int x, int y, int width, int height) {
		if (((Frame)target).isUndecorated()) {
			super.reshape(x, y, width, height);
		} else {
			reshapeFrame(x, y, width, height);
		}
	}
	
	public Dimension getMinimumSize() {
		Dimension d = new Dimension();
		if (!((Frame)target).isUndecorated()) {
			d.setSize(getSysMinWidth(), getSysMinHeight());
		}
		
		// windows adds the height of the menu bar?!
		
		return d;
	}
	
	public boolean shouldClearRectBeforePaint() {
		return false;
	}
}
