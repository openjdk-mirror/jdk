package sun.awt.haiku;

import java.awt.*;
import java.awt.peer.*;

class BTextFieldPeer extends BTextComponentPeer implements TextFieldPeer {
	BTextFieldPeer(Component target) {
		super(target);
	}
	
	public void create(BComponentPeer parent) {
		_create(parent);
	}
	private native void _create(BComponentPeer parent);

	//
	//
	// TextViewPeer
	//
	//

    public void setEchoChar(char echoChar) {
		_setEchoChar(echoChar);
	}
	private native void _setEchoChar(char echoChar);

    public Dimension getPreferredSize(int columns) {
		return _getPreferredSize(columns);
	}
	private native Dimension _getPreferredSize(int columns);

    public Dimension getMinimumSize(int columns) {
		return _getMinimumSize(columns);
	}
	private native Dimension _getMinimumSize(int columns);

    /**
     * DEPRECATED:  Replaced by setEchoChar(char echoChar).
     */
    public void setEchoCharacter(char c) {
		setEchoChar(c);
	}

    /**
     * DEPRECATED:  Replaced by getPreferredSize(int).
     */
    public Dimension preferredSize(int cols) {
		return getPreferredSize(cols);
	}

    /**
     * DEPRECATED:  Replaced by getMinimumSize(int).
     */
    public Dimension minimumSize(int cols) {
		return getMinimumSize(cols);
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
