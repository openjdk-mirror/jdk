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

import java.util.EventListener;

/**
 * The listener interface for receiving repository events.
 * <p>
 * The class that is interested in processing repository events implements
 * this interface, and the object created with that class is registered, using
 * the {@link Repository#addRepositoryListener(RepositoryListener)
 * <tt>addRepositoryListener</tt>} method of the {@code Repository} class.
 * <p>
 * The object that is no longer interested in processing any repository event
 * is unregistered, using the
 * {@link Repository#removeRepositoryListener(RepositoryListener)
 * <tt>removeRepositoryListener</tt>} method of the {@code Repository} class.
 *
 * @see java.module.Repository
 * @see java.module.RepositoryEvent
 *
 * @since 1.7
 */
public interface RepositoryListener extends EventListener {

    /**
     * Invoked when a change has occurred in a repository.
     *
     * @param e repository event
     */
    public void handleEvent(RepositoryEvent e);
}
