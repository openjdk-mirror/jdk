package sun.awt.haiku;

import java.awt.*;
import java.awt.peer.*;

abstract class BViewPeer extends BComponentPeer {
	BViewPeer(Component target) {
		super(target);
	}

	// override the Component shouldClearRectBeforePaint
	// based on the sun.awt.noerasebackground property

	private static boolean eraseBackgroundDisabled;
	
	static {
		String noerasebackground = (String)java.security.AccessController.doPrivileged(
			new sun.security.action.GetPropertyAction("sun.awt.noerasebackground"));
		eraseBackgroundDisabled = (noerasebackground != null 
								&& noerasebackground.length() > 0
								&& noerasebackground.charAt(0) == 't');
	}
	
	public boolean shouldClearRectBeforePaint() {
		return (eraseBackgroundDisabled == false);
	}

	// Clears the peer's haikuGraphicsConfig member.
	// Overridden by BWindowPeer, which shouldn't have a null haikuGraphicsConfig.
	void clearLocalGC() {
		haikuGraphicsConfig = null;
	}

	// BCanvasPeer.resetTargetGC() hook.
	void resetTargetGC() {}

	/*
	 * paint methods utilize 2D graphics acceleration to
	 * clear the background if possible
	 */

	public void paint(Graphics g) {
		Dimension d = ((Component)target).getSize();
		if (g instanceof Graphics2D ||
			g instanceof sun.awt.Graphics2Delegate) {
			// background color is setup correctly, so just use clearRect
			g.clearRect(0, 0, d.width, d.height);
		} else {
			// emulate clearRect
			g.setColor(((Component)target).getBackground());
			g.fillRect(0, 0, d.width, d.height);
			g.setColor(((Component)target).getForeground());
		}
		super.paint(g);
	}

	public void print(Graphics g) {
		Dimension d = ((Component)target).getSize();
			if (g instanceof Graphics2D ||
			g instanceof sun.awt.Graphics2Delegate) {
			// background color is setup correctly, so just use clearRect
			g.clearRect(0, 0, d.width, d.height);
		} else {
			// emulate clearRect
			g.setColor(((Component)target).getBackground());
			g.fillRect(0, 0, d.width, d.height);
			g.setColor(((Component)target).getForeground());
		}
		super.print(g);
	}
	


}
