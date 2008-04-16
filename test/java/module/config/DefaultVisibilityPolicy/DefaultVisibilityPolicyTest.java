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

import java.lang.annotation.Annotation;
import java.io.File;
import java.io.FileNotFoundException;
import java.module.ImportDependency;
import java.module.Modules;
import java.module.ModuleDefinition;
import java.module.ModuleContent;
import java.module.ModuleSystem;
import java.module.PackageDefinition;
import java.module.Repository;
import java.module.Version;
import java.module.VersionConstraint;
import java.module.VisibilityPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import sun.module.config.DefaultVisibilityPolicy;
import sun.module.config.ModuleSystemConfig;

/*
 * @test DefaultVisibilityPolicyTest.java
 * @summary Test DefaultVisibilityPolicyTest
 * @compile -XDignore.symbol.file DefaultVisibilityPolicyTest.java
 * @author Stanley M. Ho
 */

public class DefaultVisibilityPolicyTest {

    private static class MockModuleDefinition extends ModuleDefinition {
        private final String name;
        private final Version version;
        private final Map<String, String> attributes = new HashMap<String, String>();

        MockModuleDefinition(String name, Version version) {
            this.name = name;
            this.version = version;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public java.module.Version getVersion() {
            return version;
        }

        @Override
        public List<ImportDependency> getImportDependencies() {
            return Collections.unmodifiableList(new ArrayList<ImportDependency>());
        }

        @Override
        public Set<String> getAttributeNames() {
            return Collections.unmodifiableSet(attributes.keySet());
        }

        @Override
        public String getAttribute(String name) {
            return attributes.get(name);
        }

        @Override
        public Set<PackageDefinition> getMemberPackageDefinitions() {
            return Collections.unmodifiableSet(new HashSet<PackageDefinition>());
        }

        @Override
        public Set<PackageDefinition> getExportedPackageDefinitions() {
            return Collections.unmodifiableSet(new HashSet<PackageDefinition>());
        }

        @Override
        public Set<String> getMemberClasses() {
            return Collections.unmodifiableSet(new HashSet<String>());
        }

        @Override
        public Set<String> getExportedClasses() {
            return Collections.unmodifiableSet(new HashSet<String>());
        }

        @Override
        public Set<String> getExportedResources() {
            return Collections.unmodifiableSet(new HashSet<String>());
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return null;
        }

        public List<Annotation> getAnnotations() {
            return Collections.emptyList();
        }

        @Override
        public Repository getRepository() {
            return null;
        }

        @Override
        public ModuleSystem getModuleSystem() {
            return null;
        }

        @Override
        public ModuleContent getModuleContent() {
            return null;
        }

        @Override
        public boolean isModuleReleasable() {
            return true;
        }
    }

    public static void realMain(String[] args) throws Throwable {
        testConstructor01();
        testGetDefaultVisibilityPolicy01();
        testSystemPropertyOverride01();
    }

    static public void testConstructor01() throws Exception {
        try {
            MockModuleDefinition moduleDef1 = new MockModuleDefinition("a.b.c", Version.valueOf(1, 1, 1));
            MockModuleDefinition moduleDef2 = new MockModuleDefinition("a.b.c", Version.valueOf(3, 0, 0));
            MockModuleDefinition moduleDef3 = new MockModuleDefinition("d.e.f", Version.valueOf(1, 1, 1));
            MockModuleDefinition moduleDef4 = new MockModuleDefinition("d.e.f", Version.valueOf(0, 0, 0, 7));
            MockModuleDefinition moduleDef5 = new MockModuleDefinition("g.h.i", Version.valueOf(2, 0, 0));
            MockModuleDefinition moduleDef6 = new MockModuleDefinition("j.k.l", Version.valueOf(3, 0, 0));
            MockModuleDefinition moduleDef7 = new MockModuleDefinition("x.y.z", Version.valueOf(4, 0, 0));

            File f = new File(System.getProperty("test.src", "."), "WellFormed.visibility.policy");
            ModuleSystemConfig.setProperty(ModuleSystemConfig.VISIBILITY_POLICY_URL_PREFIX+2, f.toURI().toURL().toString());

            VisibilityPolicy vp = new DefaultVisibilityPolicy();

            check(vp.isVisible(moduleDef1) == true);
            check(vp.isVisible(moduleDef2) == false);
            check(vp.isVisible(moduleDef3) == true);
            check(vp.isVisible(moduleDef4) == true);
            check(vp.isVisible(moduleDef5) == false);
            check(vp.isVisible(moduleDef6) == false);
            check(vp.isVisible(moduleDef7) == false);
        }
        catch (Throwable ex) {
            unexpected(ex);
        }
    }

