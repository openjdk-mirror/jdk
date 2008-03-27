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

${header}

package ${pkg};

import java.io.*;
import java.util.*;

import java.module.*;
import java.module.annotation.MainClass;

${import}

public class ${name} ${super} {

    private static final ${name} INSTANCE = new ${name}();

    public final Module module;
    public final ModuleDefinition moduleDefinition;
    public final Repository repository;
    public final String version;

    public ${name}() {
        module = getClass().getClassLoader().getModule();
        moduleDefinition = module.getModuleDefinition();
        repository = moduleDefinition.getRepository();
        version = moduleDefinition.getVersion().toString();
    }

    public static ${name} get() {
        return INSTANCE;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Starting main: ${pkg}.${name}...");
        get().run(args);
    }

    public void info(String ... args) throws Exception {
        System.out.println("Running " + toString()
            + (args.length == 0 ? "" : " " + Arrays.toString(args)));
        ${info}
    }

    public String toString() {
        return getClass() + " (" + moduleDefinition.getName()
             + " v" + moduleDefinition.getVersion() + ")";
    }

    public void run(String ... args) throws Exception {
        info(args);
        ${run}
    }

    private void runOtherModule(Module m) throws Exception {
        ClassLoader cl = m.getClassLoader();
        String mainClass = m.getModuleDefinition().getAnnotation(MainClass.class).value();
        Class<?> clazz = cl.loadClass(mainClass);
        Object o = clazz.newInstance();
        java.lang.reflect.Method method = clazz.getMethod("run", String[].class);
        method.invoke(o, (Object)new String[] { "from " + moduleDefinition.getName() });
    }

    ${body}
}
