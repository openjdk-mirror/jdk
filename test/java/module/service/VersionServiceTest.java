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
import java.module.ModuleArchiveInfo;
import java.module.ModuleDefinition;
import java.module.Modules;
import java.module.Repository;
import java.util.*;
import javax.tools.*;
import sun.module.JamUtils;
import sun.module.tools.JRepo;

/**
 * @test VersionServiceTest.java
 * @summary Test that expected versions of service provider modules are
 * obtained.
 * @compile -XDignore.symbol.file ServiceTest.java VersionServiceTest.java
 * @run main VersionServiceTest
 */
public class VersionServiceTest extends ServiceTest {
    String getScratchDirName() {
        return "VerServTest";
    }

    String getPkgName() {
        return "verserv";
    }

    public static void realMain(String args[]) throws Throwable {
        new VersionServiceTest().run(args);
    }

    /**
     * Creates multiple versions of a service and provider.  The service has a
     * default (i.e., the service module is also a service provider module).
     * In concert with the Main class, this verifies that a service provider
     * is used by the requestor of a service iff the service provider imports
     * the same version of the service as that used by the requestor.
     */
    void run(String args[]) throws Throwable {
        File classesDir = new File(scratchDir, "classes");
        compileSources(srcDir, classesDir);

        File serviceJam_10 = createJam(
            pkgName, "service", scratchDir, "service-1.0.jam");
        File providerJam_10 = createJam(
            pkgName, "provider", scratchDir,  "provider-1.0.jam");
        File clientJam = createJam(pkgName, "client", scratchDir);

        JamUtils.recursiveDelete(new File(classesDir, "verserv/provider"));
        Map<String, String> annotations = new HashMap<String, String>();

        annotations.put("@Version(", "@Version(\"1.5\")");
        redefineAnnotations(annotations, "provider");
        compileSources(scratchDir, classesDir);
        File providerJam_15 = createJam(
            pkgName, "provider", scratchDir,  "provider-1.5.jam");

        annotations.put("@Version(", "@Version(\"3.3\")");
        redefineAnnotations(annotations, "service");
        compileSources(scratchDir, classesDir);
        File serviceJam_33 = createJam(
            pkgName, "service", scratchDir,  "service-3.3.jam");

        annotations.put("@Version(", "@Version(\"3.3\")\n");
        annotations.put("@ImportModules(",
                     "@ImportModules({\n"
                     + "    @ImportModule(name=\"verserv.service\", version=\"[3.0, 4.0)\")\n"
                     + "})");
        redefineAnnotations(annotations, "provider");
        compileSources(scratchDir, classesDir);
        File providerJam_33 = createJam(
            pkgName, "provider", scratchDir,  "provider-3.3.jam");

        repo.install(serviceJam_10.toURI().toURL());
        repo.install(serviceJam_33.toURI().toURL());
        repo.install(providerJam_10.toURI().toURL());
        repo.install(providerJam_15.toURI().toURL());
        repo.install(providerJam_33.toURI().toURL());
        ModuleArchiveInfo client = repo.install(clientJam.toURI().toURL());

        // Check JAM files in repository
        check(repo.findAll().size() > 0);
        check(repo.list().size() > 0);

        dump(repo);

        String jamName = JamUtils.getJamFilename(
            client.getName(), client.getVersion(), null, null) + ".jam";
        runJavaCmd("client.jam",
                   "-jam",
                   new File(repoDir, jamName).getCanonicalPath());
    }

    //--------------------- Infrastructure ---------------------------
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.println("\nPassed = " + passed + " failed = " + failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
