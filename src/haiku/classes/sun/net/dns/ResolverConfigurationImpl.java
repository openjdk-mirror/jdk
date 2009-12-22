/*
 * Copyright 2002 Sun Microsystems, Inc.  All Rights Reserved.
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

package sun.net.dns;

import java.util.List;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/*
 * An implementation of ResolverConfiguration for Haiku
 */

public class ResolverConfigurationImpl
    extends ResolverConfiguration
{
    /**
     * Returns a list corresponding to the domain search path. The
     * list is ordered by the search order used for host name lookup.
     * Each element in the list returns a {@link java.lang.String}
     * containing a domain name or suffix.
     *
     * @return list of domain names 
     */
    public List searchlist() {
      throw new RuntimeException("TODO: implement ResolverConfigurationImpl.searchlist()");
    }

    /**
     * Returns a list of name servers used for host name lookup.
     * Each element in the list returns a {@link java.lang.String} 
     * containing the textual representation of the IP address of
     * the name server.
     *
     * @return list of the name servers 
     */
    public List nameservers() {
      throw new RuntimeException("TODO: implement ResolverConfigurationImpl.nameservers()");
    }
    
    /**
     * Returns the {@link #Options} for the resolver.
     *
     * @return options for the resolver
     */
    public Options options() {
      throw new RuntimeException("TODO: implement ResolverConfigurationImpl.options()");
    }
    
    static {
	java.security.AccessController.doPrivileged(
            new sun.security.action.LoadLibraryAction("net"));
    }

}

/** 
 * Implementation of {@link ResolverConfiguration.Options}
 */
class OptionsImpl extends ResolverConfiguration.Options {
}
