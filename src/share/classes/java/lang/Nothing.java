/*
 * Copyright 2008 Neal M Gafter.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Neal designates this
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
 * A non-instantiable type representing values that can't occur at
 * runtime due to an expression not completing normally. The compiler
 * treats Nothing as a subytpe of every object type, but it is
 * restricted to only contexts in which a void result is allowed.
 * 
 * @author Neal Gafter
 */
public class Nothing {
    private Nothing() { throw null; }

    // TODO(gafter)9: Nothing.class.isAssignableFrom(x) should return
    // false unless x == Nothing.class
    // TODO(gafter)9: X.class.isAssignableFrom(Nothing.class) should
    // return true if X is an object type.
}
