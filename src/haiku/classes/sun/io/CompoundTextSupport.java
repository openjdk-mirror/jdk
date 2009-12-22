/*
 * Copyright 2001-2005 Sun Microsystems, Inc.  All Rights Reserved.
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

package sun.io;

import java.io.UnsupportedEncodingException;

import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


final class CompoundTextSupport {

    private static final class ControlSequence {

        final int hash;
        final byte[] escSequence;
        final byte[] encoding;

        ControlSequence(byte[] escSequence) {
            this(escSequence, null);
        }
        ControlSequence(byte[] escSequence, byte[] encoding) {
            if (escSequence == null) {
                throw new NullPointerException();
            }

            this.escSequence = escSequence;
            this.encoding = encoding;

            int hash = 0;
            int length = escSequence.length;

            for (int i = 0; i < escSequence.length; i++) {
                hash += (((int)escSequence[i]) & 0xff) << (i % 4); 
            }
            if (encoding != null) {
                for (int i = 0; i < encoding.length; i++) {
                    hash += (((int)encoding[i]) & 0xff) << (i % 4); 
                }
                length += 2 /* M L */ + encoding.length + 1 /* 0x02 */;
            }

            this.hash = hash;

            if (MAX_CONTROL_SEQUENCE_LEN < length) {
                MAX_CONTROL_SEQUENCE_LEN = length;
            }
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ControlSequence)) {
                return false;
            }
            ControlSequence rhs = (ControlSequence)obj;
            if (escSequence != rhs.escSequence) {
                if (escSequence.length != rhs.escSequence.length) {
                    return false;
                }
                for (int i = 0; i < escSequence.length; i++) {
                    if (escSequence[i] != rhs.escSequence[i]) {
                        return false;
                    }
                }
            }
            if (encoding != rhs.encoding) {
                if (encoding == null || rhs.encoding == null ||
                    encoding.length != rhs.encoding.length)
                {
                    return false;
                }
                for (int i = 0; i < encoding.length; i++) {
                    if (encoding[i] != rhs.encoding[i]) {
                        return false;
                    }
                }
            }
            return true;
        }

        public int hashCode() {
            return hash;
        }

        ControlSequence concatenate(ControlSequence rhs) {
            if (encoding != null) {
                throw new IllegalArgumentException
                    ("cannot concatenate to a non-standard charset escape " +
                     "sequence");
            }

            int len = escSequence.length + rhs.escSequence.length;
            byte[] newEscSequence = new byte[len];
            System.arraycopy(escSequence, 0, newEscSequence, 0,
                             escSequence.length);
            System.arraycopy(rhs.escSequence, 0, newEscSequence,
                             escSequence.length, rhs.escSequence.length);
            return new ControlSequence(newEscSequence, rhs.encoding);
        }
    }

    static int MAX_CONTROL_SEQUENCE_LEN;

    /**
     * Maps a GL or GR escape sequence to an encoding.
     */
    private static final Map sequenceToEncodingMap;

    /**
     * Indicates whether a particular encoding wants the high bit turned on
     * or off.
     */
    private static final Map highBitsMap;

    /**
     * Maps an encoding to an escape sequence. Rather than manage two
     * converters in CharToByteCOMPOUND_TEXT, we output escape sequences which
     * modify both GL and GR if necessary. This makes the output slightly less
     * efficient, but our code much simpler.
     */
    private static final Map encodingToSequenceMap;

    /**
     * The keys of 'encodingToSequenceMap', sorted in preferential order.
     */
    private static final List encodings;

    static {
        HashMap tSequenceToEncodingMap = new HashMap(33, 1.0f);
        HashMap tHighBitsMap = new HashMap(31, 1.0f);
        HashMap tEncodingToSequenceMap = new HashMap(21, 1.0f);
        ArrayList tEncodings = new ArrayList(21);

        if (!(isEncodingSupported("ASCII") &&
              isEncodingSupported("ISO8859_1")))
        {
            throw new ExceptionInInitializerError
                ("US-ASCII and ISO-8859-1 unsupported");
        }

        ControlSequence leftAscii = // high bit off, leave off
            new ControlSequence(new byte[] { 0x1B, 0x28, 0x42 });
        tSequenceToEncodingMap.put(leftAscii, "ASCII");
        tHighBitsMap.put(leftAscii, Boolean.FALSE);

        {
            ControlSequence rightAscii = // high bit on, turn off
                new ControlSequence(new byte[] { 0x1B, 0x29, 0x42 });
            tSequenceToEncodingMap.put(rightAscii, "ASCII");
            tHighBitsMap.put(rightAscii, Boolean.FALSE);
        }

        {
            ControlSequence rightHalf = // high bit on, leave on
                new ControlSequence(new byte[] { 0x1B, 0x2D, 0x41 });
            tSequenceToEncodingMap.put(rightHalf, "ISO8859_1");
            tHighBitsMap.put(rightHalf, Boolean.TRUE);

            ControlSequence fullSet = leftAscii.concatenate(rightHalf);
            tEncodingToSequenceMap.put("ISO8859_1", fullSet);
            tEncodings.add("ISO8859_1");
        }
        if (isEncodingSupported("ISO8859_2")) {
            ControlSequence rightHalf = // high bit on, leave on
                new ControlSequence(new byte[] { 0x1B, 0x2D, 0x42 });
            tSequenceToEncodingMap.put(rightHalf, "ISO8859_2");
            tHighBitsMap.put(rightHalf, Boolean.TRUE);

            ControlSequence fullSet = leftAscii.concatenate(rightHalf);
            tEncodingToSequenceMap.put("ISO8859_2", fullSet);
            tEncodings.add("ISO8859_2");
        }
        if (isEncodingSupported("ISO8859_3")) {
            ControlSequence rightHalf = // high bit on, leave on
                new ControlSequence(new byte[] { 0x1B, 0x2D, 0x43 });
            tSequenceToEncodingMap.put(rightHalf, "ISO8859_3");
            tHighBitsMap.put(rightHalf, Boolean.TRUE);

            ControlSequence fullSet = leftAscii.concatenate(rightHalf);
            tEncodingToSequenceMap.put("ISO8859_3", fullSet);
            tEncodings.add("ISO8859_3");
        }
        if (isEncodingSupported("ISO8859_4")) {
            ControlSequence rightHalf = // high bit on, leave on
                new ControlSequence(new byte[] { 0x1B, 0x2D, 0x44 });
            tSequenceToEncodingMap.put(rightHalf, "ISO8859_4");
            tHighBitsMap.put(rightHalf, Boolean.TRUE);

            ControlSequence fullSet = leftAscii.concatenate(rightHalf);
            tEncodingToSequenceMap.put("ISO8859_4", fullSet);
            tEncodings.add("ISO8859_4");
        }
        if (isEncodingSupported("ISO8859_5")) {
            ControlSequence rightHalf = // high bit on, leave on
                new ControlSequence(new byte[] { 0x1B, 0x2D, 0x4C });
            tSequenceToEncodingMap.put(rightHalf, "ISO8859_5");
            tHighBitsMap.put(rightHalf, Boolean.TRUE);

            ControlSequence fullSet = leftAscii.concatenate(rightHalf);
            tEncodingToSequenceMap.put("ISO8859_5", fullSet);
            tEncodings.add("ISO8859_5");
        }
        if (isEncodingSupported("ISO8859_6")) {
            ControlSequence rightHalf = // high bit on, leave on
                new ControlSequence(new byte[] { 0x1B, 0x2D, 0x47 });
            tSequenceToEncodingMap.put(rightHalf, "ISO8859_6");
            tHighBitsMap.put(rightHalf, Boolean.TRUE);

            ControlSequence fullSet = leftAscii.concatenate(rightHalf);
            tEncodingToSequenceMap.put("ISO8859_6", fullSet);
            tEncodings.add("ISO8859_6");
        }
        if (isEncodingSupported("ISO8859_7")) {
            ControlSequence rightHalf = // high bit on, leave on
                new ControlSequence(new byte[] { 0x1B, 0x2D, 0x46 });
            tSequenceToEncodingMap.put(rightHalf, "ISO8859_7");
            tHighBitsMap.put(rightHalf, Boolean.TRUE);

            ControlSequence fullSet = leftAscii.concatenate(rightHalf);
            tEncodingToSequenceMap.put("ISO8859_7", fullSet);
            tEncodings.add("ISO8859_7");
        }
        if (isEncodingSupported("ISO8859_8")) {
            ControlSequence rightHalf = // high bit on, leave on
                new ControlSequence(new byte[] { 0x1B, 0x2D, 0x48 });
            tSequenceToEncodingMap.put(rightHalf, "ISO8859_8");
            tHighBitsMap.put(rightHalf, Boolean.TRUE);

            ControlSequence fullSet = leftAscii.concatenate(rightHalf);
            tEncodingToSequenceMap.put("ISO8859_8", fullSet);
            tEncodings.add("ISO8859_8");
        }
        if (isEncodingSupported("ISO8859_9")) {
            ControlSequence rightHalf = // high bit on, leave on
                new ControlSequence(new byte[] { 0x1B, 0x2D, 0x4D });
            tSequenceToEncodingMap.put(rightHalf, "ISO8859_9");
            tHighBitsMap.put(rightHalf, Boolean.TRUE);

            ControlSequence fullSet = leftAscii.concatenate(rightHalf);
            tEncodingToSequenceMap.put("ISO8859_9", fullSet);
            tEncodings.add("ISO8859_9");
        }
        if (isEncodingSupported("JIS0201")) {
            ControlSequence glLeft = // high bit off, leave off
                new ControlSequence(new byte[] { 0x1B, 0x28, 0x4A });
            ControlSequence glRight = // high bit off, turn on
                new ControlSequence(new byte[] { 0x1B, 0x28, 0x49 });
            ControlSequence grLeft = // high bit on, turn off
                new ControlSequence(new byte[] { 0x1B, 0x29, 0x4A });
            ControlSequence grRight = // high bit on, leave on
                new ControlSequence(new byte[] { 0x1B, 0x29, 0x49 });
            tSequenceToEncodingMap.put(glLeft, "JIS0201");
            tSequenceToEncodingMap.put(glRight, "JIS0201");
            tSequenceToEncodingMap.put(grLeft, "JIS0201");
            tSequenceToEncodingMap.put(grRight, "JIS0201");
            tHighBitsMap.put(glLeft, Boolean.FALSE);
            tHighBitsMap.put(glRight, Boolean.TRUE);
            tHighBitsMap.put(grLeft, Boolean.FALSE);
            tHighBitsMap.put(grRight, Boolean.TRUE);
            
            ControlSequence fullSet = glLeft.concatenate(grRight);
            tEncodingToSequenceMap.put("JIS0201", fullSet);
            tEncodings.add("JIS0201");
        }
        if (isEncodingSupported("X11GB2312")) {
            ControlSequence leftHalf =  // high bit off, leave off
                new ControlSequence(new byte[] { 0x1B, 0x24, 0x28, 0x41 });
            ControlSequence rightHalf = // high bit on, turn off
                new ControlSequence(new byte[] { 0x1B, 0x24, 0x29, 0x41 });
            tSequenceToEncodingMap.put(leftHalf, "X11GB2312");
            tSequenceToEncodingMap.put(rightHalf, "X11GB2312");
            tHighBitsMap.put(leftHalf, Boolean.FALSE);
            tHighBitsMap.put(rightHalf, Boolean.FALSE);
            
            tEncodingToSequenceMap.put("X11GB2312", leftHalf);
            tEncodings.add("X11GB2312");
        }
        if (isEncodingSupported("JIS0208")) {
            ControlSequence leftHalf = // high bit off, leave off
                new ControlSequence(new byte[] { 0x1B, 0x24, 0x28, 0x42 });
            ControlSequence rightHalf = // high bit on, turn off
                new ControlSequence(new byte[] { 0x1B, 0x24, 0x29, 0x42 });
            tSequenceToEncodingMap.put(leftHalf, "JIS0208");
            tSequenceToEncodingMap.put(rightHalf, "JIS0208");
            tHighBitsMap.put(leftHalf, Boolean.FALSE);
            tHighBitsMap.put(rightHalf, Boolean.FALSE);

            tEncodingToSequenceMap.put("JIS0208", leftHalf);
            tEncodings.add("JIS0208");
        }
        if (isEncodingSupported("X11KSC5601")) {
            ControlSequence leftHalf = // high bit off, leave off
                new ControlSequence(new byte[] { 0x1B, 0x24, 0x28, 0x43 });
            ControlSequence rightHalf = // high bit on, turn off
                new ControlSequence(new byte[] { 0x1B, 0x24, 0x29, 0x43 });
            tSequenceToEncodingMap.put(leftHalf, "X11KSC5601");
            tSequenceToEncodingMap.put(rightHalf, "X11KSC5601");
            tHighBitsMap.put(leftHalf, Boolean.FALSE);
            tHighBitsMap.put(rightHalf, Boolean.FALSE);
                
            tEncodingToSequenceMap.put("X11KSC5601", leftHalf);
            tEncodings.add("X11KSC5601");
        }

        // Encodings not listed in Compound Text Encoding spec

        // Esc seq: -b
        if (isEncodingSupported("ISO8859_15")) {
            ControlSequence rightHalf = // high bit on, leave on
                new ControlSequence(new byte[] { 0x1B, 0x2D, 0x62 });
            tSequenceToEncodingMap.put(rightHalf, "ISO8859_15");
            tHighBitsMap.put(rightHalf, Boolean.TRUE);

            ControlSequence fullSet = leftAscii.concatenate(rightHalf);
            tEncodingToSequenceMap.put("ISO8859_15", fullSet);
            tEncodings.add("ISO8859_15");
        }
        // Esc seq: -T
        if (isEncodingSupported("TIS620")) {
            ControlSequence rightHalf = // high bit on, leave on
                new ControlSequence(new byte[] { 0x1B, 0x2D, 0x54 });
            tSequenceToEncodingMap.put(rightHalf, "TIS620");
            tHighBitsMap.put(rightHalf, Boolean.TRUE);

            ControlSequence fullSet = leftAscii.concatenate(rightHalf);
            tEncodingToSequenceMap.put("TIS620", fullSet);
            tEncodings.add("TIS620");
        }
        if (isEncodingSupported("JIS0212")) {
            ControlSequence leftHalf = // high bit off, leave off
                new ControlSequence(new byte[] { 0x1B, 0x24, 0x28, 0x44 });
            ControlSequence rightHalf = // high bit on, turn off
                new ControlSequence(new byte[] { 0x1B, 0x24, 0x29, 0x44 });
            tSequenceToEncodingMap.put(leftHalf, "JIS0212");
            tSequenceToEncodingMap.put(rightHalf, "JIS0212");
            tHighBitsMap.put(leftHalf, Boolean.FALSE);
            tHighBitsMap.put(rightHalf, Boolean.FALSE);
            
            tEncodingToSequenceMap.put("JIS0212", leftHalf);
            tEncodings.add("JIS0212");
        }
        if (isEncodingSupported("X11CNS11643P1")) {
            ControlSequence leftHalf = // high bit off, leave off
                new ControlSequence(new byte[] { 0x1B, 0x24, 0x28, 0x47 });
            ControlSequence rightHalf = // high bit on, turn off
                new ControlSequence(new byte[] { 0x1B, 0x24, 0x29, 0x47 });
            tSequenceToEncodingMap.put(leftHalf, "X11CNS11643P1");
            tSequenceToEncodingMap.put(rightHalf, "X11CNS11643P1");
            tHighBitsMap.put(leftHalf, Boolean.FALSE);
            tHighBitsMap.put(rightHalf, Boolean.FALSE);
            
            tEncodingToSequenceMap.put("X11CNS11643P1", leftHalf);
            tEncodings.add("X11CNS11643P1");
        }
        if (isEncodingSupported("X11CNS11643P2")) {
            ControlSequence leftHalf = // high bit off, leave off
                new ControlSequence(new byte[] { 0x1B, 0x24, 0x28, 0x48 });
            ControlSequence rightHalf = // high bit on, turn off
                new ControlSequence(new byte[] { 0x1B, 0x24, 0x29, 0x48 });
            tSequenceToEncodingMap.put(leftHalf, "X11CNS11643P2");
            tSequenceToEncodingMap.put(rightHalf, "X11CNS11643P2");
            tHighBitsMap.put(leftHalf, Boolean.FALSE);
            tHighBitsMap.put(rightHalf, Boolean.FALSE);
            
            tEncodingToSequenceMap.put("X11CNS11643P2", leftHalf);
            tEncodings.add("X11CNS11643P2");
        }
        if (isEncodingSupported("X11CNS11643P3")) {
            ControlSequence leftHalf = // high bit off, leave off
                new ControlSequence(new byte[] { 0x1B, 0x24, 0x28, 0x49 });
            ControlSequence rightHalf = // high bit on, turn off
                new ControlSequence(new byte[] { 0x1B, 0x24, 0x29, 0x49 });
            tSequenceToEncodingMap.put(leftHalf, "X11CNS11643P3");
            tSequenceToEncodingMap.put(rightHalf, "X11CNS11643P3");
            tHighBitsMap.put(leftHalf, Boolean.FALSE);
            tHighBitsMap.put(rightHalf, Boolean.FALSE);
            
            tEncodingToSequenceMap.put("X11CNS11643P3", leftHalf);
            tEncodings.add("X11CNS11643P3");
        }
        // Esc seq: %/2??SUN-KSC5601.1992-3
        if (isEncodingSupported("Johab")) {
            // 0x32 looks wrong. It's copied from the Sun X11 Compound Text
            // support code. It implies that all Johab characters comprise two
            // octets, which isn't true. Johab supports the ASCII/KS-Roman
            // characters from 0x21-0x7E with single-byte representations.
            ControlSequence johab = new ControlSequence(
                new byte[] { 0x1b, 0x25, 0x2f, 0x32 },
                new byte[] { 0x53, 0x55, 0x4e, 0x2d, 0x4b, 0x53, 0x43, 0x35,
                             0x36, 0x30, 0x31, 0x2e, 0x31, 0x39, 0x39, 0x32,
                             0x2d, 0x33 });
            tSequenceToEncodingMap.put(johab, "Johab");
            tEncodingToSequenceMap.put("Johab", johab);
            tEncodings.add("Johab");
        }
        // Esc seq: %/2??SUN-BIG5-1
        if (isEncodingSupported("Big5")) {
            // 0x32 looks wrong. It's copied from the Sun X11 Compound Text
            // support code. It implies that all Big5 characters comprise two
            // octets, which isn't true. Big5 supports the ASCII/CNS-Roman
            // characters from 0x21-0x7E with single-byte representations.
            ControlSequence big5 = new ControlSequence(
                new byte[] { 0x1b, 0x25, 0x2f, 0x32 },
                new byte[] { 0x53, 0x55, 0x4e, 0x2d, 0x42, 0x49, 0x47, 0x35,
                             0x2d, 0x31 });
            tSequenceToEncodingMap.put(big5, "Big5");
            tEncodingToSequenceMap.put("Big5", big5);
            tEncodings.add("Big5");
        }

        sequenceToEncodingMap =
            Collections.unmodifiableMap(tSequenceToEncodingMap);
        highBitsMap = Collections.unmodifiableMap(tHighBitsMap);
        encodingToSequenceMap =
            Collections.unmodifiableMap(tEncodingToSequenceMap);
        encodings = Collections.unmodifiableList(tEncodings);
    }

    private static boolean isEncodingSupported(String encoding) {
        String enc = CharacterEncoding.aliasName(encoding);
        if (enc == null) {
            enc = encoding;
        }
        return (encodingToByteToCharConverter(enc) != null &&
                encodingToCharToByteConverter(enc) != null);
    }        


    // For ByteToCharConverter
    static ByteToCharConverter getStandardConverter(byte[] escSequence) {
        return getNonStandardConverter(escSequence, null);
    }
    static boolean getHighBit(byte[] escSequence) {
        Boolean bool = (Boolean)highBitsMap.get
            (new ControlSequence(escSequence));
        return (bool == Boolean.TRUE);
    }
    static ByteToCharConverter getNonStandardConverter(byte[] escSequence,
                                                       byte[] encoding) {
        return encodingToByteToCharConverter((String)sequenceToEncodingMap.get
            (new ControlSequence(escSequence, encoding)));
    }
    static ByteToCharConverter encodingToByteToCharConverter(String enc) {
        if (enc == null) {
            return null;
        }
        Class cls;
        try {
            cls = Class.forName("sun.io.ByteToChar" + enc);
        } catch (ClassNotFoundException e) {
            try {
                cls = Class.forName("sun.awt.motif.ByteToChar" + enc);
            } catch (ClassNotFoundException ee) {
                return null;
            }
        }
        try {
            return (ByteToCharConverter)cls.newInstance();
        } catch (InstantiationException e) {
            return null;
        } catch (IllegalAccessException e) {
            return null;
        }
    }


    // For CharToByteConverter
    static byte[] getEscapeSequence(String encoding) {
        ControlSequence seq = (ControlSequence)
            encodingToSequenceMap.get(encoding);
        if (seq != null) {
            return seq.escSequence;
        }
        return null;
    }
    static byte[] getEncoding(String encoding) {
        ControlSequence seq = (ControlSequence)
            encodingToSequenceMap.get(encoding);
        if (seq != null) {
            return seq.encoding;
        }
        return null;
    }
    static List getEncodings() {
        return encodings;
    }
    static CharToByteConverter encodingToCharToByteConverter(String enc) {
        if (enc == null) {
            return null;
        }
        Class cls;
        try {
            cls = Class.forName("sun.io.CharToByte" + enc);
        } catch (ClassNotFoundException e) {
            try {
                cls = Class.forName("sun.awt.motif.CharToByte" + enc);
            } catch (ClassNotFoundException ee) {
                return null;
            }
        }
        try {
            return (CharToByteConverter)cls.newInstance();
        } catch (InstantiationException e) {
            return null;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    // Not an instantiable class
    private CompoundTextSupport() {}
}
