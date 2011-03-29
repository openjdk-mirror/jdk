// This file was generated AUTOMATICALLY from a template file Tue Mar 29 05:26:54 GMT 2011
/*
 * Copyright 2003-2006 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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

package build.tools.spp;

/** The CharacterData class encapsulates the large tables once found in
 *  java.lang.Character. 
 */

class CharacterData01 extends CharacterData {
    /* The character properties are currently encoded into 32 bits in the following manner:
        1 bit   mirrored property
        4 bits  directionality property
        9 bits  signed offset used for converting case
        1 bit   if 1, adding the signed offset converts the character to lowercase
        1 bit   if 1, subtracting the signed offset converts the character to uppercase
        1 bit   if 1, this character has a titlecase equivalent (possibly itself)
        3 bits  0  may not be part of an identifier
                1  ignorable control; may continue a Unicode identifier or Java identifier
                2  may continue a Java identifier but not a Unicode identifier (unused)
                3  may continue a Unicode identifier or Java identifier
                4  is a Java whitespace character
                5  may start or continue a Java identifier;
                   may continue but not start a Unicode identifier (underscores)
                6  may start or continue a Java identifier but not a Unicode identifier ($)
                7  may start or continue a Unicode identifier or Java identifier
                Thus:
                   5, 6, 7 may start a Java identifier
                   1, 2, 3, 5, 6, 7 may continue a Java identifier
                   7 may start a Unicode identifier
                   1, 3, 5, 7 may continue a Unicode identifier
                   1 is ignorable within an identifier
                   4 is Java whitespace
        2 bits  0  this character has no numeric property
                1  adding the digit offset to the character code and then
                   masking with 0x1F will produce the desired numeric value
                2  this character has a "strange" numeric value
                3  a Java supradecimal digit: adding the digit offset to the
                   character code, then masking with 0x1F, then adding 10
                   will produce the desired numeric value
        5 bits  digit offset
        5 bits  character type

        The encoding of character properties is subject to change at any time.
     */

    int getProperties(int ch) {
        char offset = (char)ch;
        int props = A[Y[(X[offset>>5]<<4)|((offset>>1)&0xF)]|(offset&0x1)];
        return props;
    }

    int getType(int ch) {
        int props = getProperties(ch);
        return (props & 0x1F);
    }

    boolean isJavaIdentifierStart(int ch) {
        int props = getProperties(ch);
        return ((props & 0x00007000) >= 0x00005000);
    }

    boolean isJavaIdentifierPart(int ch) {
        int props = getProperties(ch);
        return ((props & 0x00003000) != 0);
    }

    boolean isUnicodeIdentifierStart(int ch) {
        int props = getProperties(ch);
        return ((props & 0x00007000) == 0x00007000);
    }

    boolean isUnicodeIdentifierPart(int ch) {
        int props = getProperties(ch);
        return ((props & 0x00001000) != 0);
    }

    boolean isIdentifierIgnorable(int ch) {
        int props = getProperties(ch);
        return ((props & 0x00007000) == 0x00001000);
    }

    int toLowerCase(int ch) {
        int mapChar = ch;
        int val = getProperties(ch);

        if ((val & 0x00020000) != 0) {
            int offset = val << 5 >> (5+18);
            mapChar = ch + offset;
        }
        return  mapChar;
    }

    int toUpperCase(int ch) {
        int mapChar = ch;
        int val = getProperties(ch);

        if ((val & 0x00010000) != 0) {
            int offset = val  << 5 >> (5+18);
            mapChar =  ch - offset;
        }
        return  mapChar;
    }

