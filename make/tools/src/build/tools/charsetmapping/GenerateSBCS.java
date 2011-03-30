/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
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
import java.util.Iterator;
import java.util.regex.*;
import java.nio.charset.*;

public class GenerateSBCS {

    public static void genSBCS(String args[]) throws Exception {

        Scanner s = new Scanner(new File(args[0], args[2]));
        while (s.hasNextLine()) {
            String line = s.nextLine();
            if (line.startsWith("#") || line.length() == 0)
                continue;
            String[] fields = line.split("\\s+");
            if (fields.length < 5) {
                System.err.println("Misconfiged sbcs line <" + line + ">?");
                continue;
            }
            String clzName = fields[0];
            String csName  = fields[1];
            String hisName = fields[2];
            boolean isASCII = Boolean.valueOf(fields[3]).booleanValue();
            String pkgName  = fields[4];
            System.out.println(clzName + "," + csName + "," + hisName + "," + isASCII + "," + pkgName);

            genClass(args[0], args[1], "SingleByte-X.java",
                     clzName, csName, hisName, pkgName, isASCII);
        }
    }

    private static void toString(char[] sb, int off, int end,
                                 Formatter out, String closure,
                                 boolean comment) {
        while (off < end) {
            out.format("        \"", new Object[] { });
            for (int j = 0; j < 8; j++) {
                if (off == end)
                    break;
                char c = sb[off++];
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
            if (comment) {
                if (off == end)
                    out.format("\" %s      // 0x%02x - 0x%02x%n",
                               new Object[] { closure, new Integer(off-8), new Integer(off-1) });
                else
                    out.format("\" +      // 0x%02x - 0x%02x%n",
                               new Object[] { new Integer(off-8), new Integer(off-1) });
            } else {
                if (off == end)
                    out.format("\"%s%n", new Object[] { closure });
                else
                    out.format("\" +%n", new Object[] { });
            }
        }
    }

    static Pattern sbmap = Pattern.compile("0x(\\p{XDigit}++)\\s++U\\+(\\p{XDigit}++)(\\s++#.*)?");

    private static void genClass(String srcDir, String dstDir,
                                 String template,
                                 String clzName,
                                 String csName,
                                 String hisName,
                                 String pkgName,
                                 boolean isASCII)
        throws Exception
    {
        StringBuilder b2cSB = new StringBuilder();
        StringBuilder b2cNRSB = new StringBuilder();
        StringBuilder c2bNRSB = new StringBuilder();

        char[] sb = new char[0x100];
        char[] c2bIndex = new char[0x100];
        int    c2bOff = 0;
        Arrays.fill(sb, CharsetMapping.UNMAPPABLE_DECODING);
        Arrays.fill(c2bIndex, CharsetMapping.UNMAPPABLE_DECODING);

        // (1)read in .map to parse all b->c entries
        FileInputStream in = new FileInputStream(
                                 new File(srcDir, clzName + ".map"));
        CharsetMapping.Parser p = new CharsetMapping.Parser(in, sbmap);
        CharsetMapping.Entry  e = null;

        while ((e = p.next()) != null) {
            sb[e.bs] = (char)e.cp;
            if (c2bIndex[e.cp>>8] == CharsetMapping.UNMAPPABLE_DECODING) {
                c2bOff += 0x100;
                c2bIndex[e.cp>>8] = 1;
            }
        }

        Formatter fm = new Formatter(b2cSB);
        fm.format("%n", new Object[] { });

        // vm -server shows cc[byte + 128] access is much faster than
        // cc[byte&0xff] so we output the upper segment first
        toString(sb, 0x80, 0x100, fm, "+", true);
        toString(sb, 0x00, 0x80,  fm, ";", true);
        fm.close();

        // (2)now the .nr file which includes "b->c" non-roundtrip entries
        File f = new File(srcDir, clzName + ".nr");
        if (f.exists()) {
            in = new FileInputStream(f);
            fm = new Formatter(b2cNRSB);
            p = new CharsetMapping.Parser(in, sbmap);
            e = null;

            fm.format("// remove non-roundtrip entries%n", new Object[] { });
            fm.format("        b2cMap = b2cTable.toCharArray();%n", new Object[] { });
            while ((e = p.next()) != null) {
                fm.format("        b2cMap[%d] = CharsetMapping.UNMAPPABLE_DECODING;%n",
                          new Object[] { new Integer((e.bs>=0x80)?(e.bs-0x80):(e.bs+0x80)) });
            }
            fm.close();
        }

        // (3)finally the .c2b file which includes c->b non-roundtrip entries
        f = new File(srcDir, clzName + ".c2b");
        if (f.exists()) {
            in = new FileInputStream(f);
            fm = new Formatter(c2bNRSB);
            p = new CharsetMapping.Parser(in, sbmap);
            e = null;
            ArrayList es = new ArrayList();
            while ((e = p.next()) != null) {
                if (c2bIndex[e.cp>>8] == CharsetMapping.UNMAPPABLE_DECODING) {
                    c2bOff += 0x100;
                    c2bIndex[e.cp>>8] = 1;
                }
                es.add(e);
            }
            fm.format("// non-roundtrip c2b only entries%n", new Object[] { });
            if (es.size() < 100) {
                fm.format("        c2bNR = new char[%d];%n", new Object[] { new Integer(es.size() * 2) });
                int i = 0;
                Iterator iter = es.iterator();
                while (iter.hasNext()) {
                	CharsetMapping.Entry entry = (CharsetMapping.Entry)iter.next();
                    fm.format("        c2bNR[%d] = 0x%x; c2bNR[%d] = 0x%x;%n",
                              new Object[] { new Integer(i++), new Integer(entry.bs), new Integer(i++), new Integer(entry.cp) });
                }
            } else {
                char[] cc = new char[es.size() * 2];
                int i = 0;
                Iterator iter = es.iterator();
                while (iter.hasNext()) {
                	CharsetMapping.Entry entry = (CharsetMapping.Entry)iter.next();
                    cc[i++] = (char)entry.bs;
                    cc[i++] = (char)entry.cp;
                }
                fm.format("        c2bNR = (%n", new Object[] { });
                toString(cc, 0, i,  fm, ").toCharArray();", false);
            }
            fm.close();
        }

        // (4)it's time to generate the source file
        String b2c = b2cSB.toString();
        String b2cNR = b2cNRSB.toString();
        String c2bNR = c2bNRSB.toString();

        Scanner s = new Scanner(new File(srcDir, template));
        PrintStream out = new PrintStream(new FileOutputStream(
                              new File(dstDir, clzName + ".java")));

        while (s.hasNextLine()) {
            String line = s.nextLine();
            int i = line.indexOf("$");
            if (i == -1) {
                out.println(line);
                continue;
            }
            if (line.indexOf("$PACKAGE$", i) != -1) {
                line = replace(line,"$PACKAGE$", pkgName);
            }
            if (line.indexOf("$NAME_CLZ$", i) != -1) {
                line = replace(line,"$NAME_CLZ$", clzName);
            }
            if (line.indexOf("$NAME_CS$", i) != -1) {
                line = replace(line,"$NAME_CS$", csName);
            }
            if (line.indexOf("$NAME_ALIASES$", i) != -1) {
                if ("sun.nio.cs".equals(pkgName))
                    line = replace(line,"$NAME_ALIASES$",
                                        "StandardCharsets.aliases_" + clzName);
                else
                    line = replace(line,"$NAME_ALIASES$",
                                        "ExtendedCharsets.aliasesFor(\"" + csName + "\")");
            }
            if (line.indexOf("$NAME_HIS$", i) != -1) {
                line = replace(line,"$NAME_HIS$", hisName);
            }
            if (line.indexOf("$CONTAINS$", i) != -1) {
                if (isASCII)
                    line = "        return ((cs.name().equals(\"US-ASCII\")) || (cs instanceof " + clzName + "));";
                else
                    line = "        return (cs instanceof " + clzName + ");";
            }
            if (line.indexOf("$B2CTABLE$") != -1) {
                line = replace(line,"$B2CTABLE$", b2c);
            }
            if (line.indexOf("$C2BLENGTH$") != -1) {
                line = replace(line,"$C2BLENGTH$", "0x" + Integer.toString(c2bOff, 16));
            }
            if (line.indexOf("$NONROUNDTRIP_B2C$") != -1) {
                if (b2cNR.length() == 0)
                    continue;
                line = replace(line,"$NONROUNDTRIP_B2C$", b2cNR);
            }

            if (line.indexOf("$NONROUNDTRIP_C2B$") != -1) {
                if (c2bNR.length() == 0)
                    continue;
                line = replace(line,"$NONROUNDTRIP_C2B$", c2bNR);
            }
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
