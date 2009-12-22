package sun.awt.haiku;

import java.awt.*;
import java.awt.peer.*;

class BChoicePeer extends BComponentPeer implements ChoicePeer {
	static {
		initIDs(java.awt.Choice.class);
	}
	static private native void initIDs(Class cls);

	//
	// instance creation/initialization
	//

	BChoicePeer(Component target) {
		super(target);
	}

	void create(BComponentPeer parent) {
		_create(parent);
	}
	private native void _create(BComponentPeer parent);

	void initialize() {
		Choice choice = (Choice)target;
		int itemCount = choice.getItemCount();
		String[] items = new String[itemCount];
		for (int i = 0 ; i < itemCount ; i++) {
			items[i] = choice.getItem(i);
		}
		if (itemCount > 0) {
			_initialize(items);
			select(choice.getSelectedIndex());
		}
		super.initialize();
	}
	private native void _initialize(String[] items);

	//
	// ChoicePeer
	//

    public void add(String item, int index) {
		_add(item, index);
	}
	private native void _add(String item, int index);

    public void remove(int index) {
		_remove(index);
	}
	private native void _remove(int index);

    public void removeAll() {
		_removeAll();
	}
	private native void _removeAll();

    public void select(int index) {
		_select(index);
	}
	private native void _select(int index);

    /*
     * DEPRECATED:  Replaced by add(String, int).
     */
    public void addItem(String item, int index) {
		add(item, index);
	}

	//
	// ComponentPeer
	//

	public Dimension getMinimumSize() {
		Choice choice = (Choice)target;
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
