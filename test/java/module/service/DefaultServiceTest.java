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
import java.util.ArrayList;
import java.util.List;
import javax.tools.*;
import sun.module.JamUtils;
import sun.module.tools.JRepo;

/**
 * @test DefaultServiceTest.java
 * @summary Tests default service providers in modules.
 * @compile -XDignore.symbol.file ServiceTest.java DefaultServiceTest.java
 * @run main DefaultServiceTest
 */
public class DefaultServiceTest extends ServiceTest {
    String getScratchDirName() {
        return "DefServTest";
    }

    String getPkgName() {
        return "defserv";
    }

    public static void realMain(String args[]) throws Throwable {
        new DefaultServiceTest().run(args);
    }

    void run(String args[]) throws Throwable {
        compileSources(srcDir, new File(scratchDir, "classes"));

        File serviceJam = createJam(pkgName, "service", scratchDir);
        File providerJam = createJam(pkgName, "provider", scratchDir);
        File clientJam = createJam(pkgName, "client", scratchDir);

        repo.install(serviceJam.toURI());
        repo.install(providerJam.toURI());
        ModuleArchiveInfo client = repo.install(clientJam.toURI());

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
