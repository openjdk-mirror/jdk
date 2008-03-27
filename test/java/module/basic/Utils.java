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

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/*
 * Utils.java
 *
 * Created on Jun 21, 2007, 7:56:11 AM
 *
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.util.ArrayList;
import java.util.List;


public class Utils {

    // All static
    private Utils() {}

    private static BasicLauncherTests blt;

    static String baseName(File f) {
        return baseName(f.getName());
    }

    static String baseName(String filename) {
        return filename.substring(0,filename.lastIndexOf("."));
    }

    static final FileFilter CLASS_FILTER = new FileFilter() {
        public boolean accept(File pathname) {
            return pathname.getName().endsWith(blt.CLASS_EXT);
        }
    };

    static final FileFilter JAM_FILTER = new FileFilter() {
        public boolean accept(File pathname) {
            return pathname.getName().endsWith(blt.JAM_EXT);
        }
    };

    static final FileFilter DIRECTORY_FILTER = new FileFilter() {
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    };

    // Makes a jam file in dst with contents that are in srcDir.
    static void makeJam(File srcDir, File dst) {
        sun.module.tools.Jam jamtool =
                new sun.module.tools.Jam(System.out, System.err, "JamCreator");
        String[] args = new String[] {
            "cfsS",
            dst.getAbsolutePath(),
            srcDir.getName(),
            srcDir.getAbsolutePath(),
            "-C",
            srcDir.getAbsolutePath(),
            srcDir.getName()
        };
        blt.abort(jamtool.run(args), "JamCreator Failed");
    }

    // Makes a jar file in dst with contents that are in srcDir.
    static void makeJar(File srcDir, File dst, String entry) {
        sun.tools.jar.Main jartool =
                new sun.tools.jar.Main(System.out, System.err, "JarCreator");
        String[] args;
        if (entry != null) {
            args = new String[] {
                "cfe",
                dst.getAbsolutePath(),
                entry,
                "-C",
                srcDir.getAbsolutePath(),
                "."
            };
        } else {
            args = new String[]{
                "cf",
                dst.getAbsolutePath(),
                "-C",
                srcDir.getAbsolutePath(),
                "."
            };
        }
        blt.abort(jartool.run(args), "JarCreator Failed") ;
    }

    // A very fast copying method for files, takes advantage of the OS.
    static void copyFile(File src, File dst) throws IOException {
        if (src.isDirectory()) {
            dst.mkdirs();
            return;
        } else {
            File baseDirFile = dst.getParentFile();
            if (!baseDirFile.exists()) baseDirFile.mkdirs();
        }

        FileChannel srcChannel = (new FileInputStream(src)).getChannel();
        FileChannel dstChannel = (new FileOutputStream(dst)).getChannel();

        long retval = srcChannel.transferTo(0, src.length(), dstChannel);
        if (src.length() != dst.length()) {
            throw new IOException("file copy failed for " + src);
        }
    }

    static void recursiveDelete(File dir) throws IOException {
        if (dir.isFile()) {
            dir.delete();
        } else if (dir.isDirectory()) {
            File[] entries = dir.listFiles();
            for (File entry : entries) {
                if (entry.isDirectory()) {
                    recursiveDelete(entry);
                }
                entry.delete();
            }
            dir.delete();
        }
    }

    static File[] getDirs(String dir) {
        return getDirs(new File(dir));
    }

    static File[] getDirs(File dirFile) {
        return dirFile.listFiles(DIRECTORY_FILTER);
    }
}
