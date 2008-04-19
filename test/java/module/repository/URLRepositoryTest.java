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
import java.io.FilenameFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.module.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import sun.module.JamUtils;
import sun.module.repository.MetadataXMLWriter;
import sun.module.repository.RepositoryConfig;
import sun.module.repository.RepositoryUtils;
import sun.module.repository.URLRepository;

/**
 * @test URLRepositoryTest.java
 * @summary Test URLRepository via file: and http: protocols.
 * @library ../tools
 * @compile -XDignore.symbol.file URLRepositoryTest.java EventChecker.java URLRepositoryServer.java ../tools/JamBuilder.java
 * @run main/othervm/timeout=600 URLRepositoryTest
 */
public class URLRepositoryTest  {
    static final boolean debug = System.getProperty("repository.debug") != null;

    // Directory for repository's source
    File repoSrcDir;

    // Directory for repository's download cache
    File repoDownloadDir;

    // Directory for creating JAM files
    File jamDir;

    // JAMs to be installed & tested.
    List<File> jamFiles = new ArrayList<File>();

    // The file-based repository
    URLRepository fileBasedRepo;

    // Configuration of the URLRepository.
    Map<String, String> repoConfig;

    final EventChecker ec = new EventChecker();

    static public void realMain(String[] args) throws Throwable {
        new URLRepositoryTest().runMain(args);
    }

    void runMain(String[] args) throws Throwable {
        // Enables shadow file copies in the repository if we're running
        // on Windows. This is to prevent file locking in the
        // source location.
        if (System.getProperty("os.platform").equalsIgnoreCase("windows"))   {
            System.setProperty("java.module.repository.shadowcopyfiles", "true");
        }

        File baseDir = new File(System.getProperty("test.scratch", "."),
                                getTestBaseDirName()).getCanonicalFile();

        check(JamUtils.recursiveDelete(baseDir));

        repoSrcDir = new File(baseDir, "source");
        println("=repoSrcDir=" + repoSrcDir);

        repoDownloadDir = new File(baseDir, "download");
        check(repoDownloadDir.mkdirs());
        println("=repoDownloadDir=" + repoDownloadDir);

        jamDir = new File(baseDir, "jam");
        jamDir.mkdirs();
        check(jamDir.isDirectory());

        File packedJam = JamBuilder.createJam(
                "main", "SampleMain", "ModuleMain",
                "1.0", "windows", "i586", true, jamDir);
        check(packedJam.getName().endsWith(".pack.gz"));
        jamFiles.add(packedJam);
        jamFiles.addAll(getJams());

        repoConfig = new HashMap<String, String>();
        repoConfig.put("sun.module.repository.URLRepository.cacheDirectory",
            repoDownloadDir.getAbsolutePath());

        // These configs are so that this test will run on any platform
        repoConfig.put("sun.module.repository.URLRepository.test.platform", "windows");
        repoConfig.put("sun.module.repository.URLRepository.test.arch", "i586");

        // Running the file-based test first creates the filesystem layout of
        // JAM, metadata, and repository-metadata.xml files required by an
        // http-based URLRepository.
        runFileTest();
        runHttpTest();

        // Keep test results if debugging or there are any failures, otherwise
        // remove them.
        if (!debug && failed == 0) {
            JamUtils.recursiveDelete(repoSrcDir);
            JamUtils.recursiveDelete(repoDownloadDir);
            // XXX Remove the Windows-specificity once disableModuleDefinition is implemented
            if (!RepositoryUtils.getPlatform().startsWith("windows")) {
                check(JamUtils.recursiveDelete(jamDir));
                check(JamUtils.recursiveDelete(baseDir));
            }
        }
    }

    String getTestBaseDirName() {
        return "URLRepoTestDir";
    }

    List<File> getJams() throws Throwable {
        List<File> rc = new ArrayList<File>();
        rc.add(
            JamBuilder.createJam(
                "grumpf", "SampleGrumpf", "ModuleGrumpf",
                "3.1.4.1", "windows", "i586", false, jamDir));
        rc.add(
            JamBuilder.createJam(
                "fred", "Fred", "ModuleFred",
                "1.1.1", "linux", "i586", false, jamDir));
        rc.add(
            JamBuilder.createJam(
                "mumble", "Sample", "ModuleMumble",
                "1.2.3.4-alpha", "solaris", "sparc", true, jamDir));
        rc.add(
            JamBuilder.createJam(
                "uninstall", "SampleUninstall", "ModuleUninstall",
                "2.0", null, null, false, jamDir));
        rc.add(
            JamBuilder.createJam(
                "xml", "SampleXML", "ModuleXML",
                "2.0", "windows", "x64", false, jamDir));
        return rc;
    }

