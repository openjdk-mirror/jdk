/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

module modserv.client;
package modserv.client;

import modserv.service.CodecSet;
import modserv.service.Encoder;

public class Main {
    static final boolean DEBUG = System.getProperty("service.debug") != null;

    public static void realMain(String[] args) throws Throwable {
        Encoder e = CodecSet.getEncoder("foo");
        boolean rc = check(e.getClass().getName().equals("modserv.provider1.StandardCodecs$FooEncoder"));
        debug("e.getClass().getName()=" + e.getClass().getName());
        debug("e cl=" + e.getClass().getClassLoader());
        debug("e cl=" + e.getClass().getClassLoader().hashCode());

        // Another class from the same module is accessible
        Class<?> c = Class.forName("modserv.provider1.AdvancedCodecs", false, e.getClass().getClassLoader());
        debug("c.getName()=" + c.getName());
        debug("c cl= " + c.getClassLoader());
        debug("c cl= " + c.getClassLoader().hashCode());

        // Another class from a different module is NOT accessible
        try {
            c = Class.forName("modserv.provider2.AdvancedCodecs", false, e.getClass().getClassLoader());
            fail(c.getName());
        } catch (ClassNotFoundException ex) {
            pass();
        } catch (Throwable t) {
            fail(t.getMessage());
        }

        Class<?> sc = c.getSuperclass();
        debug("sc.getName()=" + sc.getName());
        debug("sc cl= " + sc.getClassLoader());
        debug("sc cl= " + sc.getClassLoader().hashCode());

        if (!rc || args.length > 0) {
            System.err.println("encoded result: " + e.encode("hello, world"));
        }
    }

    static void debug(String s) {
        if (DEBUG) System.err.println("*** " + s);
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static boolean pass() {passed++; return true;}
    static boolean fail() {failed++; Thread.dumpStack(); return false;}
    static boolean fail(String msg) {System.out.println(msg); return fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static boolean check(boolean cond) {if (cond) pass(); else fail(); return cond;}
    static boolean equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) return pass();
        else return fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.println("\nPassed = " + passed + " failed = " + failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
