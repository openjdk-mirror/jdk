package sun.awt.haiku;

import java.awt.*;
import java.awt.peer.*;

class BTextAreaPeer extends BTextComponentPeer implements TextAreaPeer {
	BTextAreaPeer(Component target) {
		super(target);
	}
	
	public void create(BComponentPeer parent) {
		_create(parent);
	}
	private native void _create(BComponentPeer parent);

	//
	//
	// TextAreaPeer
	//
	//

    public void insert(String text, int pos) {
		_insert(text, pos);
	}
	private native void _insert(String text, int pos);

    public void replaceRange(String text, int start, int end) {
		_replaceRange(text, start, end);
	}
	private native void _replaceRange(String text, int start, int end);

    public Dimension getPreferredSize(int rows, int columns) {
		return _getPreferredSize(rows, columns);
	}
	private native Dimension _getPreferredSize(int rows, int columns);

    public Dimension getMinimumSize(int rows, int columns) {
		return _getMinimumSize(rows, columns);
	}
	private native Dimension _getMinimumSize(int rows, int columns);

    /**
     * DEPRECATED:  Replaced by insert(String, int).
     */
    public void insertText(String txt, int pos) {
		insert(txt, pos);
    }

    /**
     * DEPRECATED:  Replaced by ReplaceRange(String, int, int).
     */
    public void replaceText(String txt, int start, int end) {
		replaceRange(txt, start, end);
    }

    /**
     * DEPRECATED:  Replaced by getPreferredSize(int, int).
     */
    public Dimension preferredSize(int rows, int cols) {
		return getPreferredSize(rows, cols);
    }

    /**
     * DEPRECATED:  Replaced by getMinimumSize(int, int).
     */
    public Dimension minimumSize(int rows, int cols) {
		return getMinimumSize(rows, cols);
    }

	//
	//
	// ComponentPeer
	//
	//

	public Dimension getPreferredSize() {
		return _getPreferredSize();
	}
	private native Dimension _getPreferredSize();

	public Dimension getMinimumSize() {
		return _getMinimumSize();
	}
	private native Dimension _getMinimumSize();
}
