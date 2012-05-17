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

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.*;

public class EventData {

    private static final int BUTTON_DOWN_MASK =
        MouseEvent.BUTTON1_DOWN_MASK
        | MouseEvent.BUTTON2_DOWN_MASK
        | MouseEvent.BUTTON3_DOWN_MASK;

    /**
     * The even id.
     */
    private int id;

    /**
     * The event source.
     */
    private Object source;

    /**
     * The timestamp of an input event.
     */
    private long time;

    /**
     * The modifiers of an input event.
     */
    private int modifiers;

    /**
     * The X location of a mouse event.
     */
    private int x;

    /**
     * The Y location of a mouse event.
     */
    private int y;

    /**
     * The click count of a mouse event.
     */
    private int clickCount;

    /**
     * The button of a mouse event.
     */
    private int button;

    /**
     * The update rectangle for paint events.
     */
    private Rectangle updateRect;

    /**
     * The key code for key events.
     */
    private int keyCode;

    /**
     * The key char for key events.
     */
    private char keyChar;


    private int lastModifierState;

    /**
     * Returns the event ID. This ID corresponds to the AWT event IDs.
     * The CacioEventPump creates AWT events depending on this ID.
     *
     * @return the event ID
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the event id.
     *
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Returns the source of the event. This will be either a
     * ManagedWindowContainer, a CacioComponent or an AWT Component. The
     * CacioEventPump dispatches the event to the source for further
     * processing, or, in the case of an AWT Component directly to the
     * AWT event queue.
     *
     * @return the event source
     */
    public Object getSource() {
        return source;
    }

    /**
     * Sets the event source.
     *
     * @param s the source to set
     */
    public void setSource(Object s) {
        source = s;
    }

    /**
     * Returns the timestamp of the event. This is only used for input
     * events.
     *
     * @return the timestamp of an event
     */
    public long getTime() {
        return time;
    }

    /**
     * Sets the time of the event.
     *
     * @param t the time to set
     */
    public void setTime(long t) {
        time = t;
    }

    /**
     * Returns the modifiers of the event. This is only used for input events.
     *
     * @return the modifiers of the event
     */
    public int getModifiers() {
        return modifiers;
    }

    /**
     * Sets the modifiers of the event. This is only used for input events.
     *
     * @param mods the modifiers of the event
     */
    public void setModifiers(int mods) {
        modifiers = mods;
    }

    /**
     * Returns the X location of the event. This is only used for mouse events.
     *
     * @return the X location of the event
     */
    public int getX() {
        return x;
    }

    /**
     * Sets the X location of the event. This is only used for mouse events.
     *
     * @param x the X location to set
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * Returns the Y location of the event. This is only used for mouse events.
     *
     * @return the Y location of the event
     */
    public int getY() {
        return y;
    }

    /**
     * Sets the Y location of the event. This is only used for mouse events.
     *
     * @param y the Y location to set
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * Returns the click count of the event. This is only used for mouse
     * events.
     *
     * @return the click count of the event
     */
    public int getClickCount() {
        return clickCount;
    }

    /**
     * Sets the click count of the event. This is only used for mouse events.
     *
     * @param c the clickCount to set
     */
    public void setClickCount(int c) {
        clickCount = c;
    }

    /**
     * Returns the button of the event. This is only used for mouse events.
     *
     * @return the button of the event
     */
    public int getButton() {
        return button;
    }

    /**
     * Sets the button of the event. This is only used for mouse events.
     *
     * @param b the button to set
     */
    public void setButton(int b) {
        button = b;
    }

    /**
     * Returns the update rectangle for paint events.
     *
     * @return the update rectangle for paint events
     */
    public Rectangle getUpdateRect() {
        return updateRect;
    }

    /**
     * Sets the update rectangle for paint events.
     *
     * @param r the update rectangle to set
     */
    public void setUpdateRect(Rectangle r) {
        updateRect = r;
    }

    /**
     * Returns the key code for key events.
     *
     * @return the key code for key events
     */
    public int getKeyCode() {
        return keyCode;
    }

    /**
     * Sets the key code for key events.
     *
     * @param keyCode the key code to set
     */
    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    /**
     * Returns the key char for key events.
     *
     * @return the key char for key events
     */
    public char getKeyChar() {
        return keyChar;
    }

    /**
     * Sets the key char for key events.
     *
     * @param keyChar the key code to set
     */
    public void setKeyChar(char keyChar) {
        this.keyChar = keyChar;
    }

    /**
     * Creates the corresponding AWT event from this event data.
     * This requires that the event source in this object is of the appropriate
     * type, i.e. an AWT Component for input events or an AWT Window for
     * window events, etc.
     *
     * @return the AWT event that corresponds to this event data
     */
    public AWTEvent createAWTEvent() {
        switch (id) {
        case MouseEvent.MOUSE_CLICKED:
        case MouseEvent.MOUSE_DRAGGED:
        case MouseEvent.MOUSE_ENTERED:
        case MouseEvent.MOUSE_EXITED:
        case MouseEvent.MOUSE_MOVED:
        case MouseEvent.MOUSE_PRESSED:
        case MouseEvent.MOUSE_RELEASED:
            int modifierChange = (lastModifierState ^ modifiers);
            lastModifierState = modifiers;
            return new MouseEvent((Component) source, id, time, modifiers,
                                  x, y, clickCount, false,
                                  getButton(modifierChange));
            
        case MouseEvent.MOUSE_WHEEL:
            modifierChange = (lastModifierState ^ modifiers);
            lastModifierState = modifiers;
            return new MouseWheelEvent((Component) source,MouseEvent.MOUSE_WHEEL, time,
                    modifiers,
                    x, y,
                    1, false, MouseWheelEvent.WHEEL_UNIT_SCROLL,
                    3, button==4 ?  -1 : 1);
            
        case KeyEvent.KEY_PRESSED:
        case KeyEvent.KEY_TYPED:
        case KeyEvent.KEY_RELEASED:
            return new KeyEvent((Component) source, id, time, modifiers,
                                keyCode, keyChar);
        case ComponentEvent.COMPONENT_MOVED:
        case ComponentEvent.COMPONENT_RESIZED:
        case ComponentEvent.COMPONENT_SHOWN:
        case ComponentEvent.COMPONENT_HIDDEN:
            return new ComponentEvent((Component) source, id);
        case PaintEvent.PAINT:
        case PaintEvent.UPDATE:
            return new PaintEvent((Component) source, id, updateRect);
        default:
            // TODO: Implement the others.
            return null;
        }
    }

    private int getButton(int theModifierChange) {
        switch (theModifierChange & BUTTON_DOWN_MASK) {
        case MouseEvent.BUTTON1_DOWN_MASK :
            return MouseEvent.BUTTON1;
        case MouseEvent.BUTTON2_DOWN_MASK :
            return MouseEvent.BUTTON2;
        case MouseEvent.BUTTON3_DOWN_MASK :
            return MouseEvent.BUTTON3;
        default :
            return MouseEvent.NOBUTTON;
        }
    }

    public void clear() {
        id = 0;
        source = null;
        time = 0L;
        modifiers = 0;
        x = 0;
        y = 0;
        clickCount = 0;
        button = 0;
        updateRect = null;
    }
}
