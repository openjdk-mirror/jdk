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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.module.Module;
import java.module.ModuleArchiveInfo;
import java.module.Modules;
import java.module.Repository;
import java.module.ModuleDefinition;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import sun.module.JamUtils;
import sun.module.repository.RepositoryUtils;

/**
 * @test LocalRepositoryTest.java
 * @summary Test LocalRepository.
 * @library ../tools
 * @compile -XDignore.symbol.file LocalRepositoryTest.java ../tools/JamBuilder.java
 * @run main LocalRepositoryTest
 */
public class LocalRepositoryTest {
    static final boolean debug = System.getProperty("module.debug") != null;

    private static void println(String s) {
        if (debug) System.err.println(s);
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

    public static void realMain(String[] args) throws Throwable {
        File srcDir =
            new File(
                System.getProperty("test.scratch", "."), "LocalRepoSrc").getCanonicalFile();
        if (srcDir.exists()) {
            JamUtils.recursiveDelete(srcDir);
        }
        File expandDir = makeTestDir("LocalRepoExpand");

        // Check that getSystemRepository() doesn't return null and is
        // configured OK.
        Repository systemRepo = Repository.getSystemRepository();
        check(systemRepo != null);

        Map<String, String> config = new HashMap<String, String>();
        config.put("sun.module.repository.LocalRepository.expansionDirectory",
                expandDir.getAbsolutePath());
        Repository repo = Modules.newLocalRepository(
            systemRepo,
            "test", srcDir, config);

        // In a different repository, verify we get an exception if we require
        // the source dir to exist, but it does not.
        try {
            config.put("sun.module.repository.LocalRepository.sourceLocationMustExist", "true");
            Repository r2 = Modules.newLocalRepository(
                systemRepo,
                "test", new File("doesNotExist"), config);
            fail();
        } catch (IOException ex) {
            check(ex.getMessage().contains("does not exist or is not a directory"));
        } catch (Throwable t) {
            unexpected(t);
        }

        // Verify the repository is active and reloadable
        check(repo.isActive());
        check(repo.isReloadSupported());

        // Verify the repository is read-only, since it's source location does
        // not yet exist
        check(repo.isReadOnly());

        // Verify subsequent initialize is a no-op
        try {
            repo.initialize();
            pass();
        } catch (Throwable t) {
            unexpected(t);
        }

        // Check that find() and list() return nothing from repo, since the
        // its source dir does not exist.
        check(repo.list().size() == 0);
        check(repo.findAll().size() == 12); // java.se, java.classpath, ...

        srcDir.mkdirs();
        repo.reload();

        // Verify that repository is now *not* read-only
        check(repo.isReadOnly() == false);

        // Check that find() and list() return nothing from repo, since its
        // source dir is empty.
        check(repo.list().size() == 0);
        check(repo.findAll().size() == 12); // java.se, java.classpath, ...

        File jamDir = makeTestDir("LocalRepoJam");

        // Create a JAM file in the LocalRepository's sourceLocation
        File jamFile = JamBuilder.createJam(
            "localrepotest", "LocalRepoTestA", "LocalRepoModuleA", "3.1",
            null, null, false, srcDir);

        // Verify that we can reload from a read-only location
        boolean readOnlyChangeOK = (srcDir.setWritable(false) == true);
        repo.reload();
        if (readOnlyChangeOK) {
            check(repo.isReadOnly());
        }

        // Check initial module is installed
        List<ModuleArchiveInfo> installed = repo.list();
        println("=installed size " + installed.size());
        check(installed.size() == 1);
        check(installed.get(0).getName().equals("LocalRepoModuleA"));

        // Verify module is runnable from read-only source location
        runModule(repo, "LocalRepoModuleA");

        // Verify that we can reload from a writable directory
        readOnlyChangeOK = (srcDir.setWritable(true) == true);
        repo.reload();
        if (readOnlyChangeOK) {
            check(repo.isReadOnly() == false);
        }

        // Check initial module is (still) installed
        installed = repo.list();
        println("=installed size " + installed.size());
        check(installed.size() == 1);
        check(installed.get(0).getName().equals("LocalRepoModuleA"));

        // Create platform-specific JAM
        String platform = RepositoryUtils.getPlatform();
        String arch = RepositoryUtils.getArch();

        jamFile = JamBuilder.createJam(
            "localrepotest", "LocalRepoTestB", "LocalRepoModuleB", "4.2",
            platform, arch, false, jamDir);
        ModuleArchiveInfo mai = repo.install(jamFile.getCanonicalFile().toURI().toURL());
        check(mai != null);
        println("LocalRepoModuleB mai: " + mai);

        // Verify that same module cannot be over itself
        try {
            repo.install(jamFile.getCanonicalFile().toURI().toURL());
            fail();
        } catch (IllegalStateException ex) {
            pass();
            println("Caught expected " + ex);
        }

        // Check that all modules are installed
        installed = repo.list();
        check(installed.size() == 2);
        check(installed.get(0).getName().equals("LocalRepoModuleA"));
        check(installed.get(1).getName().equals("LocalRepoModuleB"));

        // Check that modules run
        runModule(repo, "LocalRepoModuleA");
        runModule(repo, "LocalRepoModuleB");

        for (File f : srcDir.listFiles()) {
            println("srcDir contains " + f.getName());
        }

        repo.uninstall(installed.get(0));
        installed = repo.list();
        check(installed.size() == 1);
        check(installed.get(0).getName().equals("LocalRepoModuleB"));

        runModule(repo, "LocalRepoModuleB");

        int numFound = repo.findAll().size();
        int numInstalled = repo.list().size();

        // See below where reload() is checked
        ModuleArchiveInfo deleteMe = repo.list().get(0);

        // Create another JAM for a different platform binding that
        // does not match any in existence
        jamFile = JamBuilder.createJam(
            "localrepotest", "LocalRepoTestC", "LocalRepoModuleC", "2.7",
            "abc" + platform, "def" + arch, false, jamDir);
        mai = repo.install(jamFile.getCanonicalFile().toURI().toURL());
        check(mai != null);
        println("LocalRepoModuleC mai: " + mai);
        installed = repo.list();
        check(installed.size() == 2);
        String name = installed.get(1).getName();
        check(name.equals("LocalRepoModuleC"));
        ModuleDefinition md = repo.find(name);
        check(md == null);

        // Check that one more module is listed, but the same number
        // is findable.
        check(repo.list().size() == numInstalled + 1);
        check(repo.findAll().size() == numFound);

        // Verify that if a JAM file is removed and the repository reloaded,
        // the module corresponding to that JAM is not installed.
        jamFile = new File(deleteMe.getFileName());
        check(jamFile.isFile());
        jamFile.delete();
        repo.reload();
        check(repo.find(mai.getName()) == null);

        // Verify that updating a module works
        JamBuilder jb = new JamBuilder(
            "localrepotest", "LocalRepoTestD", "LocalRepoModuleD", "4.2",
            platform, arch, false, jamDir);
        jb.setMethod("foo");
        jamFile = jb.createJam();
        repo.install(jamFile.getCanonicalFile().toURI().toURL());
        runModule(repo, "LocalRepoModuleD", "foo");

        // Wait a bit before overwriting the just-created JAM
        Thread.currentThread().sleep(1000);
        jb = new JamBuilder(
            "localrepotest", "LocalRepoTestD", "LocalRepoModuleD", "4.2",
            platform, arch, false, srcDir); // Note: srcDir, not jamDir
        jb.setMethod("bar");
        jamFile = jb.createJam();
        repo.reload();
        runModule(repo, "LocalRepoModuleD", "bar");

        // XXX TODO: try reload() of a .jam.pack.gz

        repo.shutdown();

        // Verify cannot initialize() after shutdown()
        try {
            repo.initialize();
            fail();
        } catch (IllegalStateException ex) {
            pass();
        }

        // When not debugging and there are no failures, remove test dirs
        if (!debug && failed == 0) {
            JamUtils.recursiveDelete(srcDir);
            JamUtils.recursiveDelete(jamDir);
            JamUtils.recursiveDelete(expandDir);

            repo.shutdown(); // sic: multiple shutdowns are OK
        }
    }

    static void runModule(Repository repo, String name) throws Exception {
        runModule(repo, name, null);
    }

    static void runModule(Repository repo, String name, String methodName) throws Exception {
        ModuleDefinition md = repo.find(name);
        check(md != null);
        println("=definition: " + md);

        Module m = md.getModuleInstance();
        String mainClass = md.getAnnotation(java.module.annotation.MainClass.class).value();
        println("=mainclass: " + mainClass);
        if (mainClass == null) {
            throw new Exception("No Main-Class attribute in the module definition");
        }

        println("=module: " + m);
        ClassLoader loader = m.getClassLoader();
        println("=loader: " + loader);

        Class<?> clazz = loader.loadClass(mainClass);
        println("=class: " + clazz);

        if (methodName == null) {
            Method method = clazz.getMethod("main", String[].class);
            method.invoke(null, (Object) (new String[0]));
        } else {
            Method method = clazz.getMethod(methodName, (Class<?>[]) null);
            String rc = (String) method.invoke(null, new Object[0]);
            check(methodName.equals(rc));
        }
        pass();
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
