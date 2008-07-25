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
import java.lang.reflect.Method;
import java.module.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import sun.module.JamUtils;
import sun.module.repository.*;

/**
 * @test MetadataCompareTest.java
 * @summary Test that the comparison between the MODULE.METADATA which is
 * downloaded when a URLRepository is created and the MODULE.METADATA that is
 * downloaded as part of a module definition works.
 * @library ../tools
 * @compile -XDignore.symbol.file MetadataCompareTest.java ../tools/JamBuilder.java
 * @run main MetadataCompareTest
 */
public class MetadataCompareTest {
    static final boolean DEBUG = System.getProperty("repository.debug") != null;

    // Directory under which all files are written by test
    private final File testDir;

    // Directory where JAM files are written by test
    private final File jamDir;

    // Repository for test
    private Repository urlRepo;


    private static void debug(String s) {
        if (DEBUG) System.err.println(s);
    }

    public static void realMain(String[] args) throws Throwable {
        MetadataCompareTest t = new MetadataCompareTest();
        t.setup();
        t.runTest();
        t.shutdown();
    }

    MetadataCompareTest() throws Throwable {
        // Create a directory for all files related to this test
        testDir = new File(
            System.getProperty("test.scratch", "."), "MDCompareTestDir").getCanonicalFile();
        JamUtils.recursiveDelete(testDir);
        testDir.mkdirs();

        // Create a directory for JAM files
        jamDir = new File(testDir, "JamDir");
        JamUtils.recursiveDelete(jamDir);
        jamDir.mkdirs();
    }

    void setup() throws Throwable {
        // Create a URLRepository
        File urlRepoDir = new File(testDir, "URLRepoDir");
        JamUtils.recursiveDelete(urlRepoDir);
        urlRepoDir.mkdirs();
        String urlRepoLocation = urlRepoDir.getCanonicalPath();
        if (!urlRepoLocation.startsWith("/")) {
            urlRepoLocation = "/" + urlRepoLocation;
        }
        urlRepoLocation = "file://" + urlRepoLocation;

        File repoDownloadDir = new File(testDir, "download");
        check(JamUtils.recursiveDelete(repoDownloadDir));

        Map<String, String> config = new HashMap<String, String>();
        config.put("sun.module.repository.URLRepository.cacheDirectory", repoDownloadDir.getCanonicalPath());
        urlRepo = Modules.newURLRepository(
            "MDCompareTestURLRepository",
            new URL(urlRepoLocation),
            config);

        // Create a JAM file and install it
        File jamFile;
        jamFile = JamBuilder.createJam(
                "metadatacomparetest", "MDCompare", "Bad.MDFile", "3.1", null, null, false, jamDir);
        jamFile.deleteOnExit();
        String path = jamFile.getCanonicalPath();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        urlRepo.install(new URL("file://" + path).toURI());

        // Create another JAM file and install it
        jamFile = JamBuilder.createJam(
                "metadatacomparetest", "MDCompare", "Bad.MDInJam", "2.0", null, null, false, jamDir);
        jamFile.deleteOnExit();
        path = jamFile.getCanonicalPath();
        if (!path.startsWith("/"))  {
            path = "/" + path;
        }
        urlRepo.install(new URL("file://" + path).toURI());

        // Create yet another JAM file and install it
        jamFile = JamBuilder.createJam(
                "metadatacomparetest", "MDCompare", "OkModule", "2.7", null, null, false, jamDir);
        jamFile.deleteOnExit();
        path = jamFile.getCanonicalPath();
        if (!path.startsWith("/"))  {
            path = "/" + path;
        }
        urlRepo.install(new URL("file://" + path).toURI());

        // Create a dummy JAM file for the purpose of using its
        // MODULE.METADATA and JAM file to setup invalid
        // module metadata in other modules in the URLRepository.
        jamFile = JamBuilder.createJam(
                "metadatacomparetest", "XYZ", "ProblematicModule", "2.7", null, null, false, jamDir);
        jamFile.deleteOnExit();

        // Extract its MODULE.METADATA, and copy it into the repository for
        // the "Bad.MDFile" module, thereby causing a mismatch
        // between the metadata that is in the repository's JAM.
        ZipFile zf = new ZipFile(jamFile);
        ZipEntry ze = zf.getEntry("MODULE-INF/MODULE.METADATA");
        InputStream is = zf.getInputStream(ze);
        byte[] data = new byte[4096];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int count = 0;
        while ((count = is.read(data, 0, 4096)) > -1) {
            baos.write(data, 0, count);
        }
        is.close();
        zf.close();
        data = baos.toByteArray();

        URL u = new URL(urlRepoLocation);
        File f = JamUtils.getFile(u);

        // Overwrite the MODULE.METADATA for Bad.MDFile v3.1
        File mdFile = new File(f,
            "Bad.MDFile" + File.separator + "3.1" + File.separator + "MODULE.METADATA");
        OutputStream os = new FileOutputStream(mdFile);
        os.write(data, 0, data.length);
        os.close();

        // Copy the problematic JAM file into the repository for
        // the "Bad.MDInJam" module, thereby causing a mismatch
        // between the metadata that is in the repository's JAM.
        File jFile = new File(f, "Bad.MDInJam" + File.separator + "2.0" + File.separator + "Bad.MDInJam-2.0.jam");
        JamUtils.copyFile(jamFile, jFile);

        // Force the repository to reload the repository metadata
        urlRepo.reload();
    }

    void runTest() throws Throwable {
        // Check list()
        List<ModuleArchiveInfo> installed = urlRepo.list();
        debug("### installed.size=" + installed.size());
        for (ModuleArchiveInfo mai : installed) {
            debug("=mai: " + mai.getName() + ", "
                + mai.getPlatform() + ", "
                + mai.getArch() + ", "
                + mai.getVersion() + ", "
                + mai.getFileName());
        }

        List<ModuleDefinition> defns = urlRepo.findAll();
        check(defns != null && defns.size() != 0);
        debug("### defns.size=" + defns.size());
        for (ModuleDefinition md : defns) {
            debug("=definition: " + md);
            if ("OkModule".equals(md.getName())) {
                pass();
            } else if ("Bad.MDFile".equals(md.getName())) {
                fail("Module with bad MODULE.METADATA file should not be "
                    + "loaded by the URLRepository.");
            } else if ("Bad.MDInJam".equals(md.getName())) {
                try {
                    Module m = md.getModuleInstance();
                } catch (ModuleInitializationException mie) {
                    mie.printStackTrace();
                    pass();
                } catch (Throwable t) {
                    unexpected(t);
                }
            }
        }
    }

    void shutdown() throws Throwable {
        if (failed == 0) {
            boolean deleted = JamUtils.recursiveDelete(testDir);
            // XXX Enable once Repository.shutdown disposes of all its Modules
            //check(deleted);
            urlRepo.shutdown();
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
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.println("\nPassed = " + passed + " failed = " + failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
