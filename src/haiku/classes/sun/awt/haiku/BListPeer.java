package sun.awt.haiku;

import java.awt.*;
import java.awt.peer.*;

class BListPeer extends BComponentPeer implements ListPeer {
	static {
		initIDs(java.awt.List.class);
	}
	static private native void initIDs(Class cls);

	//
	// instance creation/initialization
	//

	BListPeer(Component target) {
		super(target);
	}
	
	void create(BComponentPeer parent) {
		_create(parent);
	}
	private native void _create(BComponentPeer parent);

	void initialize() {
		List list = (List)target;
		int itemCount = list.getItemCount();
		String[] items = list.getItems();
		if (itemCount > 0) {
			_initialize(items);
			select(list.getSelectedIndex());
		}
		super.initialize();
	}
	private native void _initialize(String[] items);

	//
	// ListPeer
	//

    public int[] getSelectedIndexes() {
		return _getSelectedIndexes();
	}
	private native int[] _getSelectedIndexes();

    public void add(String item, int index) {
		_add(item, index);
	}
	private native void _add(String item, int index);

    public void delItems(int start, int end) {
		_delItems(start, end);
	}
	private native void _delItems(int start, int end);

    public void removeAll() {
		_removeAll();
	}
	private native void _removeAll();

    public void select(int index) {
		_select(index);
	}
	private native void _select(int index);

    public void deselect(int index) {
		_deselect(index);
	}
	private native void _deselect(int index);

    public void makeVisible(int index) {
		_makeVisible(index);
	}
	private native void _makeVisible(int index);

    public void setMultipleMode(boolean b) {
		_setMultipleMode(b);
	}
	private native void _setMultipleMode(boolean b);

    public Dimension getPreferredSize(int rows) {
		return _getPreferredSize(rows);
	}
	private native Dimension _getPreferredSize(int rows);

    public Dimension getMinimumSize(int rows) {
		return _getMinimumSize(rows);
	}
	private native Dimension _getMinimumSize(int rows);

    /**
     * DEPRECATED:  Replaced by add(String, int).
     */
    public void addItem(String item, int index) {
		add(item, index);
    }

    /**
     * DEPRECATED:  Replaced by removeAll().
     */
    public void clear() {
		removeAll();
    }

    /**
     * DEPRECATED:  Replaced by setMultipleMode(boolean).
     */
    public void setMultipleSelections(boolean v) {
		setMultipleMode(v);
    }

    /**
     * DEPRECATED:  Replaced by getPreferredSize(int).
     */
    public Dimension preferredSize(int v) {
		return getPreferredSize(v);
    }

    /**
     * DEPRECATED:  Replaced by getMinimumSize(int).
     */
    public Dimension minimumSize(int v) {
		return getMinimumSize(v);
    }

	//
	// ComponentPeer
	//

    public Dimension getPreferredSize() {
		return _getPreferredSize();
	}
	private native Dimension _getPreferredSize();

    public Dimension getMinimumSize() {
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
