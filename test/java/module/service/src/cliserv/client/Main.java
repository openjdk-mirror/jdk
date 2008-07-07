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

package cliserv.client;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.module.ModuleDefinition;
import java.module.Version;
import java.util.NoSuchElementException;
import cliserv.service.FooService;

/**
 * Checks that providers for service Foo are available.  This class is in the
 * client module, and so the providers must be returned in the expected order
 * (i.e. default provider first).
 */
public class Main {
    public static void realMain(String[] args) throws Throwable {
        Iterator<FooService> loader =
            ServiceLoader.load(FooService.class).iterator();

        FooService fs = loader.next();

        // Check by name rather than by java.lang.class to ensure that we
        // don't load the class by other than the ServiceLoader
        check(fs.getClass().getName().equals("cliserv.service.FooServiceDefaultProvider"));

        try {
            // Check that the next service is the non-default provider
            fs = loader.next();
            check(fs.getClass().getName().equals("cliserv.provider.FooService2Provider"));
            try {
                // Check that the provider of another service is *not*
                // returned.
                loader.next();
                fail();
            } catch (NoSuchElementException ex) {
                pass();
            }
        } catch (NoSuchElementException ex) {
            fail();
        }
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