    int toTitleCase(int ch) {
        int mapChar = ch;
        int val = getProperties(ch);

        if ((val & 0x00008000) != 0) {
            // There is a titlecase equivalent.  Perform further checks:
            if ((val & 0x00010000) == 0) {
                // The character does not have an uppercase equivalent, so it must
                // already be uppercase; so add 1 to get the titlecase form.
                mapChar = ch + 1;
            }
            else if ((val & 0x00020000) == 0) {
                // The character does not have a lowercase equivalent, so it must
                // already be lowercase; so subtract 1 to get the titlecase form.
                mapChar = ch - 1;
            }
            // else {
            // The character has both an uppercase equivalent and a lowercase
            // equivalent, so it must itself be a titlecase form; return it.
            // return ch;
            //}
        }
        else if ((val & 0x00010000) != 0) {
            // This character has no titlecase equivalent but it does have an
            // uppercase equivalent, so use that (subtract the signed case offset).
            mapChar = toUpperCase(ch);
        }
        return  mapChar;
    }

    int digit(int ch, int radix) {
        int value = -1;
        if (radix >= Character.MIN_RADIX && radix <= Character.MAX_RADIX) {
            int val = getProperties(ch);
            int kind = val & 0x1F;
            if (kind == Character.DECIMAL_DIGIT_NUMBER) {
                value = ch + ((val & 0x3E0) >> 5) & 0x1F;
            }
            else if ((val & 0xC00) == 0x00000C00) {
                // Java supradecimal digit
                value = (ch + ((val & 0x3E0) >> 5) & 0x1F) + 10;
            }
        }
        return (value < radix) ? value : -1;
    }

