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

package sun.module.repository;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.module.ModuleArchiveInfo;
import java.net.URL;
import java.util.List;

/**
 * Writes XML for a repository-metadata.xml file.
 * @since 1.7
 */
public class MetadataXMLWriter {
    private final PrintWriter pw;
    private int indent = 0;

    // Amount of indentation
    private static final int WIDTH = 4;

    // Used for indentation
    private static final String spaces32 = "                                ";

    /**
     * Writes the repository's repository-metadata.xml file based on {@code
     * contents}.
     * @param mai {@code ModuleArchiveInfo} on behalf of which the metadata is
     * written.
     * @param writeMAI if true, then write the given {@code mai} in addition
     * to the repository's other contents.
     */
    public static void write(
            URLRepository repo,
            List<JamModuleArchiveInfo> maiList,
            JamModuleArchiveInfo mai,
            boolean writeMAI) throws IOException {
        URL repoMD = new URL(
            repo.getSourceLocation().toURL().toExternalForm()
            + "/repository-metadata.xml");
        File repoMDFile = new File(repoMD.getFile());
        File repoMDDir = repoMDFile.getParentFile();
        File tmpRepoMDFile = new File(repoMDDir, "repository-metadata.xml.tmp");
        if (tmpRepoMDFile.exists()) {
            if (!tmpRepoMDFile.delete()) {
                throw new IOException(
                    "Cannot update repository metadata file for "
                        + mai.getName()
                        + ": cannot create temporary repository metadata file "
                        + tmpRepoMDFile);
            }
        }
        MetadataXMLWriter writer = new MetadataXMLWriter(tmpRepoMDFile);
        writer.begin();
        for (JamModuleArchiveInfo m : maiList) {
            if (!writeMAI && m.equals(mai)) {
                // Don't write this ModuleArchiveInfo if it matches that given
            } else {
                writer.writeModule(m);
            }
        }
        if (writeMAI) {
            writer.writeModule(mai);
        }

        if (!writer.end()) {
            throw new IOException(
                "Cannot update repository metadata file for "
                    + mai.getName()
                    + ": failure while writing temporary repository metadata file "
                    + tmpRepoMDFile);
        }

        File prev = new File(repoMDFile.getCanonicalPath() + ".prev");
        prev.delete();
        if (!repoMDFile.renameTo(prev)) {
            throw new IOException(
                "Cannot update repository metadata file for "
                    + mai.getName()
                    + ": cannot rename " + repoMDFile + " to " + prev);
        }
        prev.deleteOnExit();

        if (!tmpRepoMDFile.renameTo(repoMDFile)) {
            throw new IOException(
                "Cannot update repository metadata file for "
                    + mai.getName()
                    + ": cannot create updated repository-metadata.xml file"
                    + " by renaming " + tmpRepoMDFile + " to " + repoMDFile);
        }
    }

    private MetadataXMLWriter(File out) throws IOException {
        pw = new PrintWriter(
            new BufferedOutputStream(
                new FileOutputStream(out)), false);
    }

    void begin() {
        pw.println("<modules>");
        indent++;
    }

    boolean end() {
        boolean rc = false;
        indent--;
        pw.println("</modules>");
        rc = pw.checkError();
        pw.close();
        return !rc; // Return false if error
    }

    void writeModule(JamModuleArchiveInfo mai) {
        output("<module>");
        indent++;
        output("<name>" + mai.getName() + "</name>");
        output("<version>" + mai.getVersion().toString() + "</version>");
        if (!mai.isPlatformArchNeutral())  {
            output("<platform-binding>");
            indent++;
            output("<platform>" + mai.getPlatform() + "</platform>");
            output("<arch>" + mai.getArch() + "</arch>");
            indent--;
            output("</platform-binding>");
        }

        // Entries with <path> are not created, since there's no way to
        // specify that via Repository.install().

        indent--;
        output("</module>");
    }

    void output(String s) {
        pw.print(spaces32.substring(0, indent * WIDTH));
        pw.println(s);
    }
}
