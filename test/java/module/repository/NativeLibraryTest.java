/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
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
 * @summary Test use of native library files in both Local and URL repositories,
 * with both default and annotation-specified paths to the native library file.
 * @library ../tools
 * @compile -XDignore.symbol.file NativeLibraryTest.java LibraryTest.java ../tools/JamBuilder.java
 * @run main NativeLibraryTest
 */
public class NativeLibraryTest extends LibraryTest {
    // Number of tests; distinguishes instances of native libary
    static int count = 0;

    String getTestName() {
        return "NatLibTest";
    }

    String getPkgName() {
        return "nativelibtest";
    }

    String getTestcaseMethodBody(String nativeLibName) {
        return
            "        System.loadLibrary(\"" + nativeLibName + "\");\n" +
            "        return \"loaded " + nativeLibName + "\";\n";
    }

    static void realMain(String[] args) throws Throwable {
        new NativeLibraryTest().run(args);
    }

    private void run(String[] args) throws Throwable {
        // Enables shadow file copies in the repository if we're running
        // on Windows. This is to prevent file locking in the
        // source location.
        if (System.getProperty("os.platform").equalsIgnoreCase("windows")) {
            System.setProperty("java.module.repository.shadowcopyfiles", "true");
        }

        // Run the tests against a URLRepository
        File srcDir = makeTestDir("urlSource");
        repo = Modules.newURLRepository(
            "NativeLibTestURLRepo", srcDir.getCanonicalFile().toURI().toURL(), null);
        runTest("DefaultTest", null);
        runTest("CustomTest", "home/on/the/range");

        // When not debugging and there are no failures, remove test dirs
        if (!debug && failed == 0) {
            JamUtils.recursiveDelete(srcDir);
            repo.shutdown();
        }

        // Run the tests against a LocalRepository
        srcDir = makeTestDir("localSource");
        repo = Modules.newLocalRepository(
            "NativeLibTestLocalRepo", srcDir.getCanonicalFile(), null);
        runTest("DefaultTest", null);
        runTest("CustomTest", "no/place/like/home");

        // When not debugging and there are no failures, remove test dirs
        if (!debug && failed == 0) {
            JamUtils.recursiveDelete(baseDir);
            repo.shutdown();
        }
    }

    void runTest(String name, String path) throws Throwable {
        String modName = name + "Module";

        File jamDir = makeTestDir("jam");

        JamBuilder jb = new JamBuilder(
            getPkgName(), name, modName, "1.0",
            platform, arch, false, jamDir);

        String libPath = "MODULE-INF/bin/" + platform + "/" + arch;
        if (path != null) {
            // Run two tests: In this block a test with a path specified, but
            // no library at that location; expect a failure.  Then fall
            // through this block to run a second with test a path specified
            // and a library at that location.

            libPath = path;
            jb.addAnnotation(
                "@NativeLibraryPaths({\n" +
                "    @NativeLibraryPath(" +
                "        platform=\"" + platform + "\", " +
                "        arch=\"" + arch + "\", " +
                "        path=\"" + libPath + "\")})");

            File jamFile = getJam(jb);
            File nativeLibDir = makeTestDir("nativeLib");
            File nativeLibFile = getNativeLibrary(jamFile, libPath, nativeLibDir, count++);
            File testcaseDir = addTestcaseToJam(jamFile, nativeLibFile);

            ModuleArchiveInfo mai = repo.install(jamFile.getCanonicalFile().toURI());
            // Check module is installed
            check(mai != null);

            // Remove intermediate by-products
            check(JamUtils.recursiveDelete(nativeLibDir));
            check(JamUtils.recursiveDelete(testcaseDir));

            try {
                runModule(repo, modName, "loaded " + nativeLibFile.getName());
                fail();
            } catch (ModuleInitializationException mie) {
                pass();
            } catch(Throwable t) {
                unexpected(t);
            }
            check(repo.uninstall(mai));
            check(jamFile.delete());
        }

        File jamFile = getJam(jb);
        File nativeLibDir = makeTestDir("nativeLib");
        File nativeLibFile = getNativeLibrary(jamFile, libPath, nativeLibDir, count++);
        addNativeLibraryToJam(jamFile, libPath, nativeLibDir, nativeLibFile);
        File testcaseDir = addTestcaseToJam(jamFile, nativeLibFile);

        ModuleArchiveInfo mai = repo.install(jamFile.getCanonicalFile().toURI());
        // Check module is installed
        check(mai != null);

        // Remove intermediate by-products
        check(JamUtils.recursiveDelete(jamDir));
        check(JamUtils.recursiveDelete(nativeLibFile.getParentFile()));
        check(JamUtils.recursiveDelete(testcaseDir));

        try {
            runModule(repo, modName, "loaded " + nativeLibFile.getName());
            if (!debug && failed == 0) {
                check(repo.uninstall(mai));
            }
        } catch(Throwable t) {
            unexpected(t);
        }
    }

