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
import java.module.*;
import java.net.*;
import java.util.*;
import sun.module.JamUtils;
import sun.module.tools.JRepo;

/**
 * @test
 * @summary Test execution of JRepo
 * @library ..
 * @compile -XDignore.symbol.file JRepoTest.java ../JamBuilder.java
 * @run main JRepoTest
 */
public class JRepoTest {
    private static final boolean debug = Boolean.getBoolean("module.tools.debug");

    static final ByteArrayOutputStream bout = new ByteArrayOutputStream();
    static final ByteArrayOutputStream berr = new ByteArrayOutputStream();

    public static void realMain(String args[]) throws Throwable {
        JRepo jr = new JRepo(new PrintStream(bout), new PrintStream(berr), null);

        /*
         * Check common error reporting.
         */
        check(!jr.run(new String[] {" "}) && usageOK(0));
        check(!jr.run(getArgs("notASubCcommand")) && usageOK(0));
        check(!jr.run(getArgs("list -+")) && usageOK(1));

        /*
         * Do many tests on a LocalRepository, then a few on a URLRepository
         * as a sanity check.
         */


        // Create a temporary directory for JAM files and repositories.
        File tmp = new File(
            System.getProperty("test.scratch", "."), "JRepoTestDir").getCanonicalFile();
        JamUtils.recursiveDelete(tmp);
        tmp.mkdirs();

        // Create a directory for JAM files
        File jamDir = new File(tmp, "JRepoTestJamDir");
        JamUtils.recursiveDelete(jamDir);
        jamDir.mkdirs();

        /* Check install command */

        check(!jr.run(getArgs("install")) && usageOK(0));
        check(!jr.run(getArgs("install repo module")) && usageOK(0));
        check(!jr.run(getArgs("install -r repoDoesNotExist module")) && errorOK(1));
        check(!jr.run(getArgs("install -p -r repoDoesNotExist module")) && errorOK(9));

        // Create a directory for a local repository.
        File localRepoDir = new File(tmp, "JRepoTestLocalRepoDir");
        JamUtils.recursiveDelete(localRepoDir);
        localRepoDir.mkdirs();
        String repo = localRepoDir.getCanonicalPath();

        // Verify that you can't install a module which doesn't exist
        check(!jr.run(getArgs("install -r " + repo + " NoSuchModule")) && errorOK(1));

        // Each install must be of a different module

        // Verify silent output
        File jamFile = JamBuilder.createJam(
                "jrepotest", "Example", "JRepoModuleA", "1.0", "platform", "arch", false, jamDir);
        jamFile.deleteOnExit();
        String jam = jamFile.getCanonicalPath();
        check(jr.run(getArgs("install -r " + repo + " " + jam)) && outputOK(0));

        // Verify verbose output
        jamFile = JamBuilder.createJam(
                "jrepotest", "Example", "JRepoModuleB", "1.0", "platform", "arch", false, jamDir);
        jamFile.deleteOnExit();
        jam = jamFile.getCanonicalPath();
        check(jr.run(getArgs("ins -v -r " + repo + " " + jam)) && outputOK(1));

        // Verify multiple versions of same named module
        jamFile = JamBuilder.createJam(
                "jrepotest", "Example", "JRepoModuleB", "2.0", "platform", "arch", false, jamDir);
        jamFile.deleteOnExit();
        jam = jamFile.getCanonicalPath();
        check(jr.run(getArgs("install -v -r " + repo + " " + jam)) && outputOK(1));
        jamFile = JamBuilder.createJam(
                "jrepotest", "Example", "JRepoModuleB", "2.5", "platform", "arch", false, jamDir);
        jamFile.deleteOnExit();
        jam = jamFile.getCanonicalPath();
        check(jr.run(getArgs("install -v -r " + repo + " " + jam)) && outputOK(1));

        // Verify modules are installed
        check(jr.run(getArgs("list -v -r " + repo)) && outputOK(6));

        // Verify -p is not applicable to install
        check(!jr.run(getArgs("install -p -r " + repo + " " + jam)) && usageOK(1));

        /* Check list command */

        check(!jr.run(getArgs("list")) && errorOK(0));
        check(jr.run(getArgs("list -p")) && outputOK(14));
        check(!jr.run(getArgs("list -v")) && errorOK(1));
        check(jr.run(getArgs("list -p -v")) && outputOK(14));

        // Common prefixes of "list"
        check(jr.run(getArgs("lis -r " + repo)) && outputOK(6));
        check(jr.run(getArgs("li -p -r " + repo)) && outputOK(20));
        check(jr.run(getArgs("l -v -r " + repo)) && outputOK(6));
        check(jr.run(getArgs("list -p -v -r " + repo)) && outputOK(20));

        // Nonexist things are not there
        check(!jr.run(getArgs("list ThisWillNotBeFound")) && outputOK(0));
        check(!jr.run(getArgs("list -v ThisWillNotBeFound")) && errorOK(1));

        // Bootstrap repository contents are there
        check(!jr.run(getArgs("list java.se.core")) && errorOK(0));
        check(jr.run(getArgs("list -p java.se.core")) && outputOK(3));
        check(!jr.run(getArgs("list -v java.se.core")) && errorOK(1));
        check(jr.run(getArgs("list -p -v java.se.core")) && outputOK(3));

        // Various options work
        check(jr.run(getArgs("list -r " + repo + " JRepoModuleA")) && outputOK(3));
        check(jr.run(getArgs("list -p -r " + repo + " JRepoModuleA")) && outputOK(3));
        check(jr.run(getArgs("list -v -r " + repo + " JRepoModuleA")) && outputOK(3));
        check(jr.run(getArgs("list -p -v -r " + repo + " JRepoModuleA")) && outputOK(3));

        // Given module name is treated as substring of full module names
        check(jr.run(getArgs("list -r " + repo + " JRepoModule")) && outputOK(6));
        check(jr.run(getArgs("list -p -r " + repo + " JRepoModu")) && outputOK(6));
        check(jr.run(getArgs("list -v -r " + repo + " JRepo")) && outputOK(6));
        check(jr.run(getArgs("list -p -v -r " + repo + " JR")) && outputOK(6));

        /* Check uninstall command */

        check(!jr.run(getArgs("uninstall")) && usageOK(0));
        check(!jr.run(getArgs("uninstall repo MODULE")) && usageOK(0));
        check(!jr.run(getArgs("uninstall -r repoDoesNotExist module")) && errorOK(1));
        check(!jr.run(getArgs("uninstall -p -r repoDoesNotExist module")) && errorOK(9));

        check(!jr.run(getArgs("uninstall -r " + repo + " Fred")) && errorOK(0));
        check(!jr.run(getArgs("uninstall -v -r " + repo + " Fred")) && errorOK(1));

        // Install one more module for tests below
        jamFile = JamBuilder.createJam(
                "jrepotest", "Example", "JRepoModuleC", "1.0", "platform", "arch", false, jamDir);
        jamFile.deleteOnExit();
        jam = jamFile.getCanonicalPath();
        check(jr.run(getArgs("ins -v -r " + repo + " " + jam)) && outputOK(1));

        // Verify a straightforward uninstall.
        check(jr.run(getArgs("unin -v -r " + repo + " JRepoModuleA")) && outputOK(1));

        // Verify we can't uninstall from module name alone when there is more
        // than one version with that name...
        check(!jr.run(getArgs("un -r " + repo + " JRepoModuleB")) && outputOK(0));

        // ... and that with -v the JRepo says what ones there are...
        check(!jr.run(getArgs("un -v -r " + repo + " JRepoModuleB")) && errorOK(4));

        // ... but that by appending the version, uninstall succeeds.
        check(jr.run(getArgs("un -v -r " + repo + " JRepoModuleB 2.0")) && outputOK(1));
        check(jr.run(getArgs("list -v -r " + repo)) && outputOK(5));

        // Verify that the -f flag causes the remaining JRepoModuleB versions
        // to be uninstalled
        check(jr.run(getArgs("un -v -f -r " + repo + " JRepoModuleB")) && outputOK(2));
        check(jr.run(getArgs("list -v -r " + repo)) && outputOK(3));

        // Install more modules to verify -i works
        jamFile = JamBuilder.createJam(
                "jrepotest", "Interact", "JRepoModuleD", "2.7", "platform", "arch", false, jamDir);
        jamFile.deleteOnExit();
        jam = jamFile.getCanonicalPath();
        check(jr.run(getArgs("install -v -r " + repo + " " + jam)) && outputOK(1));
        jamFile = JamBuilder.createJam(
                "jrepotest", "Interact", "JRepoModuleD", "3.1", "platform", "arch", false, jamDir);
        jamFile.deleteOnExit();
        jam = jamFile.getCanonicalPath();
        check(jr.run(getArgs("install -v -r " + repo + " " + jam)) && outputOK(1));

        // This reader will let us delete the module JRepoModuleD version 3.1.
        class MockReader extends BufferedReader {
            MockReader() {
                super(new StringReader("1\n"));
            }
        }
        JRepo jr2 = new JRepo(
            new PrintStream(bout), new PrintStream(berr), new MockReader());
        check(jr2.run(getArgs("un -v -i -r " + repo + " JRepoModuleD")) && outputOK(5));
        check(jr2.run(getArgs("list -v -r " + repo)) && outputOK(4));

        /*
         * End of checks on LocalRepository, now try a few on URLRepository.
         */

        // Create a directory for a url repository.
        File urlRepoDir = new File(tmp, "JRepoTestURLRepoDir");
        JamUtils.recursiveDelete(urlRepoDir);
        urlRepoDir.mkdirs();

        File repoDownloadDir = new File(urlRepoDir, "download");
        Map<String, String> config = new HashMap<String, String>();
        config.put(
            "sun.module.repository.URLRepository.cacheDirectory",
            repoDownloadDir.getAbsolutePath());

        String urlRepoPath = urlRepoDir.getCanonicalPath();
        if (!urlRepoPath.startsWith("/")) {
            urlRepoPath = "/" + urlRepoPath;
        }
        urlRepoPath = "file://" + urlRepoPath;
        Repository urlrepo = Modules.newURLRepository("JRepoTestURLRepository",
                                                      new URL(urlRepoPath), config);

        // Verify multiple versions of same named module
        jamFile = JamBuilder.createJam(
                "jrepotest", "Example", "URLModuleX", "7.0", "platform", "arch", false, jamDir);
        jamFile.deleteOnExit();
        jam = jamFile.getCanonicalPath();
        check(jr.run(getArgs("install -v -r " + urlRepoPath + " " + jam)) && outputOK(1));

        jamFile = JamBuilder.createJam(
                "jrepotest", "Example", "URLModuleX", "13.0", "platform", "arch", false, jamDir);
        jamFile.deleteOnExit();
        jam = jamFile.getCanonicalPath();
        check(jr.run(getArgs("install -v -r " + urlRepoPath + " " + jam)) && outputOK(1));

        // Verify list
        check(jr.run(getArgs("list -v -r " + urlRepoPath)) && outputOK(4));

        // Verify uninstall
        check(jr.run(getArgs("un -v -f -r " + urlRepoPath + " URLModuleX 13")) && outputOK(1));

        // Verify uninstall worked
        check(jr.run(getArgs("list -v -r " + urlRepoPath)) && outputOK(3));

        JRepo jr3 = new JRepo(System.out, System.err, null);
        jr3.run(getArgs("list -v -r " + urlRepoPath));

        // Cleanup test directories
        if (failed == 0) {
            JamUtils.recursiveDelete(jamDir);
            JamUtils.recursiveDelete(localRepoDir);
            JamUtils.recursiveDelete(urlRepoDir);
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

    /**
     * If the output is OK, returns true.  For now, being OK means only
     * having {code len} lines.
     */
    static boolean checkOutput(int len, ByteArrayOutputStream baos) throws Throwable {
        String s = baos.toString("ASCII");
        BufferedReader r = new BufferedReader(new StringReader(s));
        int count = 0;
        while (r.readLine() != null) {
            count++;
        }
        if (debug) System.err.println(
            "Checking expected length " + len
            + " = given length " + count
            + " on '" + s + "'");
        bout.reset();
        berr.reset();
        return len == count;
    }

    /** Check stdout. */
    static boolean outputOK(int len) throws Throwable {
        return checkOutput(len, bout);
    }

    /** Check stderr. */
    static boolean errorOK(int len) throws Throwable {
        return checkOutput(len, berr);
    }

    /** Check that usage is provided as expected. */
    static boolean usageOK(int len) throws Throwable {
        // Add number of default lines of usage output to given value.
        return checkOutput(8 + len, berr);
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
