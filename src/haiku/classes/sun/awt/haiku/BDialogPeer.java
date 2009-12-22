package sun.awt.haiku;

import java.awt.*;
import java.awt.peer.*;

class BDialogPeer extends BWindowPeer implements DialogPeer {
	//
	// initialization and native creation
	//

	BDialogPeer(Dialog target) {
		super(target);
		
	}

	void create(BComponentPeer parent) {
		fInsets = new Insets(0, 0, 0, 0);
		createAwtDialog(parent);
	}
	native void createAwtDialog(BComponentPeer parent);

	void initialize() {
		super.initialize();
		
		Dialog target = (Dialog)this.target;
		
		setResizable(target.isResizable());
		setTitle(target.getTitle());
		setUndecorated(target.isUndecorated());
	}

	//
	// DialogPeer
	//

	public void setResizable(boolean resizable) {
		super.setResizable(resizable);
	}

	public void setTitle(String title) {
		super.setTitle(title);
	}
	
	//
	// Overrides for Window, so that we can support modality
	//
	public void show() {
		if (((Dialog)target).isModal()) { // If we're modal, we need to block some things.
			_createFullSubset();
			excludeChildren((Dialog)target); // Exclude any of our children.
		}
		
		super.show();
	}
	
	/**
	 * Recursively excludes all the children of the current dialog from the 
	 * Native peer's Subset.
	 */
	private void excludeChildren(Window parent) {
		Window[] children = parent.getOwnedWindows();
		WindowPeer wp = null;
		
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				// Remove this child if it has a peer.
				wp = (WindowPeer)BToolkit.targetToPeer(children[i]);
				if (wp != null) {
					_removeFromSubset(wp);
				}
				
				// Remove any children of the current child.
				excludeChildren(children[i]);
			}
		}
	}
	
	/**
	 * Tells the native peer to grab all the windows it can and add them to it's
	 * modal subset.
	 */
	private native void _createFullSubset();
	
	/**
	 * Removes the given peer from the native peers subset that it will block when
	 * going into modal mode.
	 */
	private native void _removeFromSubset(WindowPeer child);

	//
	// ComponentPeer
	//

    public void reshape(int x, int y, int width, int height) {
        if (((Dialog)target).isUndecorated()) {
            super.reshape(x,y,width,height);
        } else {
            reshapeFrame(x,y,width,height);
        }
    }

}
