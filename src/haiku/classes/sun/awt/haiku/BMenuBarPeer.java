package sun.awt.haiku;

import java.awt.*;
import java.awt.peer.*;

class BMenuBarPeer extends BMenuComponentPeer implements MenuBarPeer {
	BMenuBarPeer(MenuBar target) {
		super(target);
	}

	void create(BObjectPeer parent) {
		_createMenuBar(parent);
	}
	private native void _createMenuBar(BObjectPeer parent);

	//
	//
	// MenuBarPeer
	//
	//

    public void addMenu(Menu m) {
		MenuPeer peer = (MenuPeer)getPeerForTarget(m);
		_addMenu(peer);
	}
	private native void _addMenu(MenuPeer peer);

    public void delMenu(int index) {
		_delMenu(index);
	}
	private native void _delMenu(int index);

    public void addHelpMenu(Menu m) {
		MenuPeer peer = (MenuPeer)getPeerForTarget(m);
		_addHelpMenu(peer);
	}
	private native void _addHelpMenu(MenuPeer peer);
}
