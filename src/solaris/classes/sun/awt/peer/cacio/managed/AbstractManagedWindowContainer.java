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

package sun.awt.peer.cacio.managed;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.Iterator;
import sun.awt.peer.cacio.CacioComponent;

/**
 * A base implementation of {@link ManagedWindowContainer}. This can be
 * used as a basis for PlatformWindow implementations that are aimed
 * to implement toplevel windows only, and use ManagedWindow instances
 * for nested windows.
 */
abstract class AbstractManagedWindowContainer
    implements ManagedWindowContainer {

    /**
     * The child windows of this container.
     */
    private LinkedList<ManagedWindow> children;

    /**
     * Constructs a new instance of AbstractManagedWindowContainer that
     * uses the specified parent container.
     */
    protected AbstractManagedWindowContainer() {
        children = new LinkedList<ManagedWindow>();
    }

    /**
     * Adds a child window to this container. This will be the topmost
     * window in the stack.
     *
     * @param child the window to add
     */
    @Override
    public final void add(ManagedWindow child) {
        children.add(child);

        Iterator<ManagedWindow> i = children.descendingIterator();
        while (i.hasNext()) {
            ManagedWindow _child = i.next();
            if (_child.isVisible()) {
                CacioComponent component = _child.getCacioComponent();
                component.getAWTComponent().repaint();
            }
        }
    }

    /**
     * Removes a child window from this container.
     *
     * @param child the window to be removed
     */
    @Override
    public final void remove(ManagedWindow child) {
        children.remove(child);

        Iterator<ManagedWindow> i = children.descendingIterator();
        while (i.hasNext()) {
            ManagedWindow _child = i.next();
            if (_child.isVisible()) {
                CacioComponent component = _child.getCacioComponent();
                component.getAWTComponent().repaint();
            }
        }
    }

    @Override
    public final LinkedList<ManagedWindow> getChildren() {
        return children;
    }

    /**
     * Returns the location of the specified child window on screen.
     *
     * The default implementation of this method assumes we are a toplevel
     * window and simply returns the location of the child window relative
     * to ourselves.
     */
    @Override
    public Point getLocationOnScreen() {
        return new Point(0, 0);
    }

    @Override
    public final void dispatchEvent(EventData event) {
        dispatchEventImpl(event);
    }

    boolean dispatchEventImpl(EventData event) {
        int id = event.getId();
        if (id >= MouseEvent.MOUSE_FIRST
            && id <= MouseEvent.MOUSE_LAST) {
            ManagedWindow source = findWindowAt(event.getX(), event.getY());
            if (source != null) {
                event.setSource(source);
                Rectangle b = source.getBounds();
                event.setX(event.getX() - b.x);
                event.setY(event.getY() - b.y);

                /*
                 * FIXME: This is a work around the FocusManager code, where
                 * we need to update the focus window for some selected events,
                 * a bug that shows up when mixing heavy wait and light wait
                 * components.
                 * This should be valid until the FocusManager and the focus
                 * handling code are rewritten.
                 */
                if (id == MouseEvent.MOUSE_CLICKED ||
                    id == MouseEvent.MOUSE_PRESSED)
                {
                    FocusManager.getInstance().setFocusedWindowNoEvent(source);
                }
                return source.dispatchEventImpl(event);

            } else {
                return false;
            }
        } else if (id >= KeyEvent.KEY_FIRST && id <= KeyEvent.KEY_LAST) {
            FocusManager fm = FocusManager.getInstance();
            ManagedWindow window = fm.getFocusedWindow();
            if (window != null) {
                window.dispatchKeyEvent(event);
            }
            return true;
        } else {
            return false;
        }
    }

    ManagedWindow findWindowAt(int x, int y) {
        // Search from topmost to bottommost component and see if one matches.
        Iterator<ManagedWindow> i = children.descendingIterator();
        while (i.hasNext()) {
            ManagedWindow child = i.next();
            if (child.isVisible()) {
                Rectangle b = child.getBounds();
                if (x >= b.x && y >= b.y
                    && x < (b.x + b.width) && y < (b.y + b.height)) {
                    return child;
                }
            }
        }
        // If we reach here, we found no child at those coordinates.
        return null;
    }

    @Override
    public void repaint(int x, int y, int w, int h) {
        // Non-managed window containers are usually toplevel containers.
        // Since they have no repaint mechanism of their own, we need to
        // trigger repainting on them here, so that they can clear their
        // background when necessary.
        if (! (this instanceof ManagedWindow)) {
            repaintSelf(x, y, w, h);
        }
        // Repaint the correct rectangles for all visible children that
        // are inside this rectangle.
        Iterator<ManagedWindow> i = children.descendingIterator();
        Rectangle rect = new Rectangle(x, y, w, h);
        Rectangle intersect = new Rectangle();
        while (i.hasNext()) {
            ManagedWindow child = i.next();
            if (child.isVisible()) {
                Rectangle b = child.getBounds();
                Rectangle2D.intersect(b, rect, intersect);
                if (! intersect.isEmpty()) {
                    // We need to be relative to the target.
                    Rectangle area = new Rectangle(intersect);
                    area.x -= b.x;
                    area.y -= b.y;
                    child.repaint(area.x, area.y, area.width, area.height);
                }
            }
        }
    }

    private void repaintSelf(int x, int y, int w, int h) {
        Rectangle r = new Rectangle(x, y, w, h);
        LinkedList<Rectangle> rects = new LinkedList<Rectangle>();
        for (ManagedWindow c : children) {
            if (c.isVisible()) {
                Rectangle b = c.getBounds();
                if (b.intersects(r)) {
                    rects.add(b);
                }
            }
        }
        Graphics2D g = getClippedGraphics(Color.WHITE, Color.WHITE,
                                          new Font(Font.DIALOG, Font.BOLD, 12),
                                          rects);
        g.clearRect(x, y, w, h);
    }
}
