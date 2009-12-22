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
 * An algorithmic conversion from COMPOUND_TEXT to Unicode.
 */

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;


public class ByteToCharCOMPOUND_TEXT extends ByteToCharConverter {

    private static final int NORMAL_BYTES             =  0;
    private static final int NONSTANDARD_BYTES        =  1;
    private static final int VERSION_SEQUENCE_V       =  2;
    private static final int VERSION_SEQUENCE_TERM    =  3;
    private static final int ESCAPE_SEQUENCE          =  4;
    private static final int CHARSET_NGIIF            =  5;
    private static final int CHARSET_NLIIF            =  6;
    private static final int CHARSET_NLIF             =  7;
    private static final int CHARSET_NRIIF            =  8;
    private static final int CHARSET_NRIF             =  9;
    private static final int CHARSET_NONSTANDARD_FOML = 10;
    private static final int CHARSET_NONSTANDARD_OML  = 11;
    private static final int CHARSET_NONSTANDARD_ML   = 12;
    private static final int CHARSET_NONSTANDARD_L    = 13;
    private static final int CHARSET_NONSTANDARD      = 14;
    private static final int CHARSET_LIIF             = 15;
    private static final int CHARSET_LIF              = 16;
    private static final int CHARSET_RIIF             = 17;
    private static final int CHARSET_RIF              = 18;
    private static final int CONTROL_SEQUENCE_PIF     = 19;
    private static final int CONTROL_SEQUENCE_IF      = 20;
    private static final int EXTENSION_ML             = 21;
    private static final int EXTENSION_L              = 22;
    private static final int EXTENSION                = 23;
    private static final int ESCAPE_SEQUENCE_OTHER    = 24;

    private static final String ERR_LATIN1 = "ISO8859_1 unsupported";
    private static final String ERR_ILLSTATE = "Illegal state";
    private static final String ERR_ESCBYTE =
        "Illegal byte in 0x1B escape sequence";
    private static final String ERR_ENCODINGBYTE =
        "Illegal byte in non-standard character set name";
    private static final String ERR_CTRLBYTE =
        "Illegal byte in 0x9B control sequence";
    private static final String ERR_CTRLPI =
        "P following I in 0x9B control sequence";
    private static final String ERR_VERSTART =
        "Versioning escape sequence can only appear at start of byte stream";
    private static final String ERR_VERMANDATORY =
        "Cannot parse mandatory extensions";
    private static final String ERR_ENCODING = "Unknown encoding: ";
    private static final String ERR_FLUSH =
        "Escape sequence, control sequence, or ML extension not terminated";
    

    private int state = NORMAL_BYTES ;
    private int ext_count, ext_offset;
    private boolean versionSequenceAllowed = true;
    private byte[] byteBuf = new byte[1];
    private ByteArrayOutputStream queue = new ByteArrayOutputStream(),
        encodingQueue = new ByteArrayOutputStream();

    private ByteToCharConverter glConverter, grConverter, nonStandardConverter,
        lastConverter;
    private boolean glHigh = false, grHigh = true;


    public ByteToCharCOMPOUND_TEXT() {
        try {
            // Initial state in ISO 2022 designates Latin-1 charset.
            glConverter = ByteToCharConverter.getConverter("ASCII");
            grConverter = ByteToCharConverter.getConverter("ISO8859_1");
        } catch (UnsupportedEncodingException e) {
            error(ERR_LATIN1);
        }
        initConverter(glConverter);
        initConverter(grConverter);
    }

    public String getCharacterEncoding() {
        return "COMPOUND_TEXT";
    }

    public int convert(byte[] input, int inOff, int inEnd,
                       char[] output, int outOff, int outEnd)
        throws UnknownCharacterException, MalformedInputException,
               ConversionBufferFullException
    {
        int outSave = outOff;

        byteOff = inOff;
        charOff = outOff;

        if (inOff >= inEnd) {
            return 0;
        }

        while (byteOff < inEnd) {
            // Byte parsing is done with shorts instead of bytes because
            // Java bytes are signed, while COMPOUND_TEXT bytes are not. If
            // we used the Java byte type, the > and < tests during parsing
            // would not work correctly.
            handleByte((short)(input[byteOff] & 0xFF), output, outEnd);
            byteOff++;
        }

        return charOff - outSave;
    }

