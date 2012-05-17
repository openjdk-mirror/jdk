/*
 * Copyright 2008-2009 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package sun.awt.peer.cacio;

import java.awt.AWTEvent;
import java.awt.AWTException;
import java.awt.BufferCapabilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.Window;

import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.PaintEvent;

import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.VolatileImage;
import java.awt.image.WritableRaster;

import java.awt.peer.ComponentPeer;
import java.awt.peer.ContainerPeer;

import javax.swing.JComponent;

import sun.awt.SunToolkit;
import sun.awt.CausedFocusEvent.Cause;
import sun.awt.*;

import sun.awt.RepaintArea;
import sun.awt.event.IgnorePaintEvent;
import sun.awt.image.OffScreenImage;
import sun.font.FontDesignMetrics;

import sun.java2d.pipe.Region;

/**
 * The central superclass for all peers. In the Cacio peers, all components are
 * acting like this:
 * <ul>
 * <li>Each component has its own native window. The actual implementation of
 * this window has to be done in an implementation of {@link PlatformWindow}.
 * </li>
 * <li>The native window implementation feeds all relevant AWT events back into
 * the CacioComponentPeer for further processing.</li>
 * <li>Each component is drawn using a corresponding Swing component (e.g. an
 * AWT Button is manages its Swing JButton in the background).</li>
 * <li>The logic of the components is also implemented by the corresponding
 * Swing component.</li>
 * </ul>
 */
