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
import java.module.Version;
import java.module.VersionConstraint;
import java.module.Query;
import java.module.ImportDependency;
import java.module.ModuleDefinition;
import java.module.ModuleContent;
import java.module.ModuleSystem;
import java.module.PackageDefinition;
import java.module.Repository;
import java.module.annotation.ImportPolicyClass;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import sun.module.annotation.LegacyClasses;

/*
 * @test QueryTest.java
 * @summary Test QueryTest
 * @author Stanley M. Ho
 */

public class QueryTest {

    private static class MockModuleDefinition extends ModuleDefinition {
        private final String name;
        private final Version version;
        private final Map<String, String> attributes;
        private final Map<Class, Annotation> annotations;

        MockModuleDefinition(String name, Version version, Map<String, String> attributes) {
            this(name, version, attributes, new HashMap<Class, Annotation>());
        }
        MockModuleDefinition(String name, Version version, Map<String, String> attributes, Map<Class, Annotation> annotations) {
            this.name = name;
            this.version = version;
            this.attributes = attributes;
            this.annotations = annotations;
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
            for (Class c : annotations.keySet()) {
                if (annotationClass.isAssignableFrom(c))
                    return (T) annotations.get(c);
            }
            return null;
        }

        public List<Annotation> getAnnotations() {
            return Collections.unmodifiableList(new ArrayList<Annotation>(annotations.values()));
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
        public boolean isModuleReleasable() {
            return true;
        }

        @Override
        public ModuleContent getModuleContent() {
            return null;
        }
    }

    public static void realMain(String[] args) throws Throwable {
        testAllQuery();
        testNoneQuery();
        testModuleQuery();
        testAttributeQuery();
        testAnnotationQuery();
        testNotQuery();
        testAndQuery();
        testOrQuery();
    }

    static public void testAllQuery() throws Exception {
        // Test AllQuery
        try {
            ModuleDefinition moduleDef1 = new MockModuleDefinition("javax.swing", Version.valueOf(1, 0, 0), new HashMap<String, String>());
            ModuleDefinition moduleDef2 = new MockModuleDefinition("org.foo.xml", Version.valueOf(2, 0, 0), new HashMap<String, String>());
            Query query = Query.ANY;
            check(query.match(moduleDef1) == true);
            check(query.match(moduleDef2) == true);

            Query query2 = cloneQueryBySerialization(query);
            check(query.equals(query2) == true);
            check(query2.equals(query) == true);
            check(query2.equals(query2) == true);
            check(query.toString().equals(query2.toString()) == true);

            check(query.getIndexableModuleNames() == null);

            check(query.hashCode() == query2.hashCode());
        } catch (Throwable ex) {
            unexpected(ex);
        }
    }

    static public void testNoneQuery() throws Exception {
        // Test NoneQuery
        try {
            ModuleDefinition moduleDef1 = new MockModuleDefinition("javax.swing", Version.valueOf(1, 0, 0), new HashMap<String, String>());
            ModuleDefinition moduleDef2 = new MockModuleDefinition("org.foo.xml", Version.valueOf(2, 0, 0), new HashMap<String, String>());
            Query query = Query.not(Query.ANY);
            check(query.match(moduleDef1) == false);
            check(query.match(moduleDef2) == false);

            Set<String> indexableNames = query.getIndexableModuleNames();
            check(indexableNames != null && indexableNames.size() == 0);

            Query query2 = cloneQueryBySerialization(query);
            check(query.equals(query2) == true);
            check(query2.equals(query) == true);
            check(query2.equals(query2) == true);
            check(query.toString().equals(query2.toString()) == true);

            check(query.hashCode() == query2.hashCode());
        } catch (Throwable ex) {
            unexpected(ex);
        }
    }

