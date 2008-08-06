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

module reposerv.client;
package reposerv.client;

import java.module.*;
import java.util.NoSuchElementException;
import reposerv.service.FooService;

public class Main {
    public static void realMain(String[] args) throws Throwable {
        FooService fs = FooService.getNextProvider();

        // Check by name rather than by java.lang.class to ensure that we
        // don't load the class by other than the ServiceLoader

        // Check that the first provider is the default provider
        Class clazz = fs.getClass();
        verify(fs, "1.0", "1", "reposerv.service.FooServiceDefaultProvider");

        // Check that the next service is the non-default provider
        fs = FooService.getNextProvider();
        verify(fs, "1.5", "2", "reposerv.provider.FooService2Provider");

        try {
            // Check that the default provider of another service is *not*
            // returned.
            fs = FooService.getNextProvider();
            clazz = fs.getClass();
            Version v = clazz.getClassLoader().getModule().getModuleDefinition().getVersion();
            fail(
                "Got unexpected service " + clazz.getName()
                + ", version " + v);
        } catch (NoSuchElementException ex) {
            pass();
        }
    }

    static void verify(FooService fs, String expectedVersion,
            String repoNum, String className) throws Throwable {
        Class clazz = fs.getClass();
        String n = clazz.getName();
        if (!n.equals(className)) {
            throw new Exception(
                "wrong className: expected " + className + ", got " + n);
        }

        ModuleDefinition md = clazz.getClassLoader().getModule().getModuleDefinition();
        Version v = md.getVersion();
        if (!v.equals(Version.valueOf(expectedVersion))) {
            throw new Exception(
                "wrong version: expected " + expectedVersion + ", got " + v);
        }

/*
        Repository r = md.getRepository();
        String src = r.getSourceLocation().toString();
        String repoName = "repo" + repoNum + "/";
        if (!src.endsWith(repoName)) {
            throw new Exception(
                "wrong repository: expected " + repoName + ", got " + src);
        }
 */
    }

    static void checkRepo(Class clazz) {
        Repository r = clazz.getClassLoader().getModule().getModuleDefinition().getRepository();
        System.out.println("repo=" + r);
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
