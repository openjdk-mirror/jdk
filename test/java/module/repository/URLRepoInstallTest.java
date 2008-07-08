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
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.module.*;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import sun.module.JamUtils;
import sun.module.repository.RepositoryConfig;
import sun.module.repository.RepositoryUtils;

/**
 * @summary Test URLRepository.install on a file: - based URLRepository.
 * @library ../tools
 * @compile -XDignore.symbol.file URLRepoInstallTest.java EventChecker.java ../tools/JamBuilder.java
 * @run main URLRepoInstallTest
 */
public class URLRepoInstallTest {
    static final boolean debug = System.getProperty("repository.debug") != null;

    /** Base directory for repository's temp files. */
    private File testDir;

    /** Holds repository internal cache under testDir. */
    private File repoDownloadDir;

    /** Source location of repository. */
    private final File repoDir;

    /** JAM file that gets installed. */
    private final File jamFile;

    /** Directory in which JAM file is created. */
    private final File jamDir;

    public static final String repoName = "testinstallrepo";

    static final EventChecker ec = new EventChecker();

    private static void println(String s) {
        if (debug) System.err.println(s);
    }

    public static void realMain(String[] args) throws Throwable {
        URLRepoInstallTest t = new URLRepoInstallTest();

        t.runTest();
        t.shutdown();
    }

    void shutdown() throws Throwable {
        if (failed == 0) {
            //boolean deleted = JamUtils.recursiveDelete(testDir);
            // XXX Enable once Repository.shutdown disposes of all its Modules
            //check(deleted);
            JamUtils.recursiveDelete(testDir);
        }
    }

    private URLRepoInstallTest() throws Throwable {
        testDir = new File(System.getProperty("test.scratch", "."), "URLRepoInstallTestDir");
        check(JamUtils.recursiveDelete(testDir));
        check(testDir.mkdirs());

        repoDir = new File(testDir, "RepoDir");
        check(repoDir.mkdirs());

        repoDownloadDir = new File(testDir, "RepoDownloadDir");
        check(repoDownloadDir.mkdirs());

        jamDir = new File(testDir, "JamDir");
        check(jamDir.mkdirs());

        jamFile = JamBuilder.createJam(
                "main", "SampleMain", "ModuleMain",
                "1.0", null, null, false, jamDir);
    }

