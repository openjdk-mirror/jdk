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

import java.io.File;
import java.io.IOException;
import java.module.Version;
import java.net.URL;
import java.util.Set;
import org.xml.sax.SAXException;
import sun.module.repository.URLModuleInfo;
import sun.module.repository.MetadataXMLReader;

/**
 * @test
 * @summary Test MetadataXMLReader
 * @compile -XDignore.symbol.file MetadataXMLReaderTest.java
 * @run main MetadataXMLReaderTest
 */
public class MetadataXMLReaderTest {
    private static String platform = "solaris";
    private static String arch = "sparc";

    public static void realMain(String[] args) throws Throwable {
        testRead01();
        testRead02();
        testRead03();
        testRead04();
        testRead05();
        testRead06();
    }

    //
    // Each testReadNN method checks a correspondingly named repository-metadata-NN.xml
    // The tests are with respect to validity of schema.  They are not intended to catch
    // broken XML syntax (missing '>', etc.).'
    //

    /** Checks basics of construction, optional platform-arch.  No errors. */
    static public void testRead01() throws Exception {
        Set<URLModuleInfo> result = MetadataXMLReader.read(
            getURL("repository-metadata-01.xml"));
        check(result.size() == 3);
        for (URLModuleInfo mt : result) {
            if (mt.getName().equals("org.foo.xml")) {
                check(mt.getVersion().equals(Version.valueOf("1.3")));
                check(mt.getPlatform() == null);
                check(mt.getArch() == null);
                check(mt.getPath() == null);
            } else if (mt.getName().equals("org.foo.soap")) {
                if (mt.getPlatform() == null) {
                    check(mt.getVersion().equals(Version.valueOf("2.0.0")));
                    check(mt.getArch() == null);
                    check(mt.getPath().equals("soap2Other"));
                } else if ("soap2Linux".equals(mt.getPath())) {
                    check("linux".equals(mt.getPlatform()));
                    check("x86".equals(mt.getArch()));
                } else {
                    // Fail: one must have platform binding, the other not
                    fail();
                }
            } else {
                // Fail: modules names must match
                fail();
            }
        }
    }

    /** Checks that all optional elements can really be optional. */
    static public void testRead02() throws Exception {
        Set<URLModuleInfo> result = MetadataXMLReader.read(
            getURL("repository-metadata-02.xml"));
        check(result.size() == 1);
        URLModuleInfo mt = result.iterator().next();
        check(mt.getName().equals("org.foo.xml"));
        check(mt.getVersion().equals(Version.valueOf("1.3")));
        check(mt.getPlatform() == null);
        check(mt.getArch() == null);
        check(mt.getPath() == null);
    }

    /** Checks error: platform but no arch. */
    static public void testRead03() throws Exception {
        Set<URLModuleInfo> result = null;
        try {
            result = MetadataXMLReader.read(
                getURL("repository-metadata-03.xml"));
            fail();
        } catch (SAXException ex) {
            if (ex.toString().indexOf("'{arch}' is expected") < 0) {
                unexpected(ex);
            } else {
                pass();
            }
        }
    }

    /** Checks error: arch but no platform. */
    static public void testRead04() throws Exception {
        Set<URLModuleInfo> result = null;
        try {
            result = MetadataXMLReader.read(
                getURL("repository-metadata-04.xml"));
            fail();
        } catch (SAXException ex) {
            if (ex.toString().indexOf("'{platform}' is expected") < 0) {
                unexpected(ex);
            } else {
                pass();
            }
        }
    }

    /** Checks error: empty <platform-binding> element. */
    static public void testRead05() throws Exception {
        Set<URLModuleInfo> result = null;
        try {
            result = MetadataXMLReader.read(
                getURL("repository-metadata-05.xml"));
            fail();
        } catch (SAXException ex) {
            if (ex.toString().indexOf("'platform-binding'") < 0) {
                unexpected(ex);
            } else {
                pass();
            }
        }
    }

    /** Checks that duplicates are not allowed. */
    static public void testRead06() throws Exception {
        Set<URLModuleInfo> result = null;
        try {
            result = MetadataXMLReader.read(
                getURL("repository-metadata-06.xml"));
            fail();
        } catch (IllegalArgumentException ex) {
            pass();
        }
    }

    /** Checks that XML-specified path, platform & arch work. */
    static public void testRead07() throws Exception {
        Set<URLModuleInfo> result = MetadataXMLReader.read(
            getURL("repository-metadata-07.xml"));
        check(result.size() == 1);
        URLModuleInfo mt = result.iterator().next();
        check(mt.getName().equals("org.foo.xml"));
        check(mt.getVersion().equals(Version.valueOf("1.3")));
        check(mt.getPlatform().equals(platform));
        check(mt.getArch().equals(arch));
        check(mt.getPath().equals("one/two"));
    }

    static URL getURL(String filename) throws IOException {
        return new File(
                System.getProperty("test.src", "."), filename).getCanonicalFile().toURI().toURL();
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