    static public void testModuleQuery() throws Exception {
        // Test ModuleQuery
        try {
            Query query = Query.module(null);
            fail();
        } catch (NullPointerException npe) {
            pass();
        } catch (Throwable ex) {
            unexpected(ex);
        }

        // Test ModuleQuery
        try {
            Query query = Query.module("javax.swing", null);
            fail();
        } catch (NullPointerException npe) {
            pass();
        } catch (Throwable ex) {
            unexpected(ex);
        }

        try {
            ModuleDefinition moduleDef = new MockModuleDefinition("javax.swing", Version.valueOf(1, 0, 0), new HashMap<String, String>());
            Query query = Query.module("javax.swing");
            Query query2 = Query.module("javax.swing", VersionConstraint.valueOf("2.0+"));
            check(query.match(moduleDef) == true);
            check(query2.match(moduleDef) == false);

            moduleDef = new MockModuleDefinition("java.swing", Version.valueOf(1, 0, 0), new HashMap<String, String>());
            check(query.match(moduleDef) == false);
            check(query2.match(moduleDef) == false);

            moduleDef = new MockModuleDefinition(" java.swing", Version.valueOf(1, 0, 0), new HashMap<String, String>());
            check(query.match(moduleDef) == false);
            check(query2.match(moduleDef) == false);

            moduleDef = new MockModuleDefinition("java.swing ", Version.valueOf(1, 0, 0), new HashMap<String, String>());
            check(query.match(moduleDef) == false);
            check(query2.match(moduleDef) == false);

            moduleDef = new MockModuleDefinition("JAVAX.SWING", Version.valueOf(1, 0, 0), new HashMap<String, String>());
            check(query.match(moduleDef) == false);
            check(query2.match(moduleDef) == false);

            moduleDef = new MockModuleDefinition("org.foo.xml", Version.valueOf(1, 0, 0), new HashMap<String, String>());
            check(query.match(moduleDef) == false);
            check(query2.match(moduleDef) == false);

            check(query.equals(query) == true);
            check(query.equals(query2) == false);
            check(query.equals(Query.module("javax.swing")) == true);
            check(query.equals(Query.module("org.foo.xml")) == false);
            check(query.equals(Query.module("x.y.z", VersionConstraint.valueOf("1.1.0"))) == false);
            check(query.equals(Query.attribute("my.name")) == false);

            Set<String> indexableNames = Query.module("javax.swing").getIndexableModuleNames();
            check(indexableNames != null && indexableNames.size() == 1 && indexableNames.contains("javax.swing"));

            Query query3 = cloneQueryBySerialization(query);
            check(query.equals(query3) == true);
            check(query3.equals(query) == true);
            check(query3.equals(query3) == true);
            check(query.toString().equals(query3.toString()) == true);

            check(query.hashCode() == query3.hashCode());
            check(query.hashCode() != Query.module("org.foo.xml").hashCode());
        } catch (Throwable ex) {
            unexpected(ex);
        }
    }

