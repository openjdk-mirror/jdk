/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
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

module modserv.service;
package modserv.service;

import java.util.Service;
import java.util.ServiceLoader;

/**
 * Based on the example in {@link java.util.ServiceLoader}
 */
@Service
abstract public class CodecSet {
    private static ServiceLoader<CodecSet> codecSetLoader =
        ServiceLoader.load(CodecSet.class);

    public static Encoder getEncoder(String name) {
        for (CodecSet cp : codecSetLoader) {
            System.err.println("getEncoder: " + cp);
            Encoder enc = cp.getEncoderFor(name);
            if (enc != null) {
                return enc;
            }
        }
        return null;
    }

    /** Creates a new instance of CodecSet */
    public CodecSet() {
    }

    protected abstract Encoder getEncoderFor(String name);
}
