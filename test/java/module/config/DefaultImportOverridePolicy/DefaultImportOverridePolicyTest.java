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
import java.module.ImportOverridePolicy;
import java.module.Modules;
import java.module.ModuleDefinition;
import java.module.ModuleContent;
import java.module.ModuleSystem;
import java.module.PackageDefinition;
import java.module.Repository;
import java.module.Version;
import java.module.VersionConstraint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import sun.module.config.DefaultImportOverridePolicy;
import sun.module.config.ModuleSystemConfig;

/*
 * @test DefaultImportOverridePolicyTest.java
 * @summary Test DefaultImportOverridePolicyTest
 * @compile -XDignore.symbol.file DefaultImportOverridePolicyTest.java
 * @author Stanley M. Ho
 */

public class DefaultImportOverridePolicyTest {

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
        testGetDefaultImportOverridePolicy01();
        testSystemPropertyOverride01();
        testBadNarrowing01();
    }

    static ImportDependency newImportDependency(String name) {
        return new ImportDependency("module", name, VersionConstraint.DEFAULT, false, false, null);
    }

    static public void testConstructor01() throws Exception {
        try {
            MockModuleDefinition moduleDef = new MockModuleDefinition("a.b.c", Version.valueOf(1, 0, 0));
            Map<ImportDependency, VersionConstraint> originalConstraints = new HashMap<ImportDependency, VersionConstraint>();
            ImportDependency importDep1 = newImportDependency("e.f.g");
            ImportDependency importDep2 = newImportDependency("p.q.r");
            ImportDependency importDep3 = newImportDependency("x.y.z");
            originalConstraints.put(importDep1, VersionConstraint.valueOf("1.0+"));
            originalConstraints.put(importDep2, VersionConstraint.valueOf("1.0+"));
            originalConstraints.put(importDep3, VersionConstraint.valueOf("1.0+"));

            File f = new File(System.getProperty("test.src", "."), "WellFormed.import.override.policy");
            ModuleSystemConfig.setProperty(ModuleSystemConfig.IMPORT_OVERRIDE_POLICY_URL_PREFIX+2, f.toURI().toURL().toString());

            ImportOverridePolicy iop = new DefaultImportOverridePolicy();
            Map<ImportDependency, VersionConstraint> overriddenConstraints = iop.narrow(moduleDef, Collections.unmodifiableMap(originalConstraints));

            check(overriddenConstraints.size() == originalConstraints.size());
            check(overriddenConstraints.get(importDep1).equals(VersionConstraint.valueOf("1.0+")));
            check(overriddenConstraints.get(importDep2).equals(VersionConstraint.valueOf("1.7.0")));
            check(overriddenConstraints.get(importDep3).equals(VersionConstraint.valueOf("2.3.4")));

            moduleDef = new MockModuleDefinition("a.b.z", Version.valueOf(1, 7, 0));
            overriddenConstraints = iop.narrow(moduleDef, Collections.unmodifiableMap(originalConstraints));

            check(overriddenConstraints.size() == originalConstraints.size());
            check(overriddenConstraints.get(importDep1).equals(VersionConstraint.valueOf("1.0+")));
            check(overriddenConstraints.get(importDep2).equals(VersionConstraint.valueOf("1.8.0")));
            check(overriddenConstraints.get(importDep3).equals(VersionConstraint.valueOf("2.*")));

            moduleDef = new MockModuleDefinition("a.b.c", Version.valueOf(2, 0, 0));
            overriddenConstraints = iop.narrow(moduleDef, Collections.unmodifiableMap(originalConstraints));

            check(overriddenConstraints.size() == originalConstraints.size());
            check(overriddenConstraints.get(importDep1).equals(VersionConstraint.valueOf("1.0+")));
            check(overriddenConstraints.get(importDep2).equals(VersionConstraint.valueOf("[1.0, 2.0)")));
            check(overriddenConstraints.get(importDep3).equals(VersionConstraint.valueOf("[2.0, 3.0)")));
        }
        catch (Throwable ex) {
            unexpected(ex);
        }
    }

    static public void testGetDefaultImportOverridePolicy01() throws Exception {
        try {
            MockModuleDefinition moduleDef = new MockModuleDefinition("a.b.c", Version.valueOf(1, 0, 0));
            Map<ImportDependency, VersionConstraint> originalConstraints = new HashMap<ImportDependency, VersionConstraint>();
            ImportDependency importDep1 = newImportDependency("e.f.g");
            ImportDependency importDep2 = newImportDependency("p.q.r");
            ImportDependency importDep3 = newImportDependency("x.y.z");
            originalConstraints.put(importDep1, VersionConstraint.valueOf("1.0+"));
            originalConstraints.put(importDep2, VersionConstraint.valueOf("1.0+"));
            originalConstraints.put(importDep3, VersionConstraint.valueOf("1.0+"));

            ModuleSystemConfig.setProperty(ModuleSystemConfig.IMPORT_OVERRIDE_POLICY_URL_PREFIX+2, "");
            ImportOverridePolicy iop = Modules.getImportOverridePolicy();

            if (iop instanceof DefaultImportOverridePolicy)
                pass();
            else
                fail();

            Map<ImportDependency, VersionConstraint> readOnlyOriginalConstraints = Collections.unmodifiableMap(originalConstraints);
            Map<ImportDependency, VersionConstraint> overriddenConstraints = iop.narrow(moduleDef, readOnlyOriginalConstraints);

            check(overriddenConstraints == readOnlyOriginalConstraints);
        }
        catch (Throwable ex) {
            unexpected(ex);
        }

        try {
            MockModuleDefinition moduleDef = new MockModuleDefinition("a.b.c", Version.valueOf(1, 0, 0));
            Map<ImportDependency, VersionConstraint> originalConstraints = new HashMap<ImportDependency, VersionConstraint>();
            ImportDependency importDep1 = newImportDependency("e.f.g");
            ImportDependency importDep2 = newImportDependency("p.q.r");
            ImportDependency importDep3 = newImportDependency("x.y.z");
            originalConstraints.put(importDep1, VersionConstraint.valueOf("1.0+"));
            originalConstraints.put(importDep2, VersionConstraint.valueOf("1.0+"));
            originalConstraints.put(importDep3, VersionConstraint.valueOf("1.0+"));

            File f = new File(System.getProperty("test.src", "."), "WellFormed.import.override.policy");
            ModuleSystemConfig.setProperty(ModuleSystemConfig.IMPORT_OVERRIDE_POLICY_URL_PREFIX+2, f.toURI().toURL().toString());

            ImportOverridePolicy iop = Modules.getImportOverridePolicy();

            if (iop instanceof DefaultImportOverridePolicy)
                pass();
            else
                fail();

            DefaultImportOverridePolicy diop = (DefaultImportOverridePolicy) iop;
            diop.refresh();

            Map<ImportDependency, VersionConstraint> overriddenConstraints = iop.narrow(moduleDef, Collections.unmodifiableMap(originalConstraints));

            check(overriddenConstraints.size() == originalConstraints.size());
            check(overriddenConstraints.get(importDep1).equals(VersionConstraint.valueOf("1.0+")));
            check(overriddenConstraints.get(importDep2).equals(VersionConstraint.valueOf("1.7.0")));
            check(overriddenConstraints.get(importDep3).equals(VersionConstraint.valueOf("2.3.4")));

            moduleDef = new MockModuleDefinition("a.b.z", Version.valueOf(1, 7, 0));
            overriddenConstraints = iop.narrow(moduleDef, Collections.unmodifiableMap(originalConstraints));

            check(overriddenConstraints.size() == originalConstraints.size());
            check(overriddenConstraints.get(importDep1).equals(VersionConstraint.valueOf("1.0+")));
            check(overriddenConstraints.get(importDep2).equals(VersionConstraint.valueOf("1.8.0")));
            check(overriddenConstraints.get(importDep3).equals(VersionConstraint.valueOf("2.*")));

            moduleDef = new MockModuleDefinition("a.b.c", Version.valueOf(2, 0, 0));
            overriddenConstraints = iop.narrow(moduleDef, Collections.unmodifiableMap(originalConstraints));

            check(overriddenConstraints.size() == originalConstraints.size());
            check(overriddenConstraints.get(importDep1).equals(VersionConstraint.valueOf("1.0+")));
            check(overriddenConstraints.get(importDep2).equals(VersionConstraint.valueOf("[1.0, 2.0)")));
            check(overriddenConstraints.get(importDep3).equals(VersionConstraint.valueOf("[2.0, 3.0)")));
        }
        catch (Throwable ex) {
            unexpected(ex);
        }
    }

    static public void testSystemPropertyOverride01() throws Exception {
        try {
            MockModuleDefinition moduleDef = new MockModuleDefinition("a.b.c", Version.valueOf(1, 0, 0));
            Map<ImportDependency, VersionConstraint> originalConstraints = new HashMap<ImportDependency, VersionConstraint>();
            ImportDependency importDep1 = newImportDependency("e.f.g");
            ImportDependency importDep2 = newImportDependency("p.q.r");
            ImportDependency importDep3 = newImportDependency("x.y.z");
            originalConstraints.put(importDep1, VersionConstraint.valueOf("1.0+"));
            originalConstraints.put(importDep2, VersionConstraint.valueOf("1.0+"));
            originalConstraints.put(importDep3, VersionConstraint.valueOf("1.0+"));

            // Pretend like -Djava.module.visibility.policy.file=<url to WellFormed.visibility.policy>
            //
            File f = new File(System.getProperty("test.src", "."), "WellFormed.import.override.policy");
            System.setProperty("java.module.import.override.policy.file", f.toURI().toURL().toString());
            ModuleSystemConfig.setProperty(ModuleSystemConfig.IMPORT_OVERRIDE_POLICY_URL_PREFIX+2, "");

            ImportOverridePolicy iop = Modules.getImportOverridePolicy();

            if (iop instanceof DefaultImportOverridePolicy)
                pass();
            else
                fail();

            DefaultImportOverridePolicy diop = (DefaultImportOverridePolicy) iop;
            diop.refresh();

            Map<ImportDependency, VersionConstraint> overriddenConstraints = iop.narrow(moduleDef, Collections.unmodifiableMap(originalConstraints));

            check(overriddenConstraints.size() == originalConstraints.size());
            check(overriddenConstraints.get(importDep1).equals(VersionConstraint.valueOf("1.0+")));
            check(overriddenConstraints.get(importDep2).equals(VersionConstraint.valueOf("1.7.0")));
            check(overriddenConstraints.get(importDep3).equals(VersionConstraint.valueOf("2.3.4")));

            moduleDef = new MockModuleDefinition("a.b.z", Version.valueOf(1, 7, 0));
            overriddenConstraints = iop.narrow(moduleDef, Collections.unmodifiableMap(originalConstraints));

            check(overriddenConstraints.size() == originalConstraints.size());
            check(overriddenConstraints.get(importDep1).equals(VersionConstraint.valueOf("1.0+")));
            check(overriddenConstraints.get(importDep2).equals(VersionConstraint.valueOf("1.8.0")));
            check(overriddenConstraints.get(importDep3).equals(VersionConstraint.valueOf("2.*")));

            moduleDef = new MockModuleDefinition("a.b.c", Version.valueOf(2, 0, 0));
            overriddenConstraints = iop.narrow(moduleDef, Collections.unmodifiableMap(originalConstraints));

            check(overriddenConstraints.size() == originalConstraints.size());
            check(overriddenConstraints.get(importDep1).equals(VersionConstraint.valueOf("1.0+")));
            check(overriddenConstraints.get(importDep2).equals(VersionConstraint.valueOf("[1.0, 2.0)")));
            check(overriddenConstraints.get(importDep3).equals(VersionConstraint.valueOf("[2.0, 3.0)")));
        }
        catch (Throwable ex) {
            unexpected(ex);
        }

        try {
            MockModuleDefinition moduleDef = new MockModuleDefinition("a.b.c", Version.valueOf(1, 0, 0));
            Map<ImportDependency, VersionConstraint> originalConstraints = new HashMap<ImportDependency, VersionConstraint>();
            ImportDependency importDep1 = newImportDependency("e.f.g");
            ImportDependency importDep2 = newImportDependency("p.q.r");
            ImportDependency importDep3 = newImportDependency("x.y.z");
            originalConstraints.put(importDep1, VersionConstraint.valueOf("1.0+"));
            originalConstraints.put(importDep2, VersionConstraint.valueOf("1.0+"));
            originalConstraints.put(importDep3, VersionConstraint.valueOf("1.0+"));

            // Pretend "import.override.policy.url.2" is set in the module system properties file
            //
            File f = new File(System.getProperty("test.src", "."), "WellFormed.import.override.policy");
            ModuleSystemConfig.setProperty(ModuleSystemConfig.IMPORT_OVERRIDE_POLICY_URL_PREFIX+2, f.toURI().toURL().toString());

            // Pretend like -Djava.module.import.override.policy.file==<url to Another.import.override.policy>
            //
            File f2 = new File(System.getProperty("test.src", "."), "Another.import.override.policy");
            System.setProperty("java.module.import.override.policy.file", "=" + f2.toURI().toURL().toString());

            ImportOverridePolicy iop = Modules.getImportOverridePolicy();

            if (iop instanceof DefaultImportOverridePolicy)
                pass();
            else
                fail();

            DefaultImportOverridePolicy diop = (DefaultImportOverridePolicy) iop;
            diop.refresh();

            Map<ImportDependency, VersionConstraint> overriddenConstraints = iop.narrow(moduleDef, Collections.unmodifiableMap(originalConstraints));

            check(overriddenConstraints.size() == originalConstraints.size());
            check(overriddenConstraints.get(importDep1).equals(VersionConstraint.valueOf("1.0+")));
            check(overriddenConstraints.get(importDep2).equals(VersionConstraint.valueOf("[1.7.0, 1.7.1)")));
            check(overriddenConstraints.get(importDep3).equals(VersionConstraint.valueOf("[2.3.4, 2.3.5)")));

            moduleDef = new MockModuleDefinition("a.b.z", Version.valueOf(1, 7, 0));
            overriddenConstraints = iop.narrow(moduleDef, Collections.unmodifiableMap(originalConstraints));

            check(overriddenConstraints.size() == originalConstraints.size());
            check(overriddenConstraints.get(importDep1).equals(VersionConstraint.valueOf("1.0+")));
            check(overriddenConstraints.get(importDep2).equals(VersionConstraint.valueOf("1.0+")));
            check(overriddenConstraints.get(importDep3).equals(VersionConstraint.valueOf("1.0+")));

            moduleDef = new MockModuleDefinition("a.b.c", Version.valueOf(2, 0, 0));
            overriddenConstraints = iop.narrow(moduleDef, Collections.unmodifiableMap(originalConstraints));

            check(overriddenConstraints.size() == originalConstraints.size());
            check(overriddenConstraints.get(importDep1).equals(VersionConstraint.valueOf("1.0+")));
            check(overriddenConstraints.get(importDep2).equals(VersionConstraint.valueOf("1.0+")));
            check(overriddenConstraints.get(importDep3).equals(VersionConstraint.valueOf("1.0+")));
        }
        catch (Throwable ex) {
            unexpected(ex);
        }
    }

    static public void testBadNarrowing01() throws Exception {
        try {
            // Pretend like -Djava.module.visibility.policy.file=<url to BadNarrowing.visibility.policy>
            //
            File f = new File(System.getProperty("test.src", "."), "BadNarrowing.import.override.policy");
            System.setProperty("java.module.import.override.policy.file", f.toURI().toURL().toString());
            ModuleSystemConfig.setProperty(ModuleSystemConfig.IMPORT_OVERRIDE_POLICY_URL_PREFIX+2, "");

            ImportOverridePolicy iop = Modules.getImportOverridePolicy();

            if (iop instanceof DefaultImportOverridePolicy)
                pass();
            else
                fail();

            DefaultImportOverridePolicy diop = (DefaultImportOverridePolicy) iop;
            diop.refresh();

            MockModuleDefinition moduleDef1 = new MockModuleDefinition("a.b.c", Version.valueOf(1, 0, 0));

            Map<ImportDependency, VersionConstraint> constraints = new HashMap<ImportDependency, VersionConstraint>();
            ImportDependency importDep1 = newImportDependency("e.f.g");
            ImportDependency importDep2 = newImportDependency("p.q.r");
            ImportDependency importDep3 = newImportDependency("x.y.z");
            constraints.put(importDep1, VersionConstraint.valueOf("1+"));
            constraints.put(importDep2, VersionConstraint.valueOf("1+"));
            constraints.put(importDep3, VersionConstraint.valueOf("1+"));

            Map<ImportDependency, VersionConstraint> overriddenConstraints = iop.narrow(moduleDef1, Collections.unmodifiableMap(constraints));
            fail();
        }
        catch (Throwable ex) {
            pass();
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
