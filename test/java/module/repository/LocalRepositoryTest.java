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
import java.module.ModuleDefinition;
import java.module.Modules;
import java.module.Repository;
import java.module.RepositoryEvent;
import java.module.RepositoryListener;
import java.module.ModuleDefinition;
import java.module.VersionConstraint;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import sun.module.JamUtils;
import sun.module.repository.RepositoryUtils;

/**
 * @test LocalRepositoryTest.java
 * @summary Test LocalRepository.
 * @library ../tools
 * @compile -XDignore.symbol.file LocalRepositoryTest.java ../tools/JamBuilder.java
 * @run main LocalRepositoryTest
 * */
public class LocalRepositoryTest {
    static final boolean debug = System.getProperty("module.debug") != null;

    // Setup repository listener
    static final BlockingQueue<RepositoryEvent> initEventQueue =
            new LinkedBlockingQueue<RepositoryEvent>();
    static final BlockingQueue<RepositoryEvent> shutdownEventQueue =
            new LinkedBlockingQueue<RepositoryEvent>();
    static final BlockingQueue<RepositoryEvent> installEventQueue =
            new LinkedBlockingQueue<RepositoryEvent>();
    static final BlockingQueue<RepositoryEvent> uninstallEventQueue =
            new LinkedBlockingQueue<RepositoryEvent>();

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

        // Enables shadow file copies in the repository if we're running
        // on Windows. This is to prevent file locking in the
        // source location.
        if (System.getProperty("os.platform").equalsIgnoreCase("windows")) {
            System.setProperty("java.module.repository.shadowcopyfiles", "true");
        }

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

        RepositoryListener repositoryListener = new RepositoryListener()  {
            public void handleEvent(RepositoryEvent e)  {
                if (e.getType() == RepositoryEvent.Type.REPOSITORY_INITIALIZED) {
                    initEventQueue.add(e);
                }
                if (e.getType() == RepositoryEvent.Type.REPOSITORY_SHUTDOWN) {
                    shutdownEventQueue.add(e);
                }
                if (e.getType() == RepositoryEvent.Type.MODULE_INSTALLED) {
                    installEventQueue.add(e);
                }
                if (e.getType() == RepositoryEvent.Type.MODULE_UNINSTALLED)  {
                    uninstallEventQueue.add(e);
                }
            }
        };
        Repository.addRepositoryListener(repositoryListener);

        Map<String, String> config = new HashMap<String, String>();
        config.put("sun.module.repository.LocalRepository.cacheDirectory",
                expandDir.getAbsolutePath());
        Repository repo = Modules.newLocalRepository(
            systemRepo,
            "test", srcDir, config);

        // Only REPOSITORY_INITIALIZED event should be fired.
        check(initializeEventExists(repo));
        check(!shutdownEventExists(repo));
        check(!installEventExists(repo, null));
        check(!uninstallEventExists(repo, null));

        // In a different repository, verify we get an exception if we require
        // the source dir to exist, but it does not.
        Repository r2 = null;
        try {
            config.put("sun.module.repository.LocalRepository.sourceLocationMustExist", "true");
            r2 = Modules.newLocalRepository(
                systemRepo,
                "test", new File("doesNotExist"), config);
            fail();
        } catch (IOException ex) {
            check(ex.getMessage().contains("does not exist or is not a directory"));
        } catch (Throwable t) {
            unexpected(t);
        }

        // No event should be fired.
        check(!initializeEventExists(r2));
        check(!shutdownEventExists(r2));
        check(!installEventExists(r2, null));
        check(!uninstallEventExists(r2, null));

        // Verify the repository is active and reloadable
        check(repo.isActive());
        check(repo.supportsReload());

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

        // No event should be fired.
        check(!initializeEventExists(repo));
        check(!shutdownEventExists(repo));
        check(!installEventExists(repo, null));
        check(!uninstallEventExists(repo, null));

        // Check that find() and list() return nothing from repo, since the
        // its source dir does not exist.
        check(repo.list().size() == 0);
        check(findModuleDefsInRepository(repo).size() == 0);

        srcDir.mkdirs();
        repo.reload();

