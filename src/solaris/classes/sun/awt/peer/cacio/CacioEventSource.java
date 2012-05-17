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

package sun.awt.peer.cacio;

import sun.awt.peer.cacio.managed.EventData;

/**
 * The source of events for Cacio Toolkits. Which events need to be generated
 * depends on the setup of the target. For fully managed environments (i.e.
 * all windows are managed by CacioCavallo), only mouse and key events need
 * to be generated, all other events are synthesized by the window management
 * code. For fully native setups, all kinds of events need to be generated
 * by the native backend, in particular: mouse, mouse wheel, key, focus and
 * window events.
 *
 * The Caciocavallo framework will poll the event source for new events,
 * process them if necessary, and post them to the AWT event queue. How
 * the event is processed depends on the type of event source:
 *
 * <ul>
 * <li>ManagedWindowContainer: the event data is processed in raw form by
 *     the container's dispatchEvent() method. The container is responsible
 *     for further processing, i.e. inferring synthesized events, and
 *     eventually posting to the AWT event queue.</li>
 * <li>CacioComponent: the event is turned into its correspondig AWT event
 *     and is processed by the CacioComponent's handlePeerEvent() method.
 *     The CacioComponent is responsible for further processing, and eventually
 *     posting to the AWT event queue.</li>
 * <li>AWT Component: the event is posted directly to the AWT event queue
 *     without further processing.</li>
 * <li>Other types or null events are discarded.</li>
 * </ul>
 */
public interface CacioEventSource {

    /**
     * Fetches the next event from the native event queue. This method
     * blocks until the next event is available.
     *
     * @return the next event from the event queue
     * @throws InterruptedException 
     */
    EventData getNextEvent() throws InterruptedException;
}