class CacioComponentPeer<AWTComponentType extends Component,
                         SwingComponentType extends JComponent>
    implements ComponentPeer, CacioComponent {

    /**
     * The AWT component that corresponds to this component peer.
     */
    private AWTComponentType awtComponent;

    /**
     * The backing Swing component. Some components don't have a backing
     * swing component and leave that to <code>null</code>.
     */
    private SwingComponentType swingComponent;

    /**
     * The proxy for the Swing component.
     */
    private ProxyWindow proxy;

    /**
     * The underlying native platform window.
     */
    PlatformWindow platformWindow;

    /**
     * The current repaint area.
     */
    private RepaintArea paintArea;

    private Rectangle viewRect;
    
    /**
     * Flag to indicate that a change requires a repaint 
     * of the background.
     */
    private boolean needsClearBackground = false;

    /**
     * Creates a new CacioComponentPeer.
     * 
     * @param awtC
     *            the AWT component
     * @param pwf
     *            a platform window factory for creating the platform window
     */
    CacioComponentPeer(AWTComponentType awtC, PlatformWindowFactory pwf) {
        awtComponent = awtC;
        init(pwf);
        swingComponent = initSwingComponent();
        initProxy();
        postInitSwingComponent();
        // Initialize basic properties.
        setBounds(awtC.getX(), awtC.getY(), awtC.getWidth(), awtC.getHeight(),
                  ComponentPeer.SET_SIZE);
        paintArea = new RepaintArea() {
            @Override
            protected void updateComponent(Component comp, Graphics g) {
                Graphics g2 = getGraphicsImpl();
                g2.setClip(g.getClip());
                peerPaint(g2, true);
                g2.dispose();
                super.updateComponent(comp, g);
            }
            @Override
            protected void paintComponent(Component comp, Graphics g) {
                Graphics g2 = getGraphicsImpl();
                g2.setClip(g.getClip());
                peerPaint(g2, false);
                g2.dispose();
                super.paintComponent(comp, g);
            }
        };
    }

    /**
     * Initializes this peer. Most importantly, this should initialize the
     * underlying platform window using the specified
     * {@link PlatformWindowFactory}.
     * 
     * @param pwf
     */
    void init(PlatformWindowFactory pwf) {
        
        // Figure out the heavyweight parent window.
        PlatformWindow parent = null; // Assume toplevel window for the start.
        Component parentComp = awtComponent.getParent();
        while (parentComp != null && parent == null) {
            if (parentComp.isLightweight()) {
                parentComp = parentComp.getParent();
            } else {
                CacioComponentPeer parentPeer =
                    (CacioComponentPeer) parentComp.getPeer();
                parent = parentPeer.platformWindow;
            }
        }
        platformWindow = pwf.createPlatformWindow(this, parent);
    }

    private void initProxy() {
        if (swingComponent != null) {
            // Setup the proxy window.
            proxy = new ProxyWindow(this, swingComponent);
            proxy.setBounds(awtComponent.getX(), awtComponent.getY(),
                            awtComponent.getWidth(), awtComponent.getHeight());
            proxy.setVisible(awtComponent.isVisible());
        }
    }

    void postInitSwingComponent() {
        // All AWT widgets are expected to be 100% opaque, so we force this
        // on the swing components.
        swingComponent.setOpaque(true);

        // We use the inherited properties here, not the real one.
        Color bg = awtComponent.getBackground();
        if (bg != null) {
            setBackground(bg);
        }
        Color fg = awtComponent.getForeground();
        if (fg != null) {
            setForeground(fg);
        }
        Font font = awtComponent.getFont();
        if (font != null) {
            setFont(font);
        }
        boolean enabled = AWTAccessor.getComponentAccessor().isEnabled(awtComponent);
        setEnabled(enabled);
    }

    SwingComponentType initSwingComponent() {
        
        return (SwingComponentType) new JComponent() {};
    }

    /**
     * Disposes the peer object and releases all associated native resources.
     */
    @Override
    public void dispose() {
        
        platformWindow.dispose();
	CacioToolkit.disposePeer(awtComponent, this);
    }

    @Override
    public ColorModel getColorModel() {

        return platformWindow.getColorModel();
    }

    /**
     * Creates and returns a graphics object that is used for painting.
     * The difference between this and {@link #getGraphics()} is that
     * this one returns a graphics that can be used to paint on the whole
     * component, while {@link #getGraphics()} clips away the insets. (The
     * public AWT must never ever paint over the insets of a component, e.g.
     * the menu + decorations of a window.)
     *
     * @return a prepared graphics object for painting on the component
     */
    private Graphics2D getGraphicsImpl() {

        Color bg = getBackground();
        Color fg = getForeground();
        Font font = getFont();
        Graphics2D g = platformWindow.getGraphics(fg, bg, font);
        if (viewRect != null) {
            g.clipRect(viewRect.x, viewRect.y, viewRect.width, viewRect.height);
            g.translate(viewRect.x, viewRect.y);
        }

        return g;
    }

    @Override
    public Graphics getGraphics() {
        Graphics2D g = getGraphicsImpl();
        if (hasInsets()) {
            Insets i = getInsets();
            Component c = getAWTComponent();
            int w = c.getWidth();
            int h = c.getHeight();
            // ConstrainableGraphics is the only way to ensure that the clip
            // that we set is not revertible by setClip(null).
            ((ConstrainableGraphics) g).constrain(i.left, i.top,
                                                  w - i.left - i.right,
                                                  h - i.top - i.bottom);
            g.translate(-i.left, -i.top);
        }
        return g;
    }

    /**
     * Returns {@code true} when this component has insets, {@code false}
     * otherwise. This allows for optimizations in {@link #getGraphics() }.
     *
     * @return {@code true} when this component has insets, {@code false}
     *         otherwise
     */
    boolean hasInsets() {
        return false;
    }

    @Override
    public GraphicsConfiguration getGraphicsConfiguration() {

        return platformWindow.getGraphicsConfiguration();
    }

    @Override
    public FontMetrics getFontMetrics(Font font) {

        return FontDesignMetrics.getMetrics(font);
    }

    @Override
    public Point getLocationOnScreen() {

        return platformWindow.getLocationOnScreen();
    }

    @Override
    public Dimension getMinimumSize() {

        Dimension min;
        if (swingComponent != null) {
            min = swingComponent.getMinimumSize();
        } else {
            min = new Dimension(0, 0);
        }
        return min;
    }

    @Override
    public Dimension getPreferredSize() {

        Dimension pref;
        if (swingComponent != null) {
            pref = swingComponent.getPreferredSize();
        } else {
            pref = new Dimension(0, 0);
        }
        return pref;
    }

    @Override
    public Toolkit getToolkit() {

        return Toolkit.getDefaultToolkit();
    }

    @Override
    public void handleEvent(AWTEvent e) {

        switch (e.getID())
        {
        case PaintEvent.UPDATE:
        case PaintEvent.PAINT:
            if (! isLayouting()) {
                boolean tmp = needsClearBackground;
                needsClearBackground = false;
                paintArea.paint(getAWTComponent(), tmp);
            }
          break;
        case MouseEvent.MOUSE_PRESSED:
        case MouseEvent.MOUSE_RELEASED:
        case MouseEvent.MOUSE_CLICKED:
        case MouseEvent.MOUSE_ENTERED:
        case MouseEvent.MOUSE_EXITED:
          handleMouseEvent((MouseEvent) e);
          break;
        case MouseEvent.MOUSE_MOVED:
        case MouseEvent.MOUSE_DRAGGED:
          handleMouseMotionEvent((MouseEvent) e);
          break;
        case KeyEvent.KEY_PRESSED:
        case KeyEvent.KEY_RELEASED:
        case KeyEvent.KEY_TYPED:
          handleKeyEvent((KeyEvent) e);
          break;
        case FocusEvent.FOCUS_GAINED:
        case FocusEvent.FOCUS_LOST:
          handleFocusEvent((FocusEvent)e);
          break;
        default:
          // Other event types are not handled here.
          break;
        }

    }

    /**
     * Handles mouse events on the component. This is usually forwarded to the
     * SwingComponent's processMouseEvent() method.
     *
     * @param e the mouse event
     */
    protected void handleMouseEvent(MouseEvent e) {
        
        if (proxy != null)
            proxy.handleMouseEvent(e);
    }

    /**
     * Handles mouse motion events on the component. This is usually forwarded
     * to the SwingComponent's processMouseMotionEvent() method.
     *
     * @param e the mouse motion event
     */
    protected void handleMouseMotionEvent(MouseEvent e) {
        
        if (proxy != null)
            proxy.handleMouseMotionEvent(e);
    }

    /**
     * Handles key events on the component. This is usually forwarded to the
     * SwingComponent's processKeyEvent() method.
     *
     * @param e the key event
     */
    protected void handleKeyEvent(KeyEvent e) {
        
        if (proxy != null)
            proxy.handleKeyEvent(e);
    }

    /**
     * Handles focus events on the component. This is usually forwarded to the
     * SwingComponent's processFocusEvent() method.
     *
     * @param e the key event
     */
    protected void handleFocusEvent(FocusEvent e) {
        
        if (proxy != null)
            proxy.handleFocusEvent(e);
    }

    protected void peerPaint(Graphics g, boolean update) {

        Graphics peerG = g.create();
        try {
            if (swingComponent != null) {
                JComponent c = swingComponent;
                c.paint(peerG);
            }
        } finally {
            peerG.dispose();
        }

    }

    protected void peerRepaint(int x, int y, int width, int height) {
        if (EventQueue.isDispatchThread()) {
              Graphics g = awtComponent.getGraphics();
              try {
                  g.clipRect(x, y, width, height);
                  peerPaint(g, false);
              } finally {
                  g.dispose();
              }
        } else {
            PaintEvent event = PaintEventDispatcher.getPaintEventDispatcher().createPaintEvent((Component) awtComponent, 0,0, awtComponent.getWidth(), awtComponent.getHeight());
            awtComponent.getToolkit().getSystemEventQueue().postEvent(event);
        }

    }

    @Override
    public boolean handlesWheelScrolling() {

        // Only few components handle wheel scrolling (e.g. TextArea), but
        // most don't. Therefore we generally return false here. Needs to
        // be overridden by sub peers that actually handle wheel scrolling.
        return false;

    }

    @Override
    public boolean isFocusable() {

        boolean ret;
        if (swingComponent != null) {
            ret = swingComponent.isFocusable();
        } else {
            ret = false;
        }
        return ret;
    }

    @Override
    public boolean isReparentSupported() {

        return platformWindow.isReparentSuppored();
    }

    @Override
    public void reparent(ContainerPeer newContainer) {

        platformWindow.reparent(newContainer);
    }

    @Override
    public void layout() {

        // Nothing to do here. Few peers need to perform layout, those must
        // override this method.
    }

    @Override
    public void paint(Graphics g) {

        peerPaint(g, true);
    }

    @Override
    public void print(Graphics g) {

        peerPaint(g, true);
    }

    /* FIX ME: these constants copied from java.awt.KeyboardFocusManager */
    static final int SNFH_FAILURE = 0;
    static final int SNFH_SUCCESS_HANDLED = 1;
    static final int SNFH_SUCCESS_PROCEED = 2;

    public boolean requestFocus(Component lightweightChild, boolean temporary,
            boolean focusedWindowChangeAllowed, long time, Cause cause) {

        if (KFMHelper.processSynchronousLightweightTransfer(getAWTComponent(),
                                                   lightweightChild,
                                                   temporary,
                                                   focusedWindowChangeAllowed,
                                                   time)) {
            return true;
        }

        int result = KFMHelper.shouldNativelyFocusHeavyweight(
                                                   getAWTComponent(),
                                                   lightweightChild,
                                                   temporary,
                                                   focusedWindowChangeAllowed,
                                                   time, cause);

        switch (result) {
        case SNFH_FAILURE:
            return false;
        case SNFH_SUCCESS_PROCEED:
            PlatformWindow pw = getPlatformWindow();
            pw.requestFocus();
            return CacioKeyboardFocusManagerPeer.getInstance().requestFocus(getAWTComponent(),
                                                                            lightweightChild,
                                                                            temporary,
                                                                            focusedWindowChangeAllowed,
                                                                            time, cause);
        case SNFH_SUCCESS_HANDLED:
            // Either lightweight or excessive request - all events are generated.
            return true;
        default:
            return false;
        }
    }

    public void setBackground(Color c) {

        swingComponent.setBackground(c);

    }

    Color getBackground() {
        return swingComponent.getBackground();
    }

    @Override
    public void setFont(Font f) {

        swingComponent.setFont(f);

    }

    Font getFont() {
        return swingComponent.getFont();
    }

    public void setForeground(Color c) {

        swingComponent.setForeground(c);

    }

    Color getForeground() {
        return swingComponent.getForeground();
    }

    private void setBoundsImpl(int x, int y, int width, int height, int op) {

        platformWindow.setBounds(x, y, width, height, op);

    }

    void setViewport(int vx, int vy, int vw, int vh) {
        setBoundsImpl(vx, vy, vw, vh, SET_BOUNDS);
        viewRect = new Rectangle(vx, vy, vw, vh);
    }

    public void setBounds(int x, int y, int width, int height, int op) {

        setBoundsImpl(x, y, width, height, op);

        if (proxy != null) {
            // Use the updated bounds from the awtCompnent here. The new bounds
            // may be different from the given paramenters if 'op' was SET_CLIENT_SIZE
            // or if this is a top level window and the system has forced some
            // different bounds.
            proxy.setBounds(awtComponent.getX(), awtComponent.getY(),
                            awtComponent.getWidth(), awtComponent.getHeight());
        }
        if (swingComponent != null) {
            // The swing component is laid out relative to the proxy window.
            // That's why it starts at (0, 0).
            swingComponent.setBounds(0, 0, width, height);
            // We need to validate the swing component after resizing,
            // otherwise complex components (e.g. JScrollPane, etc) are not
            // laid out correctly.
            swingComponent.validate();
        }
    }

    public void setEnabled(boolean enable) {

        setEnabledImpl(enable);
    }

    void setEnabledImpl(boolean enable) {

        if (swingComponent != null) {
            swingComponent.setEnabled(enable);
        }
    }

    boolean isEnabled() {

        return swingComponent.isEnabled();
    }

    boolean isParentsEnabled() {
        Component c = getAWTComponent();
        boolean parentsEnabled = c.isEnabled();
        if (parentsEnabled) {
            Container parent = c.getParent();
            if (parent != null) {
                ComponentPeer peer = parent.getPeer();
                if (peer instanceof CacioComponentPeer) {
                    parentsEnabled =
                            ((CacioComponentPeer) peer).isParentsEnabled();
                }
            }
        }
        return parentsEnabled;
    }

    public void setVisible(boolean b) {
        needsClearBackground = b;
        if (proxy != null) {
            proxy.setVisible(b);
        }
        platformWindow.setVisible(b);
    }

    public void updateCursorImmediately() {

        // TODO: Implement using GlobalCursorManager...

    }

    public int checkImage(Image img, int w, int h, ImageObserver o) {

        return Toolkit.getDefaultToolkit().checkImage(img, w, h, o);

    }

    public boolean prepareImage(Image img, int w, int h, ImageObserver o) {

        return Toolkit.getDefaultToolkit().prepareImage(img, w, h, o);

    }

    public Image createImage(ImageProducer producer) {

        return Toolkit.getDefaultToolkit().createImage(producer);

    }

    public Image createImage(int width, int height) {

    return ((CacioToolkit)Toolkit.getDefaultToolkit()).createOffScreenImage(awtComponent,width,height);
    }

    public VolatileImage createVolatileImage(int width, int height) {

        GraphicsConfiguration gc = getGraphicsConfiguration();
        return gc.createCompatibleVolatileImage(width, height);

    }

    public void createBuffers(int numBuffers, BufferCapabilities caps)
        throws AWTException {

        platformWindow.createBuffers(numBuffers, caps);
    }

    public void destroyBuffers() {

        platformWindow.destroyBuffers();

    }

    public void flip(int x1, int y1, int x2, int y2,
                     BufferCapabilities.FlipContents flipAction) {

        platformWindow.flip(x1, y1, x2, y2, flipAction);

    }
    
    public Image getBackBuffer() {

        return platformWindow.getBackBuffer();

    }

    public void coalescePaintEvent(PaintEvent e) {

	if (!(e instanceof IgnorePaintEvent)) {
	    paintArea.add(e.getUpdateRect(), e.getID());
        }
    }

    public void applyShape(Region shape) {

        platformWindow.applyShape(shape);

    }

    public boolean canDetermineObscurity() {

        return platformWindow.canDetermineObscurity();

    }

    public boolean isObscured() {

        return platformWindow.isObscured();

    }

    public PlatformWindow getPlatformWindow() {
        return platformWindow;
    }

    public AWTComponentType getAWTComponent() {
        return awtComponent;
    }

    public void handlePeerEvent(AWTEvent event) {
        postEvent(event);
    }

    private void postEvent(AWTEvent event) {
        SunToolkit.postEvent(SunToolkit.targetToAppContext(event.getSource()),
                             event);
    }

    public Insets getInsets() {
        return (Insets) platformWindow.getInsets().clone();
    }

    SwingComponentType getSwingComponent() {
        return swingComponent;
    }

    ProxyWindow getProxyWindow() {
        return proxy;
    }

    @Override
    public void setZOrder(ComponentPeer above) {

        System.err.println("CacioComponentPeer::setZOrder: NOT YET IMPLEMENTED");
    }

    @Override
    public boolean updateGraphicsData(GraphicsConfiguration gc) {
        System.err.println("CacioComponentPeer::updateGraphicsData: NOT YET IMPLEMENTED");
        return false;
    }

    RepaintArea getPaintArea() {
        return paintArea;
    }

    boolean isLayouting() {
        return false;
    }

    /**
     * Mark this peer so that the next paint event clears the background.
     */
    public void clearBackground()
    {
      needsClearBackground = true;
    }

}
