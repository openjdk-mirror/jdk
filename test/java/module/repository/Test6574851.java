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
import java.util.HashMap;
import java.util.Map;
import sun.module.repository.RepositoryConfig;

/**
 * @test
 * @summary Verify that initializing a URLRepository on a source that doesn't
 * have a repository-metadata.xml file throws an IOException.
 * @compile -XDignore.symbol.file Test6574851.java
 */
public class Test6574851 {
    public static void realMain(String args[]) throws Throwable {
        try {
            Map<String, String> config = new HashMap<String, String>();
            config.put(
                "sun.module.repository.URLRepository.sourceLocationMustExist",
                "true");
            Repository repo = Modules.newURLRepository(
                "test",
                new File(
                    System.getProperty("test.scratch", "."),
                    "Test6574851-DoesNotExist").getCanonicalFile().toURI().toURL(),
                config,
                RepositoryConfig.getSystemRepository());
            fail();
        } catch (IOException ex) {
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
