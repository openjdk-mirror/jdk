/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


// common infrastructure for OracleUcrypto provider tests

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

import java.security.*;

public abstract class UcryptoTest {

    protected static final boolean hasUcrypto;
    static {
        hasUcrypto = (Security.getProvider("OracleUcrypto") != null);
    }

    private static Provider getCustomizedUcrypto(String config) throws Exception {
        Class clazz = Class.forName("com.oracle.security.ucrypto.OracleUcrypto");
        Constructor cons = clazz.getConstructor(new Class[] {String.class});
        Object obj = cons.newInstance(new Object[] {config});
        return (Provider)obj;
    }

    public abstract boolean doTest(Provider p) throws Exception;

    public static void main(UcryptoTest test, String config) throws Exception {
        Provider prov = null;
        if (hasUcrypto && config != null) {
            prov = getCustomizedUcrypto(config);
        }
        List<Provider> providers = Arrays.asList(Security.getProviders());
        if (prov != null)
            providers.add(prov);
        List<String> failures = new ArrayList<String>();
        for (Provider p : providers) {
            System.err.println("Testing provider: " + p);
            if (!test.doTest(p))
                failures.add(p.getName());
        }
        if (failures.size() != 0)
            throw new RuntimeException("The following providers failed: " +
                                       failures);
        System.err.println("All providers passed.");
    }

}
