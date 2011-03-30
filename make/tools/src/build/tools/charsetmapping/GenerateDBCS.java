/*
 * Copyright 2009 Sun Microsystems, Inc.  All Rights Reserved.
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

package build.tools.charsetmapping;
import java.io.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.nio.charset.*;

public class GenerateDBCS {
    // pattern used by this class to read in mapping table
    static Pattern mPattern = Pattern.compile("(?:0x)?(\\p{XDigit}++)\\s++(?:0x)?(\\p{XDigit}++)(?:\\s++#.*)?");
    public static void genDBCS(String args[]) throws Exception {

        Scanner s = new Scanner(new File(args[0], args[2]));
        while (s.hasNextLine()) {
            String line = s.nextLine();
            if (line.startsWith("#") || line.length() == 0)
                continue;
            String[] fields = line.split("\\s+");
            if (fields.length < 10) {
                System.err.println("Misconfiged sbcs line <" + line + ">?");
                continue;
            }
            String clzName = fields[0];
            String csName  = fields[1];
            String hisName = ("null".equals(fields[2]))?null:fields[2];
            String type = fields[3].toUpperCase();
            if ("BASIC".equals(type))
                type = "";
            else
                type = "_" + type;
            String pkgName  = fields[4];
            boolean isASCII = Boolean.valueOf(fields[5]).booleanValue();
            int    b1Min = toInteger(fields[6]);
            int    b1Max = toInteger(fields[7]);
            int    b2Min    = toInteger(fields[8]);
            int    b2Max    = toInteger(fields[9]);
            System.out.println(clzName + "," + csName + "," + hisName + "," + isASCII + "," + pkgName);
            genClass(args[0], args[1], "DoubleByte-X.java",
                    clzName, csName, hisName, pkgName,
                    isASCII, type,
                    b1Min, b1Max, b2Min, b2Max);
        }
    }

    private static int toInteger(String s) {
        if (s.startsWith("0x") || s.startsWith("0X"))
            return Integer.valueOf(s.substring(2), 16).intValue();
        else
            return new Integer(s).intValue();
    }

    private static void outString(Formatter out,
                                  char[] cc, int off, int end,
                                  String closure)
    {
        while (off < end) {
            out.format("        \"", new Object[] { });
            for (int j = 0; j < 8; j++) {
                if (off == end)
                    break;
                char c = cc[off++];
                switch (c) {
                case '\b':
                    out.format("\\b", new Object[] { }); break;
                case '\t':
                    out.format("\\t", new Object[] { }); break;
                case '\n':
                    out.format("\\n", new Object[] { }); break;
                case '\f':
                    out.format("\\f", new Object[] { }); break;
                case '\r':
                    out.format("\\r", new Object[] { }); break;
                case '\"':
                    out.format("\\\"", new Object[] { }); break;
                case '\'':
                    out.format("\\'", new Object[] { }); break;
                case '\\':
                    out.format("\\\\", new Object[] { }); break;
                default:
                    out.format("\\u%04X", new Object[] { new Integer(c & 0xffff) });
                }
            }
            if (off == end)
                out.format("\" %s%n", new Object[] { closure });
            else
                out.format("\" + %n", new Object[] { } );
        }
    }

    private static void outString(Formatter out,
                                  char[] db,
                                  int b1,
                                  int b2Min, int b2Max,
                                  String closure)
    {
        char[] cc = new char[b2Max - b2Min + 1];
        int off = 0;
        for (int b2 = b2Min; b2 <= b2Max; b2++) {
            cc[off++] = db[(b1 << 8) | b2];
        }
        outString(out, cc, 0, cc.length, closure);
    }

    private static void genClass(String srcDir, String dstDir, String template,
                                 String clzName,
                                 String csName,
                                 String hisName,
                                 String pkgName,
                                 boolean isASCII,
                                 String type,
                                 int b1Min, int b1Max,
                                 int b2Min, int b2Max)
        throws Exception
    {

        StringBuilder b2cSB = new StringBuilder();
        StringBuilder b2cNRSB = new StringBuilder();
        StringBuilder c2bNRSB = new StringBuilder();

        char[] db = new char[0x10000];
        char[] c2bIndex = new char[0x100];
        int c2bOff = 0x100;    // first 0x100 for unmappable segs

        Arrays.fill(db, CharsetMapping.UNMAPPABLE_DECODING);
        Arrays.fill(c2bIndex, CharsetMapping.UNMAPPABLE_DECODING);

        char[] b2cIndex = new char[0x100];
        Arrays.fill(b2cIndex, CharsetMapping.UNMAPPABLE_DECODING);

        // (1)read in .map to parse all b->c entries
        FileInputStream in = new FileInputStream(new File(srcDir, clzName + ".map"));
        CharsetMapping.Parser p = new CharsetMapping.Parser(in, mPattern);
        CharsetMapping.Entry  e = null;
        while ((e = p.next()) != null) {
            db[e.bs] = (char)e.cp;

            if (e.bs > 0x100 &&    // db
                b2cIndex[e.bs>>8] == CharsetMapping.UNMAPPABLE_DECODING) {
                b2cIndex[e.bs>>8] = 1;
            }

            if (c2bIndex[e.cp>>8] == CharsetMapping.UNMAPPABLE_DECODING) {
                c2bOff += 0x100;
                c2bIndex[e.cp>>8] = 1;
            }
        }
        Formatter fm = new Formatter(b2cSB);
        fm.format("%n    static final String b2cSBStr =%n", new Object[] { });
        outString(fm, db, 0x00, 0x100,  ";");

        fm.format("%n        static final String[] b2cStr = {%n", new Object[] { });
        for (int i = 0; i < 0x100; i++) {
            if (b2cIndex[i] == CharsetMapping.UNMAPPABLE_DECODING) {
                fm.format("            null,%n", new Object[] { });  //unmappable segments
            } else {
                outString(fm, db, i, b2Min, b2Max, ",");
            }
        }

        fm.format("        };%n", new Object[] { });
        fm.close();

        // (2)now parse the .nr file which includes "b->c" non-roundtrip entries
        File f = new File(srcDir, clzName + ".nr");
        if (f.exists()) {
            StringBuffer sb = new StringBuffer();
            in = new FileInputStream(f);
            p = new CharsetMapping.Parser(in, mPattern);
            e = null;
            while ((e = p.next()) != null) {
                // A <b,c> pair
                sb.append((char)e.bs);
                sb.append((char)e.cp);
            }
            char[] nr = sb.toString().toCharArray();
            fm = new Formatter(b2cNRSB);
            fm.format("String b2cNR =%n", new Object[] { });
            outString(fm, nr, 0, nr.length,  ";");
            fm.close();
        } else {
            b2cNRSB.append("String b2cNR = null;");
        }

        // (3)finally the .c2b file which includes c->b non-roundtrip entries
        f = new File(srcDir, clzName + ".c2b");
        if (f.exists()) {
            StringBuffer sb = new StringBuffer();
            in = new FileInputStream(f);
            p = new CharsetMapping.Parser(in, mPattern);
            e = null;
            while ((e = p.next()) != null) {
                // A <b,c> pair
                if (c2bIndex[e.cp>>8] == CharsetMapping.UNMAPPABLE_DECODING) {
                    c2bOff += 0x100;
                    c2bIndex[e.cp>>8] = 1;
                }
                sb.append((char)e.bs);
                sb.append((char)e.cp);
            }
            char[] nr = sb.toString().toCharArray();
            fm = new Formatter(c2bNRSB);
            fm.format("String c2bNR =%n", new Object[] { });
            outString(fm, nr, 0, nr.length,  ";");
            fm.close();
        } else {
            c2bNRSB.append("String c2bNR = null;");
        }

        // (4)it's time to generate the source file
        String b2c = b2cSB.toString();
        String b2cNR = b2cNRSB.toString();
        String c2bNR = c2bNRSB.toString();

        Scanner s = new Scanner(new File(srcDir, template));
        PrintStream out = new PrintStream(new FileOutputStream(
                              new File(dstDir, clzName + ".java")));
        if (hisName == null)
            hisName = "";

        while (s.hasNextLine()) {
            String line = s.nextLine();
            if (line.indexOf("$") == -1) {
                out.println(line);
                continue;
            }
            line = replace(line, "$PACKAGE$" , pkgName);
            line = replace(line, "$IMPLEMENTS$", (hisName == null)?
                                "" : "implements HistoricallyNamedCharset");
            line = replace(line, "$NAME_CLZ$", clzName);
            line = replace(line, "$NAME_ALIASES$",
                                "sun.nio.cs".equals(pkgName) ?
                                "StandardCharsets.aliases_" + clzName :
                                "ExtendedCharsets.aliasesFor(\"" + csName + "\")");
            line = replace(line, "$NAME_CS$" , csName);
            line = replace(line, "$CONTAINS$",
                                "MS932".equals(clzName)?
                                "return ((cs.name().equals(\"US-ASCII\")) || (cs instanceof JIS_X_0201) || (cs instanceof " + clzName + "));":
                                (isASCII ?
                                 "return ((cs.name().equals(\"US-ASCII\")) || (cs instanceof " + clzName + "));":
                                 "return (cs instanceof " + clzName + ");"));
            line = replace(line, "$HISTORICALNAME$",
                                (hisName == null)? "" :
                                "    public String historicalName() { return \"" + hisName + "\"; }");
            line = replace(line, "$DECTYPE$", type);
            line = replace(line, "$ENCTYPE$", type);
            line = replace(line, "$B1MIN$"   , "0x" + Integer.toString(b1Min, 16));
            line = replace(line, "$B1MAX$"   , "0x" + Integer.toString(b1Max, 16));
            line = replace(line, "$B2MIN$"   , "0x" + Integer.toString(b2Min, 16));
            line = replace(line, "$B2MAX$"   , "0x" + Integer.toString(b2Max, 16));
            line = replace(line, "$B2C$", b2c);
            line = replace(line, "$C2BLENGTH$", "0x" + Integer.toString(c2bOff, 16));
            line = replace(line, "$NONROUNDTRIP_B2C$", b2cNR);
            line = replace(line, "$NONROUNDTRIP_C2B$", c2bNR);

            out.println(line);
        }
        out.close();
    }
    
    	/**
     * Replaces each substring of this string that matches the literal target
     * sequence with the specified literal replacement sequence. The
     * replacement proceeds from the beginning of the string to the end, for
     * example, replacing "aa" with "b" in the string "aaa" will result in
     * "ba" rather than "ab".
     *
     * @param  target The sequence of char values to be replaced
     * @param  replacement The replacement sequence of char values
     * @return  The resulting string
     * @throws NullPointerException if <code>target</code> or
     *         <code>replacement</code> is <code>null</code>.
     * @since 1.5
     */
    public static String replace(String source, CharSequence target, CharSequence replacement) {
        return Pattern.compile(target.toString(), Pattern.LITERAL).matcher(
            source).replaceAll(Matcher.quoteReplacement(replacement.toString()));
    }

}
