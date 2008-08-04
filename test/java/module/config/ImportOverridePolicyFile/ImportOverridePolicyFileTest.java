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
import java.module.ModuleArchiveInfo;
import java.module.ModuleDefinition;
import java.module.ModuleDependency;
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
import sun.module.config.ImportOverridePolicyFile;

/*
 * @test ImportOverridePolicyFileTest.java
 * @summary Test ImportOverridePolicyFileTest
 * @compile -XDignore.symbol.file ImportOverridePolicyFileTest.java
 * @author Stanley M. Ho
 */

public class ImportOverridePolicyFileTest {

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
        public String getMainClass() {
            return null;
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
        public ModuleArchiveInfo getModuleArchiveInfo() {
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
        testGoodImportOverridePolicyFile();
        testBadImportOverridePolicyFile();
    }

    static ImportDependency newModuleDependency(String name) {
        return Modules.newModuleDependency(name, VersionConstraint.DEFAULT, false, false, null);
    }

    static public void testGoodImportOverridePolicyFile() throws Exception {
        try {
            File f = new File(System.getProperty("test.src", "."), "WellFormed.import.override.policy");
            ImportOverridePolicy iop = ImportOverridePolicyFile.parse(f.toURI().toURL());
            pass();

            MockModuleDefinition moduleDef1 = new MockModuleDefinition("a.b.c", Version.valueOf(1, 0, 0));
            MockModuleDefinition moduleDef2 = new MockModuleDefinition("a.b.d", Version.valueOf(1, 1, 0));
            MockModuleDefinition moduleDef3 = new MockModuleDefinition("a.b.d", Version.valueOf(2, 0, 0));
            MockModuleDefinition moduleDef4 = new MockModuleDefinition("d.e.f", Version.valueOf(0, 0, 7));
            MockModuleDefinition moduleDef5 = new MockModuleDefinition("*",     Version.valueOf(1, 2, 3));
            MockModuleDefinition moduleDef6 = new MockModuleDefinition("x.y.*", Version.valueOf(3, 4, 5));

            Map<ImportDependency, VersionConstraint> constraints = new HashMap<ImportDependency, VersionConstraint>();
            ImportDependency importDep1 = newModuleDependency("p.q.r");
            ImportDependency importDep2 = newModuleDependency("x.y.z");
            ImportDependency importDep3 = newModuleDependency("i.j.k");
            constraints.put(importDep1, VersionConstraint.valueOf("1+"));
            constraints.put(importDep2, VersionConstraint.valueOf("1+"));
            constraints.put(importDep3, VersionConstraint.valueOf("1+"));

            Map<ImportDependency, VersionConstraint> overriddenConstraints = iop.narrow(moduleDef1, Collections.unmodifiableMap(constraints));
            check(overriddenConstraints.size() == 3);
            check(overriddenConstraints.get(importDep1).equals(VersionConstraint.valueOf("1.7.0")));
            check(overriddenConstraints.get(importDep2).equals(VersionConstraint.valueOf("2.3.4")));
            check(overriddenConstraints.get(importDep3).equals(VersionConstraint.valueOf("1+")));

            overriddenConstraints = iop.narrow(moduleDef2, Collections.unmodifiableMap(constraints));
            check(overriddenConstraints.size() == 3);
            check(overriddenConstraints.get(importDep1).equals(VersionConstraint.valueOf("1.8.0")));
            check(overriddenConstraints.get(importDep2).equals(VersionConstraint.valueOf("2.*")));
            check(overriddenConstraints.get(importDep3).equals(VersionConstraint.valueOf("1+")));

            overriddenConstraints = iop.narrow(moduleDef3, Collections.unmodifiableMap(constraints));
            check(overriddenConstraints.size() == 3);
            check(overriddenConstraints.get(importDep1).equals(VersionConstraint.valueOf("[1, 2)")));
            check(overriddenConstraints.get(importDep2).equals(VersionConstraint.valueOf("[2, 3)")));
            check(overriddenConstraints.get(importDep3).equals(VersionConstraint.valueOf("1+")));

            overriddenConstraints = iop.narrow(moduleDef4, Collections.unmodifiableMap(constraints));
            check(overriddenConstraints.size() == 3);
            check(overriddenConstraints.get(importDep1).equals(VersionConstraint.valueOf("[1, 2)")));
            check(overriddenConstraints.get(importDep2).equals(VersionConstraint.valueOf("[2, 3)")));
            check(overriddenConstraints.get(importDep3).equals(VersionConstraint.valueOf("1+")));

            overriddenConstraints = iop.narrow(moduleDef5, Collections.unmodifiableMap(constraints));
            check(overriddenConstraints.size() == 3);
            check(overriddenConstraints.get(importDep1).equals(VersionConstraint.valueOf("[1, 2)")));
            check(overriddenConstraints.get(importDep2).equals(VersionConstraint.valueOf("[2, 3)")));
            check(overriddenConstraints.get(importDep3).equals(VersionConstraint.valueOf("1+")));

            overriddenConstraints = iop.narrow(moduleDef6, Collections.unmodifiableMap(constraints));
            check(overriddenConstraints.size() == 3);
            check(overriddenConstraints.get(importDep1).equals(VersionConstraint.valueOf("[1, 2)")));
            check(overriddenConstraints.get(importDep2).equals(VersionConstraint.valueOf("[2, 3)")));
            check(overriddenConstraints.get(importDep3).equals(VersionConstraint.valueOf("1+")));
        }
        catch (Throwable ex) {
            unexpected(ex);
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "NoOp.import.override.policy");
            ImportOverridePolicy iop = ImportOverridePolicyFile.parse(f.toURI().toURL());
            pass();

            MockModuleDefinition moduleDef1 = new MockModuleDefinition("a.b.c", Version.valueOf(1, 0, 0));
            MockModuleDefinition moduleDef2 = new MockModuleDefinition("d.e.f", Version.valueOf(0, 0, 7));
            MockModuleDefinition moduleDef3 = new MockModuleDefinition("*",     Version.valueOf(1, 2, 3));
            MockModuleDefinition moduleDef4 = new MockModuleDefinition("x.y.*", Version.valueOf(3, 4, 5));

            Map<ImportDependency, VersionConstraint> constraints = new HashMap<ImportDependency, VersionConstraint>();
            ImportDependency importDep1 = newModuleDependency("p.q.r");
            ImportDependency importDep2 = newModuleDependency("x.y.z");
            ImportDependency importDep3 = newModuleDependency("i.j.k");
            constraints.put(importDep1, VersionConstraint.valueOf("1+"));
            constraints.put(importDep2, VersionConstraint.valueOf("1+"));
            constraints.put(importDep3, VersionConstraint.valueOf("1+"));

            Map<ImportDependency, VersionConstraint> overriddenConstraints = iop.narrow(moduleDef1, Collections.unmodifiableMap(constraints));
            check(overriddenConstraints.size() == 3);
            check(overriddenConstraints.get(importDep1).equals(VersionConstraint.valueOf("1+")));
            check(overriddenConstraints.get(importDep2).equals(VersionConstraint.valueOf("1+")));
            check(overriddenConstraints.get(importDep3).equals(VersionConstraint.valueOf("1+")));

            overriddenConstraints = iop.narrow(moduleDef2, Collections.unmodifiableMap(constraints));
            check(overriddenConstraints.size() == 3);
            check(overriddenConstraints.get(importDep1).equals(VersionConstraint.valueOf("1+")));
            check(overriddenConstraints.get(importDep2).equals(VersionConstraint.valueOf("1+")));
            check(overriddenConstraints.get(importDep3).equals(VersionConstraint.valueOf("1+")));

            overriddenConstraints = iop.narrow(moduleDef3, Collections.unmodifiableMap(constraints));
            check(overriddenConstraints.size() == 3);
            check(overriddenConstraints.get(importDep1).equals(VersionConstraint.valueOf("1+")));
            check(overriddenConstraints.get(importDep2).equals(VersionConstraint.valueOf("1+")));
            check(overriddenConstraints.get(importDep3).equals(VersionConstraint.valueOf("1+")));

            overriddenConstraints = iop.narrow(moduleDef4, Collections.unmodifiableMap(constraints));
            check(overriddenConstraints.size() == 3);
            check(overriddenConstraints.get(importDep1).equals(VersionConstraint.valueOf("1+")));
            check(overriddenConstraints.get(importDep2).equals(VersionConstraint.valueOf("1+")));
            check(overriddenConstraints.get(importDep3).equals(VersionConstraint.valueOf("1+")));
        }
        catch (Throwable ex) {
            unexpected(ex);
        }
    }

