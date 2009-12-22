package sun.awt.haiku;

import java.awt.*;
import java.awt.peer.*;

class BMenuItemPeer extends BMenuComponentPeer implements MenuItemPeer {
	BMenuItemPeer(MenuItem target) {
		super(target);
	}

	void create(BObjectPeer parent) {
		MenuItem item = (MenuItem)target;
		if (item.getLabel().equals("-")) {
			_createSeparator(parent);
		} else {
			_createMenuItem(parent);
		}
		setLabel(item.getLabel());
		setEnabled(item.isEnabled());
	}
	private native void _createMenuItem(BObjectPeer parent);
	private native void _createSeparator(BObjectPeer parent);

	// used internally during setLabel (see below)
	private void setShortcut(MenuShortcut s) {
		if (s != null) {
			_setShortcut(s.getKey(), s.usesShiftModifier());
		} else {
			_setShortcut(0, false);
		}
	}
	private native void _setShortcut(int keycode, boolean shifted);

	//
	//
	// MenuItemPeer
	//
	//
	
	public void setLabel(String label) {
		_setLabel(label);
		// MenuItem.addShortcut(<>) calls setLabel, so update the shortcut
		setShortcut(((MenuItem)target).getShortcut());
	}
	private native void _setLabel(String label);

    public void setEnabled(boolean b) {
		_setEnabled(b);
	}
	private native void _setEnabled(boolean b);

    /**
     * DEPRECATED:  Replaced by setEnabled(boolean).
     */
    public void enable() {
		setEnabled(true);
	}

    /**
     * DEPRECATED:  Replaced by setEnabled(boolean).
     */
    public void disable() {
		setEnabled(false);
	}

}
