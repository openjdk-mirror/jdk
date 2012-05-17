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

package sun.awt.peer.cacio;

import java.awt.FileDialog;
import java.awt.peer.FileDialogPeer;
import java.io.File;
import java.io.FilenameFilter;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

class CacioFileDialogPeer extends CacioDialogPeer
                          implements FileDialogPeer {

    private static class ProxyFilter extends FileFilter {

        private FilenameFilter target;
        ProxyFilter(FilenameFilter f) {
            target = f;
        }

        @Override
        public boolean accept(File f) {
            return target.accept(f.getAbsoluteFile().getParentFile(),
                                 f.getName());
        }

        @Override
        public String getDescription() {
            return "No description";
        }

    }

    private JFileChooser fileChooser;

    CacioFileDialogPeer(FileDialog d, PlatformWindowFactory pwf) {
        super(d, pwf);
    }

    @Override
    void postInitSwingComponent() {
        super.postInitSwingComponent();
        fileChooser = new JFileChooser();
        getSwingComponent().getContentPane().add(fileChooser);
        getSwingComponent().layout();
        FileDialog fd = (FileDialog) getAWTComponent();
        setFile(fd.getFile());
        setDirectory(fd.getDirectory());
        setFilenameFilter(fd.getFilenameFilter());
    }

    public void setFile(String file) {
        fileChooser.setSelectedFile(new File(file));
    }

    public void setDirectory(String dir) {
        System.err.println("setting dir: " + dir);
        fileChooser.setCurrentDirectory(new File(dir));
    }

    public void setFilenameFilter(FilenameFilter filter) {
        fileChooser.setFileFilter(new ProxyFilter(filter));
    }

}