    private void handleByte(short newByte, char[] output, int outEnd)
        throws UnknownCharacterException, MalformedInputException,
               ConversionBufferFullException
    {
        switch (state) {
        case NORMAL_BYTES:
            normalBytes(newByte, output, outEnd);
            break;
        case NONSTANDARD_BYTES:
            nonStandardBytes(newByte, output, outEnd);
            break;
        case VERSION_SEQUENCE_V:
        case VERSION_SEQUENCE_TERM:
            versionSequence(newByte);
            break;
        case ESCAPE_SEQUENCE:
            escapeSequence(newByte);
            break;
        case CHARSET_NGIIF:
            charset94N(newByte);
            break;
        case CHARSET_NLIIF:
        case CHARSET_NLIF:
            charset94NL(newByte, output, outEnd);
            break;
        case CHARSET_NRIIF:
        case CHARSET_NRIF:
            charset94NR(newByte, output, outEnd);
            break;
        case CHARSET_NONSTANDARD_FOML:
        case CHARSET_NONSTANDARD_OML:
        case CHARSET_NONSTANDARD_ML:
        case CHARSET_NONSTANDARD_L:
        case CHARSET_NONSTANDARD:
            charsetNonStandard(newByte, output, outEnd);
            break;
        case CHARSET_LIIF:
        case CHARSET_LIF:
            charset9496L(newByte, output, outEnd);
            break;
        case CHARSET_RIIF:
        case CHARSET_RIF:
            charset9496R(newByte, output, outEnd);
            break;
        case CONTROL_SEQUENCE_PIF:
        case CONTROL_SEQUENCE_IF:
            controlSequence(newByte);
            break;
        case EXTENSION_ML:
        case EXTENSION_L:
        case EXTENSION:
            extension(newByte);
            break;
        case ESCAPE_SEQUENCE_OTHER:
            escapeSequenceOther(newByte);
            break;
        default:
            error(ERR_ILLSTATE);
        }
    }
 
    private void normalBytes(short newByte, char[] output, int outEnd)
        throws UnknownCharacterException, MalformedInputException,
               ConversionBufferFullException
    {
        if ((newByte >= 0x00 && newByte <= 0x1F) || // C0 
            (newByte >= 0x80 && newByte <= 0x9F)) { // C1
            char newChar;

            switch (newByte) {
            case 0x1B:
                state = ESCAPE_SEQUENCE;
                queue.write(newByte);
                return;
            case 0x9B:
                state = CONTROL_SEQUENCE_PIF;
                versionSequenceAllowed = false;
                queue.write(newByte);
                return;
            case 0x09:
                versionSequenceAllowed = false;
                newChar = '\t';
                break;
            case 0x0A:
                versionSequenceAllowed = false;
                newChar = '\n';
                break;
            default:
                versionSequenceAllowed = false;
                return;
            }

            if (charOff < outEnd) {
                output[charOff++] = newChar;
            } else {
                throw new ConversionBufferFullException();
            }
        } else {
            ByteToCharConverter converter;
            boolean high;
            versionSequenceAllowed = false;

            if (newByte >= 0x20 && newByte <= 0x7F) {
                converter = glConverter;
                high = glHigh;
            } else /* if (newByte >= 0xA0 && newByte <= 0xFF) */ {
                converter = grConverter;
                high = grHigh;
            } 

            if (lastConverter != null && converter != lastConverter) {
                flushConverter(lastConverter, output, outEnd);
            }
            lastConverter = converter;

            if (converter != null) {
                byteBuf[0] = (byte)newByte;
                if (high) {
                    byteBuf[0] |= 0x80;
                } else {
                    byteBuf[0] &= 0x7F;
                }

                try {
                    charOff += converter.convert(byteBuf, 0, 1, output,
                                                 charOff, outEnd);
                } catch (MalformedInputException e) {
                    badInputLength = converter.getBadInputLength();
                    throw e;
                }
            } else if (charOff < outEnd) {
                output[charOff++] = (subChars.length > 0) ? subChars[0] : 0;
            } else {
                throw new ConversionBufferFullException();
            }
        }
    }

