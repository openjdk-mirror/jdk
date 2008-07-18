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
import java.module.*;
import java.util.*;
import sun.module.tools.JRepo;

/**
 * @test
 * @library ../tools ../tools/jrepo
 * @compile -XDignore.symbol.file
 *    RunMTest.java
 *    classp/MainX.java
 *    JRepoDependenciesTest.java
 * @run main/othervm
 *    -DTestDescriptionFactory.classname=JRepoDependenciesTest$MyFactory
 *    JRepoDependenciesTest
 */
public class JRepoDependenciesTest {
    private static final boolean debug = Boolean.getBoolean("module.tools.debug");

    private static PrintStream outStream = null;

    private static final String[] tests = {
        "basic/import.mtest",
        "circular/circular2.mtest",
        "importpolicy/import1.mtest",
        "importpolicy/optional1.mtest",
        "importpolicy/optional2.mtest",
        "importpolicy/recurse1.mtest",
        "importpolicy/version1.mtest",
        "importpolicy/version3.mtest",
        "optional/basic.mtest",
        "optional/indirect.mtest",
        "optional/missing.mtest",
        "validation/shadow4.mtest",
        "version/version3.mtest"
    };

    private static PrintStream resultStream;

    public static void realMain(String args[]) throws Throwable {
        if (args.length == 0) {
            args = tests;
        }
        File resultFile = new File(
            System.getProperty("test.scratch", "."), "JRepoDepOut.txt");
        resultFile.delete();
        resultStream = new PrintStream(new FileOutputStream(resultFile));
        RunMTest.main(args);
        resultStream.close();
        compare(resultFile);
    }

    static void compare(File resultFile) throws Throwable {
        File goldFile = new File(
            System.getProperty("test.src", "."), "JRepoDepGold.txt");
        if (goldFile.exists()) {
            BufferedReader goldReader = new BufferedReader(
                new FileReader(goldFile));
            BufferedReader resultReader = new BufferedReader(
                new FileReader(resultFile));
            String goldLine = null;
            String resultLine = null;
            while ((goldLine = goldReader.readLine()) != null) {
                goldLine = goldLine.replace('/', File.separatorChar);
                resultLine = resultReader.readLine();
                if (!goldLine.equals(resultLine)) {
                    System.err.println(
                        "MISMATCH: expected\n\t" + goldLine
                        + "but got\n\t" + resultLine);
                    fail();
                    break;
                }
            }
            goldReader.close();
            resultReader.close();
            pass();
        } else {
            System.err.println(
                "Error: JRepoDepGold.txt does not exist: "
                + "not comparing actual v. expected results");
            fail();
        }
    }

    /** Return an array of Strings from the given String. */
    static String[] getArgs(String s) {
        List<String> args = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(s);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (debug) System.err.println("adding arg " + token);
            args.add(token);
        }
        if (debug) System.err.println("args length is " + args.size());
        return args.toArray(new String[0]);
    }

    public static class MyFactory extends RunMTest.TestDescriptionFactory {

        protected RunMTest.TestDescription doCreate(String name) {
            return new MyTestDescription(name);
        }
    }

    public static class MyTestDescription extends RunMTest.TestDescription {
        MyTestDescription(String name) {
            super(name);
        }

        protected void runTest(RunMTest mTest) throws Exception {
            Repository parent = sun.module.repository.RepositoryConfig.getSystemRepository();
            Repository repository = Modules.newLocalRepository(
                mTest.getName(), mTest.outputDirectory, null, parent);

            File file = mTest.file;
            char SEP = File.separatorChar;
            String mString = SEP + "mtest" + SEP;
            String cPath = file.getCanonicalPath();
            int k = cPath.lastIndexOf(mString);
            String subdir;
            if (k == -1) {
                subdir = file.getName();
            } else {
                subdir = cPath.substring(k + mString.length());
            }
            resultStream.println(">>>Test " + subdir);

            JRepo jr = new JRepo(resultStream, new PrintStream(System.err), null);

            try {
                check(jr.run(getArgs(
                                 "dependencies -r "
                                 + mTest.outputDirectory.getAbsolutePath() + " "
                                 + name)));
            } catch (Exception ex) {
                if (ex instanceof ModuleInitializationException) {
                    // Ignore: Assume this is expected or that it is a new error
                    // that will be diagnosed when by examining the modinit tests
                    // themselves.
                } else {
                    unexpected(ex);
                }
            }
        }
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static boolean pass() {passed++; return true;}
    static boolean fail() {failed++; Thread.dumpStack(); return false;}
    static boolean fail(String msg) {System.err.println(msg); return fail();}
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
