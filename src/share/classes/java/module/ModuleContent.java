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
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Set;
import java.security.CodeSigner;

/**
 * This interface represents the content of a module definition. The content
 * can be obtained from a {@code ModuleDefinition} instance using the
 * {@link ModuleDefinition#getModuleContent() <tt>getModuleContent()</tt>}
 * method.
 *
 * @see java.module.ModuleDefinition
 * @see java.security.CodeSigner
 *
 * @since 1.7
 */
public interface ModuleContent {

    /**
     * Returns true if the specified entry is found.
     * <p>
     * The entry's name is specified using {@code '/'} as path separator; it
     * has no leading {@code '/'}.
     *
     * @param name the name of the entry.
     * @return true if the specified entry is found; otherwise, return false.
     * @throws IOException if an I/O error occurs.
     */
    public boolean hasEntry(String name) throws IOException;

    /**
     * Returns the readable byte channel for the specified entry or
     * {@code null} if not found.
     * <p>
     * The entry's name is specified using {@code '/'} as path separator; it
     * has no leading {@code '/'}.
     *
     * @param name the name of the entry.
     * @return the readable byte channel for the specified entry or
     *         {@code null} if not found.
     * @throws IOException if an I/O error occurs.
     */
    public ReadableByteChannel getEntryAsChannel(String name) throws IOException;

    /**
     * Returns the read-only byte buffer for the specified entry or
     * {@code null} if not found.
     * <p>
     * The entry's name is specified using {@code '/'} as path separator; it
     * has no leading {@code '/'}.
     *
     * @param name the name of the entry.
     * @return the readable byte channel for the specified entry or
     *         {@code null} if not found.
     * @throws IOException if an I/O error occurs.
     */
    public ByteBuffer getEntryAsByteBuffer(String name) throws IOException;

    /**
     * Returns an unmodifiable set of the names of the entries.
     * <p>
     * The entry's name is specified using {@code '/'} as path separator; it
     * has no leading {@code '/'}.
     *
     * @return an unmodifiable set of the names of the entries.
     * @throws IOException if an I/O error occurs.
     */
    public Set<String> getEntryNames() throws IOException;

    /**
     * Returns the path of the native library for the specified library name
     * or {@code null} if not found.
     *
     * @param libraryName the library name.
     * @return the path of the native library or {@code null} if not found.
     * @throws IOException if an I/O error occurs.
     */
    public File getNativeLibrary(String libraryName) throws IOException;

    /**
     * Returns an unmodifiable set of code signers. If there is no code
     * signer, an empty set is returned.
     *
     * @return an unmodifiable set of code signers.
     * @throws IOException if an I/O error occurs.
     */
    public Set<CodeSigner> getCodeSigners() throws IOException;

    /**
     * Returns true if the entire content is available locally.
     *
     * @return true if the entire content is available locally; false otherwise.
     */
    public boolean isDownloaded();
}