    int getNumericValue(int ch) {
        int val = getProperties(ch);
        int retval = -1;

        switch (val & 0xC00) {
        default: // cannot occur
        case (0x00000000):         // not numeric
            retval = -1;
            break;
        case (0x00000400):              // simple numeric
            retval = ch + ((val & 0x3E0) >> 5) & 0x1F;
            break;
        case (0x00000800)      :       // "strange" numeric
            switch(ch) {
            case 0x10113: retval = 40; break;      // AEGEAN NUMBER FORTY
            case 0x10114: retval = 50; break;      // AEGEAN NUMBER FIFTY
            case 0x10115: retval = 60; break;      // AEGEAN NUMBER SIXTY
            case 0x10116: retval = 70; break;      // AEGEAN NUMBER SEVENTY
            case 0x10117: retval = 80; break;      // AEGEAN NUMBER EIGHTY
            case 0x10118: retval = 90; break;      // AEGEAN NUMBER NINETY
            case 0x10119: retval = 100; break;     // AEGEAN NUMBER ONE HUNDRED
            case 0x1011A: retval = 200; break;     // AEGEAN NUMBER TWO HUNDRED
            case 0x1011B: retval = 300; break;     // AEGEAN NUMBER THREE HUNDRED
            case 0x1011C: retval = 400; break;     // AEGEAN NUMBER FOUR HUNDRED
            case 0x1011D: retval = 500; break;     // AEGEAN NUMBER FIVE HUNDRED
            case 0x1011E: retval = 600; break;     // AEGEAN NUMBER SIX HUNDRED
            case 0x1011F: retval = 700; break;     // AEGEAN NUMBER SEVEN HUNDRED
            case 0x10120: retval = 800; break;     // AEGEAN NUMBER EIGHT HUNDRED
            case 0x10121: retval = 900; break;     // AEGEAN NUMBER NINE HUNDRED
            case 0x10122: retval = 1000; break;    // AEGEAN NUMBER ONE THOUSAND
            case 0x10123: retval = 2000; break;    // AEGEAN NUMBER TWO THOUSAND
            case 0x10124: retval = 3000; break;    // AEGEAN NUMBER THREE THOUSAND
            case 0x10125: retval = 4000; break;    // AEGEAN NUMBER FOUR THOUSAND
            case 0x10126: retval = 5000; break;    // AEGEAN NUMBER FIVE THOUSAND
            case 0x10127: retval = 6000; break;    // AEGEAN NUMBER SIX THOUSAND
            case 0x10128: retval = 7000; break;    // AEGEAN NUMBER SEVEN THOUSAND
            case 0x10129: retval = 8000; break;    // AEGEAN NUMBER EIGHT THOUSAND
            case 0x1012A: retval = 9000; break;    // AEGEAN NUMBER NINE THOUSAND
            case 0x1012B: retval = 10000; break;   // AEGEAN NUMBER TEN THOUSAND
            case 0x1012C: retval = 20000; break;   // AEGEAN NUMBER TWENTY THOUSAND
            case 0x1012D: retval = 30000; break;   // AEGEAN NUMBER THIRTY THOUSAND
            case 0x1012E: retval = 40000; break;   // AEGEAN NUMBER FORTY THOUSAND
            case 0x1012F: retval = 50000; break;   // AEGEAN NUMBER FIFTY THOUSAND
            case 0x10130: retval = 60000; break;   // AEGEAN NUMBER SIXTY THOUSAND
            case 0x10131: retval = 70000; break;   // AEGEAN NUMBER SEVENTY THOUSAND
            case 0x10132: retval = 80000; break;   // AEGEAN NUMBER EIGHTY THOUSAND
            case 0x10133: retval = 90000; break;   // AEGEAN NUMBER NINETY THOUSAND
            case 0x10323: retval = 50; break;      // OLD ITALIC NUMERAL FIFTY

            case 0x010144: retval = 50; break;     // ACROPHONIC ATTIC FIFTY
            case 0x010145: retval = 500; break;    // ACROPHONIC ATTIC FIVE HUNDRED
            case 0x010146: retval = 5000; break;   // ACROPHONIC ATTIC FIVE THOUSAND
            case 0x010147: retval = 50000; break;  // ACROPHONIC ATTIC FIFTY THOUSAND
            case 0x01014A: retval = 50; break;     // ACROPHONIC ATTIC FIFTY TALENTS
            case 0x01014B: retval = 100; break;    // ACROPHONIC ATTIC ONE HUNDRED TALENTS
            case 0x01014C: retval = 500; break;    // ACROPHONIC ATTIC FIVE HUNDRED TALENTS
            case 0x01014D: retval = 1000; break;   // ACROPHONIC ATTIC ONE THOUSAND TALENTS
            case 0x01014E: retval = 5000; break;   // ACROPHONIC ATTIC FIVE THOUSAND TALENTS
            case 0x010151: retval = 50; break;     // ACROPHONIC ATTIC FIFTY STATERS
            case 0x010152: retval = 100; break;    // ACROPHONIC ATTIC ONE HUNDRED STATERS
            case 0x010153: retval = 500; break;    // ACROPHONIC ATTIC FIVE HUNDRED STATERS
            case 0x010154: retval = 1000; break;   // ACROPHONIC ATTIC ONE THOUSAND STATERS
            case 0x010155: retval = 10000; break;  // ACROPHONIC ATTIC TEN THOUSAND STATERS
            case 0x010156: retval = 50000; break;  // ACROPHONIC ATTIC FIFTY THOUSAND STATERS
            case 0x010166: retval = 50; break;     // ACROPHONIC TROEZENIAN FIFTY
            case 0x010167: retval = 50; break;     // ACROPHONIC TROEZENIAN FIFTY ALTERNATE FORM
            case 0x010168: retval = 50; break;     // ACROPHONIC HERMIONIAN FIFTY
            case 0x010169: retval = 50; break;     // ACROPHONIC THESPIAN FIFTY
            case 0x01016A: retval = 100; break;    // ACROPHONIC THESPIAN ONE HUNDRED
            case 0x01016B: retval = 300; break;    // ACROPHONIC THESPIAN THREE HUNDRED
            case 0x01016C: retval = 500; break;    // ACROPHONIC EPIDAUREAN FIVE HUNDRED
            case 0x01016D: retval = 500; break;    // ACROPHONIC TROEZENIAN FIVE HUNDRED
            case 0x01016E: retval = 500; break;    // ACROPHONIC THESPIAN FIVE HUNDRED
            case 0x01016F: retval = 500; break;    // ACROPHONIC CARYSTIAN FIVE HUNDRED
            case 0x010170: retval = 500; break;    // ACROPHONIC NAXIAN FIVE HUNDRED
            case 0x010171: retval = 1000; break;   // ACROPHONIC THESPIAN ONE THOUSAND
            case 0x010172: retval = 5000; break;   // ACROPHONIC THESPIAN FIVE THOUSAND
            case 0x010174: retval = 50; break;     // ACROPHONIC STRATIAN FIFTY MNAS
            case 0x010341: retval = 90; break;     // GOTHIC LETTER NINETY
            case 0x01034A: retval = 900; break;    // GOTHIC LETTER NINE HUNDRED
            case 0x0103D5: retval = 100; break;    // OLD PERSIAN NUMBER HUNDRED
            case 0x010919: retval = 100; break;    // PHOENICIAN NUMBER ONE HUNDRED
            case 0x010A46: retval = 100; break;    // KHAROSHTHI NUMBER ONE HUNDRED
            case 0x010A47: retval = 1000; break;   // KHAROSHTHI NUMBER ONE THOUSAND
            case 0x01D36C: retval = 40; break;     // COUNTING ROD TENS DIGIT FOUR
            case 0x01D36D: retval = 50; break;     // COUNTING ROD TENS DIGIT FIVE
            case 0x01D36E: retval = 60; break;     // COUNTING ROD TENS DIGIT SIX
            case 0x01D36F: retval = 70; break;     // COUNTING ROD TENS DIGIT SEVEN
            case 0x01D370: retval = 80; break;     // COUNTING ROD TENS DIGIT EIGHT
            case 0x01D371: retval = 90; break;     // COUNTING ROD TENS DIGIT NINE
            default: retval = -2; break;
            }
            
            break;
        case (0x00000C00):           // Java supradecimal
            retval = (ch + ((val & 0x3E0) >> 5) & 0x1F) + 10;
            break;
        }
        return retval;
    }

