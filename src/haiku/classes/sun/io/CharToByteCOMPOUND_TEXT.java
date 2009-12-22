/*
 * Copyright 2002-2003 Sun Microsystems, Inc.  All Rights Reserved.
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

/**
 * An algorithmic conversion from Unicode to COMPOUND_TEXT.
 */

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class CharToByteCOMPOUND_TEXT extends CharToByteConverter {

    /**
     * NOTE: The following four static variables should be used *only* for
     * testing whether a converter can convert a specific character. They
     * cannot be used for actual encoding because they are shared across all
     * COMPOUND_TEXT converters and may be stateful.
     */
    private static final Map encodingToConverterMap = 
        Collections.synchronizedMap(new HashMap(21, 1.0f));
    private static final CharToByteConverter latin1Converter;
    private static final CharToByteConverter defaultConverter;
    private static final boolean defaultEncodingSupported;

    static {
        CharToByteConverter converter = CharToByteConverter.getDefault();
        String encoding = converter.getCharacterEncoding();
        if ("ISO8859_1".equals(encoding)) {
            latin1Converter = converter;
            defaultConverter = converter;
            defaultEncodingSupported = true;
        } else {
            try {
                latin1Converter =
                    CharToByteConverter.getConverter("ISO8859_1");
            } catch (UnsupportedEncodingException e) {
                throw new ExceptionInInitializerError
                    ("ISO8859_1 unsupported");           
            }
            defaultConverter = converter;
            defaultEncodingSupported = CompoundTextSupport.getEncodings().
                contains(defaultConverter.getCharacterEncoding());
        }
    }

    private CharToByteConverter converter;
    private char[] charBuf = new char[1];
    private ByteArrayOutputStream nonStandardCharsetBuffer;
    private byte[] byteBuf;
    private int numNonStandardChars, nonStandardEncodingLen;

    public CharToByteCOMPOUND_TEXT() {
        try {
            converter = CharToByteConverter.getConverter("ISO8859_1");
        } catch (UnsupportedEncodingException cannotHappen) {
        }
        initConverter(converter);
    }

    public String getCharacterEncoding() {
        return "COMPOUND_TEXT";
    }

    public int convert(char[] input, int inOff, int inEnd,
                       byte[] output, int outOff, int outEnd)
        throws MalformedInputException, UnknownCharacterException,
               ConversionBufferFullException
    {
        int outSave = outOff;

        charOff = inOff;
        byteOff = outOff;

        if (inOff >= inEnd) {
            return 0;
        }

        while (charOff < inEnd) {
            charBuf[0] = input[charOff];
            if (charBuf[0] <= '\u0008' ||
                (charBuf[0] >= '\u000B' && charBuf[0] <= '\u001F') ||
                (charBuf[0] >= '\u0080' && charBuf[0] <= '\u009F'))
            {
                // The compound text specification only permits the octets
                // 0x09, 0x0A, 0x1B, and 0x9B in C0 and C1. Of these, 1B and
                // 9B must also be removed because they initiate control
                // sequences.
                charBuf[0] = '?';
            }

            CharToByteConverter c = getConverter(charBuf[0]);
            if (c == null) {
                if (subMode) {
                    charBuf[0] = '?';
                    c = latin1Converter;
                } else {
                    throw new UnknownCharacterException();
                }
            }
            if (c != converter) {
                if (nonStandardCharsetBuffer != null) {
                    byteOff +=
                        flushNonStandardCharsetBuffer(output, byteOff, outEnd);
                } else {
                    byteOff += converter.flush(output, byteOff, outEnd);
                }

                byte[] escSequence = CompoundTextSupport.
                    getEscapeSequence(c.getCharacterEncoding());
                if (escSequence == null) {
                    throw new InternalError("Unknown encoding: " + 
                                            converter.getCharacterEncoding());
                } else if (escSequence[1] == (byte)0x25 &&
                           escSequence[2] == (byte)0x2F) {
                    initNonStandardCharsetBuffer(c, escSequence);
                } else if (byteOff + escSequence.length <= outEnd) {
                    System.arraycopy(escSequence, 0, output, byteOff,
                                     escSequence.length);
                    byteOff += escSequence.length;
                } else {
                    throw new ConversionBufferFullException();
                }
                converter = c;
                continue;
            }

            if (nonStandardCharsetBuffer == null) {
                byteOff += converter.convert(charBuf, 0, 1, output, byteOff,
                                             outEnd);
            } else {
                int length = converter.convert(charBuf, 0, 1, byteBuf, 0,
                                               byteBuf.length);
                nonStandardCharsetBuffer.write(byteBuf, 0, length);
                numNonStandardChars++;
            }

            charOff++;
        }

        return byteOff - outSave;
    }

    public int flush(byte[] output, int outStart, int outEnd)
        throws ConversionBufferFullException, MalformedInputException
    {
        int retval;
        try {
            retval = (nonStandardCharsetBuffer != null)
                ? flushNonStandardCharsetBuffer(output, outStart, outEnd)
                : converter.flush(output, outStart, outEnd);
        } catch (MalformedInputException e) {
            reset();
            throw e;
        }
        reset();
        return retval;
    }

    private void initNonStandardCharsetBuffer(CharToByteConverter c,
                                              byte[] escSequence)
    {
        nonStandardCharsetBuffer = new ByteArrayOutputStream();
        byteBuf = new byte[c.getMaxBytesPerChar()];

        nonStandardCharsetBuffer.write(escSequence, 0, escSequence.length);
        nonStandardCharsetBuffer.write(0); // M placeholder
        nonStandardCharsetBuffer.write(0); // L placeholder
        byte[] encoding = CompoundTextSupport.
            getEncoding(c.getCharacterEncoding());
        if (encoding == null) {
            throw new InternalError
                ("Unknown encoding: " + converter.getCharacterEncoding());
        }
        nonStandardCharsetBuffer.write(encoding, 0, encoding.length);
        nonStandardCharsetBuffer.write(0x02); // divider
        nonStandardEncodingLen = encoding.length + 1;
    }

    private int flushNonStandardCharsetBuffer(byte[] output, int outStart,
                                              int outEnd)
        throws ConversionBufferFullException, MalformedInputException
    {
        int outSave = outStart;

        if (numNonStandardChars > 0) {
            byte[] flushBuf = new byte[converter.getMaxBytesPerChar() *
                                       numNonStandardChars];
            int flushLen = converter.flush(flushBuf, 0, flushBuf.length);
            nonStandardCharsetBuffer.write(flushBuf, 0, flushLen);
            numNonStandardChars = 0;
        }

        int numBytes = nonStandardCharsetBuffer.size(); 
        int nonStandardBytesOff = 6 + nonStandardEncodingLen;

        if (outStart + (numBytes - nonStandardBytesOff) +
            nonStandardBytesOff * (((numBytes - nonStandardBytesOff) /
                                    ((1 << 14) - 1)) + 1) > outEnd)
        {
            throw new ConversionBufferFullException();
        }

        byte[] nonStandardBytes =
            nonStandardCharsetBuffer.toByteArray();

        // The non-standard charset header only supports 2^14-1 bytes of data.
        // If we have more than that, we have to repeat the header.
        do {
            output[outStart++] = 0x1B;
            output[outStart++] = 0x25;
            output[outStart++] = 0x2F;
            output[outStart++] = nonStandardBytes[3];

            int toWrite = Math.min(numBytes - nonStandardBytesOff,
                                   (1 << 14) - 1 - nonStandardEncodingLen);

            output[outStart++] = (byte)
                (((toWrite + nonStandardEncodingLen) / 0x80) | 0x80); // M
            output[outStart++] = (byte)
                (((toWrite + nonStandardEncodingLen) % 0x80) | 0x80); // L
            
            System.arraycopy(nonStandardBytes, 6, output, outStart,
                             nonStandardEncodingLen);
            outStart += nonStandardEncodingLen;

            System.arraycopy(nonStandardBytes, nonStandardBytesOff,
                             output, outStart, toWrite);
            nonStandardBytesOff += toWrite;
            outStart += toWrite;
        } while (nonStandardBytesOff < numBytes);

        nonStandardCharsetBuffer = null;
        byteBuf = null;
        nonStandardEncodingLen = 0;

        return outStart - outSave;
    }

    /**
     * Resets the converter.
     * Call this method to reset the converter to its initial state
     */
    public void reset() {
        byteOff = charOff = numNonStandardChars = nonStandardEncodingLen = 0;
        nonStandardCharsetBuffer = null;
        byteBuf = null;
        try {
            converter = CharToByteConverter.getConverter("ISO8859_1");
        } catch (UnsupportedEncodingException cannotHappen) {
        }
        initConverter(converter);
    }

    /**
     * Return whether a character is mappable or not
     * @return true if a character is mappable
     */
    public boolean canConvert(char ch) {
        return getConverter(ch) != null;
    }

    /**
     * the maximum number of bytes needed to hold a converted char
     * @return the maximum number of bytes needed for a converted char
     */
    public int getMaxBytesPerChar() {
        // We only use single- and double-byte encodings
        return CompoundTextSupport.MAX_CONTROL_SEQUENCE_LEN + 2;
    }

    public int getBadInputLength() {
        return converter.getBadInputLength();
    }

    public void setSubstitutionMode(boolean doSub) {
        super.setSubstitutionMode(doSub);
        converter.setSubstitutionMode(doSub);
    }

    public void setSubstitutionBytes(byte[] newSubBytes)
        throws IllegalArgumentException
    {
        super.setSubstitutionBytes(newSubBytes);
        converter.setSubstitutionBytes(newSubBytes);
    }

    /**
     * Try to figure out which CharToByteConverter to use for conversion 
     * of the specified Unicode character. The target character encoding 
     * of the returned converter is approved to be used with Compound Text.
     *
     * @param ch Unicode character 
     * @return CharToByteConverter to convert the given character
     */
    private CharToByteConverter getConverter(char ch) {
        // 1. Try the current converter.
        if (converter.canConvert(ch)) {
            return converter;
        }

        // 2. Try the default converter.
        if (defaultEncodingSupported && defaultConverter.canConvert(ch)) {
            CharToByteConverter retval = null;
            try {
                retval = CharToByteConverter.getConverter
                    (defaultConverter.getCharacterEncoding());
            } catch (UnsupportedEncodingException cannotHappen) {
            }
            initConverter(retval);
            return retval;
        }

        // 3. Try ISO8859-1.
        if (latin1Converter.canConvert(ch)) {
            CharToByteConverter retval = null;
            try {
                retval = CharToByteConverter.getConverter("ISO8859_1");
            } catch (UnsupportedEncodingException cannotHappen) {
            }
            initConverter(retval);
            return retval;
        }

        // 4. Brute force search of all supported encodings.
        for (Iterator iter = CompoundTextSupport.getEncodings().iterator();
             iter.hasNext();)
        {
            String encoding = (String)iter.next();
            CharToByteConverter conv = 
                (CharToByteConverter)encodingToConverterMap.get(encoding);
            if (conv == null) {
                conv = CompoundTextSupport.
                    encodingToCharToByteConverter(encoding);
                if (conv == null) {
                    throw new InternalError("Unsupported encoding: " +
                                            encoding);
                }
                encodingToConverterMap.put(encoding, conv);
            }
            if (conv.canConvert(ch)) {
                CharToByteConverter retval = CompoundTextSupport.
                    encodingToCharToByteConverter(encoding);
                initConverter(retval);
                return retval;
            }
        }

        return null;
    }

    private void initConverter(CharToByteConverter conv) {
        conv.setSubstitutionMode(subMode);
        conv.setSubstitutionBytes(subBytes);
    }
}