    void runFileTest() throws Throwable {
        runTest(repoSrcDir.getCanonicalFile().toURI().toURL(), true);
    }

    void runHttpTest() throws Throwable {
        URLRepositoryServer server = new URLRepositoryServer(repoSrcDir);

        try {
            server.start();
            runTest(server.getURL(), false);
        } finally {
            server.stop();
        }
    }

    void runTest(URL url, boolean fileBased) throws Throwable {
        Repository repo = Modules.newURLRepository(
            RepositoryConfig.getSystemRepository(), "test", url, repoConfig);

        // Only REPOSITORY_INITIALIZED event should be fired.
        check(ec.initializeEventExists(repo));
        check(!ec.shutdownEventExists(repo));
        check(!ec.installEventExists(repo, null));
        check(!ec.uninstallEventExists(repo, null));

        if (fileBased) {
            fileBasedRepo = (URLRepository) repo;
        }
      //  check(JamUtils.recursiveDelete(repoSrcDir));
        JamUtils.recursiveDelete(repoSrcDir);
        repoSrcDir.mkdirs();

        println("=" + getClass().getName() + " with " + url);
        runTest0(repo, fileBased);
    }

    void runTest0(Repository repo, boolean fileBased) throws Throwable {
        // In a different repository, verify we get an exception if we require
        // the source location to exist, but it does not.
        Repository r2 = null;
        try {
            Map<String, String> tmpConfig = new HashMap<String, String>();
            tmpConfig.putAll(repoConfig);
            tmpConfig.put("sun.module.repository.URLRepository.sourceLocationMustExist", "true");
            r2 = Modules.newURLRepository(
                RepositoryConfig.getSystemRepository(),
                "test2", new URL("file:///doesNotExist"), tmpConfig);
            fail();
        } catch (FileNotFoundException ex) {
            check(ex.getMessage().contains("does not exist"));
        } catch (Throwable t) {
            unexpected(t);
        }

        // No event should be fired.
        check(!ec.initializeEventExists(r2));
        check(!ec.shutdownEventExists(r2));
        check(!ec.installEventExists(r2, null));
        check(!ec.uninstallEventExists(r2, null));

        // Verify the repository is active and reloadable
        check(repo.isActive());
        check(repo.supportsReload());

        // Verify that subsequent initialize is a no-op
        try {
            repo.initialize();
            pass();
        } catch (Throwable t) {
            unexpected(t);
        }

        // No event should be fired.
        check(!ec.initializeEventExists(repo));
        check(!ec.shutdownEventExists(repo));
        check(!ec.installEventExists(repo, null));
        check(!ec.uninstallEventExists(repo, null));

        // Check that find() and list() return nothing from repo since, the
        // repository's source dir does not exist.  Do this only for fileBased
        // repository: the logic is the same for both repositories, and is
        // much harder to test for the non-fileBased case.
        if (fileBased) {
            check(repo.list().size() == 0);
            check(findModuleDefsInRepository(repo).size() == 0);
        }

        println("=repoSrcDir=" + repoSrcDir + " exists=" + repoSrcDir.exists());

        installJAMs(repo, fileBased);

        checkInstall(repo, fileBased);

        checkUninstall(repo, fileBased);
        uninstallJAMs(repo, fileBased);

        checkShutdown(repo, fileBased);
    }

    void installJAMs(Repository repo, boolean fileBased) throws Throwable {
        Repository workRepo = fileBased ? repo : fileBasedRepo;

        for (File f : jamFiles) {
            ModuleArchiveInfo mai =
                workRepo.install(f.getCanonicalFile().toURI().toURL());
            check(mai != null);

            // MODULE_INSTALLED event should be fired.
            check(!ec.initializeEventExists(workRepo));
            check(!ec.shutdownEventExists(workRepo));
            check(ec.installEventExists(workRepo, mai));
            check(!ec.uninstallEventExists(workRepo, null));
        }

        if (!fileBased) {
            repo.reload();

            // MODULE_UNINSTALLED and MODULE_INSTALLED events
            // should be fired for the existing modules
            check(!ec.initializeEventExists(repo));
            check(!ec.shutdownEventExists(repo));
            check(ec.getInstallEventQueueSize() == jamFiles.size());
            check(!ec.uninstallEventExists(repo, null));
            ec.clear();
        }

        List<ModuleArchiveInfo> installed = repo.list();
        dumpMai("Installed after install", installed);
        check(installed.size() == jamFiles.size());
    }

