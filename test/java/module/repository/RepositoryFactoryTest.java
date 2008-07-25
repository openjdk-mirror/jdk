/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
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

import java.io.File;
import java.module.Modules;
import java.module.Repository;
import sun.module.JamUtils;
import sun.module.bootstrap.BootstrapRepository;
import sun.module.repository.LocalRepository;
import sun.module.repository.URLRepository;

/**
 * @test
 * @compile -XDignore.symbol.file RepositoryFactoryTest.java
 * @run main RepositoryFactoryTest
 */
public class RepositoryFactoryTest {
    static final boolean debug = System.getProperty("repository.debug") != null;

    private static void println(String s) {
        if (debug) System.err.println(s);
    }

    public static void realMain(String[] args) throws Throwable {
        File props = new File(System.getProperty("test.src", "."),
                              "RepositoryFactoryTest.properties");
        System.setProperty("java.module.repository.properties.file",
                           props.getAbsolutePath());

        File userRepoDir = new File(System.getProperty("test.scratch", "."),
                                    "RepositoryFactoryTestDir");
        userRepoDir.mkdirs();

        // Check that getApplicationRepository() doesn't return null and is
        // configured OK.
        Repository appRepo = Repository.getApplicationRepository();
        check(appRepo != null);

        Repository r = appRepo;
        int count = 0;
        String[] expectedName = new String[] {
            "user", "global", "system-extension", "extension", "bootstrap"
        };
        Class[] expectedClass = new Class[] {
            LocalRepository.class, LocalRepository.class, LocalRepository.class,
            URLRepository.class, BootstrapRepository.class
        };
        while (r != null) {
            println("=repository name=" + r.getName()
                    + ", class=" + r.getClass());
            check(expectedName[count].equals(r.getName()));
            check(expectedClass[count].equals(r.getClass()));
            count++;
            r = r.getParent();
        }
        check(count == 5);

        if (failed == 0) {
            JamUtils.recursiveDelete(userRepoDir);
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
