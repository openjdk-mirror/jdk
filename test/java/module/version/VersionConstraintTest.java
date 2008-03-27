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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.module.Version;
import java.module.VersionConstraint;
import java.module.Query;
import java.util.ArrayList;
import java.util.List;

/*
 * @test VersionConstraintTest.java
 * @summary Test VersionConstraintTest
 * @author Stanley M. Ho
 */

public class VersionConstraintTest {

    public static void realMain(String[] args) throws Throwable {
        testParse01();
        testParse02();
        testEqual01();
        testHashCode01();
        testContains01();
        testIntersection01();
        testToString01();
        testSerialization();
        //      testNormalize01();
    }

    /** Checks equals method.  No errors. */
    static public void testEqual01() throws Exception {
        try {
            VersionConstraint[] array = new VersionConstraint[6];
            array[0] = VersionConstraint.valueOf("[1, 6)");
            array[1] = VersionConstraint.valueOf("[1, 3);[4, 6);3.*");
            array[2] = VersionConstraint.valueOf("[5, 6);[2, 3);[4, 5.0);[1, 2);[3.0, 4.0.0.0)");
            array[3] = VersionConstraint.valueOf("[1, 2);1.7.0;2.3.4;[3, 5);[2, 6)");
            array[4] = VersionConstraint.valueOf("1.*;[2, 2.5.6);3.*;5.*;[2.5.5, 3.8.9];(3.8.9, 4.0);4.*");
            array[5] = VersionConstraint.valueOf("(1, 2);(2, 3);(3, 4);(4, 5);(5, 6);[2, 2];[3, 3];[4, 4];[5, 5];[1, 1]");

            for (int i=0 ; i < array.length; i++) {
                for (int j=0 ; j < array.length; j++) {
                    if (array[i] == array[j]) {
                        check(array[i].equals(array[j]) == true);
                    }
                    else {
                        check(array[i].equals(array[j]) == true);
                    }
                }
            }
            pass();
        }
        catch (Throwable e) {
            unexpected(e);
        }

        try {
            VersionConstraint[] array = new VersionConstraint[6];
            array[0] = VersionConstraint.valueOf("[1, 6)");
            array[1] = VersionConstraint.valueOf("[1, 3);[4, 6)");
            array[2] = VersionConstraint.valueOf("[5, 6);[2, 3);[4, 5.0);[3.0, 4.0.0.0)");
            array[3] = VersionConstraint.valueOf("[1, 2);1.7.0;2.3.4;[3, 5);[3, 6)");
            array[4] = VersionConstraint.valueOf("1.*;[2, 2.5.6);3.*;5.*;[2.5.7, 3.8.9];(3.8.9, 4.0);4.*");
            array[5] = VersionConstraint.valueOf("(1, 2);(2, 3);(3, 4);(4, 5);(5, 6)");

            for (int i=0 ; i < array.length; i++) {
                for (int j=0 ; j < array.length; j++) {
                    if (array[i] == array[j]) {
                        check(array[i].equals(array[j]) == true);
                    }
                    else {
                        check(array[i].equals(array[j]) == false);
                    }
                }
            }
            pass();
        }
        catch (Throwable e) {
            unexpected(e);
        }

        try {
            VersionConstraint[] array = new VersionConstraint[6];
            array[0] = VersionConstraint.valueOf("1+");
            array[1] = VersionConstraint.valueOf("[1, 3);[4, 6);6+;[3, 4)");
            array[2] = VersionConstraint.valueOf("[5, 6);[2, 3);2+;[4, 5.0);[3.0, 4.0.0.0);[1,2)");
            array[3] = VersionConstraint.valueOf("[1, 2);1.7.0;2.3.4+;[3, 5);[3, 6);2+");
            array[4] = VersionConstraint.valueOf("1.*;[2, 2.5.6);3.*;5.*;[2.5.5, 3.8.9];2.4+;(3.8.9, 4.0);4.*");
            array[5] = VersionConstraint.valueOf("(1, 2);(2, 3);(3, 4);(4, 5);(5, 6);2+;[1, 1]");

            for (int i=0 ; i < array.length; i++) {
                for (int j=0 ; j < array.length; j++) {
                    if (array[i] == array[j]) {
                        check(array[i].equals(array[j]) == true);
                    }
                    else {
                        check(array[i].equals(array[j]) == true);
                    }
                }
            }
            pass();
        }
        catch (Throwable e) {
            unexpected(e);
        }
    }

