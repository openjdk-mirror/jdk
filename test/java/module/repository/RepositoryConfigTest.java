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
    public static void realMain(String[] args) throws Throwable {
        File repoDir = new File(System.getProperty("test.scratch", "."), "RepositoryConfigTestDir");
        File propsFile = null;

        try {
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

            // xxx Need test: Verify if multiple furthest repositories => exception

            Repository repo;

            // Verify: specify properties file via system property.  This will
            // work, but expect an exception because repository 'ide' doesn't
            // specify a parent.
            try {
                propsFile = new File("RepoConfigTest.properties");
                FileOutputStream fos = new FileOutputStream(propsFile);
                p.store(fos, "For RepositoryConfigTest");
                fos.close();
                Properties sysProps = System.getProperties();
                sysProps.put(
                    ModuleSystemConfig.REPOSITORY_PROPERTIES_FILE,
                    propsFile.getCanonicalPath());
                repo = RepositoryConfig.getSystemRepository();
                fail();
            } catch (RuntimeException ex) {
                expected(ex);
            } catch (Throwable t) {
                unexpected(t);
            }

            // Verify: if parent not specified => exception
            try {
                repo = RepositoryConfig.configRepositories(p);
                fail();
            } catch (RuntimeException ex) {
                expected(ex);
            } catch (Throwable t) {
                unexpected(t);
            }

            // Verify if parent is not found => exception
            try {
                p.put("ide.parent", "NoParent");
                repo = RepositoryConfig.configRepositories(p);
                fail();
            } catch (RuntimeException ex) {
                expected(ex);
            } catch (Throwable t) {
                unexpected(t);
            }
            p.remove("ide.parent");

            // Verify if invalid system property syntax => exception
            /* Disabled, since PropertyExpander does not recognize the below
             * as a problem.
            try {
                p.put("ide.parent", "${user.home");
                repo = RepositoryConfig.configRepositories(p);
                fail();
            } catch (IllegalArgumentException ex) {
                expected(ex);
            } catch (Throwable t) {
                unexpected(t);
            }
            p.remove("ide.parent");
            */

            // Make the configuration OK for remaining tests.
            p.put("ide.parent", "user");

            // Verify: if classname not fond => exception
            try {
                p.put("ide.classname", "foo.bar.RepositoryImpl");
                repo = RepositoryConfig.configRepositories(p);
                fail();
            } catch (IllegalArgumentException ex) {
                expected(ex);
            } catch (Throwable t) {
                unexpected(t);
            }
            p.remove("ide.classname");

            // Verify if dangling repository that is not under main repository chain => exception
            try {
                p.put("dangling.parent", "missing");
                p.put("dangling.source", getDir(repoDir, "dangling").toString());
                repo = RepositoryConfig.configRepositories(p);
                fail();
            } catch (RuntimeException ex) {
                expected(ex);
                p.remove("dangling.parent");
                p.remove("dangling.source");
            } catch (Throwable t) {
                unexpected(t);
            }

            // Verify that if correct parent, etc. => ok
            p.put("ide.parent", "user");
            repo = RepositoryConfig.configRepositories(p);

            check(repo != null);
            RepositoryConfig.setSystemRepository(repo);

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

        } finally {
            if (failed == 0) {
                JamUtils.recursiveDelete(repoDir);
                if (propsFile != null) propsFile.delete();
            }
        }
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
