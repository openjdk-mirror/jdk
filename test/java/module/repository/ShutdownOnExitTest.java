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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.module.Module;
import java.module.ModuleArchiveInfo;
import java.module.Modules;
import java.module.Repository;
import java.module.ModuleDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import sun.module.JamUtils;
import sun.module.repository.RepositoryUtils;

/**
 * @test
 * @summary Verify that Repository.shutdownOnExit works.
 * @library ../tools
 * @compile -XDignore.symbol.file ShutdownOnExitTest.java ../tools/JamBuilder.java
 * @run main/timeout=60 ShutdownOnExitTest
 */
public class ShutdownOnExitTest {
    static final boolean debug = System.getProperty("repository.debug") != null;

    /**
     * Verify that shutdownOnExit works by checking for the existence of a
     * LocalRepository's expansion directory exists after the JVM exits.
     */
    public static void realMain(String[] args) throws Throwable {
        List<File> dirs = new ArrayList<File>();
        File dir;

        /*
         * Each run below causes some number of invocations of shutdownOnExit
         * with the given values.  Shutting down a LocalRepository causes
         * removal of the contents of its source directory, hence the check
         * on the length of entries in the directory.
         */

        // XXX Remove the Windows-specificity once disableModuleDefinition is implemented
        if (System.getProperty("os.name").startsWith("Windows")) {
            System.out.println("This test does not do anything when run on Windows"
                               + " fix it once disableModuleDefinition is implemented");
            pass();
            return;
        }

        File src = new File(
                System.getProperty("test.scratch", "."), "ShutdownOnExitSrc").getCanonicalFile();

        run("t", new String[] {"true"});
        check(src.list().length == 0);

        run("f", new String[] {"false"});
        check(src.list().length == 1);

        run("tf", new String[] {"true", "false"});
        check(src.list().length == 1);

        run("ft", new String[] {"false", "true"});
        check(src.list().length == 0);

        run("ff", new String[] {"false", "false"});
        check(src.list().length == 1);

        run("tt", new String[] {"true", "true"});
        check(src.list().length == 0);

        if (failed == 0) {
            JamUtils.recursiveDelete(src);
        }
    }

    /**
     * Create a and run a sub-process for a specific testcase that is given in
     * args[].  Each value of "true" in args causes an invocation of
     * shutdownOnExit with that {@code true}; any other value cause an
     * invocation with {@code false}.
     */
    static void run(String description, String[] args) throws Throwable {
        String javaCmd = System.getProperty("java.home")
            + File.separator + "bin" + File.separator + "java";
        if (!new File(javaCmd).exists()) {
            javaCmd = javaCmd + ".exe";
        }

        List<String> javaCmdArgs = new ArrayList<String>();

        javaCmdArgs.add(javaCmd);
        if (debug) {
            javaCmdArgs.add("-Drepository.debug=true");
        }
        javaCmdArgs.add("-cp");
        javaCmdArgs.add(System.getProperty("java.class.path"));
        javaCmdArgs.add("ShutdownOnExitTest$Tester");

        for (String s : args) {
            javaCmdArgs.add(s);
        }

        ProcessBuilder pb = new ProcessBuilder(javaCmdArgs);
        pb.redirectErrorStream(true);
        try {
            pb.directory(new File(System.getProperty("test.scratch", ".")).getCanonicalFile());
            Process p = pb.start();
            p.waitFor();
            println("run for " + description + " returned " + p.exitValue());
            check(p.exitValue() == 0);
            BufferedReader br = new BufferedReader(
                new InputStreamReader(
                    p.getInputStream()), 8192);
            String s;
            String msg = "";
            while ((s = br.readLine()) != null) {
                msg += s + "\n";
            }
            println(msg);
        } catch (Throwable t) {
            unexpected(t);
        }
    }

    /**
     * Represents an individual testcase that runs in a JVM separate from
     * that running the entire ShutdownOnExitTest.
     */
    public static class Tester {
        private static Repository repo;

        private static File jamDir;     // Where JAM files are created
        private static File srcDir;     // Repository's source directory
        private static File expandDir;  // Where repository puts native libs & embedded jars

        public static void main(String[] args) {
            try {
                runModule(args);
            } catch (Throwable t) {
                System.err.println(t);
                System.exit(1);
            }
            System.exit(0);
        }

        /**
         * Create a repository and invoke shutdownOnExit on it based on values
         * in args.
         */
        static void runModule(String[] args) throws Throwable {
            println("ShutdownOnExitTest.Tester with " + args[0]);

            File srcDir = makeTestDir("ShutdownOnExitSrc");

            Map<String, String> config = new HashMap<String, String>();
            config.put("sun.module.repository.LocalRepository.sourceLocationMustExist",
                       "true");
            config.put("sun.module.repository.LocalRepository.uninstallOnShutdown",
                       "true");
            Repository repo = Modules.newLocalRepository(
                Repository.getSystemRepository(),
                "test", srcDir, config);

            // Create a JAM and install it
            File jamDir = makeTestDir("ShutdownOnExitJam");
            File jamFile = JamBuilder.createJam(
                "shutdownonexittest", "ShutdownOnExitTestSrc",
                "ShutdownOnExitModule", "1.0",
                RepositoryUtils.getPlatform(), RepositoryUtils.getArch(),
                false, jamDir);
            ModuleArchiveInfo mai = repo.install(jamFile.getCanonicalFile().toURI().toURL());
            check(mai != null);
            println("ShutdownOnExitModule mai: " + mai);

            for (int i = 0; i < args.length; i++) {
                repo.shutdownOnExit(args[i].equals("true"));
            }

            // Check that module is installed
            List<ModuleArchiveInfo> installed = repo.list();
            check(installed.size() == 1);
            check(installed.get(0).getName().equals("ShutdownOnExitModule"));

            // Check that module can run
            runModule(repo, "ShutdownOnExitModule");

            if (failed == 0) {
                // It test fails, keep dirs for debugging.
                JamUtils.recursiveDelete(jamDir);
            }
        }

        static void runModule(Repository repo, String name) throws Exception {
            ModuleDefinition md = repo.find(name);
            check(md != null);
            println("definition: " + md);

            Module m = md.getModuleInstance();
            String mainClass = md.getAnnotation(java.module.annotation.MainClass.class).value();
            println("mainclass: " + mainClass);
            if (mainClass == null) {
                throw new Exception("No Main-Class attribute in the module definition");
            }

            println("module: " + m);
            ClassLoader loader = m.getClassLoader();
            println("loader: " + loader);

            Class<?> clazz = loader.loadClass(mainClass);
            println("class: " + clazz);

            Method method = clazz.getMethod("main", String[].class);
            method.invoke(null, (Object) (new String[0]));
            pass();
        }

    }

    private static File makeTestDir(String name) throws IOException {
        File rc =
            new File(
                System.getProperty("test.scratch", "."), name).getCanonicalFile();
        if (rc.exists() ) {
            JamUtils.recursiveDelete(rc);
        }
        rc.mkdirs();
        return rc;
    }

    private static void println(String s) {
        if (debug) System.err.println("=" + s);
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
