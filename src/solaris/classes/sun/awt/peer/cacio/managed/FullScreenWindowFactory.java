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

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import sun.awt.peer.cacio.CacioComponent;
import sun.awt.peer.cacio.CacioEventPump;
import sun.awt.peer.cacio.CacioEventSource;
import sun.awt.peer.cacio.PlatformToplevelWindow;
import sun.awt.peer.cacio.PlatformWindow;
import sun.awt.peer.cacio.PlatformWindowFactory;
import sun.security.action.GetPropertyAction;

public class FullScreenWindowFactory implements PlatformWindowFactory {

    private static final Dimension screenSize;
    static {
        String size = AccessController.doPrivileged(
                new GetPropertyAction("cacio.managed.screensize", "1024x768"));
        int x = size.indexOf('x');
        int width = Integer.parseInt(size.substring(0, x));
        int height = Integer.parseInt(size.substring(x + 1));
        screenSize = new Dimension(width, height);
    }

    /**
     * This is used to re-source the events coming from the
     * PlatformScreen to the corresponding ManagedWindowContainer.
     */
    private class FullScreenEventSource implements CacioEventSource {
        public EventData getNextEvent() throws InterruptedException {
            EventData d = eventSource.getNextEvent();
            PlatformScreen source = (PlatformScreen) d.getSource();
            d.setSource(screenMap.get(source));
            return d;
        }
    }

    /**
     * We need a selector to select the actual screen in case
     * of multiple screens. A default selector implementation
     * is given if only one screen is available.
     * the selector is used in createPlatformToplevelWindow
     * to define
     */
    private final PlatformScreenSelector selector;

    /**
     * This allows the mappings between PlatformScreen and
     * ScreenManagedWindowContainer, and is needed to re-source events
     * from PlatformScreen to ManagedWindowContainer.
     */
    private final Map<PlatformScreen, ScreenManagedWindowContainer> screenMap;

    /**
     * The event source that generates the basic events.
     * Note: We create and return a FullScreenEventSource that
     * uses this eventSource as 'backend' and infers all the higher
     * level events.
     */
    private CacioEventSource eventSource;

    /**
     * Constructs a new FullScreenWindowFactory that uses the
     * specified container as container for all toplevel windows.
     *
     * The event source is expected to generate the following event types:
     * <ul>
     * <li>{@code MouseEvent.MOUSE_PRESSED}</li>
     * <li>{@code MouseEvent.MOUSE_RELEASED}</li>
     * <li>{@code MouseEvent.MOUSE_MOVED}</li>
     * <li>{@code KeyEvent.KEY_PRESSED}</li>
     * <li>{@code KeyEvent.KEY_RELEASED}</li>
     * </ul>
     *
     * All the other events (component, window, focus, remaining mouse and key
     * events) are inferred and synthesized by the event source that is
     * created from this factory.
     *
     * @param screen the container to be used for toplevel windows
     * @param s the event source to use
     */
    public FullScreenWindowFactory(PlatformScreen screen,
                                   CacioEventSource s) {

        this(new DefaultScreenSelector(screen), s);
    }

    public FullScreenWindowFactory(PlatformScreenSelector screenSelector,
                                   CacioEventSource s) {

        this.selector = screenSelector;
        this.eventSource = s;
        screenMap =
                Collections.synchronizedMap(new HashMap<PlatformScreen,
                                            ScreenManagedWindowContainer>());
    }

    /**
     * Creates a {@link PlatformWindow} instance.
     *
     * @param cacioComponent the corresponding Cacio component
     * @parent the parent window, or <code>null</code> for top level windows
     *
     * @return the platform window instance
     */
    public final PlatformWindow createPlatformWindow(CacioComponent awtComponent,
                                                     PlatformWindow parent) {
        if (parent == null) {
            throw new IllegalArgumentException("parent cannot be null");
        }

        ManagedWindow p = (ManagedWindow) parent;
        return new ManagedWindow(p, awtComponent);
    }

    @Override
    public final
    PlatformToplevelWindow createPlatformToplevelWindow(CacioComponent comp) {

        GraphicsConfiguration gc =
            comp.getAWTComponent().getGraphicsConfiguration();

        PlatformScreen screen = selector.getPlatformScreen(gc);
        ScreenManagedWindowContainer smwc = screenMap.get(screen);
        if (smwc == null) {
            smwc = new ScreenManagedWindowContainer(screen);
            screenMap.put(screen, smwc);
        }

        return new ManagedWindow(smwc, comp);
    }

    /**
     * {@inheritDoc }
     * <br /><br />
     * <strong>Note</strong>: owners are currently ignored in fully managed
     * windows.
     */
    @Override
    public PlatformWindow createPlatformToplevelWindow(CacioComponent component,
                                                       PlatformWindow notUsed) {

        return createPlatformToplevelWindow(component);
    }

    @Override
    public CacioEventPump<?> createEventPump() {
        FullScreenEventSource s = new FullScreenEventSource();
        return new FullScreenEventPump(s);
    }

    public static Dimension getScreenDimension() {
        return screenSize;
    }

    /**
     * Default implementation for the PlatformScreenSelector. Just return
     * the single screen instance we have.
     */
    private static final class DefaultScreenSelector implements
        PlatformScreenSelector {

        PlatformScreen screen = null;

        DefaultScreenSelector(PlatformScreen screen) {

            this.screen = screen;
        }

        @Override
        public PlatformScreen getPlatformScreen(GraphicsConfiguration config) {

            return this.screen;
        }
    }
    
    public ScreenManagedWindowContainer getScreenManagedWindowContainer(PlatformScreen screen) {
	return screenMap.get(screen);
    }
}
