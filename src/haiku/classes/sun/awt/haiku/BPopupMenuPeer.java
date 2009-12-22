package sun.awt.haiku;

import java.awt.*;
import java.awt.peer.*;

class BPopupMenuPeer extends BMenuPeer implements PopupMenuPeer {
	BPopupMenuPeer(PopupMenu target) {
		super(target);
	}

	void create(BObjectPeer parent) {
		_create(parent);
	}
	private native void _create(BObjectPeer parent);
	
	//
	//
	// PopupMenuPeer
	//
	//

	public void show(Event e) {
		Component component = (Component)e.target;
		ComponentPeer peer = (ComponentPeer)getPeerForTarget(component);
		_show(peer, e.x, e.y);
	}
	private native void _show(ComponentPeer peer, int x, int y);
}
