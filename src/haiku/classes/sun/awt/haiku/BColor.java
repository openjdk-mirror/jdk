package sun.awt.haiku;

import java.awt.Color;

/**
 * This helper class maps Haiku system colors to AWT Color objects.
 */
class BColor {
	static final int WINDOW_BKGND = 1;
	static final int WINDOW_TEXT  = 2;
	
	static native Color getDefaultColor(int index);
}