    boolean isWhitespace(int ch) {
        int props = getProperties(ch);
        return ((props & 0x00007000) == 0x00004000);
    }

    byte getDirectionality(int ch) {
        int val = getProperties(ch);
        byte directionality = (byte)((val & 0x78000000) >> 27);
        if (directionality == 0xF ) {
            directionality = Character.DIRECTIONALITY_UNDEFINED;
        }
        return directionality;
    }

    boolean isMirrored(int ch) {
        int props = getProperties(ch);
        return ((props & 0x80000000) != 0);
    }

    static final CharacterData instance = new CharacterData01();
    private CharacterData01() {};

    // The following tables and code generated using:
  // java GenerateCharacter -plane 1 -template ../../tools/GenerateCharacter/CharacterData01.java.template -spec ../../tools/UnicodeData/UnicodeData.txt -specialcasing ../../tools/UnicodeData/SpecialCasing.txt -o ../../../build/haiku-i586/gensrc/java/lang/CharacterData01.java -string -usecharforbyte 11 4 1
  // The X table has 2048 entries for a total of 4096 bytes.

  static final char X[] = (
    "\000\001\002\003\004\004\004\005\006\007\010\011\012\003\013\014\003\003\003"+
    "\003\015\004\016\003\017\020\021\003\022\004\023\003\024\025\026\004\027\030"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\031\032\003\003\003\003\003\003\033\034\003\003"+
    "\003\003\003\003\035\036\037\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\004\004\004\004\004\004\004\004\004\004"+
    "\004\004\004\004\004\004\004\004\004\004\004\004\004\004\004\004\004\040\003"+
    "\003\003\003\041\042\043\044\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\045\045\045\045\045\045\045\046"+
    "\045\047\045\050\051\052\053\003\054\054\055\003\003\003\003\003\054\054\056"+
    "\057\003\003\003\003\060\061\062\063\064\065\066\067\070\071\072\073\074\060"+
    "\061\075\063\076\077\100\067\101\102\103\104\105\106\107\110\111\112\113\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\054\114\054\054\115\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003").toCharArray();

