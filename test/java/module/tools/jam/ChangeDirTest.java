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

/*
 * @test
 * @summary Test change directory in Jam
 * @library ..
 * @compile -XDignore.symbol.file ChangeDirTest.java ../JamBuilder.java
 * @run main ChangeDirTest
 */

import java.io.*;
import java.util.*;
import java.util.jar.*;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import sun.module.JamUtils;
import sun.module.tools.Jam;

public class ChangeDirTest {
    private final static String jamName = "hello.jam";
    private final static String moduleName = "hello";
    private final static String fileName = "hello.txt";
    private static File srcDir;
    private static File scratchDir;

    public static void realMain(String[] args) throws Throwable {
        srcDir = new File(
            System.getProperty("test.src", ".")).getCanonicalFile();
        scratchDir = new File(
            System.getProperty("test.scratch", ".")).getCanonicalFile();

        File modSrcFile = new File(srcDir, moduleName + File.separator + "module_info.java");
        File classSrcFile = new File(srcDir, moduleName + File.separator + "Main.java");

        // Compile the source files
        JamBuilder.compileFile(modSrcFile, scratchDir);
        JamBuilder.compileFile(classSrcFile, scratchDir);

        doTest("/");
        doTest("//");
        doTest("///");
        doTest("////");
        if (System.getProperty("os.name").startsWith("Windows")) {
            doTest("\\");
            doTest("\\\\");
            doTest("\\\\\\");
            doTest("\\\\\\\\");
            doTest("\\/");
        }
    }

    static void doTest(String sep) throws Throwable {
        JarFile jf = null;
        try {
            // Create a jam file from the module-info and the classes, and the
            // hello.txt file in the subdirectory. The command is equivalent to:
            //
            //    jam cfsS {test.scratch}/hello.jam hello {test.scratch} \
            //             -C {test.scratch} hello/Main.class \
            //             -C {test.scratch} hello/module_info.class \
            //             -C {test.src}/a/b hello.txt
            //
            List<String> argList = new ArrayList<String>();
            argList.add("cfsS");
            argList.add(new File(scratchDir, jamName).getCanonicalPath());
            argList.add(moduleName);
            argList.add(scratchDir.getCanonicalPath());
            argList.add("-C");
            argList.add(scratchDir.getCanonicalPath());
            argList.add(moduleName + sep + "Main.class");
            argList.add("-C");
            argList.add(scratchDir.getCanonicalPath());
            argList.add(moduleName + sep + "module_info.class");
            argList.add("-C");
            argList.add(new File(srcDir, "a" + sep + "b").getCanonicalPath());
            argList.add(fileName);

            String jamArgs[] = new String[argList.size()];
            jamArgs = argList.toArray(jamArgs);

            Jam jamTool = new Jam(System.out, System.err, "jam");
            if (!jamTool.run(jamArgs)) {
                fail("Could not create jam file.");
            }

            // Check that the entry for hello.txt does *not* have a pathname.
            jf = new JarFile(new File(scratchDir, jamName));
            for (Enumeration<JarEntry> i = jf.entries(); i.hasMoreElements();) {
                JarEntry je = i.nextElement();
                String name = je.getName();
                if (name.indexOf(fileName) != -1) {
                    if (name.indexOf(fileName) != 0) {
                        fail(String.format(
                                 "Expected '%s' but got '%s'%n", fileName, name));
                    }
                }
            }

            pass();
        } finally {
            if (jf != null) {
                jf.close();
            }
        }
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void fail() {failed++; Thread.dumpStack();}
    static void fail(String msg) {System.out.println(msg); fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static void check(boolean cond) {if (cond) pass(); else fail();}
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.println("\nPassed = " + passed + " failed = " + failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