    /** Checks hashCode method.  No errors. */
    static public void testHashCode01() throws Exception {
        try {
            VersionConstraint[] array1 = new VersionConstraint[6];
            array1[0] = VersionConstraint.valueOf("[1, 6)");
            array1[1] = VersionConstraint.valueOf("[1, 3);[4, 6);3.*");
            array1[2] = VersionConstraint.valueOf("[5, 6);[2, 3);[4, 5.0);[1, 2);[3.0, 4.0.0.0)");
            array1[3] = VersionConstraint.valueOf("[1, 2);1.7.0;2.3.4;[3, 5);[2, 6)");
            array1[4] = VersionConstraint.valueOf("1.*;[2, 2.5.6);3.*;5.*;[2.5.5, 3.8.9];(3.8.9, 4.0);4.*");
            array1[5] = VersionConstraint.valueOf("(1, 2);(2, 3);(3, 4);(4, 5);(5, 6);[2, 2];[3, 3];[4, 4];[5, 5];[1, 1]");

            VersionConstraint[] array2 = new VersionConstraint[6];
            array2[0] = VersionConstraint.valueOf("[1, 6)");
            array2[1] = VersionConstraint.valueOf("[1, 3);[4, 6);3.*");
            array2[2] = VersionConstraint.valueOf("[5, 6);[2, 3);[4, 5.0);[1, 2);[3.0, 4.0.0.0)");
            array2[3] = VersionConstraint.valueOf("[1, 2);1.7.0;2.3.4;[3, 5);[2, 6)");
            array2[4] = VersionConstraint.valueOf("1.*;[2, 2.5.6);3.*;5.*;[2.5.5, 3.8.9];(3.8.9, 4.0);4.*");
            array2[5] = VersionConstraint.valueOf("(1, 2);(2, 3);(3, 4);(4, 5);(5, 6);[2, 2];[3, 3];[4, 4];[5, 5];[1, 1]");

            for (int i=0 ; i < array1.length; i++) {
                for (int j=0 ; j < array2.length; j++) {
                    if (array1[i].equals(array2[j])) {
                        check(array1[i].hashCode() == array2[j].hashCode());
                    }
                    else {
                        check(array1[i].hashCode() != array2[j].hashCode());
                    }
                }
            }
            pass();
        }
        catch (Throwable e) {
            unexpected(e);
        }

        try {
            VersionConstraint[] array1 = new VersionConstraint[6];
            array1[0] = VersionConstraint.valueOf("[1, 6)");
            array1[1] = VersionConstraint.valueOf("[1, 3);[4, 6)");
            array1[2] = VersionConstraint.valueOf("[5, 6);[2, 3);[4, 5.0);[3.0, 4.0.0.0)");
            array1[3] = VersionConstraint.valueOf("[1, 2);1.7.0;2.3.4;[3, 5);[3, 6)");
            array1[4] = VersionConstraint.valueOf("1.*;[2, 2.5.6);3.*;5.*;[2.5.7, 3.8.9];(3.8.9, 4.0);4.*");
            array1[5] = VersionConstraint.valueOf("(1, 2);(2, 3);(3, 4);(4, 5);(5, 6)");

            VersionConstraint[] array2 = new VersionConstraint[6];
            array2[0] = VersionConstraint.valueOf("[1, 6)");
            array2[1] = VersionConstraint.valueOf("[1, 3);[4, 6)");
            array2[2] = VersionConstraint.valueOf("[5, 6);[2, 3);[4, 5.0);[3.0, 4.0.0.0)");
            array2[3] = VersionConstraint.valueOf("[1, 2);1.7.0;2.3.4;[3, 5);[3, 6)");
            array2[4] = VersionConstraint.valueOf("1.*;[2, 2.5.6);3.*;5.*;[2.5.7, 3.8.9];(3.8.9, 4.0);4.*");
            array2[5] = VersionConstraint.valueOf("(1, 2);(2, 3);(3, 4);(4, 5);(5, 6)");

            for (int i=0 ; i < array1.length; i++) {
                for (int j=0 ; j < array2.length; j++) {
                    if (array1[i].equals(array2[j])) {
                        check(array1[i].hashCode() == array2[j].hashCode());
                    }
                    else {
                        check(array1[i].hashCode() != array2[j].hashCode());
                    }
                }
            }
            pass();
        }
        catch (Throwable e) {
            unexpected(e);
        }

        try {
            VersionConstraint[] array1 = new VersionConstraint[6];
            array1[0] = VersionConstraint.valueOf("1+");
            array1[1] = VersionConstraint.valueOf("[1, 3);[4, 6);6+;[3, 4)");
            array1[2] = VersionConstraint.valueOf("[5, 6);[2, 3);2+;[4, 5.0);[3.0, 4.0.0.0);[1,2)");
            array1[3] = VersionConstraint.valueOf("[1, 2);1.7.0;2.3.4+;[3, 5);[3, 6);2+");
            array1[4] = VersionConstraint.valueOf("1.*;[2, 2.5.6);3.*;5.*;[2.5.5, 3.8.9];2.4+;(3.8.9, 4.0);4.*");
            array1[5] = VersionConstraint.valueOf("(1, 2);(2, 3);(3, 4);(4, 5);(5, 6);2+;[1, 1]");

            VersionConstraint[] array2 = new VersionConstraint[6];
            array2[0] = VersionConstraint.valueOf("1+");
            array2[1] = VersionConstraint.valueOf("[1, 3);[4, 6);6+;[3, 4)");
            array2[2] = VersionConstraint.valueOf("[5, 6);[2, 3);2+;[4, 5.0);[3.0, 4.0.0.0);[1,2)");
            array2[3] = VersionConstraint.valueOf("[1, 2);1.7.0;2.3.4+;[3, 5);[3, 6);2+");
            array2[4] = VersionConstraint.valueOf("1.*;[2, 2.5.6);3.*;5.*;[2.5.5, 3.8.9];2.4+;(3.8.9, 4.0);4.*");
            array2[5] = VersionConstraint.valueOf("(1, 2);(2, 3);(3, 4);(4, 5);(5, 6);2+;[1, 1]");

            for (int i=0 ; i < array1.length; i++) {
                for (int j=0 ; j < array2.length; j++) {
                    if (array1[i].equals(array2[j])) {
                        check(array1[i].hashCode() == array2[j].hashCode());
                    }
                    else {
                        check(array1[i].hashCode() != array2[j].hashCode());
                    }
                }
            }
            pass();
        }
        catch (Throwable e) {
            unexpected(e);
        }
    }

