/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
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
package java.module;

import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.EventListener;


/**
 * {@code EventMulticaster} implements efficient and thread-safe multi-cast
 * event dispatching for the module system related events. This class is
 * inspired by similar functionalities in java.awt.AWTEventMulticaster.
 * <p>
 * The following example illustrates how to use this class:
 *
 * <pre><code>
 * public Repository {
 *     RepositoryListener repositoryListener = null;
 *
 *     public synchronized void addRepositoryListener(RepositoryListener l) {
 *         repositoryListener = EventMulticaster.add(repositoryListener, l);
 *     }
 *     public synchronized void removeRepositoryListener(RepositoryListener l) {
 *         repositoryListener = EventMulticaster.remove(repositoryListener, l);
 *     }
 *     protected void processEvent(RepositoryEvent e) {
 *         RepositoryListener listener = repositoryListener;
 *         if (listener != null) {
 *             listener.doSomething(e);
 *         }
 *     }
 * }
 * </code></pre>
 * The important point to note is the first argument to the {@code
 * add} and {@code remove} methods is the field maintaining the
 * listeners. In addition you must assign the result of the {@code add}
 * and {@code remove} methods to the field maintaining the listeners.
 * <p>
 * {@code EventMulticaster} is implemented as a pair of {@code
 * EventListeners} that are set at construction time. {@code
 * EventMulticaster} is immutable. The {@code add} and {@code
 * remove} methods do not alter {@code EventMulticaster} in
 * anyway. If necessary, a new {@code EventMulticaster} is
 * created. In this way it is safe to add and remove listeners during
 * the process of an event dispatching.  However, event listeners
 * added during the process of an event dispatch operation are not
 * notified of the event currently being dispatched.
 * <p>
 * All of the {@code add} methods allow {@code null} arguments. If the
 * first argument is {@code null}, the second argument is returned. If
 * the first argument is not {@code null} and the second argument is
 * {@code null}, the first argument is returned. If both arguments are
 * {@code non-null}, a new {@code EventMulticaster} is created using
 * the two arguments and returned.
 * <p>
 * For the {@code remove} methods that take two arguments, the following is
 * returned:
 * <ul>
 *   <li>{@code null}, if the first argument is {@code null}, or
 *       the arguments are equal, by way of {@code ==}.
 *   <li>the first argument, if the first argument is not an instance of
 *       {@code EventMulticaster}.
 *   <li>result of invoking {@code remove(EventListener)} on the
 *       first argument, supplying the second argument to the
 *       {@code remove(EventListener)} method.
 * </ul>
 *
 * @see java.module.ModuleSystemListener
 * @see java.module.RepositoryListener
 *
 * @since 1.7
 */

class EventMulticaster implements ModuleSystemListener, RepositoryListener {

    private final EventListener a, b;

    /**
     * Creates an event multicaster instance which chains listener-a
     * with listener-b. Input parameters <code>a</code> and <code>b</code>
     * should not be <code>null</code>, though implementations may vary in
     * choosing whether or not to throw <code>NullPointerException</code>
     * in that case.
     * @param a listener-a
     * @param b listener-b
     */
    EventMulticaster(EventListener a, EventListener b) {
        this.a = a; this.b = b;
    }

    /**
     * Removes a listener from this multicaster.
     * <p>
     * The returned multicaster contains all the listeners in this
     * multicaster with the exception of all occurrences of {@code oldl}.
     * If the resulting multicaster contains only one regular listener
     * the regular listener may be returned.  If the resulting multicaster
     * is empty, then {@code null} may be returned instead.
     * <p>
     * No exception is thrown if {@code oldl} is {@code null}.
     *
     * @param oldl the listener to be removed
     * @return resulting listener
     */
    EventListener remove(EventListener oldl) {
        if (oldl == a)  return b;
        if (oldl == b)  return a;
        EventListener a2 = removeInternal(a, oldl);
        EventListener b2 = removeInternal(b, oldl);
        if (a2 == a && b2 == b) {
            return this;        // it's not here
        }
        return addInternal(a2, b2);
    }

    /**
     * Returns the resulting multicast listener from adding listener-a
     * and listener-b together.
     * If listener-a is null, it returns listener-b;
     * If listener-b is null, it returns listener-a
     * If neither are null, then it creates and returns
     * a new EventMulticaster instance which chains a with b.
     * @param a event listener-a
     * @param b event listener-b
     */
    private static EventListener addInternal(EventListener a, EventListener b) {
        if (a == null)  return b;
        if (b == null)  return a;
        return new EventMulticaster(a, b);
    }

    /**
     * Returns the resulting multicast listener after removing the
     * old listener from listener-l.
     * If listener-l equals the old listener OR listener-l is null,
     * returns null.
     * Else if listener-l is an instance of EventMulticaster,
     * then it removes the old listener from it.
     * Else, returns listener l.
     * @param l the listener being removed from
     * @param oldl the listener being removed
     */
    private static EventListener removeInternal(EventListener l, EventListener oldl) {
        if (l == oldl || l == null) {
            return null;
        } else if (l instanceof EventMulticaster) {
            return ((EventMulticaster)l).remove(oldl);
        } else {
            return l;           // it's not here
        }
    }

    /**
     * Adds module-system-listener-a with module-system-listener-b and
     * returns the resulting multicast listener.
     * @param a module-system-listener-a
     * @param b module-system-listener-b
     */
    static ModuleSystemListener add(ModuleSystemListener a, ModuleSystemListener b) {
        return (ModuleSystemListener)addInternal(a, b);
    }

    /**
     * Removes the old module-system-listener from module-system-listener-l and
     * returns the resulting multicast listener.
     * @param l module-system-listener-l
     * @param oldl the module-system-listener being removed
     */
    static ModuleSystemListener remove(ModuleSystemListener l, ModuleSystemListener oldl) {
        return (ModuleSystemListener) removeInternal(l, oldl);
    }

    /**
     * Adds repository-listener-a with repository-listener-b and
     * returns the resulting multicast listener.
     * @param a repository-listener-a
     * @param b repository-listener-b
     */
    static RepositoryListener add(RepositoryListener a, RepositoryListener b) {
        return (RepositoryListener)addInternal(a, b);
    }

    /**
     * Removes the old repository-listener from repository-listener-l and
     * returns the resulting multicast listener.
     * @param l repository-listener-l
     * @param oldl the repository-listener being removed
     */
    static RepositoryListener remove(RepositoryListener l, RepositoryListener oldl) {
        return (RepositoryListener) removeInternal(l, oldl);
    }

    /**
     * Handles the module system event by invoking the handleEvent
     * methods on listener-a and listener-b.
     * @param e the module system event
     */
    public void handleEvent(ModuleSystemEvent e)  {
        ((ModuleSystemListener)a).handleEvent(e);
        ((ModuleSystemListener)b).handleEvent(e);
    }

    /**
     * Handles the repository event by invoking the handleEvent
     * methods on listener-a and listener-b.
     * @param e the repository event
     */
    public void handleEvent(RepositoryEvent e)  {
        ((RepositoryListener)a).handleEvent(e);
        ((RepositoryListener)b).handleEvent(e);
    }
}