    void uninstallJAMs(Repository repo, boolean fileBased) throws Throwable {
        Repository workRepo = fileBased ? repo : fileBasedRepo;
        for (ModuleArchiveInfo mai : new ArrayList<ModuleArchiveInfo>(workRepo.list())) {
            check(workRepo.uninstall(mai));

            // MODULE_UNINSTALLED event should be fired.
            check(!ec.initializeEventExists(workRepo));
            check(!ec.shutdownEventExists(workRepo));
            check(!ec.installEventExists(workRepo, null));
            check(ec.uninstallEventExists(workRepo, mai));
        }
        if (!fileBased) {
            repo.reload();

            // MODULE_UNINSTALLED and MODULE_INSTALLED events
            // should be fired for the existing modules
            check(!ec.initializeEventExists(repo));
            check(!ec.shutdownEventExists(repo));
            check(!ec.installEventExists(repo, null));
            check(ec.getUninstallEventQueueSize() == jamFiles.size());
            ec.clear();
        }

        List<ModuleArchiveInfo> installed = repo.list();
        dumpMai("Installed after uninstall", installed);
        check(installed.size() == 0);
    }

    void runModule(Repository repo, String name, String methodName) throws Exception {
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
            println("=invoking main");
            Method method = clazz.getMethod("main", String[].class);
            method.invoke(null, (Object) (new String[0]));
        } else {
            println("=invoking " + methodName);
            Method method = clazz.getMethod(methodName, (Class<?>[]) null);
            String rc = (String) method.invoke(null, new Object[0]);
            check(methodName.equals(rc));
        }
        pass();
    }

    void checkInstall(Repository repo, boolean fileBased) throws Throwable {
        if (fileBased) {
            // Check install of same module fails
            try {
                repo.install(jamFiles.get(0).getCanonicalFile().toURI().toURL());
                fail();
            } catch (IllegalStateException ex) {
                println("===" + ex.getMessage());
                check(ex.getMessage().contains("is already installed"));
            } catch (Throwable t) {
                unexpected(t);
            }

            // No event should be fired.
            check(!ec.initializeEventExists(repo));
            check(!ec.shutdownEventExists(repo));
            check(!ec.installEventExists(repo, null));
            check(!ec.uninstallEventExists(repo, null));

            // Check that one more module is listed, but the same number
            // is findable.
            int numFound = repo.findAll().size();
            int numInstalled = repo.list().size();
            File extra = JamBuilder.createJam(
                "extra", "SampleExtra", "ModuleExtra",
                "0.1", "notsolaris", "notsparc", false, jamDir);
            ModuleArchiveInfo mai = null;
            mai = repo.install(extra.getCanonicalFile().toURI().toURL());
            check(mai != null);

            // MODULE_INSTALLED event should be fired.
            check(!ec.initializeEventExists(repo));
            check(!ec.shutdownEventExists(repo));
            check(ec.installEventExists(repo, mai));
            check(!ec.uninstallEventExists(repo, null));

            check(repo.list().size() == numInstalled + 1);
            check(repo.findAll().size() == numFound);
        }

        // Non .jam/.jar/.jam.pack.gz should cause exception from URLRepository.install.
        try {
            repo.install(new URL("http://www.sun.com"));
            fail();
        } catch (UnsupportedOperationException ex) {
            if (fileBased) {
                unexpected(ex);
            } else {
                pass();
            }
        } catch(IllegalArgumentException ex) {
            fail();
        } catch(ModuleFormatException mfex) {
            pass();
        } catch (Throwable t) {
            unexpected(t);
        }

        // No event should be fired.
        check(!ec.initializeEventExists(repo));
        check(!ec.shutdownEventExists(repo));
        check(!ec.installEventExists(repo, null));
        check(!ec.uninstallEventExists(repo, null));

        // Non-existent file URL should fail
        try {
            repo.install(new URL("file:/nonexistentfileforsuredontyouthinkireallyhope.jam"));
            fail();
        } catch (UnsupportedOperationException ex) {
            if (fileBased) {
                unexpected(ex);
            } else {
                pass();
            }
        } catch (IllegalArgumentException ex) { ex.printStackTrace();
            fail();
        } catch (ModuleFormatException mfex) {
            unexpected(mfex);
        } catch (IOException ioex)  {
            pass();
        } catch (Throwable t) {
            unexpected(t);
        }

        // No event should be fired.
        check(!ec.initializeEventExists(repo));
        check(!ec.shutdownEventExists(repo));
        check(!ec.installEventExists(repo, null));
        check(!ec.uninstallEventExists(repo, null));
    }

    void checkUninstall(Repository repo, boolean fileBased) throws Throwable {
        List<ModuleDefinition> defns = repo.findAll();
        println("=pre-uninstall defns.size: " + defns.size());

        dumpMai("Checking uninstall", repo.list());
        check(defns != null && defns.size() != 0);

// Can only run ModuleMain if the platform is Windows and i586
//        runModule(repo, "ModuleMain", null);

        // Verify uninstall only works from fileBased repository
        if (fileBased) {
            for (ModuleArchiveInfo mai : repo.list()) {
                if ("ModuleMumble".equals(mai.getName())) {
                    check(repo.uninstall(mai));

                    // MODULE_UNINSTALLED event should be fired.
                    check(!ec.initializeEventExists(repo));
                    check(!ec.shutdownEventExists(repo));
                    check(!ec.installEventExists(repo, null));
                    check(ec.uninstallEventExists(repo, mai));

                    break;
                }
            }
        } else {
            try {
                repo.uninstall(repo.list().get(0));
                fail();
            } catch (UnsupportedOperationException ex) {
                pass();
            } catch (Exception ex) {
                unexpected(ex);
            }

            // No event should be fired.
            check(!ec.initializeEventExists(repo));
            check(!ec.shutdownEventExists(repo));
            check(!ec.installEventExists(repo, null));
            check(!ec.uninstallEventExists(repo, null));
        }

        List<ModuleDefinition> bootDefns = Repository.getBootstrapRepository().findAll();
        defns = repo.findAll();
        println("=post-uninstall defns.size: " + defns.size());
        for (ModuleDefinition md : defns) {
            println("=definition: " + md);
        }
    }

    void checkShutdown(Repository repo, boolean fileBased) throws Throwable {
        if (!fileBased) {
            checkShutdown0(fileBasedRepo);
            checkShutdown0(repo);

            // This check is problematic, because the download directory
            // may contain downloaded JAM files that have been loaded
            // in the memory, and they cannot be deleted until the VM
            // exits. Comment it out for now.
            //
            // check(repoDownloadDir.list().length == 0);
        }
    }

    void checkShutdown0(Repository repo) throws Throwable {
        dumpMai("Checking shutdown", repo.list());
        if (failed == 0) {
            println("=shutting down " + repo.getSourceLocation());
            repo.shutdown();

            // REPOSITORY_SHUTDOWN event should be fired.
            check(!ec.initializeEventExists(repo));
            check(ec.shutdownEventExists(repo));
            check(!ec.installEventExists(repo, null));
            check(!ec.uninstallEventExists(repo, null));

            // Verify cannot initialize() after shutdown()
            try {
                repo.initialize();
                fail();
            } catch (IllegalStateException ex) {
                pass();
            }

            // No event should be fired.
            check(!ec.initializeEventExists(repo));
            check(!ec.shutdownEventExists(repo));
            check(!ec.installEventExists(repo, null));
            check(!ec.uninstallEventExists(repo, null));

            repo.shutdown();        // sic: multiple shutdowns are OK

            // No event should be fired.
            check(!ec.initializeEventExists(repo));
            check(!ec.shutdownEventExists(repo));
            check(!ec.installEventExists(repo, null));
            check(!ec.uninstallEventExists(repo, null));
        }
    }

    static void dumpMai(String msg, List<ModuleArchiveInfo> maiInfos) {
        println(msg + ": ");
        for (ModuleArchiveInfo mai : maiInfos) {
            println("=mai: " + mai);
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

    private static void println(String s) {
        if (debug) System.err.println(s);
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