    /** Checks parse method.  No errors. */
    static public void testParse01() throws Exception {
        try {
            VersionConstraint.valueOf("[1, 6)");
            VersionConstraint.valueOf("[1, 3);[4, 6);3.*");
            VersionConstraint.valueOf("[5, 6);[2, 3);[4, 5.0);[1, 2);[3.0, 4.0.0.0)");
            VersionConstraint.valueOf("[1, 2);1.7.0;2.3.4;[3, 5);[2, 6)");
            VersionConstraint.valueOf("1.*;[2, 2.5.6);3.*;5.*;[2.5.5, 3.8.9];(3.8.9, 4.0);4.*");
            VersionConstraint.valueOf("(1, 2);(2, 3);(3, 4);(4, 5);(5, 6);[2, 2];[3, 3];[4, 4];[5, 5];[1, 1]");
            VersionConstraint.valueOf("1+");
            VersionConstraint.valueOf("[1, 3);[4, 6);6+;[3, 4)");
            VersionConstraint.valueOf("[5, 6);[2, 3);2+;[4, 5.0);[3.0, 4.0.0.0);[1,2)");
            VersionConstraint.valueOf("[1, 2);1.7.0;2.3.4+;[3, 5);[3, 6);2+");
            VersionConstraint.valueOf("1.*;[2, 2.5.6);3.*;5.*;[2.5.5, 3.8.9];2.4+;(3.8.9, 4.0);4.*");
            VersionConstraint.valueOf("(1, 2);(2, 3);(3, 4);(4, 5);(5, 6);2+;[1, 1]");
            pass();
        }
        catch (Throwable ex) {
            unexpected(ex);
        }
    }

