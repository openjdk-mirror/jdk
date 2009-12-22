package sun.awt.haiku;

import java.awt.*;
import java.awt.peer.*;
import java.lang.ref.WeakReference;
import sun.awt.HaikuGraphicsDevice;

class BCanvasPeer extends BViewPeer implements CanvasPeer {
	//
	// instance creation/initialization
	//

	BCanvasPeer(Component target) {
		super(target);
	}
	
	void create(BComponentPeer parent) {
		_create(parent);
	}
	private native void _create(BComponentPeer parent);

	//
	// BViewPeer
	//
	
	/*
	 * Reset the graphicsConfiguration member of our target Component.
	 * Component.resetGC() is a package-private method, so we have to call it
	 * through JNI.
	 */
	native void resetTargetGC();
}
