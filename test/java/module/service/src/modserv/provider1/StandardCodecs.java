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

module modserv.provider1;
package modserv.provider1;

import java.util.ServiceProvider;
import modserv.service.CodecSet;
import modserv.service.Encoder;

/**
 * Based on the example in {@link java.util.ServiceLoader}
 */
@ServiceProvider
public class StandardCodecs extends CodecSet {

    /** Creates a new instance of StandardCodecs */
    public StandardCodecs() {
    }

    public Encoder getEncoderFor(String name) {
        if ("foo".equals(name)) {
            return new FooEncoder();
        }
        return null;
    }

    static class FooEncoder extends Encoder {
        public FooEncoder() {
            super("foo");
        }

        public String  encode(String s) {
            return "foo|" + s + "|foo";
        }
    }
}