        // No event should be fired.
        check(!initializeEventExists(repo));
        check(!shutdownEventExists(repo));
        check(!installEventExists(repo, null));
        check(!uninstallEventExists(repo, null));

        // Verify that repository is now *not* read-only
        check(repo.isReadOnly() == false);

        // Check that find() and list() return nothing from repo, since its
        // source dir is empty.
        check(repo.list().size() == 0);
        check(findModuleDefsInRepository(repo).size() == 0);

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
        ModuleArchiveInfo mai = installed.get(0);
        check(mai.getName().equals("LocalRepoModuleA"));

        // MODULE_INSTALLED event should be fired.
        check(!initializeEventExists(repo));
        check(!shutdownEventExists(repo));
        check(installEventExists(repo, mai));
        check(!uninstallEventExists(repo, null));

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

        // No event should be fired.
        check(!initializeEventExists(repo));
        check(!shutdownEventExists(repo));
        check(!installEventExists(repo, null));
        check(!uninstallEventExists(repo, null));

        // Create platform-specific JAM
        final String platform = RepositoryUtils.getPlatform();
        final String arch = RepositoryUtils.getArch();

        jamFile = JamBuilder.createJam(
            "localrepotest", "LocalRepoTestB", "LocalRepoModuleB", "4.2",
            platform, arch, true, jamDir);
        mai = repo.install(jamFile.getCanonicalFile().toURI().toURL());
        check(mai != null);
        println("LocalRepoModuleB mai: " + mai);

        // MODULE_INSTALLED event should be fired.
        check(!initializeEventExists(repo));
        check(!shutdownEventExists(repo));
        check(installEventExists(repo, mai));
        check(!uninstallEventExists(repo, null));

        // Verify that same module cannot be over itself
        try {
            repo.install(jamFile.getCanonicalFile().toURI().toURL());
            fail();
        } catch (IllegalStateException ex) {
            pass();
            println("Caught expected " + ex);
        }

        // No event should be fired.
        check(!initializeEventExists(repo));
        check(!shutdownEventExists(repo));
        check(!installEventExists(repo, null));
        check(!uninstallEventExists(repo, null));

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

        mai = installed.get(0);
        repo.uninstall(mai);

        // MODULE_UNINSTALLED should be fired.
        check(!initializeEventExists(repo));
        check(!shutdownEventExists(repo));
        check(!installEventExists(repo, null));
        check(uninstallEventExists(repo, mai));

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

        // MODULE_INSTALLED event should be fired.
        check(!initializeEventExists(repo));
        check(!shutdownEventExists(repo));
        check(installEventExists(repo, mai));
        check(!uninstallEventExists(repo, null));

        installed = repo.list();
        check(installed.size() == 2);
        mai = installed.get(1);
        String name = mai.getName();
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

        // MODULE_UNINSTALLED event should be fired.
        check(!initializeEventExists(repo));
        check(!shutdownEventExists(repo));
        check(!installEventExists(repo, null));
        check(uninstallEventExists(repo, deleteMe));

        // Verify that updating a module works
        JamBuilder jb = new JamBuilder(
            "localrepotest", "LocalRepoTestD", "LocalRepoModuleD", "4.2",
            platform, arch, false, jamDir);
        jb.setMethod("foo");
        jamFile = jb.createJam();
        mai = repo.install(jamFile.getCanonicalFile().toURI().toURL());
        runModule(repo, "LocalRepoModuleD", "foo");

        // MODULE_INSTALLED event should be fired.
        check(!initializeEventExists(repo));
        check(!shutdownEventExists(repo));
        check(installEventExists(repo, mai));
        check(!uninstallEventExists(repo, null));

        // Wait a bit before overwriting the just-created JAM
        Thread.currentThread().sleep(1000);
        jb = new JamBuilder(
            "localrepotest", "LocalRepoTestD", "LocalRepoModuleD", "4.2",
            platform, arch, false, srcDir); // Note: srcDir, not jamDir
        jb.setMethod("bar");
        jamFile = jb.createJam();
        repo.reload();