    static public void testAttributeQuery() throws Exception {
        // Test AttributeQuery
        try {
            Query query = Query.attribute(null);
            fail();
        } catch (NullPointerException npe) {
            pass();
        } catch (Throwable ex) {
            unexpected(ex);
        }

        try {
            Query query = Query.attribute(null, null);
            fail();
        } catch (NullPointerException npe) {
            pass();
        } catch (Throwable ex) {
            unexpected(ex);
        }

        try {
            Query query = Query.attribute("a.b.c", null);
            fail();
        } catch (NullPointerException npe) {
            pass();
        } catch (Throwable ex) {
            unexpected(ex);
        }

        try {
            final String KEY1 = "key1";
            final String KEY2 = "key2";
            final String KEY3 = "key3";
            final String HELLO_WORLD = "hello world";
            final String HELLO_WORLD_2 = "hello world 2";
            final String RANDOM_VALUE = "random value";

            HashMap<String, String> attributes = new HashMap<String, String>();
            attributes.put(KEY1, HELLO_WORLD);
            ModuleDefinition moduleDef1 = new MockModuleDefinition("javax.swing", Version.valueOf(1, 0, 0), attributes);

            attributes = new HashMap<String, String>();
            attributes.put(KEY1, HELLO_WORLD_2);
            attributes.put(KEY2, HELLO_WORLD);
            ModuleDefinition moduleDef2 = new MockModuleDefinition("org.foo.xml", Version.valueOf(2, 0, 0), attributes);

            Query query = Query.attribute(KEY1);
            check(query.match(moduleDef1) == true);
            check(query.match(moduleDef2) == true);

            query = Query.attribute(KEY1, HELLO_WORLD);
            check(query.match(moduleDef1) == true);
            check(query.match(moduleDef2) == false);

            query = Query.attribute(KEY1, RANDOM_VALUE);
            check(query.match(moduleDef1) == false);
            check(query.match(moduleDef2) == false);

            query = Query.attribute(KEY2);
            check(query.match(moduleDef1) == false);
            check(query.match(moduleDef2) == true);

            query = Query.attribute(KEY2, HELLO_WORLD);
            check(query.match(moduleDef1) == false);
            check(query.match(moduleDef2) == true);

            query = Query.attribute(KEY2, RANDOM_VALUE);
            check(query.match(moduleDef1) == false);
            check(query.match(moduleDef2) == false);

            query = Query.attribute(KEY3);
            check(query.match(moduleDef1) == false);
            check(query.match(moduleDef2) == false);

            query = Query.attribute("x.y.z");
            check(query.equals(query) == true);
            check(query.equals(Query.module("org.foo.xml")) == false);
            check(query.equals(Query.module("org.foo.xml", VersionConstraint.valueOf("1.0.0"))) == false);
            check(query.equals(Query.attribute("my.name")) == false);
            check(query.equals(Query.attribute("x.y.z")) == true);
            check(query.equals(Query.attribute("x.y.z", HELLO_WORLD)) == false);

            Query query2 = cloneQueryBySerialization(query);
            check(query.equals(query2) == true);
            check(query2.equals(query) == true);
            check(query2.equals(query2) == true);
            check(query.toString().equals(query2.toString()) == true);

            query = Query.attribute("x.y.z", HELLO_WORLD);
            check(query.equals(query) == true);
            check(query.equals(Query.module("org.foo.xml")) == false);
            check(query.equals(Query.module("org.foo.xml", VersionConstraint.valueOf("1.0.0"))) == false);
            check(query.equals(Query.attribute("my.name")) == false);
            check(query.equals(Query.attribute("x.y.z")) == false);
            check(query.equals(Query.attribute("x.y.z", HELLO_WORLD)) == true);
            check(query.equals(Query.attribute("x.y.z", RANDOM_VALUE)) == false);

            check(query.getIndexableModuleNames() == null);

            query2 = cloneQueryBySerialization(query);
            check(query.equals(query2) == true);
            check(query2.equals(query) == true);
            check(query2.equals(query2) == true);
            check(query.toString().equals(query2.toString()) == true);

            check(query.hashCode() == query2.hashCode());
            check(query.hashCode() != Query.attribute("x.y.z").hashCode());
        } catch (Throwable ex) {
            unexpected(ex);
        }
    }

    static public void testAnnotationQuery() throws Exception {
        // Test AnnotationQuery
        try {
            Query query = Query.annotation(null);
            fail();
        } catch (NullPointerException npe) {
            pass();
        } catch (Throwable ex) {
            unexpected(ex);
        }

        try {
            Map<Class, Annotation> annotations = new HashMap<Class, Annotation>();
            LegacyClasses legacyClassesAnnotation = new LegacyClasses() {
                public String[] value() {
                    return new String[0];
                }
                public Class<? extends Annotation> annotationType() {
                    return LegacyClasses.class;
                }
            };
            annotations.put(LegacyClasses.class, legacyClassesAnnotation);

            HashMap<String, String> attributes = new HashMap<String, String>();

            ModuleDefinition moduleDef1 = new MockModuleDefinition("javax.swing", Version.valueOf(1, 0, 0), attributes);
            ModuleDefinition moduleDef2 = new MockModuleDefinition("org.foo.xml", Version.valueOf(2, 0, 0), attributes, annotations);

            Query query = Query.annotation(LegacyClasses.class);
            check(query.match(moduleDef1) == false);
            check(query.match(moduleDef2) == true);

            check(query.equals(query) == true);
            check(query.equals(Query.module("org.foo.xml")) == false);
            check(query.equals(Query.module("org.foo.xml", VersionConstraint.valueOf("1.0.0"))) == false);
            check(query.equals(Query.attribute("my.name")) == false);
            check(query.equals(Query.annotation(LegacyClasses.class)) == true);

            check(query.getIndexableModuleNames() == null);

            Query query2 = cloneQueryBySerialization(query);
            check(query.equals(query2) == true);
            check(query2.equals(query) == true);
            check(query2.equals(query2) == true);
            check(query.toString().equals(query2.toString()) == true);

            check(query.hashCode() == query2.hashCode());
            check(query.hashCode() != Query.annotation(ImportPolicyClass.class).hashCode());

            Query query3 = Query.annotation(ImportPolicyClass.class);
            check(query3.match(moduleDef1) == false);
            check(query3.match(moduleDef2) == false);
        } catch (Throwable ex) {
            unexpected(ex);
        }
    }