    void runTest() throws Throwable {
        Map<String, String> config = new HashMap<String, String>();
        config.put("sun.module.repository.URLRepository.cacheDirectory", repoDownloadDir.getAbsolutePath());

        Repository repo = Modules.newURLRepository(
            "test",
            repoDir.getCanonicalFile().toURI().toURL(),
            config,
            RepositoryConfig.getSystemRepository());

        // Only REPOSITORY_INITIALIZED event should be fired.
        check(ec.initializeEventExists(repo));
        check(!ec.shutdownEventExists(repo));
        check(!ec.installEventExists(repo, null));
        check(!ec.uninstallEventExists(repo, null));

        // Check install
        ModuleArchiveInfo installedMAI = null;
        try {
            installedMAI = repo.install(jamFile.getCanonicalFile().toURI());
            pass();
        } catch (Throwable t) {
            unexpected(t);
        }

        // Only MODULE_ARCHIVE_INSTALLED event should be fired.
        check(!ec.initializeEventExists(repo));
        check(!ec.shutdownEventExists(repo));
        check(ec.installEventExists(repo, installedMAI));
        check(!ec.uninstallEventExists(repo, null));

        // Check list(): should contain only the one module just installed
        List<ModuleArchiveInfo> installed = repo.list();
        check(installed.size() == 1);
        Set<String> names = new HashSet<String>();
        for (ModuleArchiveInfo mai : installed) {
            println("=mai: " + mai.getName() + ", "
                + mai.getPlatform() + ", "
                + mai.getArch() + ", "
                + mai.getVersion() + ", "
                + mai.getFileName());
            names.add(mai.getFileName());
        }

        // Check that we can invoke the just-installed module's main class
        List<ModuleDefinition> defns = repo.findAll();
        check(defns != null && defns.size() != 0);
        for (ModuleDefinition md : defns) {
            println("=definition: " + md);
            if ("ModDef".equals(md.getName())) {
                try {
                    Module m = md.getModuleInstance();
                    String mainClass = md.getAnnotation(java.module.annotation.MainClass.class).value();
                    println("=mainclass: " + mainClass);
                    if (mainClass == null) {
                        throw new Exception("No Main-Class attribute in the module definition");
                    }

                    println("=module: " + m);
                    ClassLoader loader = m.getClassLoader();
                    println("=loader: " + loader);

                    Class<?> clazz = loader.loadClass(mainClass);
                    println("=class: " + clazz);

                    Method method = clazz.getMethod("main", String[].class);
                    method.invoke(null, (Object) (new String[0]));
                    pass();
                } catch (Throwable t) {
                    unexpected(t);
                }
            }
        }

        // Check uninstall
        try {
            if (repo.uninstall(installedMAI)) {
                installed = repo.list();
                check(installed.size() == 0);
                pass();

                // Only MODULE_ARCHIVE_UNINSTALLED event should be fired.
                check(!ec.initializeEventExists(repo));
                check(!ec.shutdownEventExists(repo));
                check(!ec.installEventExists(repo, null));
                check(ec.uninstallEventExists(repo, installedMAI));
            } else {
                fail("Could not uninstall " + installedMAI.getName());
            }
        } catch (Throwable t) {
            unexpected(t);
        }

        // If there are three modules that have the same name and version but
        // different platform binding:
        // 1. Specific to xyz platform and arch
        // 2. Specific to current platform and arch
        // 3. Platform and arch neutral
        //
        // If they are installed in the order of #1, #2, #3, then #2 should
        // be used to provide the module definition.
        // Then, if they are uninstalled in the order of #1 and #2, then no
        // module definition would be returned from the repository even
        // #3 still exists, because #3 has not been loaded by the repository
        // before.
        //
        // On the other hand, if they are installed in the order of #1, #3,
        // #2, then #3 should be used to provide the module definition.
        // Then, if they are uninstalled in the order of #3 and #1, then
        // no module definition would be returned from the repository even
        // #2 still exists, because #2 has not been loaded by the repository.
        //
        final String platform = RepositoryUtils.getPlatform();
        final String arch = RepositoryUtils.getArch();

        File jamFile1, jamFile2, jamFile3;
        ModuleArchiveInfo mai1, mai2, mai3;
        jamFile1 = JamBuilder.createJam(
            "urlrepoinstalltest", "URLRepoTestA", "URLRepoModuleA", "7.0",
            "xyz-platform", "xyz-arch", false, jamDir);
        jamFile2 = JamBuilder.createJam(
            "urlrepoinstalltest", "URLRepoTestA", "URLRepoModuleA", "7.0",
            platform, arch, false, jamDir);
        jamFile3 = JamBuilder.createJam(
            "urlrepoinstalltest", "URLRepoTestA", "URLRepoModuleA", "7.0",
            null, null, false, jamDir);

        // Installs #1, #2, and #3
        mai1 = repo.install(jamFile1.getCanonicalFile().toURI());
        check(mai1 != null);
        mai2 = repo.install(jamFile2.getCanonicalFile().toURI());
        check(mai2 != null);
        mai3 = repo.install(jamFile3.getCanonicalFile().toURI());
        check(mai3 != null);

        ModuleDefinition md = repo.find("URLRepoModuleA", VersionConstraint.valueOf("7.0"));
        check(md != null);
        java.module.annotation.PlatformBinding platformBinding = md.getAnnotation
            (java.module.annotation.PlatformBinding.class);
        if (platformBinding != null) {
            if (platformBinding.platform().equals(platform)
                && platformBinding.arch().equals(arch)) {
                pass();
            } else {
                fail();
            }
        } else {
            fail();
        }

        // Uninstall #1 and #2
        check(repo.uninstall(mai1));
        check(repo.uninstall(mai2));

        md = repo.find("URLRepoModuleA", VersionConstraint.valueOf("7.0"));
        check (md == null);

        // Uninstall #3
        check(repo.uninstall(mai3));

        // Installs #1, #3, and #2
        mai1 = repo.install(jamFile1.getCanonicalFile().toURI());
        check(mai1 != null);
        mai3 = repo.install(jamFile3.getCanonicalFile().toURI());
        check(mai3 != null);
        mai2 = repo.install(jamFile2.getCanonicalFile().toURI());
        check(mai2 != null);

        md = repo.find("URLRepoModuleA", VersionConstraint.valueOf("7.0"));
        check(md != null);
        platformBinding = md.getAnnotation
            (java.module.annotation.PlatformBinding.class);
        if (platformBinding == null) {
            pass();
        } else {
            fail();
        }

        // Uninstall #3 and #1
        check(repo.uninstall(mai3));
        check(repo.uninstall(mai1));

        md = repo.find("URLRepoModuleA", VersionConstraint.valueOf("7.0"));
        check (md == null);

        // Clear event queues for the tests afterwards
        ec.clear();

        if (failed == 0) {
            repo.shutdown();
            check(repoDownloadDir.list().length == 0);
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
