/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
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

package java.module;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Set;
import java.security.CodeSigner;

/**
 * This class represents the content of the module definition.
 *
 * <p> Unless otherwise specified, passing a <tt>null</tt> argument to any
 * method in this class will cause a {@link NullPointerException} to be thrown.
 * <p>
 * @see java.module.ModuleDefinition
 * @see java.security.CodeSigner
 *
 * @since 1.7
 */
public abstract class ModuleDefinitionContent {

    /**
     * Constructor used by subclasses.
     */
    protected ModuleDefinitionContent() {
        // empty
    }

    /**
     * Determines if an entry exists in the module definition.
     * <p>
     * The entry's name is specified as '/' separated paths, with no
     * leading '/'.
     *
     * @param name entry's name.
     * @return true if entry exists in the module definition; otherwise,
     *         returns false.
     */
    public abstract boolean hasEntry(String name);

    /**
     * Returns an entry in the module definition as an input stream.
     * <p>
     * The entry's name is specified as '/' separated paths, with no
     * leading '/'.
     *
     * @param name entry's name.
     * @return if entry exists, return input stream; otherwise, return null.
     * @throws IOException if an I/O error occurs.
     */
    public abstract InputStream getEntryAsStream(String name) throws IOException;

    /**
     * Returns an entry in the module definition as byte array.
     * <p>
     * The entry's name is specified as '/' separated paths, with no
     * leading '/'.
     *
     * @param name entry's name.
     * @return if an entry exists, return byte array; otherwise, returns null.
     * @throws IOException if an I/O error occurs.
     */
    public byte[] getEntryAsByteArray(String name) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(getEntryAsStream(name));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];

        int byteRead = 0;

        // Read the stream until it is EOF
        while ((byteRead = bis.read(buffer, 0, 8192)) != -1)
            baos.write(buffer, 0, byteRead);

        // Close input stream
        bis.close();

        // Convert to byte array
        return baos.toByteArray();
    }

    /**
     * Returns the set of the names of the entries in the module definition.
     * <p>
     * Each entry's name is in the form of '/' separated paths, with no
     * leading '/'.
     *
     * @return the set of the names of the entries in the module definition.
     */
    public abstract Set<String> getEntryNames();

    /**
     * Returns the path of the native library associated with the module
     * definition.
     *
     * @param libraryName the library name.
     * @return native library if it is found; otherwise, returns null.
     */
    public abstract File getNativeLibrary(String libraryName);

    /**
     * Returns the code signers associated with the module definition.
     *
     * @return code signers of the module definition.
     * @throws IOException if an I/O error occurs.
     */
    public abstract CodeSigner[] getCodeSigners() throws IOException;


    /**
     * Check if the entire content of the module definition is stored locally.
     *
     * @return true if the entire content of the module definition is stored
     *         locally. Otherwise, returns false.
     */
    public abstract boolean isDownloaded();
}
