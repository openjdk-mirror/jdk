/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
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

package sun.module.tools;

import java.io.File;
import java.io.IOException;
import java.module.Module;
import java.module.ModuleArchiveInfo;
import java.module.ModuleDefinition;
import java.module.ModuleInitializationException;
import java.module.Modules;
import java.module.Query;
import java.module.Repository;
import java.module.VersionConstraint;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import sun.module.JamUtils;

/**
 * Provides a means to visit each of a module's imports.
 * @since 1.7
 */
public class ImportTraverser implements Iterable<Module> {
    private Set<Module> visited = new LinkedHashSet<Module>();

    /**
     * Invokes a visitor on each of the imports of the module identified by
     * the parameters.   The imports are visited in depth-first order with respect to
     * the graph of interconnected modules.  Note that the module identified
     * by parameters is the first one visited.
     *
     * @param repo repository in which to find a named module
     * @param name name of the module to find
     * @param constraint version constraint on the module; may be null
     * @throws ModuleInitializationException if the identified module cannot be
     * instantiated
     */
    public void traverse(Visitor visitor, Repository repo,
                         String name,
                         VersionConstraint constraint)
            throws ModuleInitializationException{

        traverse(visitor, repo, name, constraint, null, null);
    }

    /**
     * Invokes a visitor on each of the imports of the module identified by
     * the parameters.   The imports are visited in depth-first order with respect to
     * the graph of interconnected modules.  Note that the module identified
     * by parameters is the first one visited.
     *
     * @param jamName name of a JAM file for which dependencies are returned
     * @throws java.io.IOException if a Repository for the named JAM file cannot
     * be created
     * @throws ModuleInitializationException if the identified module cannot be
     * instantiated
     */
/*    public void traverse(Visitor, visitor, String jamName)
            throws IOException, ModuleInitializationException {

        File repoDir = JamUtils.createTempDir();
        Repository repo = Modules.newLocalRepository("importvisitor", repoDir);
        ModuleArchiveInfo mai = repo.install(new File(jamName).toURI().toURL());

        visit(visitor, repo, mai.getName(), mai.getVersion().toVersionConstraint(),
              mai.getPlatform(), mai.getArch());
    }
*/
    /**
     * Invokes a visitor on each of the imports of the module identified by
     * the parameters.   The imports are visited in depth-first order with respect to
     * the graph of interconnected modules.  Note that the module identified
     * by parameters is the first one visited.
     *
     * @param repo repository in which to find a named module
     * @param name name of the module to find
     * @param constraint version constraint on the module; may be null
     * @param platform platform of the module to find; may be null
     * @param arch architecture of the module to find; may be null
     * @throws ModuleInitializationException if the identified module cannot be
     * instantiated
     * @throws IllegalArgumentException if one of {@code platform, arch} is
     * null and the other is not, or if the {@code repository} finds more than
     * one module matching the given parameters.
     */
    public void traverse(Visitor visitor, Repository repo,
                         String name, VersionConstraint constraint,
                         String platform, String arch)
            throws ModuleInitializationException {

        if (repo == null || name == null) {
            throw new NullPointerException();
        }

        if (platform == null ^ arch == null) {
            throw new IllegalArgumentException(
                "module platform and arch must be either both null or both non-null");
        }

        Query q = Query.module(
            name, constraint == null ? VersionConstraint.DEFAULT : constraint);
        if (platform != null) {
            q = Query.and(
                q, Query.annotation(java.module.annotation.PlatformBinding.class));
        }

        List<ModuleDefinition> mdList = repo.find(q);
        if (mdList.size() > 1) {
            throw new IllegalArgumentException(
                "more than one matching module found for name=" + name
                + " version=" + constraint
                + " platform=" + platform + " arch=" + arch);
        } else if (mdList.size() == 1) {
            Module m = mdList.get(0).getModuleInstance();
            visitor.init(m);
            traverse(visitor, m);
        }
    }

    /**
     * Invokes a visitor on each of the imports of the {@code module} The
     * imports are visited in depth-first order with respect to the graph of
     * interconnected modules.  Note that the given module is the first one
     * visited.
     */
    public void traverse(Visitor visitor, Module module) {
        if (visited.contains(module)) {
            return;
        }
        visited.add(module);
        if (visitor.preVisit(module)) {
            List<Module> imported = module.getImportedModules();
            for (Module m : imported) {
                visitor.visit(m);
                traverse(visitor, m);
            }
        }
        visitor.postVisit(module);
    }

    /**
     * @return true if any modules were traversed, false otherwise
     */
    public boolean traversedAny() {
        return visited.size() > 0;
    }

    /**
     * @return an {@code Iterator} over the {@code Module} instances which have
     * been visited; note that the list is ordered by the depth-first
     * traversal of module imports.
     */
    public Iterator<Module> iterator() {
        return visited.iterator();
    }

    abstract static class Visitor {
        private final ImportTraverser traverser;

        protected Visitor(ImportTraverser traverser) {
            this.traverser = traverser;
        }

        /**
         * @param m {@code Module} possibly visited by the traverser
         * @return true if the given {@code Module} has been visited, false
         * otherwise
         */
        protected boolean visited(Module m) {
            return traverser.visited.contains(m);
        }

        /**
         * Invoked with the {@code Module} identified by the client-called {@code
         * visit} method before any other {@code Module} instances are visited.
         * @param m First {@code Module} visited by the traverser
         */
        protected void init(Module m) { }

        /**
         * Invoked on a {@code Module} instance before its imports are visited.
         * @param m {@code Module} which imports the module about to be visited by
         * {@link #visit(Module)}
         * @return true if the {@code Module} should be visited, false otherwise.
         */
        protected boolean preVisit(Module m) {
            return true;
        }

        /**
         * Invoked on a {@code Module} instance as it is visited
         * @param m {@code Module} being visited
         */
        protected void visit(Module m) { }

        /**
         * Invoked on a {@code Module} instance after its imports are visited.
         * @param m {@code Module} which imports the module that was just visited by
         * {@link #visit(Module)}
         */
        protected void postVisit(Module m) { }
    }
}
