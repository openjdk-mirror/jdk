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
 * @test ReexportServiceTest.java
 * @summary Test a service provider module correctly accesses services that
 * are indirectly imported.
 * @compile -XDignore.symbol.file ServiceTest.java ReexportServiceTest.java
 * @run main ReexportServiceTest
 */
public class ReexportServiceTest extends ServiceTest {
    String getScratchDirName() {
        return "RxpServTest";
    }

    String getPkgName() {
        return "rxpserv";
    }

    public static void realMain(String args[]) throws Throwable {
        new ReexportServiceTest().run(args);
    }

    /**
     * Creates a service module, provider module and a client.  The client
     * uses the ServiceLoader API directly to access the service.  Contrast
     * this with the "usual/expected" case, in which the client is expected to
     * access a service module which in turn accesses the service provider
     * module(s).
     */
    void run(String args[]) throws Throwable {
        /* The modules in this test are arranged thus:
         * Let IMP be Import
         * Let IMPT be Import Transitively
         * Let IMPRT be Import and Reexport Transitively
         *
         * ServiceLoader                     IMP        IMP             IMP
         * Caller -------------> Provider V1 ---> Extra --> Provider V2 ---> Service V2
         * |                      |
         * |                      | IMPRT
         * V                      V
         * Service V1            Service V1
         *
         * The import from Provider V1 to Service V1 is through the module
         * named "transitive".  The import from Provider V2 to Service V2 is
         * through the module named "extra".
         */

        File classesDir = new File(scratchDir, "classes");
        compileSources(srcDir, classesDir);

        File serviceV1Jam = createJam(pkgName, "service", scratchDir, "serviceV1.jam");
        File providerV1Jam = createJam(pkgName, "provider", scratchDir, "providerV1.jam");
        File transitiveJam = createJam(pkgName, "transitive", scratchDir);
        File extraJam = createJam(pkgName, "extra", scratchDir);
        File clientJam = createJam(pkgName, "client", scratchDir);

        Map<String, String> annotations = new HashMap<String, String>();

        annotations.put("@Version(", "@Version(\"2.0\")");
        redefineAnnotations(annotations, "service");
        compileSources(scratchDir, classesDir);
        File serviceV2Jam = createJam(pkgName, "service", scratchDir, "serviceV2.jam");

        annotations.clear();
        annotations.put("@Version(", "@Version(\"2.0\")");
        annotations.put("@ImportModules(",
                     "@ImportModules({\n"
                     + "    @ImportModule(name=\"rxpserv.service\", version=\"2.0\")\n"
                     + "})");
        redefineAnnotations(annotations, "provider");
        compileSources(scratchDir, classesDir);
        File providerV2Jam = createJam(pkgName, "provider", scratchDir, "providerV2.jam");

        repo.install(serviceV1Jam.toURI().toURL());
        repo.install(serviceV2Jam.toURI().toURL());
        repo.install(providerV1Jam.toURI().toURL());
        repo.install(providerV2Jam.toURI().toURL());
        repo.install(transitiveJam.toURI().toURL());
        repo.install(extraJam.toURI().toURL());
        ModuleArchiveInfo client = repo.install(clientJam.toURI().toURL());

        // Check JAM files in repository
        check(repo.findAll().size() > 0);
        check(repo.list().size() > 0);

        dump(repo);

        String jamName = JamUtils.getJamFilename(
            client.getName(), client.getVersion(), null, null) + ".jam";
        runJavaCmd("client",
                   "-jam",
                   new File(repoDir, jamName).getCanonicalPath());
    }

    //--------------------- Infrastructure ---------------------------
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.println("\nPassed = " + passed + " failed = " + failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
