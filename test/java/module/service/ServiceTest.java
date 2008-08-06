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
import javax.tools.*;
import sun.module.JamUtils;
import sun.module.tools.*;

/**
 * Base class for service loader tests.
 */
abstract public class ServiceTest  {
    static final boolean DEBUG = System.getProperty("service.debug") != null;

    static String javaCmd;

    final String scratchDirName;

    final String pkgName;

    final File scratchDir;

    final File srcDir;

    final File repoDir;

    final Repository repo;

    ServiceTest() {
        scratchDirName = getScratchDirName();
        pkgName = getPkgName();
        File scratch = null;
        File srcD = null;
        File rD = null;
        try {
            scratch = new File(
                System.getProperty("test.scratch", "."), scratchDirName).getCanonicalFile();
            srcD = new File(
                System.getProperty("test.src", "."), "src/" + pkgName ).getCanonicalFile();
            rD = new File(scratch, "repo");
        } catch (Exception ex) {
            // Should't happen, but if it does, much worse would happen later on
            throw new RuntimeException(ex);
        }
        scratchDir = scratch;
        srcDir = srcD;
        repoDir = rD;

        Repository r = null;
        try {
            check(JamUtils.recursiveDelete(scratchDir));
            check(repoDir.mkdirs());
            r = Modules.newLocalRepository(scratchDirName, repoDir, null);
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
        repo = r;
    }

    abstract String getScratchDirName();

    abstract String getPkgName();

    File getScratchDir() {
        return scratchDir;
    }

    File getSrcDir() {
        return srcDir;
    }

    /** Compiles all files under srcDir to destDir. */
    void compileSources(File srcDir, File destDir) throws Throwable {
        destDir.mkdirs();

        // compile all source files with annotation processor
        compileSources(srcDir, destDir,
                       new FileFilter() {
                           public boolean accept(File pathname) {
                               String name = pathname.getName();
                               return name.endsWith(".java") &&
                                      name.equals("module-info.java") == false;
                       }},
                       "-processor sun.module.core.ServiceProcessor");
        // compile module-info.java
        compileSources(srcDir, destDir,
                       new FileFilter() {
                           public boolean accept(File pathname) {
                               String name = pathname.getName();
                               return name.equals("module-info.java");
                       }},
                       "-verbose");
    }

    private void compileSources(File srcDir, File destDir,
                                FileFilter ff, String compilerFlags) throws Throwable {
        List<File> srcs = new ArrayList<File>();
        for (File dir : srcDir.listFiles(
                 new FileFilter() {
                     public boolean accept(File pathname) {
                         return pathname.isDirectory();
                     }
                 })) {
            File[] srcFiles = dir.listFiles(ff);
            srcs.addAll(Arrays.asList(srcFiles));
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        String cmdPrefix = "-source 7 -target 7";
        if (compilerFlags.length() > 0) {
            cmdPrefix += " " + compilerFlags;
        }
        String cmd = cmdPrefix + " -d " + destDir + " -cp " + destDir;
        for (File f : srcs) {
            cmd += " " + f.getCanonicalPath();
        }
        debug("compiling: " + cmd);
        int rc = compiler.run(null, null, null, cmd.split(" "));
        if (rc != 0) {
            throw new Exception("Failed to compile");
        }
    }

    /**
     * Takes a set of source files and changes the @Version of the module
     * which results from compiling and JAMming them.  Resulting changed
     * sources are placed under the scratch directory.
     *
     * @param srcDir directory containing all sources
     * @param annotations Map from annotation prefixes to the new annotations
     * @param dirname name of directory containing sources under srcDir
     */
    void redefineAnnotations(Map<String, String> annotations, String dirname) throws Throwable {
        File dir = new File(srcDir, dirname);
        File srcDestDir = new File(scratchDir, "src" + dirname);
        for (File src : dir.listFiles(
                 new FileFilter() {
                     public boolean accept(File pathname) {
                         return pathname.getName().endsWith(".java");
                     }})) {
            File dest = new File(srcDestDir, src.getName());
            BufferedReader r = new BufferedReader(new FileReader(src));
            dest.getParentFile().mkdirs();
            PrintWriter w = new PrintWriter(new FileWriter(dest));
            String s = null;
            while ((s = r.readLine()) != null) {
                s = s.trim();
                boolean found = false;
                for (String key : annotations.keySet()) {
                    if (s.startsWith(key)) {
                        w.println(annotations.get(key));
                        found = true;
                    }
                }
                if (!found) {
                    w.println(s);
                }
            }
            w.close();
            r.close();
        }
    }

    /**
     * Creates a JAM file
     * @param moduleName module name
     * @param pkgName name of package that locates super_package file
     * @param srcDir the name of directory that locates classes
     * @param destDir directory where the JAM should be written
     * @param destName name of resulting JAM file
     * @return a {@code File} representing the JAM file
     */
    private File createJam(String moduleName, String pkgName,
                   String srcDir, File destDir, String destName) throws Exception {
        File rc = new File(destDir, destName);
        String classesDir = destDir + "/classes";
        String cmd = "cvf ";
        cmd += rc.getCanonicalPath() + " ";
        if (new File(classesDir, "META-INF").exists()) {
            cmd += "-C " + classesDir + " META-INF ";
        }
        cmd += "-C ";
        cmd += classesDir + " ";
        cmd += pkgName + "/" + srcDir;

        debug("jam: " + cmd);
        Jam jamTool = new Jam(System.out, System.err, "jam");
        if (!jamTool.run(cmd.split(" "))) {
            throw new Exception("jam failed for " + moduleName);
        }
        return rc;
    }

    File createJam(String pkgName, String srcDir, File destDir) throws Exception {
        String moduleName = pkgName + "." + srcDir;
        String destName = moduleName + ".jam";
        return createJam(moduleName, pkgName, srcDir, destDir, destName);
    }

    File createJam(String pkgName, String srcDir, File destDir, String destName) throws Exception {
        String moduleName = pkgName + "." + srcDir;
        return createJam(moduleName, pkgName, srcDir, destDir, destName);
    }

    /**
     * @return the path to the java command
     */
    static String getJava() {
        if (javaCmd == null) {
            javaCmd = System.getProperty("java.home")
                + File.separator + "bin" + File.separator + "java";
            if (!new File(javaCmd).exists()) {
                javaCmd = javaCmd + ".exe";
            }
        }
        return javaCmd;
    }

    /**
     * @return the path to the java command followed by the given args
     */
    static List<String> getJavaCmd(String ... args) {
        List<String> rc = new ArrayList<String>();
        rc.add(getJava());
        for (String a : args) {
            rc.add(a);
        }
        if (DEBUG) {
            rc.add("-Dservice.debug=true");
        }
        return rc;
    }

    /**
     * Runs java on the given args
     * @param name name of the jam; for debugging info
     * @param args arguments to the java command
     */
    void runJavaCmd(String name, String ... args) throws Throwable {
        runJavaCmd(name, true, args);
    }

    void runJavaCmd(String name, boolean deleteIfOK, String ... args) throws Throwable {
        List<String> javaCmd = getJavaCmd(args);
        ProcessBuilder pb = new ProcessBuilder(javaCmd);
        try {
            pb.redirectErrorStream(true);
            pb.directory(new File(System.getProperty("test.scratch", ".")));
            debug("Running " + Arrays.toString(javaCmd.toArray(new String[0])));
            Process p = pb.start();
            p.waitFor();
            debug(name + " returned " + p.exitValue());
            boolean error = check(p.exitValue() == 0);
            BufferedReader br = new BufferedReader(
                new InputStreamReader(
                    p.getInputStream()), 8192);
            String msg = "";
            String s = null;
            while ((s = br.readLine()) != null) {
                msg += ">>> " + s + "\n";
            }
            if (error) {
                System.err.println(msg);
            } else {
                debug(msg);
            }
        } catch (Exception ex) {
            unexpected(ex);
        }

        // When not debugging and there are no failures, remove test dirs
        if (deleteIfOK && !DEBUG && failed == 0) {
            JamUtils.recursiveDelete(scratchDir);
        }
    }

    /*
     * Debugging
     */

    void debug(String s) {
        if (DEBUG) System.err.println("### " + s);
    }

    void dump(Repository repo) throws IOException {
        debug("findAll: " + repo.findAll().size() + ", list: " + repo.list().size());
        for (ModuleArchiveInfo mai : repo.list()) {
            debug("repo.list: " + mai);
        }
        for (ModuleDefinition md : repo.findAll()) {
            debug("repo.findAll: " + md);
        }
        if (DEBUG) {
            JRepo jrepo = new JRepo(System.out, System.err, null);
            jrepo.run(new String[] {"list", "-v", "-r", repoDir.getCanonicalPath()});
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
}
