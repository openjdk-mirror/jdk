package sun.awt.haiku;

import java.awt.*;
import java.awt.peer.*;

class BCheckboxMenuItemPeer extends BMenuItemPeer implements CheckboxMenuItemPeer {
	BCheckboxMenuItemPeer(CheckboxMenuItem target) {
		super(target);
	}

	void create(BObjectPeer parent) {
		CheckboxMenuItem item = (CheckboxMenuItem)target;
		_createCheckboxMenuItem(parent);
		setState(item.getState());
	}
	private native void _createCheckboxMenuItem(BObjectPeer parent);

	//
	//
	// CheckboxMenuItemPeer
	//
	//
	
	public void setState(boolean t) {
		_setState(t);
	}
	private native void _setState(boolean t);

	public boolean shouldClearRectBeforePaint() {
		return false;
	}
}
