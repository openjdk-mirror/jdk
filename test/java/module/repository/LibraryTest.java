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
 * @summary Support for NativeLibraryTest and LegacyJarTest.
 */
public abstract class LibraryTest {
    static final boolean debug = System.getProperty("repository.debug") != null;

    static File baseDir; // Where all test-produced tmp files are kept

    static File jamDir; // Where JAM files are built

    static Repository repo; // Repository being tested

    static String platform = RepositoryUtils.getPlatform();
    static String arch = RepositoryUtils.getArch();

    static void println(String s) {
        if (debug) System.err.println(s);
    }

    abstract String getTestName();

    abstract String getPkgName();

    File makeTestDir(String dirName) throws IOException {
        if (baseDir == null) {
            baseDir = new File(
                System.getProperty("test.scratch", "."),
                getTestName()).getCanonicalFile();
            check(!baseDir.exists() || JamUtils.recursiveDelete(baseDir));
        }

        File rc = new File(baseDir, dirName).getCanonicalFile();
        check(!rc.exists() || JamUtils.recursiveDelete(rc));
        rc.mkdirs();
        return rc;
    }

    abstract String getTestcaseMethodBody(String bodyInfo);

    File createTestcase(File testcaseDir, String bodyInfo) throws Exception {
        File dir = new File(testcaseDir, getPkgName());
        check(dir.mkdirs());
        File src = new File(dir, "Hello.java");

        PrintWriter pw = new PrintWriter(new FileWriter(src));
        pw.printf("package %s;\n\n", getPkgName());
        pw.printf("import java.util.*;\n\n");
        pw.printf("public class Hello {\n");
        pw.printf("    public static String sayHello() {\n");
        pw.printf(getTestcaseMethodBody(bodyInfo));
        pw.printf("    }\n");
        pw.printf("}\n");
        pw.close();
        if (pw.checkError()) {
            throw new Exception("Failed to write Hello.java");
        }
        JamBuilder.compileFile(src);

        File rc = new File(dir, "Hello.class");
        if (!debug) {
            src.deleteOnExit();
        }
        check(rc.isFile());
        return rc;
    }

    File getJam(JamBuilder jb) throws Exception {
        // Create and immediately rename the JAM file so that each time this
        // method is called a different JAM file is used.  (This is especially
        // important on Windows, where the jar tool otherwise complains about
        // inability to overwrite the existing file.)
        File tmpJamFile = jb.createJam();
        File jamFile = new File(
            tmpJamFile.getParentFile(),
            "" + System.currentTimeMillis() + tmpJamFile.getName());
        tmpJamFile.renameTo(jamFile);
        return jamFile.getCanonicalFile();
    }

    void runModule(Repository repo, String moduleName,
            String expectedResult) throws Exception {
        ModuleDefinition md = repo.find(moduleName);
        check(md != null);
        println("=definition: " + md);

        Module m = md.getModuleInstance();
        println("=module: " + m);

        ClassLoader loader = m.getClassLoader();
        println("=loader: " + loader);

        Class<?> clazz = loader.loadClass(getPkgName() + ".Hello");
        println("=class: " + clazz);

        Method method = clazz.getMethod("sayHello", (Class[]) null);
        println("=method: " + method);
        String answer = (String) method.invoke(null, (Object[]) null);
        println("=answer: " + answer);
        check(answer.equals(expectedResult));
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
}