    static public void testBadImportOverridePolicyFile() throws Exception {
        try {
            File f = new File(System.getProperty("test.src", "."), "BadComment0.import.override.policy");
            ImportOverridePolicy iop = ImportOverridePolicyFile.parse(f.toURI().toURL());
            fail();
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
        }
        catch (Throwable ex) {
            pass();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "BadComment1.import.override.policy");
            ImportOverridePolicy iop = ImportOverridePolicyFile.parse(f.toURI().toURL());
            fail();
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
        }
        catch (Throwable ex) {
            pass();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "BadComment2.import.override.policy");
            ImportOverridePolicy iop = ImportOverridePolicyFile.parse(f.toURI().toURL());
            fail();
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
        }
        catch (Throwable ex) {
            pass();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "InvalidImporterToken.import.override.policy");
            ImportOverridePolicy vp = ImportOverridePolicyFile.parse(f.toURI().toURL());
            fail();
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
        }
        catch (Throwable ex) {
            pass();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "InvalidModuleWildcard0.import.override.policy");
            ImportOverridePolicy vp = ImportOverridePolicyFile.parse(f.toURI().toURL());
            fail();
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
        }
        catch (Throwable ex) {
            pass();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "InvalidModuleWildcard1.import.override.policy");
            ImportOverridePolicy vp = ImportOverridePolicyFile.parse(f.toURI().toURL());
            fail();
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
        }
        catch (Throwable ex) {
            pass();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "InvalidModuleWildcard2.import.override.policy");
            ImportOverridePolicy vp = ImportOverridePolicyFile.parse(f.toURI().toURL());
            fail();
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
        }
        catch (Throwable ex) {
            pass();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "InvalidVersionConstraint0.import.override.policy");
            ImportOverridePolicy vp = ImportOverridePolicyFile.parse(f.toURI().toURL());
            fail();
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
        }
        catch (Throwable ex) {
            pass();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "InvalidVersionConstraint1.import.override.policy");
            ImportOverridePolicy vp = ImportOverridePolicyFile.parse(f.toURI().toURL());
            fail();
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
        }
        catch (Throwable ex) {
            pass();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "InvalidVersionConstraint2.import.override.policy");
            ImportOverridePolicy vp = ImportOverridePolicyFile.parse(f.toURI().toURL());
            fail();
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
        }
        catch (Throwable ex) {
            pass();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "InvalidVersionConstraint3.import.override.policy");
            ImportOverridePolicy vp = ImportOverridePolicyFile.parse(f.toURI().toURL());
            fail();
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
        }
        catch (Throwable ex) {
            pass();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "MissingSemicolon0.import.override.policy");
            ImportOverridePolicy vp = ImportOverridePolicyFile.parse(f.toURI().toURL());
            fail();
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
        }
        catch (Throwable ex) {
            pass();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "MissingSemicolon1.import.override.policy");
            ImportOverridePolicy vp = ImportOverridePolicyFile.parse(f.toURI().toURL());
            fail();
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
        }
        catch (Throwable ex) {
            pass();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "ExcessSemicolon0.import.override.policy");
            ImportOverridePolicy vp = ImportOverridePolicyFile.parse(f.toURI().toURL());
            fail();
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
        }
        catch (Throwable ex) {
            pass();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "ExcessSemicolon1.import.override.policy");
            ImportOverridePolicy vp = ImportOverridePolicyFile.parse(f.toURI().toURL());
            fail();
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
        }
        catch (Throwable ex) {
            pass();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "BadNarrowing.import.override.policy");
            ImportOverridePolicy iop = ImportOverridePolicyFile.parse(f.toURI().toURL());
            pass();

            MockModuleDefinition moduleDef1 = new MockModuleDefinition("a.b.c", Version.valueOf(1, 0, 0));

            Map<ImportDependency, VersionConstraint> constraints = new HashMap<ImportDependency, VersionConstraint>();
            ImportDependency importDep1 = newModuleDependency("p.q.r");
            ImportDependency importDep2 = newModuleDependency("x.y.z");
            ImportDependency importDep3 = newModuleDependency("i.j.k");
            constraints.put(importDep1, VersionConstraint.valueOf("1+"));
            constraints.put(importDep2, VersionConstraint.valueOf("1+"));
            constraints.put(importDep3, VersionConstraint.valueOf("1+"));

            Map<ImportDependency, VersionConstraint> overriddenConstraints = iop.narrow(moduleDef1, Collections.unmodifiableMap(constraints));
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
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