    static public void testNotQuery() throws Exception {
        // Test NotQuery
        try {
            Query query = Query.not(null);
            fail();
        } catch (NullPointerException npe) {
            pass();
        } catch (Throwable ex) {
            unexpected(ex);
        }

        try {
            final String KEY1 = "key1";
            final String HELLO_WORLD = "hello world";
            final String RANDOM_VALUE = "random value";

            HashMap<String, String> attributes = new HashMap<String, String>();
            attributes.put(KEY1, HELLO_WORLD);
            ModuleDefinition moduleDef1 = new MockModuleDefinition("javax.swing", Version.valueOf(1, 0, 0), attributes);
            ModuleDefinition moduleDef2 = new MockModuleDefinition("org.foo.xml", Version.valueOf(2, 0, 0), new HashMap<String, String>());

            Query query1 = Query.module("javax.swing");
            Query query2 = Query.not(query1);
            check(query1.match(moduleDef1) == true);
            check(query1.match(moduleDef2) == false);
            check(query2.match(moduleDef1) == false);
            check(query2.match(moduleDef2) == true);
            check(query2.getIndexableModuleNames() == null);

            query1 = Query.module("javax.swing", VersionConstraint.valueOf("1.0.0"));
            query2 = Query.not(query1);
            check(query1.match(moduleDef1) == true);
            check(query1.match(moduleDef2) == false);
            check(query2.match(moduleDef1) == false);
            check(query2.match(moduleDef2) == true);
            check(query2.getIndexableModuleNames() == null);

            query1 = Query.attribute(KEY1);
            query2 = Query.not(query1);
            check(query1.match(moduleDef1) == true);
            check(query1.match(moduleDef2) == false);
            check(query2.match(moduleDef1) == false);
            check(query2.match(moduleDef2) == true);
            check(query2.getIndexableModuleNames() == null);

            query1 = Query.attribute(KEY1, HELLO_WORLD);
            query2 = Query.not(query1);
            check(query1.match(moduleDef1) == true);
            check(query1.match(moduleDef2) == false);
            check(query2.match(moduleDef1) == false);
            check(query2.match(moduleDef2) == true);
            check(query2.getIndexableModuleNames() == null);

            query1 = Query.attribute(KEY1, RANDOM_VALUE);
            query2 = Query.not(query1);
            check(query1.match(moduleDef1) == false);
            check(query1.match(moduleDef2) == false);
            check(query2.match(moduleDef1) == true);
            check(query2.match(moduleDef2) == true);
            check(query2.getIndexableModuleNames() == null);

            query1 = Query.not(Query.module("javax.swing"));
            check(query1.equals(query1) == true);
            check(query1.equals(Query.module("javax.swing")) == false);
            check(query1.equals(Query.not(Query.module("javax.swing"))) == true);
            check(query1.equals(Query.module("org.foo.xml")) == false);
            check(query1.equals(Query.module("org.foo.xml", VersionConstraint.valueOf("1.1.0"))) == false);
            check(query1.equals(Query.attribute("my.name")) == false);
            check(query1.hashCode() == Query.not(Query.module("javax.swing")).hashCode());
            check(query1.getIndexableModuleNames() == null);

            query2 = cloneQueryBySerialization(query1);
            check(query1.equals(query2) == true);
            check(query2.equals(query1) == true);
            check(query2.equals(query2) == true);
            check(query1.toString().equals(query2.toString()) == true);

            check(query1.hashCode() == query2.hashCode());
            check(query1.hashCode() == Query.not(Query.module("javax.swing")).hashCode());
            check(query1.hashCode() != Query.not(Query.module("org.foo.xml")).hashCode());
            check(query1.hashCode() != Query.not(Query.module("a.b.c.", VersionConstraint.valueOf("1.1.0"))).hashCode());
            check(query1.hashCode() != Query.not(Query.attribute("my.name")).hashCode());
        } catch (Throwable ex) {
            unexpected(ex);
        }
    }