    private void nonStandardBytes(short newByte, char[] output, int outEnd)
        throws UnknownCharacterException, MalformedInputException,
               ConversionBufferFullException
    {
        if (nonStandardConverter != null) {
            byteBuf[0] = (byte)newByte;
            try {
                charOff += nonStandardConverter.convert(byteBuf, 0, 1, output,
                                                        charOff, outEnd);
            } catch (MalformedInputException e) {
                badInputLength = nonStandardConverter.getBadInputLength();
                throw e;
            }
        } else if (charOff < outEnd) {
            output[charOff++] = (subChars.length > 0) ? subChars[0] : 0;
        } else {
            throw new ConversionBufferFullException();
        }

        ext_offset++;
        if (ext_offset >= ext_count) {
            ext_offset = ext_count = 0;
            state = NORMAL_BYTES;
            flushConverter(nonStandardConverter, output, outEnd);
            nonStandardConverter = null;
        }
    }

    private void escapeSequence(short newByte) throws MalformedInputException {
        switch (newByte) {
        case 0x23:
            state = VERSION_SEQUENCE_V;
            break;
        case 0x24:
            state = CHARSET_NGIIF;
            versionSequenceAllowed = false;
            break;
        case 0x25:
            state = CHARSET_NONSTANDARD_FOML;
            versionSequenceAllowed = false;
            break;
        case 0x28:
            state = CHARSET_LIIF;
            versionSequenceAllowed = false;
            break;
        case 0x29:
        case 0x2D:
            state = CHARSET_RIIF;
            versionSequenceAllowed = false;
            break;
        default:
            escapeSequenceOther(newByte);
            // escapeSequenceOther will write to queue if appropriate
            return;
        }

        queue.write(newByte);
    }

    /**
     * Test for unknown, but valid, escape sequences.
     */
    private void escapeSequenceOther(short newByte)
        throws MalformedInputException
    {
        if (newByte >= 0x20 && newByte <= 0x2F) {
            // {I}
            state = ESCAPE_SEQUENCE_OTHER;
            versionSequenceAllowed = false;
            queue.write(newByte);
        } else if (newByte >= 0x30 && newByte <= 0x7E) {
            // F -- end of sequence
            state = NORMAL_BYTES;
            versionSequenceAllowed = false;
            queue.reset();
        } else {
            malformedInput(ERR_ESCBYTE);
        }
    }

    /**
     * Parses directionality, as well as unknown, but valid, control sequences.
     */
    private void controlSequence(short newByte) throws MalformedInputException
    {
        if (newByte >= 0x30 && newByte <= 0x3F) {
            // {P}
            if (state == CONTROL_SEQUENCE_IF) {
                // P no longer allowed
                malformedInput(ERR_CTRLPI);
            }
            queue.write(newByte);
        } else if (newByte >= 0x20 && newByte <= 0x2F) {
            // {I}
            state = CONTROL_SEQUENCE_IF;
            queue.write(newByte);
        } else if (newByte >= 0x40 && newByte <= 0x7E) {
            // F -- end of sequence
            state = NORMAL_BYTES;
            queue.reset();
        } else {
            malformedInput(ERR_CTRLBYTE);
        }
    }

