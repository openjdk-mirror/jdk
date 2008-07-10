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
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.module.*;
import java.module.annotation.MainClass;
import java.util.*;
import sun.module.JamUtils;
import sun.module.tools.JRepo;

/**
 * @test ModuleServiceTest.java
 * @summary Test the ServiceProcessor annotation processor.
 * @compile -XDignore.symbol.file ServiceTest.java ModuleServiceTest.java
 * @run main ModuleServiceTest
 */
public class ModuleServiceTest extends ServiceTest {
    String getScratchDirName() {
        return "ModServTest";
    }

    String getPkgName() {
        return "modserv";
    }

    public static void realMain(String args[]) throws Throwable {
        new ModuleServiceTest().run(args);
    }

    void run(String args[]) throws Throwable {
        compileSources(srcDir, new File(scratchDir, "classes"));

        /*
         * Check that annotations are present
         */
        TestLoader tl = new TestLoader("service", scratchDirName);
        Class<?> clazz = tl.findClass("modserv.service.CodecSet");
        Service serviceAnno = clazz.getAnnotation(Service.class);
        debug("Service annotation=" + serviceAnno);
        check(serviceAnno != null);
        BufferedReader br = new BufferedReader(
            new InputStreamReader(tl.getResourceAsStream("META-INF/service-index")));
        String s = br.readLine(); // skip header/comment
        s = br.readLine();
        check(s.equals("modserv.service.CodecSet"));

        tl = new TestLoader("provider1", scratchDirName);
        clazz = tl.findClass("modserv.provider1.StandardCodecs");
        ServiceProvider providerAnno = clazz.getAnnotation(ServiceProvider.class);
        debug("ServiceProvider annotation=" + providerAnno);
        check(providerAnno != null);
        br = new BufferedReader(
            new InputStreamReader(tl.getResourceAsStream("META-INF/services/modserv.service.CodecSet")));
        s = br.readLine(); // skip header/comment
        int count = 0;

        tl = new TestLoader("provider3", scratchDirName);
        clazz = tl.findClass("modserv.provider3.ImplCodecs");
        providerAnno = clazz.getAnnotation(ServiceProvider.class);
        debug("ServiceProvider annotation=" + providerAnno);
        check(providerAnno != null);
        br = new BufferedReader(
            new InputStreamReader(tl.getResourceAsStream("META-INF/services/modserv.service.CodecSet")));
        s = br.readLine(); // skip header/comment

        // Create JAM files
        File serviceJam = createJam(pkgName, "service", scratchDir);
        File provider1Jam = createJam(pkgName, "provider1", scratchDir);
        File provider2Jam = createJam(pkgName, "provider2", scratchDir);
        File clientJam = createJam(pkgName, "client", scratchDir);
        File provider3Jam = createJam(pkgName, "provider3", scratchDir);

        // Check JAM files
        repo.install(serviceJam.toURI());
        repo.install(provider1Jam.toURI());
        repo.install(provider2Jam.toURI());
        ModuleArchiveInfo client = repo.install(clientJam.toURI());
        repo.install(provider3Jam.toURI());

        dump(repo);

        /*
         * Check that the user has provided annotations on the super package
         * corresponding to those in the source files of the services and
         * providers.
         */
        ModuleDefinition md = repo.find("modserv.service");
        java.module.annotation.Services servicesAnno =
            md.getAnnotation(java.module.annotation.Services.class);
        check(servicesAnno.value().length == 1);
        check(servicesAnno.value()[0].equals("modserv.service.CodecSet"));

        ModuleDefinition mdP = repo.find("modserv.provider1");
        java.module.annotation.ServiceProviders provider1Anno =
            mdP.getAnnotation(java.module.annotation.ServiceProviders.class);
        java.module.annotation.ServiceProvider[] providerAnnos = provider1Anno.value();
        check(providerAnnos.length == 2);
        for (int i = 0; i < providerAnnos.length; i++) {
            check(providerAnnos[i].service().equals("modserv.service.CodecSet"));
        }

        ModuleDefinition mdA = repo.find("modserv.provider2");
        java.module.annotation.ServiceProviders provider2Anno =
            mdA.getAnnotation(java.module.annotation.ServiceProviders.class);
        java.module.annotation.ServiceProvider[] provider2Annos = provider2Anno.value();
        check(provider2Annos.length == 1);
        // Skip check on specifics of provider annotations as that will be
        // checked by "provider1" above and since this one includes a version,
        // it is tougher to check.

         // Check that provider's version is less than provider2's version
        check(mdP.getVersion().compareTo(mdA.getVersion()) < 0);
        String jamName = JamUtils.getJamFilename(
            client.getName(), client.getVersion(), null, null) + ".jam";
        runJavaCmd("client",
                   "-jam",
                   new File(repoDir, jamName).getCanonicalPath());
    }

    /**
     * A ClassLoader which provides access to classes compiled in this test.
     */
    static class TestLoader extends ClassLoader {
        private final String classDir;

        TestLoader(String dirName, String scratchDirName) {
            classDir = scratchDirName + "/classes/";
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            Class<?> rc = null;
            try {
                byte[] b = readFile(
                    new File(classDir, name.replaceAll("\\.", "/") + ".class"));
                rc = defineClass(name, b, 0, b.length);
            } catch (IOException ex) {
                unexpected(ex);
            }
            return rc;
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            InputStream rc = null;
            try {
                rc = new FileInputStream(new File(classDir, name));
            } catch (IOException ex) {
                unexpected(ex);
            }
            return rc;
        }

        private static byte[] readFile(File file) throws IOException {
            if (file.isFile() == false) {
                throw new IOException("Not a regular file: " + file);
            }
            long llen = file.length();
            if (llen > 64 * 1024 * 1024) { // 64 MB
                throw new IOException("File too large: " + file);
            }
            InputStream in = new FileInputStream(file);
            int len = (int)llen;
            byte[] data = new byte[len];
            int ofs = 0;
            while (len > 0) {
                int n = in.read(data, ofs, len);
                if (n < 0) {
                    break;
                }
                len -= n;
                ofs += n;
            }
            in.close();
            if (len != 0) {
                throw new IOException("Could not read file");
            }
            return data;
        }
    }

    //--------------------- Infrastructure ---------------------------
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.println("\nPassed = " + passed + " failed = " + failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