    File getNativeLibrary(File jamFile, String libPath, File nativeLibDir, int count) throws Exception {
        File orig = null;
        String libName = null;
        String jhome = System.getProperty("java.home");
        if (System.getProperty("os.name").startsWith("Window")) {
            orig = new File(jhome + File.separator + "bin", "zip.dll");
            libName = "nativetest-" + count + ".dll";
        } else {
            // When tested against a developer build, i.e. one for which "make
            // images" has not been done, os.arch returns x86 but the libs are
            // really in i386.
            String arch = System.getProperty("os.arch");
            if (arch.equals("x86")) {
                arch = "i386";
            }
            orig = new File(
                jhome + File.separator + "lib"
                + File.separator + arch,
                "libzip.so").getCanonicalFile();
            libName = "libnativetest-" + count + ".so";
        }
        println("=orig: " + orig);
        println("=libName: " + libName);

        File nativeLibPath = new File(nativeLibDir, libPath);
        check(nativeLibPath.mkdirs());
        println("=nativeLibPath: " + nativeLibPath);

        File nativeLibFile = new File(nativeLibPath, libName).getCanonicalFile();
        check(!nativeLibFile.exists() || nativeLibFile.delete());
        JamUtils.copyFile(orig, nativeLibFile);
        check(nativeLibFile.exists());
        println("=native library file: " + nativeLibFile);
        return nativeLibFile;
    }

    void addNativeLibraryToJam(File jamFile, String libPath,
            File nativeLibDir, File nativeLibFile) throws Exception {
        // Update the JAM with the native library
        File jamLibDir = new File(jamFile.getParent(), libPath).getCanonicalFile();
        println("=jamLibDir: " + jamLibDir);
        check(jamLibDir.isDirectory() || jamLibDir.mkdirs());
        JamUtils.copyFile(nativeLibFile, new File(jamLibDir, nativeLibFile.getName()));
        updateJam("NativeLibraryTest", jamFile, nativeLibDir, libPath);
    }

    File addTestcaseToJam(File jamFile, File nativeLibFile) throws Exception {
        // Create a MainClass that will check that the library is loaded.
        File testcaseDir = makeTestDir("testcase");
        File testcase = createTestcase(testcaseDir, nativeLibFile.getName());
        println("=testcase class file: " + testcase.getCanonicalPath());
        File pkgDir = testcase.getParentFile().getParentFile().getCanonicalFile();
        println("=testcase package-containing directory: " + pkgDir);

        // Update the JAM with the testcase which will load the native library
        File jamClassDir = new File(jamFile.getParentFile(), getPkgName());
        println("=jamClassDir: " + jamClassDir.getCanonicalPath());
        check(jamClassDir.isDirectory() || jamClassDir.mkdirs());
        updateJam("NativeLibraryTest", jamFile, pkgDir, testcase.getParentFile().getName());
        return testcaseDir;
    }

    //--------------------- Infrastructure ---------------------------
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.println("\nPassed = " + passed + " failed = " + failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