    static public void testAndQuery() throws Exception {
        // Test AndQuery
        try {
            Query query = Query.and(null, null);
            fail();
        } catch (NullPointerException npe) {
            pass();
        } catch (Throwable ex) {
            unexpected(ex);
        }

        try {
            Query query = Query.and(null, Query.ANY);
            fail();
        } catch (NullPointerException npe) {
            pass();
        } catch (Throwable ex) {
            unexpected(ex);
        }


        try {
            Query query = Query.and(Query.ANY, null);
            fail();
        } catch (NullPointerException npe) {
            pass();
        } catch (Throwable ex) {
            unexpected(ex);
        }

        try {
            final String KEY1 = "key1";
            final String KEY2 = "key2";
            final String HELLO_WORLD = "hello world";
            final String RANDOM_VALUE = "random value";

            HashMap<String, String> attributes = new HashMap<String, String>();
            attributes.put(KEY1, HELLO_WORLD);
            ModuleDefinition moduleDef1 = new MockModuleDefinition("javax.swing", Version.valueOf(1, 0, 0), attributes);
            ModuleDefinition moduleDef2 = new MockModuleDefinition("org.foo.xml", Version.valueOf(2, 0, 0), new HashMap<String, String>());

            Query query1 = Query.module("javax.swing", VersionConstraint.valueOf("1.0.0"));
            Query query2 = Query.module("org.foo.xml", VersionConstraint.valueOf("1.0.0"));
            check(query1.match(moduleDef1) == true);
            check(query1.match(moduleDef2) == false);
            check(query2.match(moduleDef1) == false);
            check(query2.match(moduleDef2) == false);

            Set<String> indexableNames = query1.getIndexableModuleNames();
            check(indexableNames != null && indexableNames.size() == 1 && indexableNames.contains("javax.swing"));
            indexableNames = query2.getIndexableModuleNames();
            check(indexableNames != null && indexableNames.size() == 1 && indexableNames.contains("org.foo.xml"));

            query1 = Query.and(Query.module("javax.swing"), Query.attribute(KEY1));
            query2 = Query.and(Query.module("javax.swing"), Query.attribute(KEY1, RANDOM_VALUE));
            check(query1.match(moduleDef1) == true);
            check(query1.match(moduleDef2) == false);
            check(query2.match(moduleDef1) == false);
            check(query2.match(moduleDef2) == false);

            indexableNames = query1.getIndexableModuleNames();
            check(indexableNames != null && indexableNames.size() == 1 && indexableNames.contains("javax.swing"));
            indexableNames = query2.getIndexableModuleNames();
            check(indexableNames != null && indexableNames.size() == 1 && indexableNames.contains("javax.swing"));

            query1 = Query.and(Query.module("javax.swing"), Query.attribute(KEY2));
            query2 = Query.and(Query.module("javax.swing"), Query.attribute(KEY2, RANDOM_VALUE));
            check(query1.match(moduleDef1) == false);
            check(query1.match(moduleDef2) == false);
            check(query2.match(moduleDef1) == false);
            check(query2.match(moduleDef2) == false);

            indexableNames = query1.getIndexableModuleNames();
            check(indexableNames != null && indexableNames.size() == 1 && indexableNames.contains("javax.swing"));
            indexableNames = query2.getIndexableModuleNames();
            check(indexableNames != null && indexableNames.size() == 1 && indexableNames.contains("javax.swing"));

            query1 = Query.and(Query.module("javax.swing", VersionConstraint.valueOf("1.0.0")),
                               Query.annotation(java.module.annotation.MainClass.class));
            check(query1.equals(query1) == true);
            check(query1.equals(Query.module("javax.swing")) == false);
            check(query1.equals(Query.not(Query.module("javax.swing"))) == false);
            check(query1.equals(Query.attribute("my.name")) == false);
            check(query1.equals(Query.module("javax.swing", VersionConstraint.valueOf("1.0.0")))== false);
            check(query1.equals(Query.and(Query.module("javax.swing", VersionConstraint.valueOf("1.0.0")),
                                          Query.annotation(java.module.annotation.MainClass.class))) == true);
            check(query1.equals(Query.module("javax.swing", VersionConstraint.valueOf("2.0.0"))) == false);
            check(query1.equals(Query.module("org.foo.xml", VersionConstraint.valueOf("1.0.0"))) == false);
            check(query1.equals(Query.module("org.foo.xml", VersionConstraint.valueOf("2.0.0"))) == false);

            indexableNames = Query.and(Query.attribute("hello"), Query.attribute("my.name")).getIndexableModuleNames();
            check(indexableNames == null);
            indexableNames = Query.and(Query.module("javax.swing"), Query.attribute("my.name")).getIndexableModuleNames();
            check(indexableNames != null && indexableNames.size() == 1 && indexableNames.contains("javax.swing"));
            indexableNames = Query.and(Query.module("javax.swing"), Query.module("org.xml.foo")).getIndexableModuleNames();
            check(indexableNames != null && indexableNames.size() == 0);

            query2 = cloneQueryBySerialization(query1);
            check(query1.equals(query2) == true);
            check(query2.equals(query1) == true);
            check(query2.equals(query2) == true);
            check(query1.toString().equals(query2.toString()) == true);

            check(query1.hashCode() == query2.hashCode());
            check(query1.hashCode() == Query.and(Query.module("javax.swing", VersionConstraint.valueOf("1.0.0")),
                                                 Query.annotation(java.module.annotation.MainClass.class)).hashCode());
            check(query1.hashCode() == Query.and(Query.annotation(java.module.annotation.MainClass.class),
                                                 Query.module("javax.swing", VersionConstraint.valueOf("1.0.0"))).hashCode());
            check(query1.hashCode() != Query.and(Query.module("org.foo.xml", VersionConstraint.valueOf("1.0.0")),
                                                 Query.annotation(java.module.annotation.MainClass.class)).hashCode());
            check(query1.hashCode() != Query.and(Query.annotation(java.module.annotation.MainClass.class),
                                                 Query.module("org.foo.xml", VersionConstraint.valueOf("2.0.0"))).hashCode());
        } catch (Throwable ex) {
            unexpected(ex);
        }
    }

