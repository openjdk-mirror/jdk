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
 * @test RepositoryServiceTest.java
 * @summary Tests that services and providers load correctly from different
 * repositories.
 * @compile -XDignore.symbol.file ServiceTest.java RepositoryServiceTest.java
 * @run main RepositoryServiceTest
 */
public class RepositoryServiceTest extends ServiceTest {
    String getScratchDirName() {
        return "RepoServTest";
    }

    String getPkgName() {
        return "reposerv";
    }

    public static void realMain(String args[]) throws Throwable {
        new RepositoryServiceTest().run(args);
    }

    /**
     * Creates modules in two repositories.  Launches java, providing a JAM in
     * one of the repositories, which invokes ServiceLoader.  Verifies that
     * service providers can be loaded from the application repository as well
     * as one specified by the service.  In src/reposerv, see
     * service/FooService.java and client/Main.java.
     */
    void run(String args[]) throws Throwable {
        File classesDir = new File(scratchDir, "classes");
        compileSources(srcDir, classesDir);

        File serviceJam_10 = createJam(
            pkgName, "service", scratchDir, "service-1.0.jam");
        File providerJam_10 = createJam(
            pkgName, "provider", scratchDir,  "provider-1.0.jam");
        File clientJam = createJam(pkgName, "client", scratchDir);

        JamUtils.recursiveDelete(new File(classesDir, "reposerv/provider"));
        Map<String, String> annotations = new HashMap<String, String>();

        annotations.put("@Version(", "@Version(\"1.5\")");
        redefineAnnotations(annotations, "provider");
        compileSources(scratchDir, classesDir);
        File providerJam_15 = createJam(
            pkgName, "provider", scratchDir,  "provider-1.5.jam");

        // Create test repositories
        File repoDir_1 = new File(scratchDir, "repo1");
        repoDir_1.mkdirs();
        Repository repo_1 = Modules.newLocalRepository("DefaultTest", repoDir_1, null);
        File repoDir_2 = new File(scratchDir, "repo2");
        repoDir_2.mkdirs();
        Repository repo_2 = Modules.newLocalRepository("DefaultTest", repoDir_2, null);

        repo_1.install(serviceJam_10.toURI());
        repo_1.install(providerJam_10.toURI());
        ModuleArchiveInfo client = repo_1.install(clientJam.toURI());

        repo_2.install(providerJam_15.toURI());

        // Check JAM files in repository
        check(repo_1.findAll().size() > 0);
        check(repo_1.list().size() > 0);
        check(repo_2.findAll().size() > 0);
        check(repo_2.list().size() > 0);

        dump(repo_1);
        dump(repo_2);

        String jamName = JamUtils.getJamFilename(
            client.getName(), client.getVersion(), null, null) + ".jam";
        runJavaCmd("client",
                   "-DrepoPath=" + repoDir_2, // See src/reposerv/service/FooService.java
                   "-jam",
                   new File(repoDir_1, jamName).getCanonicalPath());
    }

    //--------------------- Infrastructure ---------------------------
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.println("\nPassed = " + passed + " failed = " + failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
