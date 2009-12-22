package sun.awt.haiku;

import java.awt.*;
import java.awt.peer.*;

class BCheckboxPeer extends BComponentPeer implements CheckboxPeer {
	static {
		initIDs(java.awt.CheckboxGroup.class);
	}
	static private native void initIDs(Class checkboxGroup);

	//
	// instance creation/initialization
	//

	BCheckboxPeer(Component target) {
		super(target);
	}

	void create(BComponentPeer parent) {
		_create(parent);
	}
	private native void _create(BComponentPeer parent);

	//
	// CheckboxPeer
	//

	public void setState(boolean t) {
		_setState(t);
	}
	private native void _setState(boolean t);

    public void setCheckboxGroup(CheckboxGroup g) {
		_setCheckboxGroup(g);
	}
	private native void _setCheckboxGroup(CheckboxGroup g);

    public void setLabel(String label) {
		_setLabel(label);
	}
	private native void _setLabel(String label);

	//
	// ComponentPeer
	//

	public Dimension getMinimumSize() {
		return _getMinimumSize();
	}
	private native Dimension _getMinimumSize();

	public boolean isFocusable() {
		return true;
	}	

	//
	// BComponentPeer
	//

	public boolean shouldClearRectBeforePaint() {
		return false;
	}
}