        // Both MODULE_UNINSTALLED and MODULE_INSTALLED events should be fired.
        check(!initializeEventExists(repo));
        check(!shutdownEventExists(repo));
        check(installEventExists(repo, null));
        check(uninstallEventExists(repo, mai));

        runModule(repo, "LocalRepoModuleD", "bar");


        // If there are three modules that have the same name and version but
        // different platform binding:
        // 1. Specific to xyz platform and arch
        // 2. Specific to current platform and arch
        // 3. Platform and arch neutral
        //
        // If they are installed in the order of #1, #2, #3, then #2 should
        // be used to provide the module definition.
        // Then, if they are uninstalled in the order of #1 and #2, then no
        // module definition would be returned from the repository even
        // #3 still exists, because #3 has not been loaded by the repository
        // before.
        //
        // On the other hand, if they are installed in the order of #1, #3,
        // #2, then #3 should be used to provide the module definition.
        // Then, if they are uninstalled in the order of #3 and #1, then
        // no module definition would be returned from the repository even
        // #2 still exists, because #2 has not been loaded by the repository.
        //
        File jamFile1, jamFile2, jamFile3;
        ModuleArchiveInfo mai1, mai2, mai3;
        jamFile1 = JamBuilder.createJam(
            "localrepotest", "LocalRepoTestE", "LocalRepoModuleE", "7.0",
            "xyz-platform", "xyz-arch", false, jamDir);
        jamFile2 = JamBuilder.createJam(
            "localrepotest", "LocalRepoTestE", "LocalRepoModuleE", "7.0",
            platform, arch, false, jamDir);
        jamFile3 = JamBuilder.createJam(
            "localrepotest", "LocalRepoTestE", "LocalRepoModuleE", "7.0",
            null, null, false, jamDir);

        // Installs #1, #2, and #3
        mai1 = repo.install(jamFile1.getCanonicalFile().toURI().toURL());
        check(mai1 != null);
        mai2 = repo.install(jamFile2.getCanonicalFile().toURI().toURL());
        check(mai2 != null);
        mai3 = repo.install(jamFile3.getCanonicalFile().toURI().toURL());
        check(mai3 != null);

        md = repo.find("LocalRepoModuleE", VersionConstraint.valueOf("7.0"));
        check(md != null);
        java.module.annotation.PlatformBinding platformBinding = md.getAnnotation
            (java.module.annotation.PlatformBinding.class);
        if (platformBinding != null) {
            if (platformBinding.platform().equals(platform)
                && platformBinding.arch().equals(arch)) {
                pass();
            } else {
                fail();
            }
        } else {
            fail();
        }

        // Uninstall #1 and #2
        check(repo.uninstall(mai1));
        check(repo.uninstall(mai2));

        md = repo.find("LocalRepoModuleE", VersionConstraint.valueOf("7.0"));
        check (md == null);

        repo.reload();
        md = repo.find("LocalRepoModuleE", VersionConstraint.valueOf("7.0"));
        check (md != null);

        // Uninstall #3
        check(repo.uninstall(mai3));

        // Installs #1, #3, and #2
        mai1 = repo.install(jamFile1.getCanonicalFile().toURI().toURL());
        check(mai1 != null);
        mai3 = repo.install(jamFile3.getCanonicalFile().toURI().toURL());
        check(mai3 != null);
        mai2 = repo.install(jamFile2.getCanonicalFile().toURI().toURL());
        check(mai2 != null);

        md = repo.find("LocalRepoModuleE", VersionConstraint.valueOf("7.0"));
        check(md != null);
        platformBinding = md.getAnnotation
            (java.module.annotation.PlatformBinding.class);
        if (platformBinding == null) {
            pass();
        } else {
            fail();
        }

        // Uninstall #3 and #1
        check(repo.uninstall(mai3));
        check(repo.uninstall(mai1));

        md = repo.find("LocalRepoModuleE", VersionConstraint.valueOf("7.0"));
        check (md == null);

        // Clear event queues for the tests afterwards
        initEventQueue.clear();
        shutdownEventQueue.clear();
        installEventQueue.clear();
        uninstallEventQueue.clear();

        repo.shutdown();