    private void versionSequence(short newByte) throws MalformedInputException 
    {
        if (state == VERSION_SEQUENCE_V) {
            if (newByte >= 0x20 && newByte <= 0x2F) {
                state = VERSION_SEQUENCE_TERM;
                queue.write(newByte);
            } else {
                escapeSequenceOther(newByte);
            }
        } else /* if (state == VERSION_SEQUENCE_TERM) */ {
            switch (newByte) {
            case 0x30:
                if (!versionSequenceAllowed) {
                    malformedInput(ERR_VERSTART);
                }

                // OK to ignore extensions
                versionSequenceAllowed = false;
                state = NORMAL_BYTES;
                queue.reset();
                break;
            case 0x31:
                malformedInput((versionSequenceAllowed)
                               ? ERR_VERMANDATORY : ERR_VERSTART);
            default:
                escapeSequenceOther(newByte);
                break;
            }
        }
    }

    private void charset94N(short newByte) throws MalformedInputException {
        switch (newByte) {
        case 0x28:
            state = CHARSET_NLIIF;
            break;
        case 0x29:
            state = CHARSET_NRIIF;
            break;
        default:
            escapeSequenceOther(newByte);
            // escapeSequenceOther will write byte if appropriate
            return;
        }

        queue.write(newByte);
    }

    private void charset94NL(short newByte, char[] output, int outEnd)
        throws ConversionBufferFullException, MalformedInputException,
               UnknownCharacterException
    {
        if (newByte >= 0x21 &&
            newByte <= (state == CHARSET_NLIIF ? 0x23 : 0x2F)) {
            // {I}
            state = CHARSET_NLIF;
            queue.write(newByte);
        } else if (newByte >= 0x40 && newByte <= 0x7E) {
            // F
            switchConverter(newByte, output, outEnd);
        } else {
            escapeSequenceOther(newByte);
        }
    }

    private void charset94NR(short newByte, char[] output, int outEnd)
        throws ConversionBufferFullException, MalformedInputException,
               UnknownCharacterException
    {
        if (newByte >= 0x21 &&
            newByte <= (state == CHARSET_NRIIF ? 0x23 : 0x2F)) {
            // {I}
            state = CHARSET_NRIF;
            queue.write(newByte);
        } else if (newByte >= 0x40 && newByte <= 0x7E) {
            // F
            switchConverter(newByte, output, outEnd);
        } else {
            escapeSequenceOther(newByte);
        }
    }

    private void charset9496L(short newByte, char[] output, int outEnd)
        throws ConversionBufferFullException, MalformedInputException,
               UnknownCharacterException
    {
        if (newByte >= 0x21 &&
            newByte <= (state == CHARSET_LIIF ? 0x23 : 0x2F)) {
            // {I}
            state = CHARSET_LIF;
            queue.write(newByte);
        } else if (newByte >= 0x40 && newByte <= 0x7E) {
            // F
            switchConverter(newByte, output, outEnd);
        } else {
            escapeSequenceOther(newByte);
        }
    }

    private void charset9496R(short newByte, char[] output, int outEnd)
        throws ConversionBufferFullException, MalformedInputException,
               UnknownCharacterException
    {
        if (newByte >= 0x21 &&
            newByte <= (state == CHARSET_RIIF ? 0x23 : 0x2F)) {
            // {I}
            state = CHARSET_RIF;
            queue.write(newByte);
        } else if (newByte >= 0x40 && newByte <= 0x7E) {
            // F
            switchConverter(newByte, output, outEnd);
        } else {
            escapeSequenceOther(newByte);
        }
    }

