/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
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

package sun.awt.peer.cacio.managed;

import sun.awt.peer.cacio.*;
import java.awt.AWTEvent;
import java.awt.AWTException;
import java.awt.BufferCapabilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.BufferCapabilities.FlipContents;
import java.awt.event.MouseEvent;
import java.awt.event.PaintEvent;
import java.awt.peer.ContainerPeer;
import java.awt.image.ColorModel;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import sun.awt.CausedFocusEvent.Cause;
import sun.awt.ConstrainableGraphics;
import sun.java2d.pipe.Region;

/**
 * A {@link PlatformWindow} implementation that uses a ManagedWindowContainer
 * as parent and implements all the window management logic in Java. This
 * is most useful on target systems without or with limited window support.
 */
class ManagedWindow
    extends AbstractManagedWindowContainer
    implements PlatformToplevelWindow {

    /**
     * The parent container.
     */
    private ManagedWindowContainer parent;

    /**
     * The corresponding cacio component.
     */
    private CacioComponent cacioComponent;

    /**
     * The bounds of this window, relative to the parent container.
     */
    private int x, y, width, height;

    /**
     * The background color of the component window.
     */
    private Color background;

    /**
     * The foreground color of the component window.
     */
    private Color foreground;

    /**
     * The font of the component window.
     */
    private Font font;

    /**
     * Indicates if this window is visible or not.
     */
    private boolean visible;

    /**
     * Constructs a new ManagedWindow, that has the specified parent
     * container.
     *
     * @param p the parent container
     * @param cacioComp the cacio component for this window
     */
    ManagedWindow(ManagedWindowContainer p, CacioComponent cacioComp) {
        super();
        parent = p;
        cacioComponent = cacioComp;
        parent.add(this);
        Component c = cacioComponent.getAWTComponent();
        setBounds(c.getX(), c.getY(), c.getWidth(), c.getHeight(), 0);
    }

    @Override
    public ColorModel getColorModel() {
        return parent.getColorModel();
    }

    @Override
    public Graphics2D getClippedGraphics(Color bg, Color fg, Font font,
                                         List<Rectangle> clipRects) {
        // Translate all clip rectangles to parent's coordinate system.
        if (clipRects != null) {
            for (Rectangle r : clipRects) {
                r.x += x;
                r.y += y;
            }
        }
        return prepareClippedGraphics(bg, fg, font, clipRects);
    }

    @Override
    public Graphics2D getGraphics(Color fg, Color bg, Font font) {
        // Our children obscure the container. Clip them.
        LinkedList<Rectangle> clips;
        LinkedList<ManagedWindow> children = getChildren();
        if (children != null && children.size() > 0) {
            clips = new LinkedList<Rectangle>();
            for (ManagedWindow child : children) {
                if (child.isVisible()) {
                    clips.add(child.getBounds());
                }
            }
        } else {
            clips = null;
        }

        // Check if we have obscuring siblings and add their clip
        // rectangles to the list.
        Graphics2D g2d = getClippedGraphics(fg, bg, font, clips);
        return g2d;
    }

    private Graphics2D prepareClippedGraphics(Color fg, Color bg, Font font,
                                              List<Rectangle> clipRects) {
        // Add clip rectangles of any siblings.
        clipRects = addClipRects(clipRects);
        // Ask parent for clipped graphics.
        Graphics2D pg = parent.getClippedGraphics(fg, bg, font, clipRects);
        // Translate and clip to our own coordinate system.
        if (pg instanceof ConstrainableGraphics)  {
            ((ConstrainableGraphics) pg).constrain(x, y, width, height);
        } else {
            pg = (Graphics2D) pg.create(x, y, width, height);
        }
        return pg;
    }

    /**
     * This method adds any necessary clip rectangles to the specified
     * list. If the list is null and rectangles need to be added,
     * then a new one is created. This method might return null, if no
     * clip rectangles need to be added and the input argument has been null.
     *
     * @param clipRects the list of clip rectangles before adding new
     *        rectangles, possibly null
     *
     * @return the list of clip rectangles, possibly null
     */
    private List<Rectangle> addClipRects(List<Rectangle> clipRects) {
        Deque<ManagedWindow> siblings = parent.getChildren();
        if (siblings.getLast() != this) {
            if (clipRects == null) {
                clipRects = new LinkedList<Rectangle>();
            }
            Iterator<ManagedWindow> i = siblings.descendingIterator();
            Rectangle myBounds = getBounds();
            while (i.hasNext()) {
                ManagedWindow sibling = i.next();
                if (sibling == this) {
                    break;
                }
                if (sibling.isVisible()) {
                    Rectangle bounds = sibling.getBounds();
                    if (bounds.intersects(myBounds)) {
                        clipRects.add(bounds);
                    }
                }
            }
        }
        return clipRects;
    }

    @Override
    public GraphicsConfiguration getGraphicsConfiguration() {
        return parent.getGraphicsConfiguration();
    }

    @Override
    public void dispose() {
        setVisible(false); // To trigger repaint.
        parent.remove(this);
    }

    @Override
    public Rectangle getBounds() {
        // Return new rectangle, so client code won't mess with our data.
        return new Rectangle(x, y, width, height);
    }

    @Override
    public void setBounds(int x, int y, int width, int height, int op) {

        int oldX = this.x;
        int oldY = this.y;
        int oldW = this.width;
        int oldH = this.height;

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        // TODO: We brute-force repaint everything that could be damaged.
        // Make this a little more intelligent.

        if (isVisible()) {
            // First we repaint the parent at the old bounds.
            parent.repaint(oldX, oldY, oldW, oldH);
            // And then we repaint the parent at the new bounds.
            parent.repaint(x, y, width, height);
        }
    }

    @Override
    public Insets getInsets() {
        // No decorations yet -> no insets. Return new Insets always, so
        // client code can't mess with our data.
        return new Insets(0, 0, 0, 0);
    }

    @Override
    public Point getLocationOnScreen() {
        Point p = parent.getLocationOnScreen();
        p.x += x;
        p.y += y;
        return p;
    }

    @Override
    public boolean canDetermineObscurity() {
        return true;
    }

    @Override
    public boolean isObscured() {
        return isRegionObscured(0, 0, width, height);
    }

    private boolean isRegionObscured(int x, int y, int w, int h) {
        // We are obscured when:
        // 1. The parent is obscured. TODO: Optimize!!
        // 2. Or when we have overlapping siblings.
        return isParentObscured(x, y, w, h)
               || hasOverlappingSiblings(x, y, w, h);
    }

    private boolean isParentObscured(int x, int y, int w, int h) {
        ManagedWindowContainer parent = getParent();
        boolean isParentObscured;
        if (parent instanceof ManagedWindow) {
            ManagedWindow p = (ManagedWindow) parent;
            isParentObscured = p.isRegionObscured(x + this.x, y + this.y,
                                                  w, h);
        } else {
            // Non- ManagedWindow parents can only be toplevel containers
            // and are never obscured.
            isParentObscured = false;
        }
        return isParentObscured;
    }

    private boolean hasOverlappingSiblings(int x, int y, int w, int h) {
        Deque<ManagedWindow> siblings = getParent().getChildren();
        boolean hasOverlappingSiblings = false;
        Rectangle myBounds = new Rectangle(x, y, w, h);
        // Only windows that are 'over' the target region can be overlapping.
        Iterator<ManagedWindow> descIter = siblings.descendingIterator();
        while (descIter.hasNext() && ! hasOverlappingSiblings) {
            ManagedWindow sibling = descIter.next();
            if (sibling == this) {
                break;
            }
            Rectangle siblingBounds = sibling.getBounds();
            hasOverlappingSiblings = myBounds.intersects(siblingBounds);
        }
        return hasOverlappingSiblings;
    }

    @Override
    public void applyShape(Region shape) {
        // No support for shaped windows.
    }

    @Override
    public boolean isReparentSuppored() {
        // TODO: Implement this for real.
        return false;
    }

    @Override
    public void reparent(ContainerPeer newContainer) {
        // TODO: Implement this for real.
    }

    @Override
    public boolean isRestackSupported() {
        // TODO: Implement this for real.
        return false;
    }

    @Override
    public void restack() {
        // TODO: Implement this for real.
    }

    @Override
    public void setVisible(boolean v) {
        if (v != visible) {
            visible = v;

            Rectangle b = getBounds();
            b.x = b.y = 0;
            triggerRepaint(b);

            // We also need to notify all the children, recursively.
            for (ManagedWindow w : getChildren()) {
                if (w.getCacioComponent().getAWTComponent().isVisible()) {
                    w.setVisible(v);
                }
            }
            FocusManager.getInstance().setVisible(this, v);
        }
    }

    private void triggerRepaint(Rectangle b) {
        // If we completely contain the specified rectangle and are not
        // obscured by other windows, and we are visible ourselves,
        // then we don't need to go further up.
        Rectangle myBounds = getBounds();
        myBounds.x = myBounds.y = 0;
        if (isVisible() && myBounds.contains(b)
            && ! isRegionObscured(b.x, b.y, b.width, b.height)) {
            repaint(b.x, b.y, b.width, b.height);
        } else {
            ManagedWindowContainer c = getParent();
            if (c instanceof ManagedWindow) {

                ManagedWindow mw = (ManagedWindow) c;
                if (mw.isVisible()) {
                    b.x += x;
                    b.y += y;
                    ((ManagedWindow) c).triggerRepaint(b);
                }
            } else  {
                c.repaint(b.x + x, b.y + y, b.width, b.height);
            }
        }
    }

    boolean isVisible() {
        return visible;
    }

    @Override
    public int getState() {
        // TODO: Implement this.
        return 0;
    }

    @Override
    public void setState(int state) {
        // TODO: Implement this.
    }

    @Override
    public void setMaximizedBounds(Rectangle bounds) {
        // TODO: Implement this.
    }

    @Override
    public void setResizable(boolean resizable) {
        // TODO: Implement this.
    }

    @Override
    public void setTitle(String title) {
        // TODO: Implement this.
    }

    @Override
    public void setBlocked(boolean blocked) {
        // TODO: Implement this.
    }

    @Override
    public boolean requestFocus(Component lightweightChild, boolean temporary,
                                boolean focusedWindowChangeAllowed, long time,
                                Cause cause) {
        FocusManager fm = FocusManager.getInstance();
        fm.setFocusedWindow(this);
        return true;
    }

    CacioComponent getCacioComponent() {
        return cacioComponent;
    }

    public void dispatchEvent(AWTEvent event) {
        getCacioComponent().handlePeerEvent(event);
    }

    protected boolean dispatchEventImpl(EventData event) {
        // First try to dispatch to a child.
        boolean dispatched = super.dispatchEventImpl(event);
        if (! dispatched) {
            // If not dispatched, then dispatch to our own component.
            event.setSource(cacioComponent.getAWTComponent());
            cacioComponent.handlePeerEvent(event.createAWTEvent());
            dispatched = true;
            if (event.getId() == MouseEvent.MOUSE_PRESSED) {
                FocusManager.getInstance().mousePressed(this);
            }
        }
        return dispatched;
    }

    void dispatchKeyEvent(EventData ev) {
        ev.setSource(cacioComponent.getAWTComponent());
        cacioComponent.handlePeerEvent(ev.createAWTEvent());
    }

    @Override
    public void createBuffers(int numBuffers, BufferCapabilities caps)
        throws AWTException {

        // TODO: Implement this correctly.        
        throw new AWTException("Not yet supported.");
    }

    @Override
    public void destroyBuffers() {
        // Nothing to do here yet.        
    }

    @Override
    public void flip(int x1, int y1, int x2, int y2, FlipContents flipAction) {
        // Nothing to do here yet.        
    }

    @Override
    public Image getBackBuffer() {
        // Nothing to do here yet.        
        return null;
    }

    ManagedWindowContainer getParent() {
        return parent;
    }

    @Override
    public void repaint(int x, int y, int w, int h) {
        CacioComponent cacioComp = getCacioComponent();
        Component awtComp = cacioComp.getAWTComponent();
        // We need to be relative to the target.
        Rectangle area = new Rectangle(x, y, w, h);
        PaintEvent ev = new PaintEvent(awtComp, PaintEvent.UPDATE, area);
        cacioComp.handlePeerEvent(ev);
        super.repaint(x, y, w, h);
    }

    @Override
    public void requestFocus() {
        FocusManager.getInstance().setFocusedWindowNoEvent(this);
    }
}