    static public void testGetDefaultVisibilityPolicy01() throws Exception {
        try {
            MockModuleDefinition moduleDef1 = new MockModuleDefinition("a.b.c", Version.valueOf(1, 1, 1));
            MockModuleDefinition moduleDef2 = new MockModuleDefinition("a.b.c", Version.valueOf(3, 0, 0));
            MockModuleDefinition moduleDef3 = new MockModuleDefinition("d.e.f", Version.valueOf(1, 1, 1));
            MockModuleDefinition moduleDef4 = new MockModuleDefinition("d.e.f", Version.valueOf(0, 0, 0, 7));
            MockModuleDefinition moduleDef5 = new MockModuleDefinition("g.h.i", Version.valueOf(2, 0, 0));
            MockModuleDefinition moduleDef6 = new MockModuleDefinition("j.k.l", Version.valueOf(3, 0, 0));
            MockModuleDefinition moduleDef7 = new MockModuleDefinition("x.y.z", Version.valueOf(4, 0, 0));

            File f = new File(System.getProperty("test.src", "."), "WellFormed.visibility.policy");
            ModuleSystemConfig.setProperty(ModuleSystemConfig.VISIBILITY_POLICY_URL_PREFIX+2, f.toURI().toURL().toString());
            VisibilityPolicy vp = Modules.getVisibilityPolicy();

            if (vp instanceof DefaultVisibilityPolicy)
                pass();
            else
                fail();

            DefaultVisibilityPolicy dvp = (DefaultVisibilityPolicy) vp;
            dvp.refresh();

            check(vp.isVisible(moduleDef1) == true);
            check(vp.isVisible(moduleDef2) == false);
            check(vp.isVisible(moduleDef3) == true);
            check(vp.isVisible(moduleDef4) == true);
            check(vp.isVisible(moduleDef5) == false);
            check(vp.isVisible(moduleDef6) == false);
            check(vp.isVisible(moduleDef7) == false);
        }
        catch (Throwable ex) {
            unexpected(ex);
        }

        try {
            MockModuleDefinition moduleDef1 = new MockModuleDefinition("a.b.c", Version.valueOf(1, 1, 1));
            MockModuleDefinition moduleDef2 = new MockModuleDefinition("a.b.c", Version.valueOf(3, 0, 0));
            MockModuleDefinition moduleDef3 = new MockModuleDefinition("d.e.f", Version.valueOf(1, 1, 1));
            MockModuleDefinition moduleDef4 = new MockModuleDefinition("d.e.f", Version.valueOf(0, 0, 0, 7));
            MockModuleDefinition moduleDef5 = new MockModuleDefinition("g.h.i", Version.valueOf(2, 0, 0));
            MockModuleDefinition moduleDef6 = new MockModuleDefinition("j.k.l", Version.valueOf(3, 0, 0));
            MockModuleDefinition moduleDef7 = new MockModuleDefinition("x.y.z", Version.valueOf(4, 0, 0));

            File f = new File(System.getProperty("test.src", "."), "WellFormed.visibility.policy");
            ModuleSystemConfig.setProperty(ModuleSystemConfig.VISIBILITY_POLICY_URL_PREFIX+2, f.toURI().toURL().toString());

            VisibilityPolicy vp = new DefaultVisibilityPolicy();

            check(vp.isVisible(moduleDef1) == true);
            check(vp.isVisible(moduleDef2) == false);
            check(vp.isVisible(moduleDef3) == true);
            check(vp.isVisible(moduleDef4) == true);
            check(vp.isVisible(moduleDef5) == false);
            check(vp.isVisible(moduleDef6) == false);
            check(vp.isVisible(moduleDef7) == false);
        }
        catch (Throwable ex) {
            unexpected(ex);
        }
    }

