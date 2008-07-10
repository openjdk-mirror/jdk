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

import java.io.File;
import java.io.FileOutputStream;
import java.module.*;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import sun.module.JamUtils;
import sun.module.config.ModuleSystemConfig;
import sun.module.repository.RepositoryConfig;

/*
 * @test
 * @summary Test RepositoryConfig
 * @compile -XDignore.symbol.file RepositoryConfigTest.java
 */
public class RepositoryConfigTest {
    static File testDir;
    static File workDir;
    static File repoDir;
    static File propsFile;

    public static void realMain(String[] args) throws Throwable {
        testDir = new File(System.getProperty("test.scratch", "."));
        workDir = new File(testDir, "RepoConfigTest");
        repoDir = new File(workDir, "RepoConfigTestRepo");

        // If no args are given, set up a test and separate JVM on this same
        // class with an argument.  When launched with an arg, invoke the
        // correspondingly named test.
        if (args.length == 0) {
            JamUtils.recursiveDelete(workDir);
            check(repoDir.mkdirs());
            propsFile = new File(workDir, "RepoConfigTest.properties");

            Properties p = initProperties();

            // Verify: The initial set of properties is such that ide has no
            // parent
            launch("noParentSpecified", false);

            // Verify: syntax errors are detected
            /* Disabled, since PropertyExpander does not recognize the below
             * as a problem.
            updateProperties(p, "ide.parent", "${user.home");
            launch("propertyException", false);
            */

            // Make the properties OK for remaining tests
            updateProperties(p, "ide.parent", "user");

            // Verify: last repo is system Repository
            launch("lastRepoIsSystem", true);

            // Verify: added repo is not last repo
            launch("addedIsNotLast", true);

            // Verify: expected repositories exist
            launch("expectedRepositories", true);

            // Verify "dangling" repository causes exception (dangling meaning
            // repository specified with non-existent parent)
            p.put("dangling.parent", "missing");
            updateProperties(p, "dangling.source",
                             getDir(repoDir, "dangling").toString());
            launch("dangling", false);


            if (failed == 0) {
                JamUtils.recursiveDelete(workDir);
            }

        } else {
            String testName = args[0];
            if (testName.equals("lastRepoIsSystem")) {
                lastRepoIsSystem();
            } else if (testName.equals("addedIsNotLast")) {
                addedIsNotLast();
            } else if (testName.equals("expectedRepositories")) {
                expectedRepositories();
            }
        }
    }

    static void lastRepoIsSystem() {
        Repository r = RepositoryConfig.getLastRepository();
        check(r.equals(Repository.getSystemRepository()));
    }

    static void addedIsNotLast() throws Throwable {
        Repository r = Modules.newLocalRepository("foo", new File(repoDir, "bar"), null);
        check(r != RepositoryConfig.getLastRepository());
        check(r != Repository.getSystemRepository());
    }

    static void expectedRepositories() {
        int count = 0;
        String[] repoNames = new String[] {
            "remote", "ide", "user", "global", "extension",
            Repository.getBootstrapRepository().getName()
        };
        for (Repository r = Repository.getSystemRepository();
             r != null;
             r = r.getParent()) {
            if (!check(repoNames[count].equals(r.getName()))) {
                System.out.println("expected " + repoNames[count] + ", got " + r.getName());
            }
            count++;
        }
        check(count == 6);
    }

    // Creates repository properties.  Note that as provided they are invalid;
    // this is expected.
    static Properties initProperties() throws Throwable {
        JamUtils.recursiveDelete(repoDir);

        // A list of repositories:
        // bootstrap/
        //     extension/
        //         global/
        //             user/
        //                 ide/
        //                     remote
        // "remote" is later set as the system repository
        // Note that the parent of repository "ide" is not specifed right
        // here; see checks below.

        Properties p = new Properties();
        p.put("extension.parent", "bootstrap");
        p.put("extension.source", getDir(repoDir, "ext").toURI().toURL().toExternalForm());
        p.put("extension.classname", "sun.module.repository.LocalRepository");
        p.put("global.parent", "extension");
        p.put("global.source", getDir(repoDir, "global").toString());
        p.put("user.parent", "global");
        p.put("user.source", getDir(repoDir, "user").toString());
        // XXX To test URLRepository config, need to create a URLRepo
        //p.put("ide.source", getDir(repoDir, "ide").toURI().toURL().toExternalForm());
        p.put("ide.source", getDir(repoDir, "ide").toString());
        p.put("ide.optionOne", "a value");
        p.put("ide.optionToo", "some other value");
        p.put("remote.parent", "ide");
        p.put("remote.source", getDir(repoDir, "remote").toString());

        writeProps(p);
        return p;
    }

    static void updateProperties(Properties p, String key, String value) throws Throwable {
        p.put(key, value);
        writeProps(p);
    }

    static void writeProps(Properties p) throws Throwable {
        FileOutputStream fos = new FileOutputStream(propsFile);
        p.store(fos, "For RepositoryConfigTest");
        fos.close();
    }

    static void launch(String testName, boolean positive) throws Throwable {
        String home = System.getProperty("java.home");
        String java = home + File.separator + "bin" + File.separator + "java";
        if (System.getProperty("os.platform").equals("windows")) {
            java += ".exe";
        }
        ProcessBuilder pb = new ProcessBuilder(
            java,
            "-Djava.module.repository.properties.file=RepoConfigTest/RepoConfigTest.properties",
            "RepositoryConfigTest",
            testName);
        pb.directory(testDir);
        Process p = pb.start();
        int rc = p.waitFor();
        check(positive ? rc == 0 : rc != 0);
    }

    private static File getDir(File parent, String s) throws Throwable {
        File d = new File(parent, s);
        d.mkdirs();
        return d.getCanonicalFile();
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static boolean pass() {passed++; return true;}
    static boolean fail() {failed++; Thread.dumpStack(); return false;}
    static boolean fail(String msg) {System.out.println(msg); return fail();}
    static void expected(Throwable t) { passed++; System.out.println("Expected: " + t); }
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
