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
import java.module.*;
import java.net.URL;
import sun.module.repository.RepositoryConfig;

/**
 * @test RepositoryTest
 * @summary Test various Repository methods
 * @compile -XDignore.symbol.file RepositoryTest.java EventChecker.java
 * @run main RepositoryTest
 */
public class RepositoryTest  {
    public static void realMain(String args[]) throws Throwable {
        EventChecker ec = new EventChecker();

        // Create a new repository.
        File src = new File(".").getCanonicalFile();
        String name = "application";
        Repository rep = Modules.newLocalRepository(
            name, src, null, Repository.getApplicationRepository());

        check(rep.getParent() == Repository.getApplicationRepository());
        check(name.equals(rep.getName()));
//        check(rep.getSourceLocation().equals(src.toURI()));
//        check(rep.getModuleSystem() == ModuleSystem.getDefault());

        check(ec.initializeEventExists(rep));

        Repository bsRep = Repository.getBootstrapRepository();
        check(bsRep.getParent() == null);
        check(bsRep.isReadOnly());

        RepositoryConfig.setApplicationRepository(rep);

        Repository appRepo = Repository.getApplicationRepository();
  //      check(appRepo.getModuleSystem() == ModuleSystem.getDefault());

        rep.shutdown();

        check(ec.shutdownEventExists(rep));

        ec.clear();

        // Verify null arg checking
        try {
            rep = Modules.newLocalRepository(name, src, null, null);
            fail();
        } catch (NullPointerException ex) {
            // expected
        } catch (Throwable ex) {
            unexpected(ex);
        }

        // No event should be fired
        check(!ec.initializeEventExists(rep));
        check(!ec.shutdownEventExists(rep));

        try {
            rep = Modules.newLocalRepository(null, src, null);
            fail();
        } catch (NullPointerException ex) {
            // expected
        } catch (Throwable ex) {
            unexpected(ex);
        }

        // No event should be fired
        check(!ec.initializeEventExists(rep));
        check(!ec.shutdownEventExists(rep));

        try {
            rep = Modules.newLocalRepository(name, null, null);
            fail();
        } catch (NullPointerException ex) {
            // expected
        } catch (Throwable ex) {
            unexpected(ex);
        }

        // No event should be fired
        check(!ec.initializeEventExists(rep));
        check(!ec.shutdownEventExists(rep));

        // Remove repository listener
        ec.end();

        // Creates a new repository
        rep = Modules.newLocalRepository(
            name, src, null, RepositoryConfig.getApplicationRepository());

        // No event should be sent to an already-removed repository listener
        check(!ec.initializeEventExists(rep));

        rep.shutdown();

        // No event should be sent to an already-removed repository listener
        check(!ec.shutdownEventExists(rep));
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
