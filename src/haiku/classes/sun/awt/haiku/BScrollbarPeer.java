package sun.awt.haiku;

import java.awt.*;
import java.awt.peer.*;

class BScrollbarPeer extends BComponentPeer implements ScrollbarPeer {
	BScrollbarPeer(Component target) {
		super(target);
	}
	
	void create(BComponentPeer parent) {
		_create(parent);
	}
	private native void _create(BComponentPeer parent);

	//
	//
	// ScrollbarPeer
	//
	//

    public void setValues(int value, int visible, int minimum, int maximum) {
		_setValues(value, visible, minimum, maximum);
	}
	private native void _setValues(int value, int visible, int minimum, int maximum);

    public void setLineIncrement(int l) {
		_setLineIncrement(l);
	}
    private native void _setLineIncrement(int l);

    public void setPageIncrement(int l) {
		_setPageIncrement(l);
	}
    private native void _setPageIncrement(int l);

	public boolean shouldClearRectBeforePaint() {
		return false;
	}
}
