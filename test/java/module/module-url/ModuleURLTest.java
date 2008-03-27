/*
 * Copyright 2007 Sun Microsystems, Inc.  All Rights Reserved.
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

import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;

/*
 * @test @(#)ModuleURLTest.java 1.1 07/09/13
 * @summary Test ModuleURLTest
 * @author Stanley M. Ho
 */

public class ModuleURLTest {

    public static void realMain(String[] args) throws Throwable {
        testConstruction01();
        testConstruction02();
        testOpenConnectionAndStream();
    }

    /** Checks basics of construction.  No errors. */
    static public void testConstruction01() throws Exception {
        try {
            URL url = null;

            url = new URL("module:bootstrap!/java.se/1.7.0");
            check(url.getProtocol().equals("module"));
            System.out.println("host : " + url.getHost());
            check(url.getHost().equals(""));
            check(url.getFile().equals("bootstrap!/java.se/1.7.0"));

            url = new URL("module:bootstrap!/java.se/[1.7,2.0)");
            check(url.getProtocol().equals("module"));
            check(url.getHost().equals(""));
            check(url.getPort() == -1);
            check(url.getFile().equals("bootstrap!/java.se/[1.7,2.0)"));

            url = new URL("module:bootstrap/http://www.sun.com/!/java.se/1.7.0");
            check(url.getProtocol().equals("module"));
            check(url.getHost().equals(""));
            check(url.getPort() == -1);
            check(url.getFile().equals("bootstrap/http://www.sun.com/!/java.se/1.7.0"));

            url = new URL("module:bootstrap/http://www.sun.com/!/java.se/[1.7,2.0)");
            check(url.getProtocol().equals("module"));
            check(url.getHost().equals(""));
            check(url.getPort() == -1);
            check(url.getFile().equals("bootstrap/http://www.sun.com/!/java.se/[1.7,2.0)"));

            pass();
        }
        catch (Throwable ex) {
            unexpected(ex);
        }
    }

    /** Checks error. */
    static public void testConstruction02() throws Exception {
        try {
            // <repository-type> and <source> are missing.
            URL url = new URL("module:!/java.se/1.7.0");
            fail();
        } catch (MalformedURLException ex) {
            pass();
        }

        try {
            // <repository-type> and <source> are empty.
            URL url = new URL("module:/!/java.se/1.7.0");
            fail();
        } catch (MalformedURLException ex) {
            pass();
        }

        try {
            // <repository-type> is empty.
            URL url = new URL("module:/http://www.sun.com!/java.se/1.7.0");
            fail();
        } catch (MalformedURLException ex) {
            pass();
        }

        try {
            // <source> is empty.
            URL url = new URL("module:bootstrap/!/java.se/1.7.0");
            fail();
        } catch (MalformedURLException ex) {
            pass();
        }

        try {
            // <source> is malformed - unknown protocol.
            URL url = new URL("module:bootstrap/someprotocol://x.y.z!/java.se/1.7.0");
            fail();
        } catch (MalformedURLException ex) {
            pass();
        }

        try {
            // <module-name> and <version-constraint> are missing
            URL url = new URL("module:bootstrap!/");
            fail();
        } catch (MalformedURLException ex) {
            pass();
        }

        try {
            // <module-name> and <version-constraint> are empty
            URL url = new URL("module:bootstrap!//");
            fail();
        } catch (MalformedURLException ex) {
            pass();
        }

        try {
            // <module-name> is empty
            URL url = new URL("module:bootstrap!//1.7.0");
            fail();
        } catch (MalformedURLException ex) {
            pass();
        }

        try {
            // <version-constraint> is missing
            URL url = new URL("module:bootstrap!/java.se");
            fail();
        } catch (MalformedURLException ex) {
            pass();
        }

        try {
            // <version-constraint> is empty
            URL url = new URL("module:bootstrap!/java.se/");
            fail();
        } catch (MalformedURLException ex) {
            pass();
        }

        try {
            // <version-constraint> is malformed
            URL url = new URL("module:bootstrap!/java.se/xyz");
            fail();
        } catch (MalformedURLException ex) {
            pass();
        }

        try {
            // spec has whitespaces
            URL url = new URL("module:bootstrap!/ java.se/[1.0,2.0)");
            fail();
        } catch (MalformedURLException ex) {
            pass();
        }
    }

    /** Checks basics operations of the URL.*/
    static public void testOpenConnectionAndStream() throws Exception {
        try {
            URL url = new URL("module:bootstrap!/java.se/[1.0,2.0)");
            url.openConnection();
            fail();
        } catch (IOException ex) {
            pass();
        }
        try {
            URL url = new URL("module:bootstrap!/java.se/[1.0,2.0)");
            url.openStream();
            fail();
        } catch (IOException ex) {
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
