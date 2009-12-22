package sun.awt.haiku;

import java.awt.*;
import java.awt.peer.*;

class BButtonPeer extends BComponentPeer implements ButtonPeer {
	//
	// instance creation/initialization
	//

	BButtonPeer(Component target) {
		super(target);
	}
	
	void create(BComponentPeer parent) {
		_create(parent);
	}
	private native void _create(BComponentPeer parent);

	//
	// ButtonPeer
	//

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
