package sun.awt.haiku;

import java.awt.*;
import java.awt.peer.*;

class BScrollPanePeer extends BContainerPeer implements ScrollPanePeer {
	BScrollPanePeer(Component target) {
		super(target);
		fInsets = new Insets(2, 2, 2, 2);
	}

	protected void create(BComponentPeer parent) {
		_create(parent);
		fInsets = new Insets(2, 2, 2+getHScrollbarHeight(), 2+getVScrollbarWidth());
	}
	private native void _create(BComponentPeer parent);
	
    // ---------------------------------------------------------------------
	//
	// ScrollPanePeer
	//
	//

    public int getHScrollbarHeight() {
		return _getHScrollbarHeight();
	}
	private native int _getHScrollbarHeight();

    public int getVScrollbarWidth() {
		return _getVScrollbarWidth();
	}
	private native int _getVScrollbarWidth();

	/* Legal bounds are defined to be the rectangle:
	 * x = 0, y = 0, width = (child width - view port width),
	 * height = (child height - view port height).
	 */
    public void setScrollPosition(int x, int y) {
		_setScrollPosition(x, y);
	}
	private native void _setScrollPosition(int x, int y);

    public void childResized(int w, int h) {
		_childResized(w, h);
	}
	// this method might need to call SetProportion, SetRange
	private native void _childResized(int w, int h);

    public void setUnitIncrement(Adjustable adj, int u) {
		_setIncrements(adj.getOrientation(), u, adj.getBlockIncrement());
	}
	private native void _setIncrements(int orientation, int u, int b);

    public void setValue(Adjustable adj, int v) {
		_setValue(adj.getOrientation(), v);
	}
	private native void _setValue(int orientation, int v);

	public boolean shouldClearRectBeforePaint() {
		return false;
	}
}
