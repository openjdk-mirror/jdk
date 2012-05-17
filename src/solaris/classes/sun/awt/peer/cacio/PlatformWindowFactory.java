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

import java.awt.GraphicsDevice;

/**
 * Creates {@link PlatformWindow} instances for use by the component peers.
 */
public interface PlatformWindowFactory {

    /**
     * Creates a {@link PlatformWindow} instance.
     *
     * @param cacioComponent the corresponding Cacio component
     * @parent the parent window, or <code>null</code> for top level windows
     *
     * @return the platform window instance
     */
    PlatformWindow createPlatformWindow(CacioComponent awtComponent,
                                        PlatformWindow parent);

    /**
     * Creates and returns a toplevel window for the specified peer.
     *
     * @param cacioWindowPeer the toplevel component
     *
     * @return the created toplevel window
     */
    PlatformToplevelWindow createPlatformToplevelWindow(CacioComponent component);

    /**
     * Creates and returns the event pump to be used for getting the platform
     * events into the AWT event queue.
     *
     * @return the event source for the toolkit
     */
    CacioEventPump<?> createEventPump();

    /**
     * Creates and returns a toplevel window with the specified window as
     * owner.
     *
     * @param cacioWindowPeer the toplevel component
     *
     * @return the created toplevel window
     */
    public PlatformWindow createPlatformToplevelWindow(CacioComponent component,
                                                       PlatformWindow owner);

}
