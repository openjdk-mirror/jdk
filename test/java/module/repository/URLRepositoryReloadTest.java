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

import java.io.File;
import java.io.FilenameFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.module.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import sun.module.JamUtils;
import sun.module.repository.RepositoryConfig;
import sun.module.repository.MetadataXMLWriter;
import sun.module.repository.URLRepository;

/**
 * @test URLRepositoryReloadTest.java
 * @summary Test URLRepository reloading.
 * @library ../tools
 * @compile -XDignore.symbol.file URLRepositoryReloadTest.java URLRepositoryTest.java URLRepositoryServer.java EventChecker.java ../tools/JamBuilder.java
 * @run main/othervm/timeout=600 URLRepositoryReloadTest
 */
public class URLRepositoryReloadTest extends URLRepositoryTest {
    String getTestBaseDirName() {
        return "URLRepoReloadTestDir";
    }

    List<File> getJams() { return new ArrayList<File>(); }

    static public void realMain(String[] args) throws Throwable {
        new URLRepositoryReloadTest().runMain(args);
    }

    void runTest0(Repository repo, URL repoURL, boolean fileBased) throws Throwable {

        // Enables shadow file copies in the repository if we're running
        // on Windows. This is to prevent file locking in the
        // source location.
        if (System.getProperty("os.platform").equalsIgnoreCase("windows")) {
            System.setProperty("java.module.repository.shadowcopyfiles", "true");
        }

        /*
         * Create two repositories at the same source location as the given
         * repo.  Initializing them will populate them with some modules.
         *
         * Use this repoWork to un/install modules.  That will update the
         * repository-metadata.xml, create/delete/update the MODULE.METADATA,
         * and add/remove the JAM file from the source location.  Since repoTest
         * and repoWork share (most) filesytem state, the effects of un/install
         * on repoTest can be observed in repoWork by reloading it.
         *
         * Of course this only works if install and uninstall work.
         */
        check(repoConfig != null);

        Repository repoWork = Modules.newURLRepository(
            "repoWork",
            fileBasedURL, repoConfig,
            RepositoryConfig.getApplicationRepository());

        // Only REPOSITORY_INITIALIZED event should be fired.
        check(ec.initializeEventExists(repoWork));
        check(!ec.shutdownEventExists(repoWork));
        check(!ec.installEventExists(repoWork, null));
        check(!ec.uninstallEventExists(repoWork, null));

        Repository repoTest = Modules.newURLRepository(
            "repoTest",
            repoURL, repoConfig,
            RepositoryConfig.getApplicationRepository());

        // Only REPOSITORY_INITIALIZED event should be fired.
        check(ec.initializeEventExists(repoTest));
        check(!ec.shutdownEventExists(repoTest));
        check(!ec.installEventExists(repoTest, null));
        check(!ec.uninstallEventExists(repoTest, null));

        /*
         * Check three cases of reload() [spec 6.2.6]
         * (1) New module definition added in source location
         * (2) Existing module definition removed from source location
         * (3) Existing module definition replaced in source location
         */

        // (1) New module definition added in source location
        // Install a module into repoWork; this copies the JAM to the source
        // location, puts MODULE.METADATA in the right place, and updates
        // repository-metadata.xml.
        check(repoWork.find("ModuleJamNew") == null);
        check(repoTest.find("ModuleJamNew") == null);
        File jamNew = JamBuilder.createJam(
                "jamnew", "JamNew", "ModuleJamNew",
                "1.0", null, null, false, jamDir);
        ModuleArchiveInfo maiNew = repoWork.install(
            jamNew.getCanonicalFile().toURI());
        check(maiNew != null);
        check(repoWork.find("ModuleJamNew") != null);

        // Only MODULE_ARCHIVE_INSTALLED event should be fired.
        check(!ec.initializeEventExists(repoWork));
        check(!ec.shutdownEventExists(repoWork));
        check(ec.installEventExists(repoWork, maiNew));
        check(!ec.uninstallEventExists(repoWork, null));

        // Since repoTest and repoWork share the same filesytem structures, this
        // works:
        repoTest.reload();
        check(repoTest.find("ModuleJamNew") != null);

        // Only MODULE_ARCHIVE_INSTALLED event should be fired.
        check(!ec.initializeEventExists(repoTest));
        check(!ec.shutdownEventExists(repoTest));
        check(ec.installEventExists(repoTest, null));
        check(!ec.uninstallEventExists(repoTest, null));

        // (2) Existing module definition removed from source location
        // Uninstall from repoWork...
        check(repoWork.find("ModuleJamNew") != null);
        check(repoWork.uninstall(maiNew));
        check(repoWork.find("ModuleJamNew") == null);

        // Only MODULE_ARCHIVE_UNINSTALLED event should be fired.
        check(!ec.initializeEventExists(repoWork));
        check(!ec.shutdownEventExists(repoWork));
        check(!ec.installEventExists(repoWork, null));
        check(ec.uninstallEventExists(repoWork, maiNew));

        // ... and as with case (1), this works due to shared filesystem
        // structures.
        repoTest.reload();
        check(repoTest.find("ModuleJamNew") == null);

        // Only MODULE_ARCHIVE_UNINSTALLED event should be fired.
        check(!ec.initializeEventExists(repoTest));
        check(!ec.shutdownEventExists(repoTest));
        check(!ec.installEventExists(repoTest, null));
        check(ec.uninstallEventExists(repoTest, null));

        // (3) Existing module definition replaced in source location
        // Create a module in repoTest, make sure it operates as expected
        JamBuilder jb = new JamBuilder(
                "jamreplace", "JamReplace", "ModuleJamReplace",
                "2.0", null, null, false, jamDir);
        jb.setMethod("foo");
        File jamReplace = jb.createJam();
        ModuleArchiveInfo maiReplace =
            repoWork.install(jamReplace.getCanonicalFile().toURI());
        check(repoWork.find("ModuleJamReplace") != null);
        runModule(repoWork, "ModuleJamReplace", "foo");

        // Only MODULE_ARCHIVE_INSTALLED event should be fired.
        check(!ec.initializeEventExists(repoWork));
        check(!ec.shutdownEventExists(repoWork));
        check(ec.installEventExists(repoWork, maiReplace));
        check(!ec.uninstallEventExists(repoWork, null));

        // Now reload repoTest: as with (1), the module should be available
        repoTest.reload();
        check(repoTest.find("ModuleJamReplace") != null);
        runModule(repoTest, "ModuleJamReplace", "foo");

        // Only MODULE_ARCHIVE_INSTALLED event should be fired.
        check(!ec.initializeEventExists(repoTest));
        check(!ec.shutdownEventExists(repoTest));
        check(ec.installEventExists(repoTest, null));
        check(!ec.uninstallEventExists(repoTest, null));

        // Now remove ModuleJamReplace from repoWork, create a new JAM for a
        // module of the same name, and install it in repoWork
        check(repoWork.uninstall(maiReplace));

        // Only MODULE_ARCHIVE_UNINSTALLED event should be fired.
        check(!ec.initializeEventExists(repoWork));
        check(!ec.shutdownEventExists(repoWork));
        check(!ec.installEventExists(repoWork, null));
        check(ec.uninstallEventExists(repoWork, maiReplace));

        check(repoWork.find("ModuleJamReplace") == null);
        jb.setMethod("bar");
        Thread.currentThread().sleep(1000); // make sure repos will see it as updated
        jamReplace = jb.createJam();
        maiReplace = repoWork.install(jamReplace.getCanonicalFile().toURI());
        dumpMai("Before running module with 'bar'", repoWork.list());
        check(repoWork.find("ModuleJamReplace") != null);
        runModule(repoWork, "ModuleJamReplace", "bar");

        // Only MODULE_ARCHIVE_INSTALLED event should be fired.
        check(!ec.initializeEventExists(repoWork));
        check(!ec.shutdownEventExists(repoWork));
        check(ec.installEventExists(repoWork, maiReplace));
        check(!ec.uninstallEventExists(repoWork, null));

        // Now reload repoTest.  Insofar as it is concerned, ModuleJamReplace has
        // been replaced.
        repoTest.reload();
        check(repoTest.find("ModuleJamReplace") != null);
        runModule(repoTest, "ModuleJamReplace", "bar");

        // MODULE_ARCHIVE_UNINSTALLED and MODULE_ARCHIVE_INSTALLED event should be fired.
        check(!ec.initializeEventExists(repoTest));
        check(!ec.shutdownEventExists(repoTest));
        check(ec.installEventExists(repoTest, null));
        check(ec.uninstallEventExists(repoTest, null));

        ModuleArchiveInfo mai = repoWork.list().get(0);
        check(repoWork.uninstall(mai));

        // MODULE_ARCHIVE_UNINSTALLED  event should be fired.
        check(!ec.initializeEventExists(repoWork));
        check(!ec.shutdownEventExists(repoWork));
        check(!ec.installEventExists(repoWork, null));
        check(ec.uninstallEventExists(repoWork, mai));

        repoTest.reload();

        // MODULE_ARCHIVE_UNINSTALLED  event should be fired.
        check(!ec.initializeEventExists(repoTest));
        check(!ec.shutdownEventExists(repoTest));
        check(!ec.installEventExists(repoTest, null));
        check(ec.uninstallEventExists(repoTest, null));

        repoTest.shutdown();

        // REPOSITORY_SHUTDOWN  event should be fired.
        check(!ec.initializeEventExists(repoTest));
        check(ec.shutdownEventExists(repoTest));
        check(!ec.installEventExists(repoTest, null));
        check(!ec.uninstallEventExists(repoTest, null));

        repoWork.shutdown();

        // REPOSITORY_SHUTDOWN  event should be fired.
        check(!ec.initializeEventExists(repoWork));
        check(ec.shutdownEventExists(repoWork));
        check(!ec.installEventExists(repoWork, null));
        check(!ec.uninstallEventExists(repoWork, null));
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