  // The Y table has 1248 entries for a total of 2496 bytes.

  static final char Y[] = (
    "\000\000\000\000\000\000\002\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\004\000\000\000\000\000\000\000\000\000\004\000\002\000\000\000\000\000\000"+
    "\000\006\000\000\000\000\000\000\000\006\006\006\006\006\006\006\006\006\006"+
    "\006\006\006\006\006\006\006\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\004\006"+
    "\006\010\012\006\014\016\016\016\016\020\022\024\024\024\024\024\024\024\024"+
    "\024\024\024\024\024\024\024\024\006\026\030\030\030\030\032\034\032\032\036"+
    "\032\032\040\042\032\032\044\046\050\052\054\056\060\062\032\032\032\032\032"+
    "\032\064\066\070\072\074\074\074\074\074\074\074\074\076\006\006\074\074\074"+
    "\074\074\074\006\006\006\006\006\006\006\006\006\006\030\030\030\030\030\030"+
    "\030\030\030\030\030\030\030\030\030\030\030\030\030\030\030\030\100\006\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\004\006\000\000\000\000"+
    "\000\000\000\000\004\006\006\006\006\006\006\006\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\004\102\104\006\006\006\006\006\006\000\000"+
    "\000\000\000\000\000\000\106\000\000\000\000\110\006\006\006\006\006\006\006"+
    "\006\006\006\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\112"+
    "\000\000\006\006\000\000\000\000\114\116\120\006\006\006\006\006\122\122\122"+
    "\122\122\122\122\122\122\122\122\122\122\122\122\122\122\122\122\122\124\124"+
    "\124\124\124\124\124\124\124\124\124\124\124\124\124\124\124\124\124\124\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\006\126\126\126\126\126\006\006\006\006\006\006\006\006\006\006"+
    "\006\130\130\130\006\132\130\130\130\130\130\130\130\130\130\130\130\130\130"+
    "\130\130\130\130\130\130\130\130\130\134\132\006\132\134\130\130\130\130\130"+
    "\130\130\130\130\130\130\136\140\006\006\142\130\130\130\130\130\130\130\130"+
    "\130\130\130\130\130\006\006\144\146\150\152\154\006\006\150\150\130\130\134"+
    "\130\134\130\130\130\130\130\130\130\130\130\130\130\130\130\006\006\150\154"+
    "\006\152\156\156\160\162\006\006\006\006\164\164\164\164\166\006\006\006\000"+
    "\000\000\000\000\000\000\004\006\006\006\006\006\006\006\006\170\170\170\170"+
    "\172\172\172\174\176\176\200\202\202\202\202\204\204\206\210\212\212\212\176"+
    "\214\216\220\222\224\202\226\230\232\234\236\240\242\244\246\246\250\252\254"+
    "\256\220\260\262\262\262\262\110\006\006\006\006\006\006\264\264\006\006\006"+
    "\006\006\006\030\030\030\030\030\030\030\030\030\030\030\030\030\030\030\030"+
    "\030\030\030\030\030\030\030\030\030\030\030\006\006\006\006\006\030\030\030"+
    "\012\026\030\030\030\030\030\030\030\030\030\030\030\030\030\266\270\150\030"+
    "\266\272\272\274\276\276\276\300\150\150\150\302\100\150\150\150\030\030\030"+
    "\030\030\030\030\030\030\030\030\030\030\030\030\150\150\030\030\030\030\030"+
    "\030\030\030\030\030\030\030\030\030\030\030\030\030\030\030\030\030\030\030"+
    "\006\074\074\074\074\074\074\074\074\074\074\074\074\074\074\074\074\074\150"+
    "\304\006\006\006\006\006\006\006\006\006\006\006\006\006\074\074\074\074\074"+
    "\074\074\074\074\074\074\306\006\006\006\006\310\310\310\310\310\312\024\024"+
    "\024\006\006\006\006\006\006\006\314\314\314\314\314\314\314\314\314\314\314"+
    "\314\314\316\316\316\316\316\316\316\316\316\316\316\316\316\314\314\314\314"+
    "\314\314\314\314\314\314\314\314\314\316\316\316\320\316\316\316\316\316\316"+
    "\316\316\316\314\314\314\314\314\314\314\314\314\314\314\314\314\316\316\316"+
    "\316\316\316\316\316\316\316\316\316\316\322\314\006\322\324\322\324\314\322"+
    "\314\314\314\314\316\316\326\326\316\316\316\326\316\316\316\316\316\314\314"+
    "\314\314\314\314\314\314\314\314\314\314\314\316\316\316\316\316\316\316\316"+
    "\316\316\316\316\316\314\324\314\322\324\314\314\314\322\314\314\314\322\316"+
    "\316\316\316\316\316\316\316\316\316\316\316\316\314\324\314\322\314\314\322"+
    "\322\006\314\314\314\322\316\316\316\316\316\316\316\316\316\316\316\316\316"+
    "\314\314\314\314\314\314\314\314\314\314\314\314\314\316\316\316\316\316\316"+
    "\316\316\316\316\316\316\316\314\314\314\314\314\314\314\316\316\316\316\316"+
    "\316\316\316\316\314\316\316\316\316\316\316\316\316\316\316\316\316\316\314"+
    "\314\314\314\314\314\314\314\314\314\314\314\314\316\316\316\316\316\316\316"+
    "\316\316\316\316\316\316\314\314\314\314\314\314\314\314\316\316\316\006\314"+
    "\314\314\314\314\314\314\314\314\314\314\314\330\316\316\316\316\316\316\316"+
    "\316\316\316\316\316\332\316\316\316\314\314\314\314\314\314\314\314\314\314"+
    "\314\314\330\316\316\316\316\316\316\316\316\316\316\316\316\332\316\316\316"+
    "\314\314\314\314\314\314\314\314\314\314\314\314\330\316\316\316\316\316\316"+
    "\316\316\316\316\316\316\332\316\316\316\314\314\314\314\314\314\314\314\314"+
    "\314\314\314\330\316\316\316\316\316\316\316\316\316\316\316\316\332\316\316"+
    "\316\314\314\314\314\314\314\314\314\314\314\314\314\330\316\316\316\316\316"+
    "\316\316\316\316\316\316\316\332\316\316\316\334\006\336\336\336\336\336\340"+
    "\340\340\340\340\342\342\342\342\342\344\344\344\344\344\346\346\346\346\346"+
    "\074\074\074\074\074\074\006\006\074\074\074\074\074\074\074\074\074\074\074"+
    "\074\074\074\074\074\074\074\006\006\006\006\006\006").toCharArray();

