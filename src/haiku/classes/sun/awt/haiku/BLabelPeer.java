package sun.awt.haiku;

import java.awt.*;
import java.awt.peer.*;

class BLabelPeer extends BComponentPeer implements LabelPeer {
	BLabelPeer(Component target) {
		super(target);
	}

	void create(BComponentPeer parent) {
		_create(parent);
	}
	private native void _create(BComponentPeer parent);

	void initialize() {
		Label label = (Label)target;
		setAlignment(label.getAlignment());
		super.initialize();
	}

	//
	//
	// LabelPeer
	//
	//

    public void setText(String label) {
    	if (label == null) {
    		label = "";
    	}
		_setText(label);
	}
	private native void _setText(String label);

    public void setAlignment(int alignment) {
		_setAlignment(alignment);
	}
	private native void _setAlignment(int alignment);

	//
	//
	// ComponentPeer
	//
	//

	public Dimension getMinimumSize() {
		Label label = (Label)target;
		Font font = label.getFont();
		String text = label.getText();
		if (text == null) {
			text = "";
		}
		return _getMinimumSize(font, text);
	}
	private native Dimension _getMinimumSize(Font font, String text);

	public boolean shouldClearRectBeforePaint() {
		return false;
	}
}
