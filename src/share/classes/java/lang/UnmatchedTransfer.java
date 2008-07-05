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
 * Exception thrown when a transfer from within a lambda does't have
 * a matching frame on the stack of the current thread.
 *
 * @author gafter
 */
public class UnmatchedTransfer extends RuntimeException {
    /** The Jump that causes the control transfer. */
    private final Jump jump;

    public UnmatchedTransfer(Jump jump) {
        this.jump = jump;
    }
    /**
     * Returns the thread in which the transfer target is executing.
     */
    public Thread thread() {
        return jump.thread();
    }

    /**
     * Cause the transfer to occur.
     */
    public Nothing transfer() {
        return jump.transfer();
    }
}
