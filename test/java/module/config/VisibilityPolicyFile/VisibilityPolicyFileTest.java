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
import java.module.ModuleDefinition;
import java.module.ModuleDefinitionContent;
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
import sun.module.config.VisibilityPolicyFile;

/*
 * @test VisibilityPolicyFileTest.java
 * @summary Test VisibilityPolicyFileTest
 * @compile -XDignore.symbol.file VisibilityPolicyFileTest.java
 * @author Stanley M. Ho
 */

public class VisibilityPolicyFileTest {

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
        public ModuleDefinitionContent getModuleDefinitionContent() {
            return null;
        }

        @Override
        public boolean isModuleReleasable() {
            return true;
        }
    }

    public static void realMain(String[] args) throws Throwable {
        testGoodVisibilityPolicyFile();
        testBadVisibilityPolicyFile();
    }

    static public void testGoodVisibilityPolicyFile() throws Exception {
        try {
            File f = new File(System.getProperty("test.src", "."), "WellFormed.visibility.policy");
            VisibilityPolicy vp = VisibilityPolicyFile.parse(f.toURI().toURL());
            pass();
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

            File f = new File(System.getProperty("test.src", "."), "NoModuleWildcard.visibility.policy");
            VisibilityPolicy vp = VisibilityPolicyFile.parse(f.toURI().toURL());

            check(vp.isVisible(moduleDef1) == true);
            check(vp.isVisible(moduleDef2) == true);
            check(vp.isVisible(moduleDef3) == false);
            check(vp.isVisible(moduleDef4) == true);
            check(vp.isVisible(moduleDef5) == true);
            check(vp.isVisible(moduleDef6) == false);
            check(vp.isVisible(moduleDef7) == true);
        }
        catch (Throwable ex) {
            unexpected(ex);
        }

        try {
            MockModuleDefinition moduleDef1 = new MockModuleDefinition("a.b.c", Version.valueOf(1, 1, 1));
            MockModuleDefinition moduleDef2 = new MockModuleDefinition("a.b.c", Version.valueOf(3, 0, 0));
            MockModuleDefinition moduleDef3 = new MockModuleDefinition("a.bc", Version.valueOf(1, 1, 1));
            MockModuleDefinition moduleDef4 = new MockModuleDefinition("a.bc", Version.valueOf(3, 0, 0));
            MockModuleDefinition moduleDef5 = new MockModuleDefinition("a.b.*", Version.valueOf(1, 1, 1));
            MockModuleDefinition moduleDef6 = new MockModuleDefinition("a.b.*", Version.valueOf(3, 0, 0));
            MockModuleDefinition moduleDef7 = new MockModuleDefinition("d.e.f", Version.valueOf(1, 1, 1));
            MockModuleDefinition moduleDef8 = new MockModuleDefinition("d.e.f", Version.valueOf(0, 0, 0, 7));
            MockModuleDefinition moduleDef9 = new MockModuleDefinition("d.ef", Version.valueOf(1, 1, 1));
            MockModuleDefinition moduleDef10 = new MockModuleDefinition("d.ef", Version.valueOf(0, 0, 0, 7));
            MockModuleDefinition moduleDef11 = new MockModuleDefinition("d.e.*", Version.valueOf(1, 1, 1));
            MockModuleDefinition moduleDef12 = new MockModuleDefinition("d.e.*", Version.valueOf(0, 0, 0, 7));
            MockModuleDefinition moduleDef13 = new MockModuleDefinition("g.h.i", Version.valueOf(1, 2, 3));
            MockModuleDefinition moduleDef14 = new MockModuleDefinition("j.k.l", Version.valueOf(2, 3, 4));


            File f = new File(System.getProperty("test.src", "."), "ModuleWildcard0.visibility.policy");
            VisibilityPolicy vp = VisibilityPolicyFile.parse(f.toURI().toURL());

            check(vp.isVisible(moduleDef1) == true);
            check(vp.isVisible(moduleDef2) == true);
            check(vp.isVisible(moduleDef3) == true);
            check(vp.isVisible(moduleDef4) == true);
            check(vp.isVisible(moduleDef5) == true);
            check(vp.isVisible(moduleDef6) == true);
            check(vp.isVisible(moduleDef7) == false);
            check(vp.isVisible(moduleDef8) == true);
            check(vp.isVisible(moduleDef9) == true);
            check(vp.isVisible(moduleDef10) == true);
            check(vp.isVisible(moduleDef11) == false);
            check(vp.isVisible(moduleDef12) == true);
            check(vp.isVisible(moduleDef13) == true);
            check(vp.isVisible(moduleDef14) == true);
        }
        catch (Throwable ex) {
            unexpected(ex);
        }

        try {
            MockModuleDefinition moduleDef1 = new MockModuleDefinition("a.b.c", Version.valueOf(1, 1, 1));
            MockModuleDefinition moduleDef2 = new MockModuleDefinition("a.b.c", Version.valueOf(3, 0, 0));
            MockModuleDefinition moduleDef3 = new MockModuleDefinition("a.bc", Version.valueOf(1, 1, 1));
            MockModuleDefinition moduleDef4 = new MockModuleDefinition("a.bc", Version.valueOf(3, 0, 0));
            MockModuleDefinition moduleDef5 = new MockModuleDefinition("a.b.*", Version.valueOf(1, 1, 1));
            MockModuleDefinition moduleDef6 = new MockModuleDefinition("a.b.*", Version.valueOf(3, 0, 0));
            MockModuleDefinition moduleDef7 = new MockModuleDefinition("d.e.f", Version.valueOf(1, 1, 1));
            MockModuleDefinition moduleDef8 = new MockModuleDefinition("d.e.f", Version.valueOf(0, 0, 0, 7));
            MockModuleDefinition moduleDef9 = new MockModuleDefinition("d.ef", Version.valueOf(1, 1, 1));
            MockModuleDefinition moduleDef10 = new MockModuleDefinition("d.ef", Version.valueOf(0, 0, 0, 7));
            MockModuleDefinition moduleDef11 = new MockModuleDefinition("d.e.*", Version.valueOf(1, 1, 1));
            MockModuleDefinition moduleDef12 = new MockModuleDefinition("d.e.*", Version.valueOf(0, 0, 0, 7));
            MockModuleDefinition moduleDef13 = new MockModuleDefinition("g.h.i", Version.valueOf(1, 2, 3));
            MockModuleDefinition moduleDef14 = new MockModuleDefinition("j.k.l", Version.valueOf(2, 3, 4));


            File f = new File(System.getProperty("test.src", "."), "ModuleWildcard1.visibility.policy");
            VisibilityPolicy vp = VisibilityPolicyFile.parse(f.toURI().toURL());

            check(vp.isVisible(moduleDef1) == true);
            check(vp.isVisible(moduleDef2) == false);
            check(vp.isVisible(moduleDef3) == false);
            check(vp.isVisible(moduleDef4) == false);
            check(vp.isVisible(moduleDef5) == true);
            check(vp.isVisible(moduleDef6) == false);
            check(vp.isVisible(moduleDef7) == false);
            check(vp.isVisible(moduleDef8) == false);
            check(vp.isVisible(moduleDef9) == false);
            check(vp.isVisible(moduleDef10) == false);
            check(vp.isVisible(moduleDef11) == false);
            check(vp.isVisible(moduleDef12) == false);
            check(vp.isVisible(moduleDef13) == false);
            check(vp.isVisible(moduleDef14) == false);
        }
        catch (Throwable ex) {
            unexpected(ex);
        }
    }

    static public void testBadVisibilityPolicyFile() throws Exception {
        try {
            File f = new File(System.getProperty("test.src", "."), "BadComment0.visibility.policy");
            VisibilityPolicy vp = VisibilityPolicyFile.parse(f.toURI().toURL());
            pass();
        }
        catch (Throwable ex) {
            fail();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "BadComment1.visibility.policy");
            VisibilityPolicy vp = VisibilityPolicyFile.parse(f.toURI().toURL());
            fail();
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
        }
        catch (Throwable ex) {
            pass();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "BadComment2.visibility.policy");
            VisibilityPolicy vp = VisibilityPolicyFile.parse(f.toURI().toURL());
            fail();
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
        }
        catch (Throwable ex) {
            pass();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "BadComment3.visibility.policy");
            VisibilityPolicy vp = VisibilityPolicyFile.parse(f.toURI().toURL());
            fail();
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
        }
        catch (Throwable ex) {
            pass();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "VisibilityCharOnly.visibility.policy");
            VisibilityPolicy vp = VisibilityPolicyFile.parse(f.toURI().toURL());
            fail();
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
        }
        catch (Throwable ex) {
            pass();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "InvalidVisibilityChar.visibility.policy");
            VisibilityPolicy vp = VisibilityPolicyFile.parse(f.toURI().toURL());
            fail();
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
        }
        catch (Throwable ex) {
            pass();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "InvalidVersionConstraint.visibility.policy");
            VisibilityPolicy vp = VisibilityPolicyFile.parse(f.toURI().toURL());
            fail();
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
        }
        catch (Throwable ex) {
            pass();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "ExtraComma0.visibility.policy");
            VisibilityPolicy vp = VisibilityPolicyFile.parse(f.toURI().toURL());
            fail();
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
        }
        catch (Throwable ex) {
            pass();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "ExtraComma1.visibility.policy");
            VisibilityPolicy vp = VisibilityPolicyFile.parse(f.toURI().toURL());
            fail();
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
        }
        catch (Throwable ex) {
            pass();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "InvalidModuleWildcard0.visibility.policy");
            VisibilityPolicy vp = VisibilityPolicyFile.parse(f.toURI().toURL());
            fail();
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
        }
        catch (Throwable ex) {
            pass();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "InvalidModuleWildcard1.visibility.policy");
            VisibilityPolicy vp = VisibilityPolicyFile.parse(f.toURI().toURL());
            fail();
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
        }
        catch (Throwable ex) {
            pass();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "InvalidModuleWildcard2.visibility.policy");
            VisibilityPolicy vp = VisibilityPolicyFile.parse(f.toURI().toURL());
            fail();
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
        }
        catch (Throwable ex) {
            pass();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "InvalidModuleWildcard3.visibility.policy");
            VisibilityPolicy vp = VisibilityPolicyFile.parse(f.toURI().toURL());
            fail();
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
        }
        catch (Throwable ex) {
            pass();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "InvalidModuleWildcard4.visibility.policy");
            VisibilityPolicy vp = VisibilityPolicyFile.parse(f.toURI().toURL());
            fail();
        }
        catch (FileNotFoundException fe) {
            unexpected(fe);
        }
        catch (Throwable ex) {
            pass();
        }

        try {
            File f = new File(System.getProperty("test.src", "."), "InvalidModuleWildcard5.visibility.policy");
            VisibilityPolicy vp = VisibilityPolicyFile.parse(f.toURI().toURL());
            fail();
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
