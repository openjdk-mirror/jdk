/*
 * Copyright 2009 Sun Microsystems, Inc.  All Rights Reserved.
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

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import sun.awt.CausedFocusEvent;
import sun.awt.SunToolkit;

class KFMHelper {

    private static Method processSynchronousLightweightTransferMethod;

    private static Method shouldNativelyFocusHeavyweightMethod;

    static boolean processSynchronousLightweightTransfer(Component target,
                                                         Component lightweightChild,
                                                         boolean temporary,
                                                         boolean focusedWindowChangeAllowed,
                                                         long time) {
        try {
            if (processSynchronousLightweightTransferMethod == null) {
                processSynchronousLightweightTransferMethod =
                    (Method)AccessController.doPrivileged(
                        new PrivilegedExceptionAction() {
                            public Object run() throws IllegalAccessException, NoSuchMethodException
                            {
                                Method m = KeyboardFocusManager.class.
                                    getDeclaredMethod("processSynchronousLightweightTransfer",
                                                      new Class[] {Component.class, Component.class,
                                                                   Boolean.TYPE, Boolean.TYPE,
                                                                   Long.TYPE});
                                m.setAccessible(true);
                                return m;
                            }
                        });
            }
            Object[] params = new Object[] {
                target,
                lightweightChild,
                Boolean.valueOf(temporary),
                Boolean.valueOf(focusedWindowChangeAllowed),
                Long.valueOf(time)
            };
            return ((Boolean) processSynchronousLightweightTransferMethod.invoke(null, params)).booleanValue();
        } catch (PrivilegedActionException pae) {
            pae.printStackTrace();
            return false;
        } catch (IllegalAccessException iae) {
            iae.printStackTrace();
            return false;
        } catch (IllegalArgumentException iaee) {
            iaee.printStackTrace();
            return false;
        } catch (InvocationTargetException ite) {
            ite.printStackTrace();
            return false;
        }
    }

    static int shouldNativelyFocusHeavyweight(Component heavyweight,
         Component descendant, boolean temporary,
         boolean focusedWindowChangeAllowed, long time, CausedFocusEvent.Cause cause)
    {
        if (shouldNativelyFocusHeavyweightMethod == null) {
            Class[] arg_types =
                new Class[] { Component.class,
                              Component.class,
                              Boolean.TYPE,
                              Boolean.TYPE,
                              Long.TYPE,
                              CausedFocusEvent.Cause.class
            };

            shouldNativelyFocusHeavyweightMethod =
                SunToolkit.getMethod(KeyboardFocusManager.class,
                                   "shouldNativelyFocusHeavyweight",
                                   arg_types);
        }
        Object[] args = new Object[] { heavyweight,
                                       descendant,
                                       Boolean.valueOf(temporary),
                                       Boolean.valueOf(focusedWindowChangeAllowed),
                                       Long.valueOf(time), cause};

        int result = CacioComponentPeer.SNFH_FAILURE;
        if (shouldNativelyFocusHeavyweightMethod != null) {
            try {
                result = ((Integer) shouldNativelyFocusHeavyweightMethod.invoke(null, args)).intValue();
            }
            catch (IllegalAccessException e) {
                assert false;
            }
            catch (InvocationTargetException e) {
                assert false;
            }
        }

        return result;
    }

}
