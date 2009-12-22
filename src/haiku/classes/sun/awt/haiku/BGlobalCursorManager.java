package sun.awt.haiku;

import java.awt.*;
import sun.awt.GlobalCursorManager;

public final class BGlobalCursorManager extends GlobalCursorManager {
	private static BGlobalCursorManager manager;
	
	public static GlobalCursorManager getCursorManager() {
		if (manager == null) {
			manager = new BGlobalCursorManager();
		}
		return manager;
	}

    /**
     * Set the global cursor to the specified cursor. The component over
     * which the Cursor current resides is provided as a convenience. Not
     * all platforms may require the Component.
     */
	protected void setCursor(Component comp, Cursor cursor, boolean useCache) {
		_setCursor(cursor, useCache);
	}
	private native void _setCursor(Cursor cursor, boolean useCache);
	
	/**
	 * Populates the given point with the current mouse position in screen coordinates.
	 */
	protected void getCursorPos(Point p) {
		_getCursorPosition(p);
	}
	private native void _getCursorPosition(Point p);
	
	protected Component findComponentAt(Container con, int x, int y) {
		return _findComponentAt(con, x, y);
	}
	private native Component _findComponentAt(Container con, int x, int y);
	
	protected Point getLocationOnScreen(Component com) {
		return com.getLocationOnScreen();
	}
	
	/**
     * Returns the most specific, visible, enabled, heavyweight Component
     * under the cursor. This method should return null iff the cursor is
     * not over any Java Window.
     *
     * @param   useCache If true, the implementation is free to use caching
     * mechanisms because the Z-order, visibility, and enabled state of the
     * Components has not changed. If false, the implementation should not
     * make these assumptions.
     */
	protected Component findHeavyweightUnderCursor(boolean useCache) {
		// Due to the way we calculate mouse position for getCursorPos()
		// we already have the heavyweight Adapter cached in our native
		// Code. Natively it's easy (and fairly fast) to grab the jobject 
		// Component from the Adapter.
		return _getComponentUnderCursor();
	}
	private native Component _getComponentUnderCursor();
}
