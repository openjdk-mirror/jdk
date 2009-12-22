package sun.awt.haiku;

import java.awt.*;
import java.awt.peer.*;

abstract class BTextComponentPeer extends BComponentPeer implements TextComponentPeer {
	//
	// instance creation/initialization
	//

	BTextComponentPeer(Component target) {
		super(target);
	}

	void initialize() {
		super.initialize();
		TextComponent component = (TextComponent)this.target;
		setEditable(component.isEditable());
		setText(component.getText());
		setCaretPosition(component.getCaretPosition());
		select(component.getSelectionStart(),component.getSelectionEnd());
	}
	
	//
	//
	// TextComponentPeer
	//
	//

    public void setEditable(boolean editable) {
		_setEditable(editable);
	}
	private native void _setEditable(boolean editable);

    public String getText() {
		return _getText();
	}
	private native String _getText();

    public void setText(String l) {
		_setText(l);
	}
	private native void _setText(String l);

    public int getSelectionStart() {
		return _getSelectionStart();
	}
	private native int _getSelectionStart();

    public int getSelectionEnd() {
		return _getSelectionEnd();
	}
	private native int _getSelectionEnd();

    public void select(int selStart, int selEnd) {
		_select(selStart, selEnd);
	}
	private native void _select(int selStart, int selEnd);

    public void setCaretPosition(int pos) {
		_select(pos,pos);
	}

    public int getCaretPosition() {
		return _getCaretPosition();
	}
	private native int _getCaretPosition();

    public int getIndexAtPoint(int x, int y) {
		return _getIndexAtPoint(x, y);
	}
    private native int _getIndexAtPoint(int x, int y);

    public Rectangle getCharacterBounds(int i) {
		Rectangle result = new Rectangle(0, 0, 0, 0);
		_getCharacterBounds(i, result);
		return result;
	}
    private native void _getCharacterBounds(int i, Rectangle r);

    public long filterEvents(long mask) {
		System.out.println("BTextComponentPeer.filterEvents("+mask+")");
		return 0;
	}

	//
	// ComponentPeer
	//

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
