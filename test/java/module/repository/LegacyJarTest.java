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

import java.io.*;
import java.lang.reflect.Method;
import java.module.*;
import java.util.*;
import sun.module.*;
import sun.module.repository.*;

/**
 * @test
 * @summary Test use of legacy JAR files in both Local and URL repositories,
 * with both default and annotation-specified paths to the legacy JAR file.
 * @library ../tools
 * @compile -XDignore.symbol.file LegacyJarTest.java LibraryTest.java ../tools/JamBuilder.java
 * @run main LegacyJarTest
 */
public class LegacyJarTest extends LibraryTest {
    static File jarDir; // Where the legacy JAR file is built

    static File jarFile; // Legacy JAR that gets embedded

    static final String expectedAnswer = "world";

    String getTestName() {
        return "LegacyJarTest";
    }

    String getPkgName() {
        return "legacyjartest";
    }

    String getTestcaseMethodBody(String bodyInfo) {
        return
            "        return \"" + bodyInfo + "\";\n";
    }

    static void realMain(String[] args) throws Throwable {
        new LegacyJarTest().run(args);
    }

    private void run(String[] args) throws Throwable {
        // Enables shadow file copies in the repository if we're running
        // on Windows. This is to prevent file locking in the
        // source location.
        if (System.getProperty("os.platform").equalsIgnoreCase("windows")) {
            System.setProperty("java.module.repository.shadowcopyfiles", "true");
        }

        // Create, compile, and jar "hello, world" main class
        File testcaseDir = makeTestDir("testcase");
        File testcase = createTestcase(testcaseDir, expectedAnswer);
        println("=testcase class file: " + testcase.getCanonicalPath());
        File pkgDir = testcase.getParentFile().getParentFile().getCanonicalFile();

        println("=testcase package-containing directory: " + pkgDir);
        jarDir = makeTestDir("jar");
        jarFile = new File(jarDir, "hello.jar");
        println("=jar: " + jarFile.getCanonicalPath());
        String[] jarArgs = new String[] {
            "cf",
            jarFile.getCanonicalPath(),
            "-C",
            pkgDir.getCanonicalPath(),
            getPkgName()
        };
        sun.tools.jar.Main jartool =
            new sun.tools.jar.Main(System.out, System.err, "LegacyJarTest");
        jartool.run(jarArgs);
        if (!debug) {
            JamUtils.recursiveDelete(pkgDir);
        }

        // Run tests against URLRepository
        jamDir = makeTestDir("jam");
        File srcDir = makeTestDir("urlSource");
        Repository urlRepo = Modules.newURLRepository(
            "LegacyJarTestURLRepo", srcDir.getCanonicalFile().toURI().toURL());
        repo = urlRepo;
        runTest("LegacyDefaultTest", null);
        runTest("LegacyCustomTest", "no/place/like/home");

        // Remove dirs if no failures
        if (!debug) {
            if (failed == 0) {
                JamUtils.recursiveDelete(jamDir);
                JamUtils.recursiveDelete(srcDir);
                urlRepo.shutdown();
            } else {
                System.exit(1);
            }
        }

        // Run tests against LocalRepository
        jamDir = makeTestDir("jam");
        srcDir = makeTestDir("localSource");
        Repository localRepo = Modules.newLocalRepository(
            "LegacyJarTestLocalRepo", srcDir.getCanonicalFile());
        repo = localRepo;
        runTest("LegacyDefaultTest", null);
        runTest("LegacyCustomTest", "no/place/like/home");

        // When not debugging and there are no failures, remove test dirs
        if (!debug && failed == 0) {
            localRepo.shutdown();
            JamUtils.recursiveDelete(baseDir);
        }
    }

    void runTest(String name, String path) throws Throwable {
        String modName = name + "Module";

        JamBuilder jb = new JamBuilder(
            getPkgName(), name, modName, "1.0",
            platform, arch, false, jamDir);

        String jarPath = "MODULE-INF/lib";
        if (path != null) {
            // Run two tests: In this block a test with a path specified, but
            // no library at that location.  Then fall through this block to
            // run a second with test a path specified and a library at that
            // location.

            jarPath = path;
            jb.addAnnotation("@JarLibraryPath(\"" + jarPath + "\")\n");
            File jamFile = getJam(jb);
            ModuleArchiveInfo mai = repo.install(jamFile.toURI().toURL());
            check(mai != null);
            try {
                runModule(repo, modName, expectedAnswer);
                fail("Run without embedded legacy JAR failed");
            } catch (ModuleInitializationException mie)  {
                pass();
            } catch (Throwable t) {
                unexpected(t);
            }
            check(repo.uninstall(mai));
            check(jamFile.delete());
            jb.setVersion("2.0"); // Use different version for subsequent test
        }

        File jamFile = getJam(jb).getCanonicalFile();

        // Update the JAM with the legacy jar
        File legacyJarDir = new File(jarDir, jarPath);
        println("=legacyJarDir: " + legacyJarDir.getCanonicalPath());
        check(legacyJarDir.isDirectory() || legacyJarDir.mkdirs());
        JamUtils.copyFile(jarFile, new File(legacyJarDir, "hello.jar"));
        String[] jarArgs = new String[] {
            "uf",
            jamFile.getCanonicalPath(),
            "-C",
            jarDir.getCanonicalPath(),
            jarPath
        };
        sun.tools.jar.Main jartool =
            new sun.tools.jar.Main(System.out, System.err, "LegacyJarTest");
        jartool.run(jarArgs);

        ModuleArchiveInfo mai = repo.install(jamFile.toURI().toURL());
        check(mai != null);
        try {
            runModule(repo, modName, expectedAnswer);
            if (!debug && failed == 0) {
                check(repo.uninstall(mai));
            }
        } catch(Throwable t) {
            unexpected(t);
        }
    }

    //--------------------- Infrastructure ---------------------------
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.println("\nPassed = " + passed + " failed = " + failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