        // REPOSITORY_SHUTDOWN event should be fired.
        check(!initializeEventExists(repo));
        check(shutdownEventExists(repo));
        check(!installEventExists(repo, null));
        check(!uninstallEventExists(repo, null));

        // Verify cannot initialize() after shutdown()
        try {
            repo.initialize();
            fail();
        } catch (IllegalStateException ex) {
            pass();
        }

        // No event should be fired.
        check(!initializeEventExists(repo));
        check(!shutdownEventExists(repo));
        check(!installEventExists(repo, null));
        check(!uninstallEventExists(repo, null));

        // When not debugging and there are no failures, remove test dirs
        if (!debug && failed == 0) {
            JamUtils.recursiveDelete(srcDir);
            JamUtils.recursiveDelete(jamDir);
            JamUtils.recursiveDelete(expandDir);

            repo.shutdown(); // sic: multiple shutdowns are OK

            // No event should be fired.
            check(!initializeEventExists(repo));
            check(!shutdownEventExists(repo));
            check(!installEventExists(repo, null));
            check(!uninstallEventExists(repo, null));
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

    static boolean initializeEventExists(Repository repo) throws Exception {
        // Poll REPOSITORY_INITIALIZED event
        //
        RepositoryEvent evt = initEventQueue.poll(2L, TimeUnit.SECONDS);
        if (evt != null) {
            if (evt.getType() != RepositoryEvent.Type.REPOSITORY_INITIALIZED) {
                throw new Exception("Unexpected repository event type: " + evt.getType());
            }
            if (evt.getSource() != repo) {
                throw new Exception("Unexpected repository event from: " + evt.getSource());
            }
            return true;
        } else {
            return false;
        }
    }

    static boolean shutdownEventExists(Repository repo) throws Exception {
        // Poll REPOSITORY_SHUTDOWN event
        //
        RepositoryEvent evt = shutdownEventQueue.poll(2L, TimeUnit.SECONDS);
        if (evt != null) {
            if (evt.getType() != RepositoryEvent.Type.REPOSITORY_SHUTDOWN) {
                throw new Exception("Unexpected repository event type: " + evt.getType());
            }
            if (evt.getSource() != repo) {
                throw new Exception("Unexpected repository event from: " + evt.getSource());
            }
            return true;
        } else {
            return false;
        }
    }

    static boolean installEventExists(Repository repo, ModuleArchiveInfo mai) throws Exception {
        // Poll MODULE_INSTALLED event
        //
        RepositoryEvent evt = installEventQueue.poll(2L, TimeUnit.SECONDS);
        if (evt != null) {
            if (evt.getType() != RepositoryEvent.Type.MODULE_INSTALLED) {
                throw new Exception("Unexpected repository event type: " + evt.getType());
            }
            if (evt.getSource() != repo) {
                throw new Exception("Unexpected repository event from: " + evt.getSource());
            }
            if (mai != null && evt.getModuleArchiveInfo() != mai)  {
                throw new Exception("Unexpected repository event for: " + evt.getModuleArchiveInfo());
            }
            return true;
        } else {
            return false;
        }
    }

    static boolean uninstallEventExists(Repository repo, ModuleArchiveInfo mai) throws Exception {
        // Poll MODULE_UNINSTALLED event
        //
        RepositoryEvent evt = uninstallEventQueue.poll(2L, TimeUnit.SECONDS);
        if (evt != null)   {
            if (evt.getType() != RepositoryEvent.Type.MODULE_UNINSTALLED)  {
                throw new Exception("Unexpected repository event type: " + evt.getType());
            }
            if (evt.getSource() != repo)  {
                throw new Exception("Unexpected repository event from: " + evt.getSource());
            }
            if (mai != null && evt.getModuleArchiveInfo() != mai)  {
                throw new Exception("Unexpected repository event for: " + evt.getModuleArchiveInfo());
            }
            return true;
        } else {
            return false;
        }
    }

    static List<ModuleDefinition> findModuleDefsInRepository(Repository r) {
        List<ModuleDefinition> result = new ArrayList<ModuleDefinition>();
        for (ModuleDefinition md : r.findAll()) {
            if (md.getRepository() == r) {
                result.add(md);
            }
        }
        return result;
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
