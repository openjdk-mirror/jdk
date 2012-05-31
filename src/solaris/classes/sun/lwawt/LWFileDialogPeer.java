/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.lwawt;

import java.awt.FileDialog;
import java.awt.peer.FileDialogPeer;
import java.awt.Point;
import java.io.File;
import java.io.FilenameFilter;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

final class LWFileDialogPeer extends LWWindowPeer
        implements FileDialogPeer {

    private JFileChooser fileChooser;

    LWFileDialogPeer(FileDialog target, PlatformComponent platformComponent,
                     PlatformWindow platformWindow) {
        super(target, platformComponent, platformWindow);
    }

    @Override
    protected JComponent createDelegate() {
    	return new JFileChooserDelegate();
    }

    @Override
    public void initialize() {
        super.initialize();
        FileDialog fd = (FileDialog)getTarget();
        setFile(fd.getFile());
        setDirectory(fd.getDirectory());
        setFilenameFilter(fd.getFilenameFilter());
    }

    @Override
    public void setFile(String file) {
        if (file != null) {
            synchronized (getDelegateLock()) {
                // Ughh..
                JFileChooser delegate = (JFileChooser)getDelegate();
                delegate.setSelectedFile(new File(file));
            }
        }
    }

    @Override
    public void setDirectory(String dir) {
      	if (dir != null) {
            synchronized (getDelegateLock()) {
                JFileChooser delegate = (JFileChooser)getDelegate();
                delegate.setCurrentDirectory(new File(dir));
            }
      	}
    }

    @Override
    public void setFilenameFilter(FilenameFilter filter) {
    	if (filter != null) {
            synchronized (getDelegateLock()) {
                JFileChooser delegate = (JFileChooser)getDelegate();
                delegate.setFileFilter(new ProxyFilter(filter));
            }
        }
    }
    
    private final class JFileChooserDelegate extends JFileChooser {

        // Empty non private constructor was added because access to this
        // class shouldn't be emulated by a synthetic accessor method.
        JFileChooserDelegate() {
            super();
        }

        @Override
        public boolean hasFocus() {
            return getTarget().hasFocus();
        }

        //Needed for proper popup menu location
        @Override
        public Point getLocationOnScreen() {
            return LWFileDialogPeer.this.getLocationOnScreen();
        }
    }

    private final class ProxyFilter extends FileFilter {

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

}
