package sun.awt.haiku;

import java.awt.*;
import java.awt.peer.*;

class BMenuPeer extends BMenuItemPeer implements MenuPeer {
	BMenuPeer(Menu target) {
		super(target);
	}

	void create(BObjectPeer parent) {
		_create(parent);
	}
	private native void _create(BObjectPeer parent);

	//
	//
	// MenuPeer
	//
	//

    public void addSeparator() {
		// this method is never called (see java.awt.Menu.addSeparator())
		addItem(new MenuItem("-"));
	}

    public void addItem(MenuItem item) {
		MenuItemPeer peer = (MenuItemPeer)getPeerForTarget(item);
		_addItem(peer);
	}
    private native void _addItem(MenuItemPeer peer);

    public void delItem(int index) {
		_delItem(index);
	}
    private native void _delItem(int index);
}
