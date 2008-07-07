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
 * @test ClientServiceTest.java
 * @summary Test a service provider module can be directly accessed from
 * client code, as opposed to from a service module.
 * @compile -XDignore.symbol.file ServiceTest.java ClientServiceTest.java
 * @run main ClientServiceTest
 */
public class ClientServiceTest extends ServiceTest {
    String getScratchDirName() {
        return "CliServTest";
    }

    String getPkgName() {
        return "cliserv";
    }

    public static void realMain(String args[]) throws Throwable {
        new ClientServiceTest().run(args);
    }

    /**
     * Creates a service module, provider module and a client.  The client
     * uses the ServiceLoader API directly to access the service.  Contrast
     * this with the "usual/expected" case, in which the client is expected to
     * access a service module which in turn accesses the service provider
     * module(s).
     */
    void run(String args[]) throws Throwable {
        File classesDir = new File(scratchDir, "classes");
        compileSources(srcDir, classesDir);

        File serviceJam = createJam(
            pkgName, "service", scratchDir, "service.jam");
        File providerJam = createJam(
            pkgName, "provider", scratchDir,  "provider.jam");
        File clientJam = createJam(pkgName, "client", scratchDir);

        repo.install(serviceJam.toURI().toURL());
        repo.install(providerJam.toURI().toURL());
        ModuleArchiveInfo client = repo.install(clientJam.toURI().toURL());

        // Check JAM files in repository
        check(repo.findAll().size() > 0);
        check(repo.list().size() > 0);

        dump(repo);

        // The client imports the service and invokes ServiceLoader.load()
        // itself, instead of the more usual case in which
        // ServiceLoader.load() is invoked from within the service.

        String jamName = JamUtils.getJamFilename(
            client.getName(), client.getVersion(), null, null) + ".jam";

        // Here the client is in a module
        runJavaCmd("client",
                   false,
                   "-jam",
                   new File(repoDir, jamName).getCanonicalPath());

        // Here the client is on the classpath instead of in a module
        runJavaCmd("clientClass",
                   "-classpath",
                   classesDir.getCanonicalPath(),
                   "cliserv.client.MainCP");
    }

    //--------------------- Infrastructure ---------------------------
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.println("\nPassed = " + passed + " failed = " + failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
