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

package dep;

import hello.Main;

/**
 * Main class of the "dep" module, which has a dependency on the "hello"
 * module.
 *
 * To create the JAM file:
 * % cd sample/modules/depedencies
 * % javac -sourcepath ../hello-world dep/*.java
 * % jam cfs dep.jam dep dep/*.class
 *
 * The sample also includes pre-built files for a URLRepository in the
 * "urlrepository" directory. It can be used to launch the module either
 * locally or directly from a remote web server, for example using
 * % java -repository http://openjdk.java.net/projects/modules/samplerepo/ -module dep
 *
 * This causes the module and its dependencies to be downloaded and executed.
 * Note that the module system automatically determines from the information
 * declared in the superpackage file that the "hello" module is a dependency
 * and downloads and initializes it as well.
 *
 * (Note: running with a security manager is not yet supported. Execute
 *  JAM files downloaded from remote servers at your own risk)
 */
public class Entry {

    public void run(String[] args) {
        System.out.println("Module 'dep' calling module 'hello'...");
        new Main().run(args);
    }

    public static void main(String[] args) {
        new Entry().run(args);
    }

}
