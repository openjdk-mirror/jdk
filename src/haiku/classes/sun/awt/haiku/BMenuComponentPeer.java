package sun.awt.haiku;

import java.awt.*;
import java.awt.peer.*;

abstract class BMenuComponentPeer extends BObjectPeer implements MenuComponentPeer {
	BMenuComponentPeer(MenuComponent target) {
		this.target = target;
		create(getPeerForTarget(target.getParent()));
	}
	abstract void create(BObjectPeer parent);

	protected void disposeImpl() {
		BToolkit.targetDisposedPeer(target, this);
		_dispose();
	}
	private synchronized native void _dispose();

	/*
	 * Post an event. Queue it for execution by the callback thread.
	 */
	void postEvent(AWTEvent event) {
		BToolkit.postEvent(BToolkit.targetToAppContext(target), event);
	}
}
