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
 * @summary Test pack200-gzip in Jam
 * @library ..
 * @compile -XDignore.symbol.file Pack200Test.java ../JamBuilder.java
 * @run main Pack200Test
 */

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import sun.module.JamUtils;
import sun.module.tools.Jam;

public class Pack200Test {
    private final static String jamName = "tmp.hello.jam";
    private final static String packgzName = "hello.jam.pack.gz";
    private final static String moduleName = "hello";
    private static File srcDir;
    private static File scratchDir;

    public static void realMain(String[] args) throws Throwable {
        srcDir = new File(
            System.getProperty("test.src", ".")).getCanonicalFile();
        scratchDir = new File(
            System.getProperty("test.scratch", ".")).getCanonicalFile();

        JarFile jf = null;

        try {
            // Compile the source files
            File modSrcFile = new File(srcDir, moduleName + File.separator + "module_info.java");
            File classSrcFile = new File(srcDir, moduleName + File.separator + "Main.java");
            JamBuilder.compileFile(modSrcFile, scratchDir);
            JamBuilder.compileFile(classSrcFile, scratchDir);

            // Create a pack200-gzipped jam file from the module-info and
            // the classes. The command is equivalent to:
            //
            //    jam cfsS {test.scratch}/hello.jam.pack.gz hello {test.scratch} \
            //             -C {test.scratch} hello/Main.class \
            //             -C {test.scratch} hello/module_info.class
            //
            List<String> argList = new ArrayList<String>();
            argList = new ArrayList<String>();
            argList.add("cfsS");
            argList.add(new File(scratchDir, packgzName).getCanonicalPath());
            argList.add(moduleName);
            argList.add(scratchDir.getCanonicalPath());
            argList.add("-C");
            argList.add(scratchDir.getCanonicalPath());
            argList.add(moduleName + File.separator + "Main.class");
            argList.add("-C");
            argList.add(scratchDir.getCanonicalPath());
            argList.add(moduleName + File.separator + "module_info.class");

            String jamArgs[] = new String[argList.size()];
            jamArgs = argList.toArray(jamArgs);

            Jam jamTool = new Jam(System.out, System.err, "jam");
            if (!jamTool.run(jamArgs)) {
                fail("Could not create jam.pack.gz file.");
            }

            // Unzip and unpack the resulting files
            InputStream is = null;
            JarOutputStream os = null;
            try {
                is = new GZIPInputStream(new BufferedInputStream(
                                            new FileInputStream(new File(scratchDir, packgzName))), 8192);
                os = new JarOutputStream(new BufferedOutputStream(
                                            new FileOutputStream(new File(scratchDir, jamName))));
                Pack200.Unpacker unpacker = Pack200.newUnpacker();
                unpacker.unpack(is, os);
            } catch (IOException e) {
                fail("Could not unpack or unzip jam.pack.gz file.");
            } finally {
                if (is != null)
                    is.close();
                if (os != null)
                    os.close();
            }

            // Check that all entries are well-known and not zero length.
            jf = new JarFile(new File(scratchDir, jamName));
            for (Enumeration<JarEntry> i = jf.entries(); i.hasMoreElements();) {
                JarEntry je = i.nextElement();
                String name = je.getName();
                if ((name.equals("META-INF/")
                    || name.equals("META-INF/MANIFEST.MF")
                    || name.equals("MODULE-INF/")
                    || name.equals("MODULE-INF/MODULE.METADATA")
                    || name.equals("hello/Main.class")
                    || name.equals("hello/module_info.class")) == false) {
                    fail("Unexpected entry: " + name);
                }
                if (je.isDirectory() == false && je.getSize() <= 0) {
                    fail("Unexpected zero-byte entry: " + name);
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