    static public void testOrQuery() throws Exception {
        // Test OrQuery
        try {
            Query query = Query.or(null, null);
            fail();
        } catch (NullPointerException npe) {
            pass();
        } catch (Throwable ex) {
            unexpected(ex);
        }

        try {
            Query query = Query.or(null, Query.ANY);
            fail();
        } catch (NullPointerException npe) {
            pass();
        } catch (Throwable ex) {
            unexpected(ex);
        }


        try {
            Query query = Query.or(Query.ANY, null);
            fail();
        } catch (NullPointerException npe) {
            pass();
        } catch (Throwable ex) {
            unexpected(ex);
        }

        try {
            final String KEY1 = "key1";
            final String KEY2 = "key2";
            final String HELLO_WORLD = "hello world";
            final String RANDOM_VALUE = "random value";

            HashMap<String, String> attributes = new HashMap<String, String>();
            attributes.put(KEY1, HELLO_WORLD);
            ModuleDefinition moduleDef1 = new MockModuleDefinition("javax.swing", Version.valueOf(1, 0, 0), attributes);

            attributes = new HashMap<String, String>();
            attributes.put(KEY2, HELLO_WORLD);
            ModuleDefinition moduleDef2 = new MockModuleDefinition("org.foo.xml", Version.valueOf(2, 0, 0), attributes);

            Query query1 = Query.or(Query.module("javax.swing"), Query.annotation(java.module.annotation.MainClass.class));
            Query query2 = Query.or(Query.module("org.foo.xml", VersionConstraint.valueOf("2.0.0")),
                                    Query.annotation(java.module.annotation.MainClass.class));
            Query query3 = Query.or(Query.module("com.wombat.soap"), Query.annotation(java.module.annotation.MainClass.class));
            check(query1.match(moduleDef1) == true);
            check(query1.match(moduleDef2) == false);
            check(query2.match(moduleDef1) == false);
            check(query2.match(moduleDef2) == true);
            check(query3.match(moduleDef1) == false);
            check(query3.match(moduleDef2) == false);

            query1 = Query.or(Query.module("javax.swing"), Query.attribute(KEY1));
            query2 = Query.or(Query.module("org.foo.xml"), Query.attribute(KEY1, RANDOM_VALUE));
            query3 = Query.or(Query.module("com.wombat.soap"), Query.attribute(KEY2, HELLO_WORLD));
            check(query1.match(moduleDef1) == true);
            check(query1.match(moduleDef2) == false);
            check(query2.match(moduleDef1) == false);
            check(query2.match(moduleDef2) == true);
            check(query3.match(moduleDef1) == false);
            check(query3.match(moduleDef2) == true);

            query1 = Query.or(Query.module("javax.swing", VersionConstraint.valueOf("1.0.0")),
                              Query.annotation(java.module.annotation.MainClass.class));
            check(query1.equals(query1) == true);
            check(query1.equals(Query.module("javax.swing")) == false);
            check(query1.equals(Query.not(Query.module("javax.swing"))) == false);
            check(query1.equals(Query.attribute("my.name")) == false);
            check(query1.equals(Query.or(Query.module("javax.swing", VersionConstraint.valueOf("1.0.0")),
                                         Query.annotation(java.module.annotation.MainClass.class))) == true);
            check(query1.equals(Query.or(Query.annotation(java.module.annotation.MainClass.class),
                                         Query.module("javax.swing", VersionConstraint.valueOf("1.0.0")))) == true);
            check(query1.equals(Query.or(Query.module("org.foo.xml", VersionConstraint.valueOf("2.0.0")),
                                         Query.annotation(java.module.annotation.MainClass.class))) == false);

            Set<String> indexableNames = Query.or(Query.annotation(java.module.annotation.MainClass.class), Query.attribute("my.name")).getIndexableModuleNames();
            check(indexableNames == null);
            indexableNames = Query.or(Query.module("javax.swing"), Query.attribute("my.name")).getIndexableModuleNames();
            check(indexableNames != null && indexableNames.size() == 1 && indexableNames.contains("javax.swing"));
            indexableNames = Query.or(Query.module("javax.swing"), Query.module("org.xml.foo")).getIndexableModuleNames();
            check(indexableNames != null && indexableNames.size() == 2
                  && indexableNames.contains("javax.swing") && indexableNames.contains("org.xml.foo"));

            query2 = cloneQueryBySerialization(query1);
            check(query1.equals(query2) == true);
            check(query2.equals(query1) == true);
            check(query2.equals(query2) == true);
            check(query1.toString().equals(query2.toString()) == true);

            check(query1.hashCode() == query2.hashCode());
            check(query1.hashCode() == Query.or(Query.module("javax.swing", VersionConstraint.valueOf("1.0.0")),
                                                Query.annotation(java.module.annotation.MainClass.class)).hashCode());
            check(query1.hashCode() == Query.or(Query.annotation(java.module.annotation.MainClass.class),
                                                Query.module("javax.swing", VersionConstraint.valueOf("1.0.0"))).hashCode());
            check(query1.hashCode() != Query.or(Query.module("org.foo.xml", VersionConstraint.valueOf("1.0.0")),
                                                Query.annotation(java.module.annotation.MainClass.class)).hashCode());
        } catch (Throwable ex) {
            unexpected(ex);
        }
    }

    static public Query cloneQueryBySerialization(Query query) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(query);
        oos.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        return (Query) ois.readObject();
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