    /** Checks errors: version format is malformed. */
    static public void testParse02() throws Exception {
        try {
            VersionConstraint.valueOf("[[1, 6)");
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        catch (Throwable e) {
            unexpected(e);
        }

        try {
            VersionConstraint.valueOf("[[1, 6))");
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        catch (Throwable e) {
            unexpected(e);
        }

        try {
            VersionConstraint.valueOf("");
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        catch (Throwable e) {
            unexpected(e);
        }


        try {
            VersionConstraint.valueOf("+");
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        catch (Throwable e) {
            unexpected(e);
        }

        try {
            VersionConstraint.valueOf(";");
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        catch (Throwable e) {
            unexpected(e);
        }

        try {
            VersionConstraint.valueOf(";;;;;;;");
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        catch (Throwable e) {
            unexpected(e);
        }

        try {
            VersionConstraint.valueOf("; ; ; ; ; ; ;");
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        catch (Throwable e) {
            unexpected(e);
        }

        try {
            VersionConstraint.valueOf("(1, 2):(2, 3):(3, 4):(4, 5):(5, 6):[2, 2]:[3, 3]:[4, 4]:[5, 5]:[1, 1]");
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        catch (Throwable e) {
            unexpected(e);
        }

        try {
            VersionConstraint.valueOf("(1, 2); (2, 3)");
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        catch (Throwable e) {
            unexpected(e);
        }

        try {
            VersionConstraint.valueOf("(1, 2) ;(2, 3)");
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        catch (Throwable e) {
            unexpected(e);
        }
    }

    /** Checks toString method.  No errors. */
    static public void testToString01() throws Exception {
        try {
            String s = null;
            VersionConstraint cs = null;

            s = "[1.0.0.0, 6.0.0.0)";
            cs = VersionConstraint.valueOf(s);
            check("[1, 6)".equals(cs.toString()) == true);
            pass();

            s = "[1, 3);[4, 6);3.*";
            cs = VersionConstraint.valueOf(s);
            check("[1, 3);[4, 6);[3, 4)".equals(cs.toString()) == true);
            pass();

            s = "[5, 6);[2, 3);[4, 5.0);[1, 2);[3.0, 4.0.0.0)";
            cs = VersionConstraint.valueOf(s);
            check("[5, 6);[2, 3);[4, 5);[1, 2);[3, 4)".equals(cs.toString()) == true);
            pass();

            s = "[1, 2);1.7.0;2.3.4;[3, 5);[2, 6)";
            cs = VersionConstraint.valueOf(s);
            check("[1, 2);1.7;2.3.4;[3, 5);[2, 6)".equals(cs.toString()) == true);
            pass();

            s = "1.*;[2, 2.5.6);3.*;5.*;[2.5.5, 3.8.9];(3.8.9, 4.0);4.*";
            cs = VersionConstraint.valueOf(s);
            check("[1, 2);[2, 2.5.6);[3, 4);[5, 6);[2.5.5, 3.8.9];(3.8.9, 4);[4, 5)".equals(cs.toString()) == true);
            pass();

            s = "(1, 2);(2, 3);(3, 4);(4.0, 5);(5.0, 6);[2, 2];[3, 3.0];[4, 4];[5, 5];[1, 1]";
            cs = VersionConstraint.valueOf(s);
            check("(1, 2);(2, 3);(3, 4);(4, 5);(5, 6);[2, 2];[3, 3];[4, 4];[5, 5];[1, 1]".equals(cs.toString()) == true);
            pass();

            s = "1+";
            cs = VersionConstraint.valueOf(s);
            check("1+".equals(cs.toString()) == true);
            pass();

            s = "[1, 3);[4, 6);6.0+;[3, 4)";
            cs = VersionConstraint.valueOf(s);
            check("[1, 3);[4, 6);6+;[3, 4)".equals(cs.toString()) == true);
            pass();

            s = "[5, 6);[2, 3);2+;[4, 5.0);[3.0, 4.0.0.0);[1,2)";
            cs = VersionConstraint.valueOf(s);
            check("[5, 6);[2, 3);2+;[4, 5);[3, 4);[1, 2)".equals(cs.toString()) == true);
            pass();

            s = "[1, 2);1.7.0;2.3.4+;[3, 5);[3, 6);2+";
            cs = VersionConstraint.valueOf(s);
            check("[1, 2);1.7;2.3.4+;[3, 5);[3, 6);2+".equals(cs.toString()) == true);
            pass();

            s = "1.*;[2, 2.5.6);3.*;5.*;[2.5.5, 3.8.9];2.4+;(3.8.9, 4.0);4.*";
            cs = VersionConstraint.valueOf(s);
            check("[1, 2);[2, 2.5.6);[3, 4);[5, 6);[2.5.5, 3.8.9];2.4+;(3.8.9, 4);[4, 5)".equals(cs.toString()) == true);
            pass();

            s = "(1, 2);(2, 3);(3, 4);(4, 5);(5, 6);2+;[1, 1]";
            cs = VersionConstraint.valueOf(s);
            check("(1, 2);(2, 3);(3, 4);(4, 5);(5, 6);2+;[1, 1]".equals(cs.toString()) == true);
            pass();
        }
        catch (Throwable ex) {
            unexpected(ex);
        }
    }

    /** Checks normalize method.  No errors. */
/** static public void testNormalize01() throws Exception  {
        try {
            String s = null;
            VersionConstraint cs = null;

            s = "[1, 6)";
            cs = VersionConstraint.valueOf(s).normalize();
            check("[1, 6)".equals(cs.toString()) == true);
            pass();

            s = "[1, 3);[4, 6);3.*";
            cs = VersionConstraint.valueOf(s).normalize();
            check("[1, 6)".equals(cs.toString()) == true);
            pass();

            s = "[5, 6);[2, 3);[4, 5.0);[1, 2);[3.0, 4.0.0.0)";
            cs = VersionConstraint.valueOf(s).normalize();
            check("[1, 6)".equals(cs.toString()) == true);
            pass();

            s = "[1, 2);1.7.0;2.3.4;[3, 5);[2, 6)";
            cs = VersionConstraint.valueOf(s).normalize();
            check("[1, 6)".equals(cs.toString()) == true);
            pass();

            s = "1.*;[2, 2.5.6);3.*;5.*;[2.5.5, 3.8.9];(3.8.9, 4.0);4.*";
            cs = VersionConstraint.valueOf(s).normalize();
            check("[1, 6)".equals(cs.toString()) == true);
            pass();

            s = "(1, 2);(2, 3);(3, 4);(4, 5);(5, 6);[2, 2];[3, 3];[4, 4];[5, 5];[1, 1]";
            cs = VersionConstraint.valueOf(s).normalize();
            check("[1, 6)".equals(cs.toString()) == true);
            pass();

            s = "1+";
            cs = VersionConstraint.valueOf(s).normalize();
            check("1+".equals(cs.toString()) == true);
            pass();

            s = "[1, 3);[4, 6);6+;[3, 4)";
            cs = VersionConstraint.valueOf(s).normalize();
            check("1+".equals(cs.toString()) == true);
            pass();

            s = "[5, 6);[2, 3);2+;[4, 5.0);[3.0, 4.0.0.0);[1,2)";
            cs = VersionConstraint.valueOf(s).normalize();
            check("1+".equals(cs.toString()) == true);
            pass();

            s = "[1, 2);1.7.0;2.3.4+;[3, 5);[3, 6);2+";
            cs = VersionConstraint.valueOf(s).normalize();
            check("1+".equals(cs.toString()) == true);
            pass();

            s = "1.*;[2, 2.5.6);3.*;5.*;[2.5.5, 3.8.9];2.4+;(3.8.9, 4.0);4.*";
            cs = VersionConstraint.valueOf(s).normalize();
            check("1+".equals(cs.toString()) == true);
            pass();

            s = "(1, 2);(2, 3);(3, 4);(4, 5);(5, 6);2+;[1, 1]";
            cs = VersionConstraint.valueOf(s).normalize();
            check("1+".equals(cs.toString()) == true);
            pass();

            s = "[1, 3);[4, 6)";
            cs = VersionConstraint.valueOf(s).normalize();
            check("[1, 3);[4, 6)".equals(cs.toString()) == true);
            pass();

            s = "[5, 6);[2, 3);[4, 5.0);[3.0, 4.0.0.0)";
            cs = VersionConstraint.valueOf(s).normalize();
            check("[2, 6)".equals(cs.toString()) == true);
            pass();

            s = "[1, 2);1.7.0;2.3.4;[3, 5);[3, 6)";
            cs = VersionConstraint.valueOf(s).normalize();
            check("[1, 2);2.3.4;[3, 6)".equals(cs.toString()) == true);
            pass();

            s = "[1, 2);[3, 5);1.7.0;[3, 6);2.3.4";
            cs = VersionConstraint.valueOf(s).normalize();
            check("[1, 2);2.3.4;[3, 6)".equals(cs.toString()) == true);
            pass();

            s = "1.*;[2, 2.5.6);3.*;5.*;[2.5.7, 3.8.9];(3.8.9, 4.0);4.*";
            cs = VersionConstraint.valueOf(s).normalize();
            check("[1, 2.5.6);[2.5.7, 6)".equals(cs.toString()) == true);
            pass();

            s = "(1, 2);(2, 3);(3, 4);(4, 5);(5, 6)";
            cs = VersionConstraint.valueOf(s).normalize();
            check("[1.0.0.1, 2);[2.0.0.1, 3);[3.0.0.1, 4);[4.0.0.1, 5);[5.0.0.1, 6)".equals(cs.toString()) == true);
            pass();

            s = "(5, 6);(4, 5);(3, 4);(2, 3);(1, 2)";
            cs = VersionConstraint.valueOf(s).normalize();
            check("[1.0.0.1, 2);[2.0.0.1, 3);[3.0.0.1, 4);[4.0.0.1, 5);[5.0.0.1, 6)".equals(cs.toString()) == true);
            pass();
        }
        catch (Throwable ex) {
            unexpected(ex);
        }
     }
     */

    /** Checks contains method.  No errors. */
    static public void testContains01() throws Exception {
        try {
            VersionConstraint cs = VersionConstraint.valueOf("(1, 2);(2, 3);(3, 4);(4, 5);(5, 6)");
            check(cs.contains(Version.valueOf(0, 5, 0)) == false);
            check(cs.contains(Version.valueOf(1, 5, 0)) == true);
            check(cs.contains(Version.valueOf(2, 5, 0)) == true);
            check(cs.contains(Version.valueOf(3, 5, 0)) == true);
            check(cs.contains(Version.valueOf(4, 5, 0)) == true);
            check(cs.contains(Version.valueOf(5, 5, 0)) == true);
            check(cs.contains(Version.valueOf(6, 5, 0)) == false);
            check(cs.contains(Version.valueOf(0, 0, 0)) == false);
            check(cs.contains(Version.valueOf(1, 0, 0)) == false);
            check(cs.contains(Version.valueOf(2, 0, 0)) == false);
            check(cs.contains(Version.valueOf(3, 0, 0)) == false);
            check(cs.contains(Version.valueOf(4, 0, 0)) == false);
            check(cs.contains(Version.valueOf(5, 0, 0)) == false);
            check(cs.contains(Version.valueOf(6, 0, 0)) == false);
            check(cs.contains(Version.valueOf(7, 0, 0)) == false);
            pass();

/*
            check(cs.contains(VersionRange.parse("(0, 1)")) == false);
            check(cs.contains(VersionRange.parse("(1, 2)")) == true);
            check(cs.contains(VersionRange.parse("(2, 3)")) == true);
            check(cs.contains(VersionRange.parse("(3, 4)")) == true);
            check(cs.contains(VersionRange.parse("(4, 5)")) == true);
            check(cs.contains(VersionRange.parse("(5, 6)")) == true);
            check(cs.contains(VersionRange.parse("(6, 7)")) == false);
            pass();

            check(cs.contains(VersionRange.parse("[0.0.0.1, 1)")) == false);
            check(cs.contains(VersionRange.parse("[1.0.0.1, 2)")) == true);
            check(cs.contains(VersionRange.parse("[2.0.0.1, 3)")) == true);
            check(cs.contains(VersionRange.parse("[3.0.0.1, 4)")) == true);
            check(cs.contains(VersionRange.parse("[4.0.0.1, 5)")) == true);
            check(cs.contains(VersionRange.parse("[5.0.0.1, 6)")) == true);
            check(cs.contains(VersionRange.parse("[6.0.0.1, 7)")) == false);
            pass();

            check(cs.contains(VersionRange.parse("[0.2, 0.8)")) == false);
            check(cs.contains(VersionRange.parse("[1.2, 1.8)")) == true);
            check(cs.contains(VersionRange.parse("[2.2, 2.8)")) == true);
            check(cs.contains(VersionRange.parse("[3.2, 3.8)")) == true);
            check(cs.contains(VersionRange.parse("[4.2, 4.8)")) == true);
            check(cs.contains(VersionRange.parse("[5.2, 5.8)")) == true);
            check(cs.contains(VersionRange.parse("[6.2, 6.8)")) == false);
            pass();

            check(cs.contains(VersionRange.parse("[1.8, 2.2)")) == false);
            check(cs.contains(VersionRange.parse("[2.8, 3.2)")) == false);
            check(cs.contains(VersionRange.parse("[3.8, 4.2)")) == false);
            check(cs.contains(VersionRange.parse("[4.8, 5.2)")) == false);
            check(cs.contains(VersionRange.parse("[5.8, 6.2)")) == false);
            pass();
*/
        }
        catch (Throwable e) {
            unexpected(e);
        }
    }

    /** Checks intersection method.  No errors. */
    static public void testIntersection01() throws Exception {
        try {
            VersionConstraint cs1 = VersionConstraint.valueOf("1;2;3;4;5;6;7");
            VersionConstraint cs2 = VersionConstraint.valueOf("1.1;2.2;3.3;4.4;5.5;6.6");
            VersionConstraint result = null;
            check(cs1.intersection(cs2) == result);
            check(cs2.intersection(cs1) == result);
            pass();

            cs1 = VersionConstraint.valueOf("1");
            cs2 = VersionConstraint.valueOf("1");
            result = VersionConstraint.valueOf("1");
            check(cs1.intersection(cs2).equals(result) == true);
            check(cs2.intersection(cs1).equals(result) == true);
            check(result.equals(cs1.intersection(cs2)) == true);
            check(result.equals(cs2.intersection(cs1)) == true);
            pass();

            cs1 = VersionConstraint.valueOf("[1, 2)");
            cs2 = VersionConstraint.valueOf("[1, 2)");
            result = VersionConstraint.valueOf("[1, 2)");
            check(cs1.intersection(cs2).equals(result) == true);
            check(cs2.intersection(cs1).equals(result) == true);
            check(result.equals(cs1.intersection(cs2)) == true);
            check(result.equals(cs2.intersection(cs1)) == true);
            pass();

            cs1 = VersionConstraint.valueOf("1+");
            cs2 = VersionConstraint.valueOf("1+");
            result = VersionConstraint.valueOf("1+");
            check(cs1.intersection(cs2).equals(result) == true);
            check(cs2.intersection(cs1).equals(result) == true);
            check(result.equals(cs1.intersection(cs2)) == true);
            check(result.equals(cs2.intersection(cs1)) == true);
            pass();

            cs1 = VersionConstraint.valueOf("2");
            cs2 = VersionConstraint.valueOf("[1, 4)");
            result = VersionConstraint.valueOf("2");
            check(cs1.intersection(cs2).equals(result) == true);
            check(cs2.intersection(cs1).equals(result) == true);
            check(result.equals(cs1.intersection(cs2)) == true);
            check(result.equals(cs2.intersection(cs1)) == true);
            pass();

            cs1 = VersionConstraint.valueOf("[2, 5)");
            cs2 = VersionConstraint.valueOf("[1, 4)");
            result = VersionConstraint.valueOf("[2, 4)");
            check(cs1.intersection(cs2).equals(result) == true);
            check(cs2.intersection(cs1).equals(result) == true);
            check(result.equals(cs1.intersection(cs2)) == true);
            check(result.equals(cs2.intersection(cs1)) == true);
            pass();

            cs1 = VersionConstraint.valueOf("[2, 5)");
            cs2 = VersionConstraint.valueOf("1+");
            result = VersionConstraint.valueOf("[2, 5)");
            check(cs1.intersection(cs2).equals(result) == true);
            check(cs2.intersection(cs1).equals(result) == true);
            check(result.equals(cs1.intersection(cs2)) == true);
            check(result.equals(cs2.intersection(cs1)) == true);
            pass();

            cs1 = VersionConstraint.valueOf("2.3.4");
            cs2 = VersionConstraint.valueOf("1+");
            result = VersionConstraint.valueOf("2.3.4");
            check(cs1.intersection(cs2).equals(result) == true);
            check(cs2.intersection(cs1).equals(result) == true);
            check(result.equals(cs1.intersection(cs2)) == true);
            check(result.equals(cs2.intersection(cs1)) == true);
            pass();

            cs1 = VersionConstraint.valueOf("1;2;3;4;5;6;7");
            cs2 = VersionConstraint.valueOf("1;3;5;7");
            result = VersionConstraint.valueOf("1;3;5;7");
            check(cs1.intersection(cs2).equals(result) == true);
            check(cs2.intersection(cs1).equals(result) == true);
            check(result.equals(cs1.intersection(cs2)) == true);
            check(result.equals(cs2.intersection(cs1)) == true);
            pass();

            cs2 = VersionConstraint.valueOf("[1,2);[3,4);[5,6);[7,8)");
            result = VersionConstraint.valueOf("1;3;5;7");
            check(cs1.intersection(cs2).equals(result) == true);
            check(cs2.intersection(cs1).equals(result) == true);
            check(result.equals(cs1.intersection(cs2)) == true);
            check(result.equals(cs2.intersection(cs1)) == true);
            pass();

            cs1 = VersionConstraint.valueOf("[1,2);[3,4);[5,6);[7,8)");
            cs2 = VersionConstraint.valueOf("[1.5,2.5);[3.5,4.5);[5.5,6.5);[7.5,8.5)");
            result = VersionConstraint.valueOf("[1.5,2);[3.5,4);[5.5,6);[7.5,8)");
            check(cs1.intersection(cs2).equals(result) == true);
            check(cs2.intersection(cs1).equals(result) == true);
            check(result.equals(cs1.intersection(cs2)) == true);
            check(result.equals(cs2.intersection(cs1)) == true);
            pass();

            cs1 = VersionConstraint.valueOf("[1,2);2.5;[3,4);4.5;[5,6);6.5;[7,8)");
            cs2 = VersionConstraint.valueOf("[1.5,2.5);[3.5,4.5);[5.5,6.5);[7.5,8.5)");
            result = VersionConstraint.valueOf("[1.5,2);[3.5,4);[5.5,6);[7.5,8)");
            check(cs1.intersection(cs2).equals(result) == true);
            check(cs2.intersection(cs1).equals(result) == true);
            check(result.equals(cs1.intersection(cs2)) == true);
            check(result.equals(cs2.intersection(cs1)) == true);
            pass();

            cs1 = VersionConstraint.valueOf("[1,2);2.4;[3,4);4.4;[5,6);6.4;[7,8)");
            cs2 = VersionConstraint.valueOf("[1.5,2.5);[3.5,4.5);[5.5,6.5);[7.5,8.5)");
            result = VersionConstraint.valueOf("[1.5,2);2.4;[3.5,4);4.4;[5.5,6);6.4;[7.5,8)");
            check(cs1.intersection(cs2).equals(result) == true);
            check(cs2.intersection(cs1).equals(result) == true);
            check(result.equals(cs1.intersection(cs2)) == true);
            check(result.equals(cs2.intersection(cs1)) == true);
            pass();

            cs1 = VersionConstraint.valueOf("[1,2);2.4;[3,4);4.4;[5,6);6.4;[7,8)");
            cs2 = VersionConstraint.valueOf("[1.5,3.5);[3.6,4.3);[5.5,7.5)");
            result = VersionConstraint.valueOf("[1.5,2);2.4;[3,3.5);[3.6,4);[5.5,6);6.4;[7,7.5)");
            check(cs1.intersection(cs2).equals(result) == true);
            check(cs2.intersection(cs1).equals(result) == true);
            check(result.equals(cs1.intersection(cs2)) == true);
            check(result.equals(cs2.intersection(cs1)) == true);
            pass();

            cs1 = VersionConstraint.valueOf("[1,2);2.4;[3,4);4.4;5.0;[5.5,7.5);[8,9)");
            cs2 = VersionConstraint.valueOf("[1.5,3.5);[3.6,4.4);[5,6);6.4;[7,8)");
            result = VersionConstraint.valueOf("[1.5,2);2.4;[3,3.5);[3.6,4);5;[5.5,6);6.4;[7,7.5)");
            check(cs1.intersection(cs2).equals(result) == true);
            check(cs2.intersection(cs1).equals(result) == true);
            check(result.equals(cs1.intersection(cs2)) == true);
            check(result.equals(cs2.intersection(cs1)) == true);
            pass();
        }
        catch (Throwable e) {
            unexpected(e);
        }
    }

    /** Checks serialzation/de-serialization method.  No errors. */
    static public void testSerialization() throws Exception {
        try
        {
            VersionConstraint versionConstraint = VersionConstraint.valueOf("1.*;[2, 2.5.6);3.*;5.*;[2.5.5, 3.8.9];(3.8.9, 4.0);4.*;7.8.9");
            VersionConstraint versionConstraint2 = cloneVersionConstraintBySerialization(versionConstraint);
            check(versionConstraint.equals(versionConstraint2) == true);
            check(versionConstraint2.equals(versionConstraint) == true);
            check(versionConstraint.toString().equals(versionConstraint2.toString()) == true);
            check(versionConstraint.hashCode() == versionConstraint2.hashCode());
        }  catch (Throwable ex)  {
            unexpected(ex);
        }
    }

    static public VersionConstraint cloneVersionConstraintBySerialization(VersionConstraint versionConstraint) throws Exception  {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(versionConstraint);
        oos.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        return (VersionConstraint)ois.readObject();
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