    private void charsetNonStandard(short newByte, char[] output, int outEnd)
        throws ConversionBufferFullException, MalformedInputException,
               UnknownCharacterException
    {
        switch (state) {
        case CHARSET_NONSTANDARD_FOML:
            if (newByte == 0x2F) {
                state = CHARSET_NONSTANDARD_OML;
                queue.write(newByte);
            } else {
                escapeSequenceOther(newByte);
            }
            break;
        case CHARSET_NONSTANDARD_OML:
            if (newByte >= 0x30 && newByte <= 0x34) {
                state = CHARSET_NONSTANDARD_ML;
                queue.write(newByte);
            } else if (newByte >= 0x35 && newByte <= 0x3F) {
                state = EXTENSION_ML;
                queue.write(newByte);
            } else {
                escapeSequenceOther(newByte);
            }
            break;
        case CHARSET_NONSTANDARD_ML:
            ext_count = (newByte & 0x7F) * 0x80;
            state = CHARSET_NONSTANDARD_L;
            break;
        case CHARSET_NONSTANDARD_L:
            ext_count = ext_count + (newByte & 0x7F);
            state = (ext_count > 0) ? CHARSET_NONSTANDARD : NORMAL_BYTES;
            break;
        case CHARSET_NONSTANDARD:
            if (newByte == 0x3F || newByte == 0x2A) {
                queue.reset(); // In this case, only current byte is bad.
                malformedInput(ERR_ENCODINGBYTE);
            }
            ext_offset++;
            if (ext_offset >= ext_count) {
                ext_offset = ext_count = 0;
                state = NORMAL_BYTES;
                queue.reset();
                encodingQueue.reset();
            } else if (newByte == 0x02) {
                // encoding name terminator
                switchConverter((short)0, output, outEnd);
            } else {
                encodingQueue.write(newByte);
            }
            break;
        default:
            error(ERR_ILLSTATE);
        }
    }

    private void extension(short newByte) {
        switch (state) {
        case EXTENSION_ML:
            ext_count = (newByte & 0x7F) * 0x80;
            state = EXTENSION_L;
            break;
        case EXTENSION_L:
            ext_count = ext_count + (newByte & 0x7F);
            state = (ext_count > 0) ? EXTENSION : NORMAL_BYTES;
            break;
        case EXTENSION:
            // Consume 'count' bytes. Don't bother putting them on the queue.
            // There may be too many and we can't do anything with them anyway.
            ext_offset++;
            if (ext_offset >= ext_count) {
                ext_offset = ext_count = 0;
                state = NORMAL_BYTES;
                queue.reset();
            }
            break;
        default:
            error(ERR_ILLSTATE);
        }
    }

    /**
     * Preconditions:
     *   1. 'queue' contains ControlSequence.escSequence
     *   2. 'encodingQueue' contains ControlSequence.encoding
     */
    private void switchConverter(short lastByte, char[] output, int outEnd)
        throws ConversionBufferFullException, MalformedInputException,
               UnknownCharacterException
    {
        ByteToCharConverter converter = null;
        boolean high = false;
        byte[] escSequence;
        byte[] encoding = null;

        if (lastByte != 0) {
            queue.write(lastByte);
        }

        escSequence = queue.toByteArray();
        queue.reset();

        if (state == CHARSET_NONSTANDARD) {
            encoding = encodingQueue.toByteArray();
            encodingQueue.reset();
            converter = CompoundTextSupport.
                getNonStandardConverter(escSequence, encoding);
        } else {
            converter = CompoundTextSupport.getStandardConverter(escSequence);
            high = CompoundTextSupport.getHighBit(escSequence);
        }
        
        if (converter != null) {
            initConverter(converter);
        } else if (!subMode) {
            String encodingStr = "";
            if (encoding != null) {
                try {
                    encodingStr = new String(encoding, "ISO-8859-1");
                } catch (UnsupportedEncodingException e) {
                    error(ERR_LATIN1);
                }
            } else if (escSequence.length > 0) {
                char[] buf = new char[escSequence.length * 3 - 1];
                int in = 0, out = 0;
                while (true) {
                    byte b = escSequence[in++]; 
                    int sa = (b >> 4) & 0x0F, sb = b & 0x0F;
                    buf[out++] = (sa < 0x0A)
                        ? (char)('0' + sa) : (char)('A' + sa - 0x0A);
                    buf[out++] = (sb < 0x0A)
                        ? (char)('0' + sb) : (char)('A' + sb - 0x0A);

                    if (in < escSequence.length) {
                        buf[out++] = ' ';
                    } else {
                        break;
                    }
                }
                encodingStr = new String(buf);
            }
            throw new UnknownCharacterException(ERR_ENCODING + encodingStr);
        }

        if (state == CHARSET_NLIIF || state == CHARSET_NLIF ||
            state == CHARSET_LIIF || state == CHARSET_LIF)
        {
            if (lastConverter == glConverter) {
                flushConverter(glConverter, output, outEnd);
            }
            glConverter = lastConverter = converter;
            glHigh = high;
            state = NORMAL_BYTES;
        } else if (state == CHARSET_NRIIF || state == CHARSET_NRIF ||
                   state == CHARSET_RIIF || state == CHARSET_RIF) {
            if (lastConverter == grConverter) {
                flushConverter(grConverter, output, outEnd);
            }
            grConverter = lastConverter = converter;
            grHigh = high;
            state = NORMAL_BYTES;
        } else if (state == CHARSET_NONSTANDARD) {
            if (lastConverter != null) {
                flushConverter(lastConverter, output, outEnd);
                lastConverter = null;
            }
            nonStandardConverter = converter;
            state = NONSTANDARD_BYTES;
        } else {
            error(ERR_ILLSTATE);
        }
    }

