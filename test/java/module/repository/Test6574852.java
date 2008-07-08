/*
 * Copyright 2007 Sun Microsystems, Inc.  All Rights Reserved.
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

import java.io.*;
import java.module.Modules;
import java.module.Repository;
import java.net.URL;
import sun.module.repository.RepositoryConfig;

/**
 * @test
 * @summary Verify that a the source location with which a URLRepository is
 * created does not need to end with a '/'.
 * @compile -XDignore.symbol.file Test6574852.java
 */
public class Test6574852 {
    public static void realMain(String args[]) throws Throwable {
        File dir = new File(
            System.getProperty("test.scratch", "."),
            "Test6574852-DoesNotExist").getCanonicalFile();
        URL url = dir.toURI().toURL();
        System.out.println("dir=" + dir);
        System.out.println("url=" + url);
        Repository repo = Modules.newURLRepository(
            "test", url, null,
            RepositoryConfig.getSystemRepository());

        try {
            repo.initialize();
        } catch (IOException ex) {
            // The repository created above doesn't have repository-metadata.xml,
            // so we expect an IOException.  Without the fix for this bug,
            // a NullPointerException would have occurred in repo.initialize().
            pass();
        } catch (Throwable t) {
            unexpected(t);
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
