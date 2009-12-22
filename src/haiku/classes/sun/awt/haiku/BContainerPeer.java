package sun.awt.haiku;

import java.awt.*;
import java.awt.peer.*;

import sun.awt.SunGraphicsCallback;
import java.awt.event.PaintEvent;

abstract class BContainerPeer extends BViewPeer implements ContainerPeer {
	/**
	 * Class creation/instantiation
	 */
	static {
		initIDs();
	}
	
	private static native void initIDs();
	
	/**
	 * Instance creation/initialization
	 */
	BContainerPeer(Component target) {
		super(target);
	}
	
	// ComponentPeer overrides

	public void paint(Graphics g) {
		super.paint(g);
		SunGraphicsCallback.PaintHeavyweightComponentsCallback.getInstance().
			runComponents(((Container)target).getComponents(), g,
				  SunGraphicsCallback.LIGHTWEIGHTS |
				  SunGraphicsCallback.HEAVYWEIGHTS);
	}
	
	public void print(Graphics g) {
		super.print(g);
		SunGraphicsCallback.PrintHeavyweightComponentsCallback.getInstance().
			runComponents(((Container)target).getComponents(), g,
				SunGraphicsCallback.LIGHTWEIGHTS | 
				SunGraphicsCallback.HEAVYWEIGHTS);
	}
	
	//
	//
	// ContainerPeer implementation
	//
	//
	
	protected Insets fInsets; // set from native

	public Insets getInsets() {
		return fInsets;
	}

    /**
     * DEPRECATED:  Replaced by getInsets().
     */
    public Insets insets() {
		return getInsets();
	}

	//
	// The following methods are all used around layout to help
	// us to cleanly update the GUI.  Without them, components
	// would presumably be jumping all around during layout.  With
	// them we can (hopefully) supress this ugly behavior, and do
	// a single batched update at the end.  The order of them is:
	//
	//    1. beginValidate() - called from Container.validate(), just 
	//           prior to "validateTree();", which recursively descends 
	//           the container tree and recomputes the layout for any 
	//           subtrees marked as needing it (those marked invalid)
	//           It will only do so, if this container is marked as !valid.
	//    2. beginLayout() - called from Container.validateTree() if our 
	//           Container was not valid.  it's called prior to doLayout
	//           on our Container.
	//    3. endLayout() - called from Container.validateTree() if our
	//           Container was not valid.  it's called after doLayout() 
	//           on our Container and validateTree on our non-window 
	//           contained Containers and validate on all other
	//           contained Components.
	//    4. endValidate() - called from Container.validate(), after
	//           after "validateTree();" [see above], and "valid = true;"
	//
	
    public void beginValidate() {
		_layoutBeginning();
	}
    public void beginLayout() {
		// Skip all painting till endLayout
		isLayouting = true;
	}
    public void endLayout() {
		if(!paintArea.isEmpty() && !paintPending &&
			!((Component)target).getIgnoreRepaint()) 
		{
			// if not waiting for native painting repaint damanged area
			postEvent(new PaintEvent((Component)target, PaintEvent.PAINT,
					new Rectangle()));
		}
		isLayouting = false;
	}
    public void endValidate() {
		_layoutEnded();
	}

	private native void _layoutBeginning();
	private native void _layoutEnded();

    public boolean isPaintPending() {
		// called from Component.reshape (!?), only if the component is a
		// lightweight, visible component, with a heavyweight ancestor
		return paintPending && isLayouting;
	}
}
