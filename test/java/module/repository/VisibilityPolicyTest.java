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
import java.module.ModuleDefinition;
import java.module.Modules;
import java.module.Repository;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import sun.module.JamUtils;
import sun.module.repository.RepositoryConfig;

/**
 * @test VisibilityPolicyTest.java
 * @summary Test VisibilityPolicy as used by a repository.
 * @library ../tools
 * @compile -XDignore.symbol.file VisibilityPolicyTest.java NoMainVisibilityPolicy.java ../tools/JamBuilder.java
 * @run main/othervm -Djava.module.visibility.policy.classname=NoMainVisibilityPolicy VisibilityPolicyTest
 */
public class VisibilityPolicyTest {
    public static void realMain(String[] args) throws Throwable {

        File testDir = new File(System.getProperty("test.scratch", "."),
                                "VisibilityPolicyTestDir").getCanonicalFile();
        check(JamUtils.recursiveDelete(testDir));
        check(testDir.mkdirs());

        File jamDir = new File(testDir, "JamDir");
        jamDir.mkdirs();

        List<File> jamFiles = new ArrayList<File>();
        jamFiles.add(
            JamBuilder.createJam(
                "main", "SampleMain", "ModDef",
                "1.0", null, null, false, jamDir));
        jamFiles.add(
            JamBuilder.createJam(
                "grumpf", "SampleGrumpf", "ModuleGrumpf",
                "3.1.4.1", "solaris", "i586", false, jamDir));
        jamFiles.add(
            JamBuilder.createJam(
                "mumble", "Sample", "ModuleMumble",
                "1.2.3.4-alpha", null, null, false, jamDir));
        jamFiles.add(
            JamBuilder.createJam(
                "xml", "SampleXML", "ModuleXML", "2.0",
                null, null, false, jamDir));

        File repoDir =  new File(testDir, "RepoDir").getCanonicalFile();
        repoDir.mkdirs();

        Repository repo = Modules.newLocalRepository(
            "test", repoDir, null, RepositoryConfig.getSystemRepository());

        for (File f : jamFiles) {
            repo.install(f.getCanonicalFile().toURI());
        }

        List<ModuleDefinition> defns = repo.findAll();
        check(defns.size() > 0);
        for (ModuleDefinition md : defns) {
            if ("ModDef".equals(md.getName())) {
                fail();
            }
        }

        if (failed == 0) {
            JamUtils.recursiveDelete(testDir);
        }
        repo.shutdown();
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
