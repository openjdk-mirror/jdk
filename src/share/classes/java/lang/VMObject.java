/*
 * Copyright 1994-2006, 2008 Sun Microsystems, Inc.  All Rights Reserved.
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

package java.lang;

/**
 * VM interface for java.lang.Object.
 */
final class VMObject {
   
    /**
     * Returns the class of the given object.
     */
    static final native Class<?> getClass(Object obj);

    /**
     * Returns the hashcode of the given object.
     */
    static final native int hashCode(Object obj);

    /**
     * Returns a clone of the given object.
     */
    static final native Object clone(Object obj);

    /**
     * Notifies a single thread waiting on the given object.
     */
    static final native void notify(Object obj);

    /**
     * Notifies all threads waiting on the given object.
     */
    static final native void notifyAll(Object obj);

    /**
     * Wait to be notified on the given object for the specified
     * length of time (0 is indefinitely).
     */
    static final native void wait(Object obj, long timeout);


}
