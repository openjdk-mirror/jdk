package sun.awt.haiku;

import java.awt.*;
import java.awt.peer.*;

class BPanelPeer extends BContainerPeer implements PanelPeer {

	BPanelPeer(Component target) {
		super(target);
		fInsets = new Insets(0, 0, 0, 0);
	}
	
	protected void create(BComponentPeer parent) {
		_create(parent);
	}
	private native void _create(BComponentPeer parent);
}