    static public void testSystemPropertyOverride01() throws Exception {
        try {
            MockModuleDefinition moduleDef1 = new MockModuleDefinition("a.b.c", Version.valueOf(1, 1, 1));
            MockModuleDefinition moduleDef2 = new MockModuleDefinition("a.b.c", Version.valueOf(3, 0, 0));
            MockModuleDefinition moduleDef3 = new MockModuleDefinition("d.e.f", Version.valueOf(1, 1, 1));
            MockModuleDefinition moduleDef4 = new MockModuleDefinition("d.e.f", Version.valueOf(0, 0, 0, 7));
            MockModuleDefinition moduleDef5 = new MockModuleDefinition("g.h.i", Version.valueOf(2, 0, 0));
            MockModuleDefinition moduleDef6 = new MockModuleDefinition("j.k.l", Version.valueOf(3, 0, 0));
            MockModuleDefinition moduleDef7 = new MockModuleDefinition("x.y.z", Version.valueOf(4, 0, 0));

            // Pretend like -Djava.module.visibility.policy.file=<url to WellFormed.visibility.policy>
            //
            File f = new File(System.getProperty("test.src", "."), "WellFormed.visibility.policy");
            System.setProperty("java.module.visibility.policy.file", f.toURI().toURL().toString());

            VisibilityPolicy vp = Modules.getVisibilityPolicy();

            if (vp instanceof DefaultVisibilityPolicy)
                pass();
            else
                fail();

            DefaultVisibilityPolicy dvp = (DefaultVisibilityPolicy) vp;
            dvp.refresh();

            check(vp.isVisible(moduleDef1) == true);
            check(vp.isVisible(moduleDef2) == false);
            check(vp.isVisible(moduleDef3) == true);
            check(vp.isVisible(moduleDef4) == true);
            check(vp.isVisible(moduleDef5) == false);
            check(vp.isVisible(moduleDef6) == false);
            check(vp.isVisible(moduleDef7) == false);
        }
        catch (Throwable ex) {
            unexpected(ex);
        }

        try {
            MockModuleDefinition moduleDef1 = new MockModuleDefinition("a.b.c", Version.valueOf(1, 1, 1));
            MockModuleDefinition moduleDef2 = new MockModuleDefinition("a.b.c", Version.valueOf(3, 0, 0));
            MockModuleDefinition moduleDef3 = new MockModuleDefinition("d.e.f", Version.valueOf(1, 1, 1));
            MockModuleDefinition moduleDef4 = new MockModuleDefinition("d.e.f", Version.valueOf(0, 0, 0, 7));
            MockModuleDefinition moduleDef5 = new MockModuleDefinition("g.h.i", Version.valueOf(2, 0, 0));
            MockModuleDefinition moduleDef6 = new MockModuleDefinition("j.k.l", Version.valueOf(3, 0, 0));
            MockModuleDefinition moduleDef7 = new MockModuleDefinition("x.y.z", Version.valueOf(4, 0, 0));

            // Pretend "visibility.policy.url.2" is set in the module system properties file
            //
            File f = new File(System.getProperty("test.src", "."), "WellFormed.visibility.policy");
            ModuleSystemConfig.setProperty(ModuleSystemConfig.VISIBILITY_POLICY_URL_PREFIX+2, f.toURI().toURL().toString());

            // Pretend like -Djava.module.visibility.policy.file==<url to EverythingVisible.visibility.policy>
            //
            File f2 = new File(System.getProperty("test.src", "."), "EverythingVisible.visibility.policy");
            System.setProperty("java.module.visibility.policy.file", "=" + f2.toURI().toURL().toString());

            VisibilityPolicy vp = Modules.getVisibilityPolicy();

            if (vp instanceof DefaultVisibilityPolicy)
                pass();
            else
                fail();

            DefaultVisibilityPolicy dvp = (DefaultVisibilityPolicy) vp;
            dvp.refresh();

            check(vp.isVisible(moduleDef1) == true);
            check(vp.isVisible(moduleDef2) == true);
            check(vp.isVisible(moduleDef3) == true);
            check(vp.isVisible(moduleDef4) == true);
            check(vp.isVisible(moduleDef5) == true);
            check(vp.isVisible(moduleDef6) == true);
            check(vp.isVisible(moduleDef7) == true);
        }
        catch (Throwable ex) {
            unexpected(ex);
        }

        try {
            MockModuleDefinition moduleDef1 = new MockModuleDefinition("a.b.c", Version.valueOf(1, 1, 1));
            MockModuleDefinition moduleDef2 = new MockModuleDefinition("a.b.c", Version.valueOf(3, 0, 0));
            MockModuleDefinition moduleDef3 = new MockModuleDefinition("d.e.f", Version.valueOf(1, 1, 1));
            MockModuleDefinition moduleDef4 = new MockModuleDefinition("d.e.f", Version.valueOf(0, 0, 0, 7));
            MockModuleDefinition moduleDef5 = new MockModuleDefinition("g.h.i", Version.valueOf(2, 0, 0));
            MockModuleDefinition moduleDef6 = new MockModuleDefinition("j.k.l", Version.valueOf(3, 0, 0));
            MockModuleDefinition moduleDef7 = new MockModuleDefinition("x.y.z", Version.valueOf(4, 0, 0));

            // Pretend "visibility.policy.url.2" is set in the module system properties file
            //
            File f = new File(System.getProperty("test.src", "."), "WellFormed.visibility.policy");
            ModuleSystemConfig.setProperty(ModuleSystemConfig.VISIBILITY_POLICY_URL_PREFIX+2, f.toURI().toURL().toString());

            // Pretend like -Djava.module.visibility.policy.file==<url to NothingVisible.visibility.policy>
            //
            File f2 = new File(System.getProperty("test.src", "."), "NothingVisible.visibility.policy");
            System.setProperty("java.module.visibility.policy.file", "=" + f2.toURI().toURL().toString());

            VisibilityPolicy vp = Modules.getVisibilityPolicy();

            if (vp instanceof DefaultVisibilityPolicy)
                pass();
            else
                fail();

            DefaultVisibilityPolicy dvp = (DefaultVisibilityPolicy) vp;
            dvp.refresh();

            check(vp.isVisible(moduleDef1) == false);
            check(vp.isVisible(moduleDef2) == false);
            check(vp.isVisible(moduleDef3) == false);
            check(vp.isVisible(moduleDef4) == false);
            check(vp.isVisible(moduleDef5) == false);
            check(vp.isVisible(moduleDef6) == false);
            check(vp.isVisible(moduleDef7) == false);
        }
        catch (Throwable ex) {
            unexpected(ex);
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
