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
import java.module.Query;
import java.util.ArrayList;
import java.util.List;

/*
 * @test VersionTest.java
 * @summary Test VersionTest
 * @author Stanley M. Ho
 */

public class VersionTest {

    public static void realMain(String[] args) throws Throwable {
        testConstructor01();
        testConstructor02();
        testQualifier01();
        testQualifier02();
//        testIsVersion01();
        testEqual01();
        testHashCode01();
        testParse01();
        testParse02();
        testCompareTo01();
        testToString01();
        testSerialization();
    }

    /** Checks basics of construction.  No errors. */
    static public void testConstructor01() throws Exception {
        try {
            // Test constructors that take no qualifer
            Version version = Version.valueOf(0, 0, 0);
            check(version.getMajorNumber() == 0);
            check(version.getMinorNumber() == 0);
            check(version.getMicroNumber() == 0);
            check(version.getUpdateNumber() == 0);
            check(version.getQualifier() == null);
            pass();

            version = Version.valueOf(1, 2, 3);
            check(version.getMajorNumber() == 1);
            check(version.getMinorNumber() == 2);
            check(version.getMicroNumber() == 3);
            check(version.getUpdateNumber() == 0);
            check(version.getQualifier() == null);
            pass();

            version = Version.valueOf(9, 8, 7);
            check(version.getMajorNumber() == 9);
            check(version.getMinorNumber() == 8);
            check(version.getMicroNumber() == 7);
            check(version.getUpdateNumber() == 0);
            check(version.getQualifier() == null);
            pass();

            version = Version.valueOf(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
            check(version.getMajorNumber() == Integer.MAX_VALUE);
            check(version.getMinorNumber() == Integer.MAX_VALUE);
            check(version.getMicroNumber() == Integer.MAX_VALUE);
            check(version.getUpdateNumber() == 0);
            check(version.getQualifier() == null);
            pass();

            version = Version.valueOf(0, 0, 0, 0);
            check(version.getMajorNumber() == 0);
            check(version.getMinorNumber() == 0);
            check(version.getMicroNumber() == 0);
            check(version.getUpdateNumber() == 0);
            check(version.getQualifier() == null);
            pass();

            version = Version.valueOf(1, 2, 3, 4);
            check(version.getMajorNumber() == 1);
            check(version.getMinorNumber() == 2);
            check(version.getMicroNumber() == 3);
            check(version.getUpdateNumber() == 4);
            check(version.getQualifier() == null);
            pass();

            version = Version.valueOf(9, 8, 7, 6);
            check(version.getMajorNumber() == 9);
            check(version.getMinorNumber() == 8);
            check(version.getMicroNumber() == 7);
            check(version.getUpdateNumber() == 6);
            check(version.getQualifier() == null);
            pass();

            version = Version.valueOf(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
            check(version.getMajorNumber() == Integer.MAX_VALUE);
            check(version.getMinorNumber() == Integer.MAX_VALUE);
            check(version.getMicroNumber() == Integer.MAX_VALUE);
            check(version.getUpdateNumber() == Integer.MAX_VALUE);
            check(version.getQualifier() == null);
            pass();

            // Test constructors that take null qualifer
            String qualifier = null;
            version = Version.valueOf(0, 0, 0, qualifier);
            check(version.getMajorNumber() == 0);
            check(version.getMinorNumber() == 0);
            check(version.getMicroNumber() == 0);
            check(version.getUpdateNumber() == 0);
            check(version.getQualifier() == null);
            pass();

            version = Version.valueOf(1, 2, 3, qualifier);
            check(version.getMajorNumber() == 1);
            check(version.getMinorNumber() == 2);
            check(version.getMicroNumber() == 3);
            check(version.getUpdateNumber() == 0);
            check(version.getQualifier() == null);
            pass();

            version = Version.valueOf(9, 8, 7, qualifier);
            check(version.getMajorNumber() == 9);
            check(version.getMinorNumber() == 8);
            check(version.getMicroNumber() == 7);
            check(version.getUpdateNumber() == 0);
            check(version.getQualifier() == null);
            pass();

            version = Version.valueOf(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, qualifier);
            check(version.getMajorNumber() == Integer.MAX_VALUE);
            check(version.getMinorNumber() == Integer.MAX_VALUE);
            check(version.getMicroNumber() == Integer.MAX_VALUE);
            check(version.getUpdateNumber() == 0);
            check(version.getQualifier() == null);
            pass();

            version = Version.valueOf(0, 0, 0, 0, qualifier);
            check(version.getMajorNumber() == 0);
            check(version.getMinorNumber() == 0);
            check(version.getMicroNumber() == 0);
            check(version.getUpdateNumber() == 0);
            check(version.getQualifier() == null);
            pass();

            version = Version.valueOf(1, 2, 3, 4, qualifier);
            check(version.getMajorNumber() == 1);
            check(version.getMinorNumber() == 2);
            check(version.getMicroNumber() == 3);
            check(version.getUpdateNumber() == 4);
            check(version.getQualifier() == null);
            pass();

            version = Version.valueOf(9, 8, 7, 6, qualifier);
            check(version.getMajorNumber() == 9);
            check(version.getMinorNumber() == 8);
            check(version.getMicroNumber() == 7);
            check(version.getUpdateNumber() == 6);
            check(version.getQualifier() == null);
            pass();

            version = Version.valueOf(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, qualifier);
            check(version.getMajorNumber() == Integer.MAX_VALUE);
            check(version.getMinorNumber() == Integer.MAX_VALUE);
            check(version.getMicroNumber() == Integer.MAX_VALUE);
            check(version.getUpdateNumber() == Integer.MAX_VALUE);
            check(version.getQualifier() == null);
            pass();

            // Test constructors that take full qualifier
            qualifier = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_0123456789";
            version = Version.valueOf(0, 0, 0, qualifier);
            check(version.getMajorNumber() == 0);
            check(version.getMinorNumber() == 0);
            check(version.getMicroNumber() == 0);
            check(version.getUpdateNumber() == 0);
            check(version.getQualifier().equals(qualifier));
            pass();

            version = Version.valueOf(1, 2, 3, qualifier);
            check(version.getMajorNumber() == 1);
            check(version.getMinorNumber() == 2);
            check(version.getMicroNumber() == 3);
            check(version.getUpdateNumber() == 0);
            check(version.getQualifier().equals(qualifier));
            pass();

            version = Version.valueOf(9, 8, 7, qualifier);
            check(version.getMajorNumber() == 9);
            check(version.getMinorNumber() == 8);
            check(version.getMicroNumber() == 7);
            check(version.getUpdateNumber() == 0);
            check(version.getQualifier().equals(qualifier));
            pass();

            version = Version.valueOf(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, qualifier);
            check(version.getMajorNumber() == Integer.MAX_VALUE);
            check(version.getMinorNumber() == Integer.MAX_VALUE);
            check(version.getMicroNumber() == Integer.MAX_VALUE);
            check(version.getUpdateNumber() == 0);
            check(version.getQualifier().equals(qualifier));
            pass();

            version = Version.valueOf(0, 0, 0, 0, qualifier);
            check(version.getMajorNumber() == 0);
            check(version.getMinorNumber() == 0);
            check(version.getMicroNumber() == 0);
            check(version.getUpdateNumber() == 0);
            check(version.getQualifier().equals(qualifier));
            pass();

            version = Version.valueOf(1, 2, 3, 4, qualifier);
            check(version.getMajorNumber() == 1);
            check(version.getMinorNumber() == 2);
            check(version.getMicroNumber() == 3);
            check(version.getUpdateNumber() == 4);
            check(version.getQualifier().equals(qualifier));
            pass();

            version = Version.valueOf(9, 8, 7, 6, qualifier);
            check(version.getMajorNumber() == 9);
            check(version.getMinorNumber() == 8);
            check(version.getMicroNumber() == 7);
            check(version.getUpdateNumber() == 6);
            check(version.getQualifier().equals(qualifier));
            pass();

            version = Version.valueOf(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, qualifier);
            check(version.getMajorNumber() == Integer.MAX_VALUE);
            check(version.getMinorNumber() == Integer.MAX_VALUE);
            check(version.getMicroNumber() == Integer.MAX_VALUE);
            check(version.getUpdateNumber() == Integer.MAX_VALUE);
            check(version.getQualifier().equals(qualifier));
            pass();
        }
        catch (Throwable ex) {
            unexpected(ex);
        }
    }

    /** Checks error: major, minor, micro, update is negative. */
    static public void testConstructor02() throws Exception {
        try {
            Version.valueOf(-1, 0, 0);
            fail();
        } catch (IllegalArgumentException ex) {
            pass();
        }
        try {
            Version.valueOf(0, -1, 0);
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        try {
            Version.valueOf(0, 0, -1);
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        try {
            Version.valueOf(Integer.MIN_VALUE, 0, 0);
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        try {
            Version.valueOf(0, Integer.MIN_VALUE, 0);
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        try {
            Version.valueOf(0, 0, Integer.MIN_VALUE);
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        try {
            Version.valueOf(-1, 0, 0, 0);
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        try {
            Version.valueOf(0, -1, 0, 0);
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        try {
            Version.valueOf(0, 0, -1, 0);
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        try {
            Version.valueOf(0, 0, 0, -1);
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        try {
            Version.valueOf(Integer.MIN_VALUE, 0, 0, 0);
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        try {
            Version.valueOf(0, Integer.MIN_VALUE, 0, 0);
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        try {
            Version.valueOf(0, 0, Integer.MIN_VALUE, 0);
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        try {
            Version.valueOf(0, 0, 0, Integer.MIN_VALUE);
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
    }

    /** Checks basics of qualifier.  No errors. */
    static public void testQualifier01() throws Exception {
        try {
            String qualifier = "xyz";
            Version version = Version.valueOf(0, 0, 0, qualifier);
            check(version.getQualifier().equals(qualifier));
            version = Version.valueOf(1, 2, 3, 4, qualifier);
            check(version.getQualifier().equals(qualifier));
            pass();

            qualifier = "-xyz-";
            version = Version.valueOf(0, 0, 0, qualifier);
            check(version.getQualifier().equals(qualifier));
            version = Version.valueOf(1, 2, 3, 4, qualifier);
            check(version.getQualifier().equals(qualifier));
            pass();

            qualifier = "_xyz_";
            version = Version.valueOf(0, 0, 0, qualifier);
            check(version.getQualifier().equals(qualifier));
            version = Version.valueOf(1, 2, 3, 4, qualifier);
            check(version.getQualifier().equals(qualifier));
            pass();

            qualifier = "---___xyz___---xyz___abc---abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            version = Version.valueOf(0, 0, 0, qualifier);
            check(version.getQualifier().equals(qualifier));
            version = Version.valueOf(1, 2, 3, 4, qualifier);
            check(version.getQualifier().equals(qualifier));
            pass();
        }
        catch (Throwable ex) {
            unexpected(ex);
        }
    }

    /** Checks error: qualifier contains illegal character. */
    static public void testQualifier02() throws Exception {
        for (int i = (int)Character.MIN_VALUE; i <= (int)Character.MAX_VALUE; i++) {
            char c = (char) i;
            if (Character.isLetterOrDigit(c) == false && c != '_' && c != '-') {
                try {
                    Version.valueOf(0, 0, 0, "" + c);
                    fail();
                }
                catch (IllegalArgumentException ex) {
                    pass();
                }
                try {
                    Version.valueOf(0, 0, 0, 0, "" + c);
                    fail();
                }
                catch (IllegalArgumentException ex) {
                    pass();
                }
            }
        }
    }

    /** Checks isVersion method.  No errors. */
/*    static public void testIsVersion01() throws Exception {

        try {
            check(Version.isVersion("1") == true);
            pass();

            check(Version.isVersion("1.2") == true);
            pass();

            check(Version.isVersion("1.2.3") == true);
            pass();

            check(Version.isVersion("1.2.3.4") == true);
            pass();

            check(Version.isVersion("1.2.3.4.5") == false);
            pass();

            check(Version.isVersion("1.2.3.4.5.6") == false);
            pass();

            check(Version.isVersion("1.2.3.4-abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890-_") == true);
            pass();

            check(Version.isVersion("1.2.3.4--_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890") == true);
            pass();

            check(Version.isVersion("1.2.3.4-_abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890") == true);
            pass();

            check(Version.isVersion("1.2.3.4.5-abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890-_") == false);
            pass();

            check(Version.isVersion("1.2.3.4.5.6-abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890-_") == false);
            pass();

            check(Version.isVersion("1.2.3.4.5.6-abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890-_") == false);
            pass();

            check(Version.isVersion("1.") == false);
            pass();

            check(Version.isVersion("1.2.") == false);
            pass();

            check(Version.isVersion("1.2.3.") == false);
            pass();

            check(Version.isVersion("1.2.3.4-") == false);
            pass();

            check(Version.isVersion("1+") == false);
            pass();

            check(Version.isVersion("1.2+") == false);
            pass();

            check(Version.isVersion("1.2.3+") == false);
            pass();

            check(Version.isVersion("1.2.3.4+") == false);
            pass();

            check(Version.isVersion("1.2.3.4.5+") == false);
            pass();

            check(Version.isVersion("1.*") == false);
            pass();

            check(Version.isVersion("1.2.*") == false);
            pass();

            check(Version.isVersion("1.2.3.*") == false);
            pass();

            check(Version.isVersion("1.2.3.4.*") == false);
            pass();

            check(Version.isVersion("[1.0, 2.0)") == false);
            pass();

            check(Version.isVersion("[1.0, 2.0]") == false);
            pass();

            check(Version.isVersion("(1.0, 2.0]") == false);
            pass();

            check(Version.isVersion("(1.0, 2.0)") == false);
            pass();

            check(Version.isVersion("01") == true);
            pass();

            check(Version.isVersion("1.02") == true);
            pass();

            check(Version.isVersion("1.2.03") == true);
            pass();

            check(Version.isVersion("1.2.3.04") == true);
            pass();
        }
        catch (Throwable ex) {
            unexpected(ex);
        }
    }
*/
    /** Checks equals method.  No errors. */
    static public void testEqual01() throws Exception {
        Version[] array = new Version[6];
        array[0] = Version.valueOf(1, 2, 3, 4);
        array[1] = Version.valueOf(1, 6, 7, 8);
        array[2] = Version.valueOf(5, 2, 7, 8);
        array[3] = Version.valueOf(5, 6, 3, 8);
        array[4] = Version.valueOf(5, 6, 7, 4);
        array[5] = Version.valueOf(5, 6, 7, 8);

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

        String qualifier = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_0123456789";
        array[0] = Version.valueOf(1, 2, 3, 4, qualifier);
        array[1] = Version.valueOf(1, 6, 7, 8, qualifier);
        array[2] = Version.valueOf(5, 2, 7, 8, qualifier);
        array[3] = Version.valueOf(5, 6, 3, 8, qualifier);
        array[4] = Version.valueOf(5, 6, 7, 4, qualifier);
        array[5] = Version.valueOf(5, 6, 7, 8, qualifier);

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

    /** Checks hashCode method.  No errors. */
    static public void testHashCode01() throws Exception {
        Version[] array1 = new Version[6];
        Version[] array2 = new Version[6];
        array1[0] = Version.valueOf(1, 2, 3, 4);
        array1[1] = Version.valueOf(1, 6, 7, 8);
        array1[2] = Version.valueOf(5, 2, 7, 8);
        array1[3] = Version.valueOf(5, 6, 3, 8);
        array1[4] = Version.valueOf(5, 6, 7, 4);
        array1[5] = Version.valueOf(5, 6, 7, 8);
        array2[0] = Version.valueOf(1, 2, 3, 4);
        array2[1] = Version.valueOf(1, 6, 7, 8);
        array2[2] = Version.valueOf(5, 2, 7, 8);
        array2[3] = Version.valueOf(5, 6, 3, 8);
        array2[4] = Version.valueOf(5, 6, 7, 4);
        array2[5] = Version.valueOf(5, 6, 7, 8);

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

        String qualifier = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_0123456789";
        array1[0] = Version.valueOf(1, 2, 3, 4, qualifier);
        array1[1] = Version.valueOf(1, 6, 7, 8, qualifier);
        array1[2] = Version.valueOf(5, 2, 7, 8, qualifier);
        array1[3] = Version.valueOf(5, 6, 3, 8, qualifier);
        array1[4] = Version.valueOf(5, 6, 7, 4, qualifier);
        array1[5] = Version.valueOf(5, 6, 7, 8, qualifier);
        array2[0] = Version.valueOf(1, 2, 3, 4, qualifier);
        array2[1] = Version.valueOf(1, 6, 7, 8, qualifier);
        array2[2] = Version.valueOf(5, 2, 7, 8, qualifier);
        array2[3] = Version.valueOf(5, 6, 3, 8, qualifier);
        array2[4] = Version.valueOf(5, 6, 7, 4, qualifier);
        array2[5] = Version.valueOf(5, 6, 7, 8, qualifier);

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

    /** Checks parse method.  No errors. */
    static public void testParse01() throws Exception {
        try {
            Version version = Version.valueOf("1");
            check(version.equals(Version.valueOf(1, 0, 0)));
            pass();

            version = Version.valueOf("1.2");
            check(version.equals(Version.valueOf(1, 2, 0)));
            pass();

            version = Version.valueOf("1.2.3");
            check(version.equals(Version.valueOf(1, 2, 3)));
            pass();

            version = Version.valueOf("1.2.3.4");
            check(version.equals(Version.valueOf(1, 2, 3, 4)));
            pass();

            version = Version.valueOf("01");
            check(version.equals(Version.valueOf(1, 0, 0)));
            pass();

            version = Version.valueOf("1.02");
            check(version.equals(Version.valueOf(1, 2, 0)));
            pass();

            version = Version.valueOf("1.2.03");
            check(version.equals(Version.valueOf(1, 2, 3)));
            pass();

            version = Version.valueOf("1.2.3.04");
            check(version.equals(Version.valueOf(1, 2, 3, 4)));
            pass();

            version = Version.valueOf("1-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_");
            check(version.equals(Version.valueOf(1, 0, 0, "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_")));
            pass();

            version = Version.valueOf("1.2-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_");
            check(version.equals(Version.valueOf(1, 2, 0, "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_")));
            pass();

            version = Version.valueOf("1.2.3-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_");
            check(version.equals(Version.valueOf(1, 2, 3, "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_")));
            pass();

            version = Version.valueOf("1.2.3.4-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_");
            check(version.equals(Version.valueOf(1, 2, 3, 4, "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_")));
            pass();
        }
        catch (Throwable ex) {
            unexpected(ex);
        }
    }

    /** Checks errors: version format is malformed. */
    static public void testParse02() throws Exception {
        try {
            Version version = Version.valueOf("1.");
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        catch (Throwable e) {
            unexpected(e);
        }


        try {
            Version version = Version.valueOf("1.2.");
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        catch (Throwable e) {
            unexpected(e);
        }


        try {
            Version version = Version.valueOf("1.2.3.");
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        catch (Throwable e) {
            unexpected(e);
        }

        try {
            Version version = Version.valueOf("1.2.3.4.");
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        catch (Throwable e) {
            unexpected(e);
        }

        try {
            Version version = Version.valueOf("1.beta");
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        catch (Throwable e) {
            unexpected(e);
        }

        try {
            Version version = Version.valueOf("1.2.beta");
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        catch (Throwable e) {
            unexpected(e);
        }

        try {
            Version version = Version.valueOf("1.2.3.beta");
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        catch (Throwable e) {
            unexpected(e);
        }

        try {
            Version version = Version.valueOf("1.2.3.4.beta");
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        catch (Throwable e) {
            unexpected(e);
        }

        try {
            Version version = Version.valueOf("1.2.3.4-");
            fail();
        }
        catch (IllegalArgumentException ex) {
            pass();
        }
        catch (Throwable e) {
            unexpected(e);
        }
    }

    /** Checks compareTo method.  No errors. */
    static public void testCompareTo01() throws Exception {
        try {
            Version[] array = new Version[6];
            array[0] = Version.valueOf(1, 2, 3, 4);
            array[1] = Version.valueOf(1, 6, 7, 8);
            array[2] = Version.valueOf(5, 2, 7, 8);
            array[3] = Version.valueOf(5, 6, 3, 8);
            array[4] = Version.valueOf(5, 6, 7, 4);
            array[5] = Version.valueOf(5, 6, 7, 8);

            for (int i=0 ; i < array.length; i++) {
                for (int j=0 ; j < array.length; j++) {
                    if (i == j) {
                        check(array[i].compareTo(array[j]) == 0);
                    }
                    else if (i < j) {
                        check(array[i].compareTo(array[j]) < 0);
                        check(array[j].compareTo(array[i]) > 0);
                    }
                    else {
                        check(array[i].compareTo(array[j]) > 0);
                        check(array[j].compareTo(array[i]) < 0);
                    }
                }
            }
            pass();

            String qualifier = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_0123456789";
            array[0] = Version.valueOf(1, 2, 3, 4, qualifier);
            array[1] = Version.valueOf(1, 6, 7, 8, qualifier);
            array[2] = Version.valueOf(5, 2, 7, 8, qualifier);
            array[3] = Version.valueOf(5, 6, 3, 8, qualifier);
            array[4] = Version.valueOf(5, 6, 7, 4, qualifier);
            array[5] = Version.valueOf(5, 6, 7, 8, qualifier);

            for (int i=0 ; i < array.length; i++) {
                for (int j=0 ; j < array.length; j++) {
                    if (i == j) {
                        check(array[i].compareTo(array[j]) == 0);
                    }
                    else if (i < j) {
                        check(array[i].compareTo(array[j]) < 0);
                        check(array[j].compareTo(array[i]) > 0);
                    }
                    else {
                        check(array[i].compareTo(array[j]) > 0);
                        check(array[j].compareTo(array[i]) < 0);
                    }
                }
            }
            pass();

            array[0] = Version.valueOf(1, 2, 3, 4, "1234");
            array[1] = Version.valueOf(1, 2, 3, 4, "12340");
            array[2] = Version.valueOf(1, 2, 3, 4, "234");
            array[3] = Version.valueOf(1, 2, 3, 4, "beta");
            array[4] = Version.valueOf(1, 2, 3, 4, "ea");
            array[5] = Version.valueOf(1, 2, 3, 4);

            for (int i=0 ; i < array.length; i++) {
                for (int j=0 ; j < array.length; j++) {
                    if (i == j) {
                        check(array[i].compareTo(array[j]) == 0);
                    }
                    else if (i < j) {
                        check(array[i].compareTo(array[j]) < 0);
                        check(array[j].compareTo(array[i]) > 0);
                    }
                    else {
                        check(array[i].compareTo(array[j]) > 0);
                        check(array[j].compareTo(array[i]) < 0);
                    }
                }
            }
            pass();
        }
        catch (Throwable ex) {
            unexpected(ex);
        }
    }

    /** Checks toString method.  No errors. */
    static public void testToString01() throws Exception {
        try {
            Version version = Version.valueOf(0, 0, 0);
            check(version.toString().equals("0.0"));
            pass();

            version = Version.valueOf(1, 2, 3);

            System.out.println(version.toString());

            check(version.toString().equals("1.2.3"));
            pass();

            version = Version.valueOf(1, 2, 3, 4);
            check(version.toString().equals("1.2.3.4"));
            pass();

            String qualifier = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_0123456789";
            version = Version.valueOf(1, 2, 3, qualifier);
            check(version.toString().equals("1.2.3-" + qualifier));
            pass();

            version = Version.valueOf(1, 2, 3, 4, qualifier);
            check(version.toString().equals("1.2.3.4-" + qualifier));
            pass();
        }
        catch (Throwable ex) {
            unexpected(ex);
        }
    }

    /** Checks serialzation/de-serialization method.  No errors. */
    static public void testSerialization() throws Exception {
        try {
            Version version = Version.valueOf(1, 7, 8, 9);
            Version version2 = cloneVersionBySerialization(version);
            check(version.equals(version2) == true);
            check(version2.equals(version) == true);
            check(version.toString().equals(version2.toString()) == true);
            check(version.hashCode() == version2.hashCode());
        }  catch (Throwable ex)  {
            unexpected(ex);
        }
    }

    static public Version cloneVersionBySerialization(Version version) throws Exception  {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(version);
        oos.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        return (Version)ois.readObject();
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