  // The A table has 232 entries for a total of 928 bytes.

  static final int A[] = new int[232];
  static final String A_DATA =
    "\000\u7005\000\u7005\u7800\000\000\u7005\000\u7005\u7800\000\u7800\000\u7800"+
    "\000\000\030\u6800\030\000\034\u7800\000\u7800\000\000\u074B\000\u074B\000"+
    "\u074B\000\u074B\000\u046B\000\u058B\000\u080B\000\u080B\000\u080B\u7800\000"+
    "\000\034\000\034\000\034\u6800\u780A\u6800\u780A\u6800\u77EA\u6800\u744A\u6800"+
    "\u77AA\u6800\u742A\u6800\u780A\u6800\u76CA\u6800\u774A\u6800\u780A\u6800\u780A"+
    "\u6800\u766A\u6800\u752A\u6800\u750A\u6800\u74EA\u6800\u74EA\u6800\u74CA\u6800"+
    "\u74AA\u6800\u748A\u6800\u74CA\u6800\u754A\u6800\u752A\u6800\u750A\u6800\u74EA"+
    "\u6800\u74CA\u6800\u772A\u6800\u780A\u6800\u764A\u6800\u780A\u6800\u080B\u6800"+
    "\u080B\u6800\u080B\u6800\u080B\u6800\034\u6800\034\u6800\034\u6800\u06CB\u7800"+
    "\000\000\034\u4000\u3006\000\u042B\000\u048B\000\u050B\000\u080B\000\u7005"+
    "\000\u780A\000\u780A\u7800\000\u7800\000\000\030\000\030\000\u760A\000\u760A"+
    "\000\u76EA\000\u740A\000\u780A\242\u7001\242\u7001\241\u7002\241\u7002\000"+
    "\u3409\000\u3409\u0800\u7005\u0800\u7005\u0800\u7005\u7800\000\u7800\000\u0800"+
    "\u7005\u0800\u056B\u0800\u066B\u0800\u078B\u0800\u080B\u7800\000\u6800\030"+
    "\u7800\000\u0800\030\u0800\u7005\u4000\u3006\u4000\u3006\u4000\u3006\u7800"+
    "\000\u4000\u3006\u4000\u3006\u7800\000\u0800\u042B\u0800\u042B\u0800\u04CB"+
    "\u0800\u05EB\u0800\u080B\u0800\u080B\u0800\030\u0800\030\u0800\030\u7800\000"+
    "\000\u744A\000\u744A\000\u776A\000\u776A\000\u776A\000\u76AA\000\u76AA\000"+
    "\u76AA\000\u76AA\000\u758A\000\u758A\000\u758A\000\u746A\000\u746A\000\u746A"+
    "\000\u77EA\000\u77EA\000\u77CA\000\u77CA\000\u77CA\000\u76AA\000\u768A\000"+
    "\u768A\000\u768A\000\u700A\000\u700A\000\u75AA\000\u75AA\000\u75AA\000\u758A"+
    "\000\u752A\000\u750A\000\u750A\000\u74EA\000\u74CA\000\u74AA\000\u74CA\000"+
    "\u74CA\000\u74AA\000\u748A\000\u748A\000\u746A\000\u746A\000\u744A\000\u742A"+
    "\000\u740A\000\u770A\000\u770A\000\u770A\000\u764A\000\u764A\000\u764A\000"+
    "\u764A\000\u762A\000\u762A\000\u760A\000\u752A\000\u752A\000\u780A\000\u780A"+
    "\000\030\000\030\000\034\000\u3008\000\u3008\u4000\u3006\000\u3008\000\u3008"+
    "\000\u3008\u4800\u1010\u4800\u1010\u4800\u1010\u4800\u1010\u4000\u3006\u4000"+
    "\u3006\000\034\u4000\u3006\u6800\034\u6800\034\u7800\000\000\u042B\000\u042B"+
    "\000\u054B\000\u066B\000\u7001\000\u7001\000\u7002\000\u7002\000\u7002\u7800"+
    "\000\000\u7001\u7800\000\u7800\000\000\u7001\u7800\000\000\u7002\000\u7001"+
    "\000\031\000\u7002\u8000\031\000\u7001\000\u7002\u1800\u3649\u1800\u3649\u1800"+
    "\u3509\u1800\u3509\u1800\u37C9\u1800\u37C9\u1800\u3689\u1800\u3689\u1800\u3549"+
    "\u1800\u3549";

  // In all, the character property tables require 7520 bytes.

    static {
                { // THIS CODE WAS AUTOMATICALLY CREATED BY GenerateCharacter:
            char[] data = A_DATA.toCharArray();
            assert (data.length == (232 * 2));
            int i = 0, j = 0;
            while (i < (232 * 2)) {
                int entry = data[i++] << 16;
                A[j++] = entry | data[i++];
            }
        }

    }        
}
