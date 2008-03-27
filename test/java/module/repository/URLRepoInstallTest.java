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

/**
 * @summary Test URLRepository.install on a file: - based URLRepository.
 * @library ../tools
 * @compile -XDignore.symbol.file URLRepoInstallTest.java ../tools/JamBuilder.java
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
        config.put("sun.module.repository.URLRepository.downloadDirectory", repoDownloadDir.getAbsolutePath());

        Repository repo = Modules.newURLRepository(
            RepositoryConfig.getSystemRepository(),
            "test",
            repoDir.getCanonicalFile().toURI().toURL(),
            config);

        // Check install
        ModuleArchiveInfo installedMAI = null;
        try {
            installedMAI = repo.install(jamFile.getCanonicalFile().toURI().toURL());
            pass();
        } catch (Throwable t) {
            unexpected(t);
        }

        // Check list(): should contain only the one module just installed
        List<ModuleArchiveInfo> installed = repo.list();
        check(installed.size() == 1);
        Set<String> names = new HashSet<String>();
        for (ModuleArchiveInfo mai : installed) {
            println("=mai: " + mai.getName() + ", "
                + mai.getPlatform() + ", "
                + mai.getArchitecture() + ", "
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
        // XXX Enable on Win32 once Repository.shutdown disposes of all its
        // Modules (until then, files remain locked).
        if (!System.getProperty("os.name").startsWith("Windows")) {
            try {
                if (repo.uninstall(installedMAI)) {
                    installed = repo.list();
                    check(installed.size() == 0);
                    pass();
                } else {
                    fail("Could not uninstall " + installedMAI.getName());
                }
            } catch (Throwable t) {
                unexpected(t);
            }
        }


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
