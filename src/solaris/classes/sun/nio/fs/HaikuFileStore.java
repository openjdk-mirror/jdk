/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.fs;

import java.nio.file.attribute.*;
import java.util.*;
import java.io.IOException;

/**
 * Haiku implementation of FileStore
 */

class HaikuFileStore
    extends UnixFileStore
{
    // used when checking if extended attributes are enabled or not
    private volatile boolean xattrChecked;
    private volatile boolean xattrEnabled;

    HaikuFileStore(UnixPath file) throws IOException {
        super(file);
    }

    HaikuFileStore(UnixFileSystem fs, UnixMountEntry entry) throws IOException {
        super(fs, entry);
    }

    /**
     * Finds, and returns, the mount entry for the file system where the file
     * resides.
     */
    @Override
    UnixMountEntry findMountEntry() throws IOException {
        HaikuFileSystem fs = (HaikuFileSystem)file().getFileSystem();

        try {
        	UnixMountEntry entry = new UnixMountEntry();
            HaikuNativeDispatcher.entryfordev(dev(), entry);
            return entry;
        } catch (UnixException x) {
            x.rethrowAsIOException(file());
        }

        throw new IOException("Mount point not found");
    }

    // returns true if extended attributes enabled on file system where given
    // file resides, returns false if disabled or unable to determine.
    private boolean isExtendedAttributesEnabled(UnixPath path) {
        try {
            int fd = path.openForAttributeAccess(false);
            try {
                // fgetxattr returns size if called with size==0
                HaikuNativeDispatcher.fgetxattr(fd, "user.java".getBytes(), 0L, 0);
                return true;
            } catch (UnixException e) {
                // attribute does not exist
                if (e.errno() == UnixConstants.ENODATA)
                    return true;
            } finally {
                UnixNativeDispatcher.close(fd);
            }
        } catch (IOException ignore) {
            // nothing we can do
        }
        return false;
    }

    @Override
    public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
        // support DosFileAttributeView and UserDefinedAttributeView if extended
        // attributes enabled
        if (type == DosFileAttributeView.class ||
            type == UserDefinedFileAttributeView.class)
        {
            // lookup fstypes.properties
            FeatureStatus status = checkIfFeaturePresent("user_xattr");
            if (status == FeatureStatus.PRESENT)
                return true;
            if (status == FeatureStatus.NOT_PRESENT)
                return false;

            // We can add one of these early exits for BFS

            // if file system is mounted with user_xattr option then assume
            // extended attributes are enabled
            if ((entry().hasOption("user_xattr")))
                return true;

            // user_xattr option not present but we special-case ext3/4 as we
            // know that extended attributes are not enabled by default.
            if (entry().fstype().equals("ext3") || entry().fstype().equals("ext4"))
                return false;

            // not ext3/4 so probe mount point
            if (!xattrChecked) {
                UnixPath dir = new UnixPath(file().getFileSystem(), entry().dir());
                xattrEnabled = isExtendedAttributesEnabled(dir);
                xattrChecked = true;
            }
            return xattrEnabled;
        }
        return super.supportsFileAttributeView(type);
    }

    @Override
    public boolean supportsFileAttributeView(String name) {
        if (name.equals("user"))
            return supportsFileAttributeView(UserDefinedFileAttributeView.class);
        return super.supportsFileAttributeView(name);
    }
}