    private void flushConverter(ByteToCharConverter converter, char[] output,
                                int outEnd)
        throws ConversionBufferFullException, MalformedInputException
    {
        try {
            charOff += converter.flush(output, charOff, outEnd);
        } catch (MalformedInputException e) {
            badInputLength = converter.getBadInputLength();
            throw e;
        }
    }

    private void malformedInput(String msg) throws MalformedInputException {
        badInputLength = queue.size() + 1 /* current byte */ ;
        queue.reset();
        throw new MalformedInputException(msg);
    }

    private void error(String msg) {
        // For now, throw InternalError. Convert to 'assert' keyword later.
        throw new InternalError(msg);
    }

    public int flush(char[] output, int outStart, int outEnd)
        throws ConversionBufferFullException, MalformedInputException
    {
        int retval;
        try {
            retval = (lastConverter != null)
                ? lastConverter.flush(output, outStart, outEnd)
                : 0;

            if (state != NORMAL_BYTES) {
                throw new MalformedInputException(ERR_FLUSH);
            }

            reset();
            return retval;
        } catch (MalformedInputException e) {
            reset();
            throw e;
        }
    }

    /**
     * Resets the converter.
     * Call this method to reset the converter to its initial state
     */
    public void reset() {
        state = NORMAL_BYTES;
        byteOff = charOff = badInputLength = ext_count = ext_offset = 0;
        versionSequenceAllowed = true;
        queue.reset();
        encodingQueue.reset();
        nonStandardConverter = lastConverter = null;
        glHigh = false;
        grHigh = true;
        try {
            // Initial state in ISO 2022 designates Latin-1 charset.
            glConverter = ByteToCharConverter.getConverter("ASCII");
            grConverter = ByteToCharConverter.getConverter("ISO8859_1");
        } catch (UnsupportedEncodingException e) {
            error(ERR_LATIN1);
        }
        initConverter(glConverter);
        initConverter(grConverter);
    }

    public void setSubstitutionMode(boolean doSub) {
        super.setSubstitutionMode(doSub);
        if (glConverter != null) {
            glConverter.setSubstitutionMode(doSub);
        }
        if (grConverter != null) {
            grConverter.setSubstitutionMode(doSub);
        }
        if (nonStandardConverter != null) {
            nonStandardConverter.setSubstitutionMode(doSub);
        }
    }

    public void setSubstitutionChars(char[] c)
        throws IllegalArgumentException
    {
        super.setSubstitutionChars(c);
        if (glConverter != null) {
            glConverter.setSubstitutionChars(c);
        }
        if (grConverter != null) {
            grConverter.setSubstitutionChars(c);
        }
        if (nonStandardConverter != null) {
            nonStandardConverter.setSubstitutionChars(c);
        }
    }

    private void initConverter(ByteToCharConverter conv) {
        conv.setSubstitutionMode(subMode);
        conv.setSubstitutionChars(subChars);
    }
}
