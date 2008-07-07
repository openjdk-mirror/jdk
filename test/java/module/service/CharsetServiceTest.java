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
 * @test CharsetServiceTest.java
 * @summary Test that a service-provider module can be accessed via a service
 * that is in java.se.core.
 * @compile -XDignore.symbol.file ServiceTest.java CharsetServiceTest.java
 * @run main CharsetServiceTest
 */
public class CharsetServiceTest extends ServiceTest {
    String getScratchDirName() {
        return "CharServTest";
    }

    String getPkgName() {
        return "charserv";
    }

    public static void realMain(String args[]) throws Throwable {
        new CharsetServiceTest().run(args);
    }

    /**
     * Creates a service-provider module and a client.  The client verifies
     * that it can access the service provider in the service-provider
     * module.
     */
    void run(String args[]) throws Throwable {
        File classesDir = new File(scratchDir, "classes");
        compileSources(srcDir, classesDir);

        File providerJam = createJam(pkgName, "provider", scratchDir, "provider.jam");
        File clientJam = createJam(pkgName, "client", scratchDir);

        // Create a JAR file containing a provider and put it on the classpath
        File services = new File(scratchDir, "classes/META-INF/services");
        services.mkdirs();
        services = new File(services, "java.nio.charset.spi.CharsetProvider");
        PrintWriter pw = new PrintWriter(services);
        pw.println("charserv.other.CharsetServiceProviderOnClasspath");
        pw.close();
        check(!pw.checkError());
        File providerJar = createJar(pkgName, "other", scratchDir,
                                     "other/CharsetServiceProviderOnClasspath.class");

        repo.install(providerJam.toURI().toURL());
        ModuleArchiveInfo client = repo.install(clientJam.toURI().toURL());

        // Check JAM files in repository
        check(repo.findAll().size() > 0);
        check(repo.list().size() > 0);

        dump(repo);

        String jamName = JamUtils.getJamFilename(
            client.getName(), client.getVersion(), null, null) + ".jam";
        runJavaCmd("client",
                   "-classpath",
                   providerJar.getCanonicalPath(),
                   "-jam",
                   new File(repoDir, jamName).getCanonicalPath());
    }

    File createJar(String pkgName, String srcDir, File destDir, String contentPath) throws Exception {
        File rc = new File(destDir, srcDir + ".jar");
        String classesDir = destDir + "/classes";
        String cmd = "cf ";
        cmd += rc.getCanonicalPath() + " ";
        if (new File(classesDir, "META-INF").exists()) {
            cmd += "-C " + classesDir + " META-INF ";
        }
        cmd += "-C ";
        cmd += classesDir + " ";
        cmd += pkgName + "/" + contentPath;

        debug("jar: " + cmd);
        sun.tools.jar.Main jarTool = new sun.tools.jar.Main(System.out, System.err, "jam");
        if (!jarTool.run(cmd.split(" "))) {
            throw new Exception("jar failed for " + srcDir);
        }
        return rc;
    }

    //--------------------- Infrastructure ---------------------------
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.println("\nPassed = " + passed + " failed = " + failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
