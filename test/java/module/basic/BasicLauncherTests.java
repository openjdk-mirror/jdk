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

/**
 * @test
 * @compile -XDignore.symbol.file BasicLauncherTests.java Utils.java LauncherTestHelper.java HttpRepositoryTest.java ../repository/URLRepositoryServer.java
 * @run main/othervm/timeout=300  BasicLauncherTests
 */

/*
 * This is the main driver for the launcher tests, to ensure that
 * the launcher shall:
 *      a) process the appropriate arguments
 *      b) pass the application arguments correctly to the application
 *      c) upon an error prints appropriate diagnostics
 *      d) returns an appropriate exit value.
 *
 * There are 3 types of files we create:
 *      a> A  main class, with a dependency on another module,
 *         which also compares the arguments passed, and returns
 *         a non-zero exit value if it does not get all the expected arguments.
 *      b> an alternate main class
 * These tests are used by all java launcher argument combinations.
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class BasicLauncherTests {

    // A singleton
    private BasicLauncherTests() {}


    public static void realMain(String[] args) throws Exception {
         LauncherTestHelper.createRepo();
        // Setup the test list
        ArrayList<TestExec> tests = new ArrayList<TestExec>();
        addNegativeTests(tests);

        // TODO: XXX Uncomment when ModuleLauncher provides an ExpandedRepository
        // addModuleTests(tests);
        addJamTests(tests);
        // TODO: XXX Uncomment when ModuleLauncher supports .jam.pack.gz
        // addPackTests(tests);

        // run all the tests
        for (TestExec t: tests) {
            t.run();
        }
        //Start the HttpRepositoryTests
        HttpRepositoryTest.realMain(null);
    }

    // httpTests called from HttpRepositoryTest
    static void runHttpTests(URL u) throws IOException {
        TestExec t = new TestExec("HttpURLRepository tests");
        // run module test using file url
        t = new TestExec("modules: Http protocol");
        t.repository = u.toString();
        t.run();
    }

    // negative tests.
    static void addNegativeTests(List<TestExec> params) throws IOException {
        TestExec t;

        // run module test at an absolute directory with wrong args
        t = new TestExec("negative: wrong arguments", false);
        t.repository = LauncherTestHelper.urlRepository;
        String[] args = {"one"};
        t.cmdArgs = args;
        params.add(t);

        t = new TestExec("negative: no module", false);
        t.module = "";
        params.add(t);

        t = new TestExec("negative: module and jam", false);
        t.jam    = LauncherTestHelper.jamRepository + Utils.SEP + t.module + Utils.JAM_EXT;
        params.add(t);

        String jamfile = LauncherTestHelper.jamRepository + Utils.SEP + t.module + Utils.JAM_EXT;

        t = new TestExec("negative: repository and jam", false);
        t.repository =  LauncherTestHelper.urlRepository;
        t.jam = jamfile;
        t.module = null;
        params.add(t);

        t = new TestExec("negative: module, repository and jam", false);
        t.jam    = jamfile;
        t.repository = LauncherTestHelper.urlRepository;
        params.add(t);

        String jarfile = LauncherTestHelper.jamRepository + Utils.SEP + "t2" + Utils.JAR_EXT;

        t = new TestExec("negative: module and jar", false);
        t.jar = jarfile;
        params.add(t);

        t = new TestExec("negative: repository and jar", false);
        t.module = null;
        t.repository = LauncherTestHelper.urlRepository;
        t.jar = jarfile;
        params.add(t);

        t = new TestExec("negative: jam and jar", false);
        t.module = null;
        t.jam = jamfile;
        t.jar = jarfile;
        params.add(t);

        t = new TestExec("negative: module, repository, jam, jar", false);
        t.jam    = jamfile;
        t.repository = LauncherTestHelper.urlRepository;
        t.jar = jarfile;
        params.add(t);
    }

    static void addModuleTests(List<TestExec> params) throws IOException {
        TestExec t;
        t = new TestExec("modules: current directory");
        t.workingdir = LauncherTestHelper.rawRepository;
        params.add(t);

        t = new TestExec("modules: relative directory");
        t.workingdir = ".";
        t.repository = "." + Utils.SEP + LauncherTestHelper.REPODIRNAME + Utils.SEP +
                LauncherTestHelper.RAW_REPO;
        params.add(t);

        t = new TestExec("modules: absolute directory");
        t.repository = LauncherTestHelper.rawRepository;
        params.add(t);

        t = new TestExec("modules: file protocol");
        t.repository =new File(LauncherTestHelper.urlRepository).toURI().toURL().toString();
        params.add(t);
    }

    static void addJamTests(List<TestExec>params) throws IOException {
        String jamfile = LauncherTestHelper.jamRepository + Utils.SEP + Utils.T1_JAM;
        TestExec t;

        t = new TestExec("jam: current directory");
        t.workingdir = LauncherTestHelper.jamRepository;
        t.jam = Utils.T1_JAM;
        t.module = null;
        params.add(t);

        t = new TestExec("jam: relative directory");
        t.workingdir = ".";
        t.jam = "." + Utils.SEP +  LauncherTestHelper.REPODIRNAME + Utils.SEP +
                LauncherTestHelper.JAM_REPO + Utils.SEP + Utils.T1_JAM;
        t.module = null;
        params.add(t);

        t = new TestExec("jam: absolute directory");
        t.jam = jamfile;
        t.module = null;
        params.add(t);

        t = new TestExec("jam: absolute directory with an alternate main");
        t.classpath = LauncherTestHelper.jarRepository + Utils.SEP +
                "HelloWorld" + Utils.JAR_EXT;
        t.jam = jamfile;
        t.module = null;
        t.modulemain = "p1.AltMaint1";
        params.add(t);
    }

   static void addPackTests(List<TestExec>params) throws IOException {
        String jamfile = LauncherTestHelper.packRepository + Utils.SEP + Utils.T1_PACK;
        TestExec t;

        t = new TestExec("jam-packed: absolute directory");
        t.jam = jamfile;
        t.module = null;
        params.add(t);

        t = new TestExec("jam-packed: absolute directory with an alternate main");
        t.jam = jamfile;
        t.module = null;
        t.modulemain = "p1.AltMaint1";
        params.add(t);
    }

    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {Utils.unexpected(t);}
        System.out.println("\nPassed = " + Utils.passed + " failed = " +
                Utils.failed);
        if (Utils.failed > 0) throw new AssertionError("Some tests failed");
    }
}

class TestExec {
    // Set by the ctor to indicate test type.
    boolean positive;
    String testname;

    String workingdir   = ".";
    String repository   = null;
    String jam          = null;
    String jar          = null;
    String module       = "t1";
    String modulemain   = null;
    String classpath    = null;

    final File downloadDir;
    final String downloadDirName;

    String[] cmdArgs    = { "one", "two", "three", "four" };
    static BasicLauncherTests blt;

    private ArrayList<String> javaCmdArgs = new ArrayList<String>();
    private ArrayList<String> errlog = new ArrayList<String>();

    public TestExec(String tname) {
        testname = tname;
        positive = true;
        File baseDir = new File(System.getProperty("test.scratch", "."));
        downloadDir = new File(baseDir, "download" + System.currentTimeMillis());
        try {
            Utils.recursiveDelete(downloadDir);
            downloadDir.mkdirs();
            downloadDirName = downloadDir.getCanonicalPath();
        } catch (IOException ex) {
            throw new IllegalArgumentException(
                "Can't create test " + tname + ": " + ex);
        }
    }

    public TestExec(String tname, boolean v) {
        this(tname);
        positive = v;
    }

    void run() {
        String javaCmd = Utils.TESTJAVA + File.separator + "bin" +
                File.separator + "java";
        if (!new File(javaCmd).exists()) {
            javaCmd = javaCmd + ".exe";
        }

        javaCmdArgs.add(javaCmd);

        javaCmdArgs.add("-Dsun.module.repository.URLRepository.cacheDirectory="
                        + downloadDirName);

        // Enables shadow file copies in the repository if we're running
        // on Windows. This is to prevent file locking in the
        // source location.
        if (System.getProperty("os.platform").equalsIgnoreCase("windows")) {
            javaCmdArgs.add("-Djava.module.repository.shadowcopyfiles=true");
        }

        if (classpath != null) {
            javaCmdArgs.add("-classpath");
            javaCmdArgs.add(classpath);
        }

        if (jam != null) {
            javaCmdArgs.add("-jam");
            javaCmdArgs.add(jam);
        }

        if (jar != null) {
            javaCmdArgs.add("-jar");
            javaCmdArgs.add(jar);
        }

        if (repository != null) {
            javaCmdArgs.add("-repository");
            javaCmdArgs.add(repository);
        }

        if (module != null) {
            javaCmdArgs.add("-module");
            javaCmdArgs.add(module);
        }

        if (modulemain != null) {
            javaCmdArgs.add("-modulemain");
            javaCmdArgs.add(modulemain);
        }

        for (String arg : cmdArgs) {
            javaCmdArgs.add(arg);
        }

        ProcessBuilder pb = new ProcessBuilder(javaCmdArgs);
        Map<String, String> env = pb.environment();

        try {
            File thisDir = new File(workingdir).getCanonicalFile();
            pb.directory(thisDir);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader rd = new BufferedReader(
                new InputStreamReader(p.getInputStream()),
                8192);
            String in = rd.readLine();
            while (in != null) {
                errlog.add(in);
                in = rd.readLine();
            }
            int retval = p.waitFor();
            System.out.print(testname);
            if (positive && retval != 0) {
                Utils.fail(this.toString());
                System.out.println(":FAIL");
            } else  if (!positive && retval == 0) {
                Utils.fail(this.toString());
                System.out.println(":FAIL");
            } else {
                Utils.pass();
                System.out.println(":PASS");
            }
            p.destroy();

        } catch (Exception ex) {
            Utils.unexpected(ex);
        }

        try {
            Utils.recursiveDelete(downloadDir);
        } catch (IOException ex) {
            // ignore: it will get cleaned up by normal test mechanism
        }
    }

    public String toString() {
        StringBuilder out = new StringBuilder();
        out = out.append("Test: " + testname + "\n");
        out = out.append("  type:\t " + (positive ? "positive" : "negative") + "\n");
        out = out.append("  workdir:\t" + workingdir + "\n");
        out = out.append("  classpath:\t" + classpath + "\n");
        out = out.append("  repository:\t" + repository + "\n");
        out = out.append("  jam:\t" + jam + "\n");
        out = out.append("  module:\t" + module + "\n");
        out = out.append("  modulemain:\t" + modulemain + "\n");
        out = out.append("The full command line:\n");
        for (String arg: javaCmdArgs) {
            out = out.append(arg + " ");
        }
        out = out.append("\nThe cmd output:\n");
        for (String a : errlog) {
            out.append(a + "\n");
        }
        return new String(out.append("\n"));
    }
}
