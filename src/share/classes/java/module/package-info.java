/*
 * Copyright 2006-2008 Sun Microsystems, Inc.  All Rights Reserved.
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

/**
 * Provides classes and interfaces for the Java Module System framework and
 * the JAM (JAva Module) module system.
 *
 * <h2>Java Module System</h2>
 *
 * The Java Module System defines a framework for <i>deployment modules</i>
 * that enables one or more concrete <i>module systems</i> to run and
 * interoperate in a running Java virtual machine (JVM). A
 * {@linkplain java.module.ModuleSystem <i>module system</i>}
 * is responsible for creating, managing, and releasing
 * instances of its <i>deployment modules</i>.
 * <p>
 * In the context of the <i>deployment module</i>, there is a further
 * distinction between
 * {@linkplain java.module.ModuleDefinition <i>module definitions</i>} and
 * {@linkplain java.module.Module <i>module instances</i>}.
 * <p>
 * A <i>module definition</i> identifies a logical module in a
 * <i>module system</i>, and specifies which classes and resources are
 * provided by the module, and what the module
 * {@linkplain java.module.ModuleDefinition#getImportDependencies() imports}
 * and {@linkplain java.module.ModuleDefinition#isClassExported exports}.
 * The classes in the <i>module definition</i> are from one or more Java
 * packages. The Java Module System allows <i>module definitions</i> from
 * one or more <i>module systems</i> to coexist in the same Java virtual
 * machine (JVM) instance. A <i>module definition</i> is stateless
 * by definition, and the identity of a <i>module definition</i> is
 * represented by its name and version.
 * <p>
 * A <i>module system</i> may
 * {@linkplain java.module.ModuleSystem#getModule instantiate}
 * a <i>module definition</i> creating
 * a <i>module instance</i> at runtime. Each <i>module instance</i> has
 * its own copies of the classes defined by its
 * {@linkplain java.module.Module#getClassLoader module class loader}.
 * Each <i>module instance</i> is also
 * interconnected with instances of the modules it imports. Hence, each
 * <i>module instance</i> creates a distinct namespace at runtime. A useful
 * intuition is that <i>module definitions</i> are analogous to classes, and
 * <i>module instances</i> to objects.
 * <p>
 * A <i>module archive</i> identifies a physical module in a
 * <i>module system</i> for distribution. It contains metadata, classes, and
 * resources that are part of the module. The metadata carries the information
 * about the module itself, including the name, the version, the imports that
 * define its dependencies upon other modules, the exports that define what
 * classes and resources are visible to other modules, etc. The distribution
 * format of the <i>module archive</i> and the file format of the metadata
 * are module system specific.
 * <p>
 * The relationship between physical module (i.e. <i>module archive</i>) and
 * logical module (i.e. <i>module definition</i>) varies in each module system.
 * In some <i>module systems</i>, one <i>module archive</i> may yield one
 * <i>module definition</i> at runtime, while some other <i>module systems</i>
 * may combine multiple <i>module archives</i> to yield a single
 * <i>module definition</i>.
 * <p>
 * A {@linkplain java.module.Repository <i>repository</i>} is responsible for
 * storing, discovering, and retrieving <i>module definitions</i>. All
 * <i>module definitions</i> in a <i>repository</i> are associated with one
 * or more <i>module systems</i>, and a <i>module system</i> can be tied to
 * <i>module definitions</i> in one or more <i>repositories</i>. A
 * <i>repository</i> can have multiple versions of <i>module definitions</i>,
 * and it is the basis in enabling side-by-side deployment.
 * <p>
 * The <i>module definitions</i>, <i>module instances</i>,
 * <i>module systems</i>, and <i>repositories</i>
 * are reified via a reflective API:
 * {@link java.module.ModuleDefinition ModuleDefinition},
 * {@link java.module.Module Module},
 * {@link java.module.ModuleSystem ModuleSystem}, and
 * {@link java.module.Repository}.
 * This allows modules to be manipulated using the full power of a
 * general purpose programming language. This API constitutes a lingua
 * franca for tools and JVMs to manage modules.
 *
 * <blockquote>
 * <table cellspacing=1 summary="Description of the Java Module System">
 * <tr>
 * <th><p align="left">Java Module System API</p></th>
 * <th><p align="left">Description</p></th>
 * </tr>
 * <tr>
 * <td> <tt>{@link java.module.ImportDependency}</tt> </td>
 * <td> Generic import dependency on a module definition in the Java Module System.</td>
 * </tr>
 * <tr>
 * <td> <tt>{@link java.module.Module}</tt> </td>
 * <td> Module instance in a module system within the Java Module System.</td>
 * </tr>
 * <tr>
 * <td> <tt>{@link java.module.ModuleArchiveInfo}</tt> </td>
 * <td> Information of an installed module archive in a repository in the Java Module System.</td>
 * </tr>
 * <tr>
 * <td> <tt>{@link java.module.ModuleContent}</tt> </td>
 * <td> Content of a module definition in the Java Module System.</td>
 * </tr>
 * <tr>
 * <td> <tt>{@link java.module.ModuleDefinition}</tt> </td>
 * <td> Module definition in a module system within the Java Module System.</td>
 * </tr>
 * <tr>
 * <td> <tt>{@link java.module.ModuleSystem}</tt> </td>
 * <td> Module system within the Java Module System.</td>
 * </tr>
 * <tr>
 * <td> <tt>{@link java.module.ModuleSystemEvent}</tt> </td>
 * <td> Event that occurs in a module system within the Java Module System.</td>
 * </tr>
 * <tr>
 * <td> <tt>{@link java.module.ModuleSystemPermission}</tt> </td>
 * <td> Permission which the SecurityManager will check when code that is running with a SecurityManager calls methods defined in the Java Module System.</td>
 * </tr>
 * <tr>
 * <td> <tt>{@link java.module.ModuleDependency}</tt> </td>
 * <td> Import dependency on a module definition in the Java Module System based on a module's name.</td>
 * </tr>
 * <tr>
 * <td> <tt>{@link java.module.ModuleSystemListener}</tt> </td>
 * <td> Listener interface for receiving module system events in the Java Module System.</td>
 * </tr>
 * <tr>
 * <td> <tt>{@link java.module.PackageDependency}</tt> </td>
 * <td> Import dependency on a module definition in the Java Module System based on an exported package's name.</td>
 * </tr>
 * <tr>
 * <td> <tt>{@link java.module.PackageDefinition}</tt> </td>
 * <td> Package definition in a module definition in the Java Module System.</td>
 * </tr>
 * <tr>
 * <td> <tt>{@link java.module.Query}</tt> </td>
 * <td> Query that determines whether or not a particular module definition in the Java Module System matches some criteria.</td>
 * </tr>
 * <tr>
 * <td> <tt>{@link java.module.Repository}</tt> </td>
 * <td> Repository in the Java Module System.</td>
 * </tr>
 * <tr>
 * <td> <tt>{@link java.module.RepositoryEvent}</tt> </td>
 * <td> Event that occurs in a repository.</td>
 * </tr>
 * <tr>
 * <td> <tt>{@link java.module.RepositoryListener}</tt> </td>
 * <td> Listener interface for receiving repository events in the Java Module System.</td>
 * </tr>
 * <tr>
 * <td> <tt>{@link java.module.Version}</tt> </td>
 * <td> Version in the Java Module System.</td>
 * </tr>
 * <tr>
 * <td> <tt>{@link java.module.VersionConstraint}</tt> </td>
 * <td> Version constraint in the Java Module System.</td>
 * </tr>
 * <tr>
 * <td> <tt>{@link java.module.VisibilityPolicy}</tt> </td>
 * <td> Visibility policy for the module definitions in the Java Module System.</td>
 * </tr>
 * </table>
 * </blockquote>
 *
 * <h2>JAM Module System</h2>
 *
 * The JAM (JAva Module) module system is a concrete module system
 * implementation and the default module system within the Java Module System.
 * <p>
 * <h3> JAM modules</h3>
 * Modules in the JAM module system are called JAM modules. JAM modules are
 * <i>portable</i> by default and can run under any platform. The distribution
 * format of a JAM module is also called JAM. Each JAM file
 * is self-contained by definition, and it does not reference any resource
 * externally. See the JAM Module System Specification for more details.
 * <p>
 * <A NAME="PlatformSpecificModules"></A><h4> Platform-specific modules</h4>
 * JAM modules with
 * {@link java.module.annotation.PlatformBinding platform binding}
 * are <i>platform specific</i>. They can only run under the specific
 * platform and architecture.
 * <p>
 * <h4> Resource modules</h4>
 * Resources of different locales can be placed in different
 * {@linkplain java.util.ResourceBundle resource modules}. A JAM module
 * can load resources of a specific locale from the resource module
 * through the {@link java.util.ResourceBundle ResourceBundle}, and the
 * {@code ResourceBundle} is able to find the appropriate
 * <i>resource module</i> from the repositories in the Java Module System.
 *
 * <p>
 * <h4> Service modules and service provider modules</h4>
 * {@linkplain java.util.ServiceLoader Services} and
 * {@linkplain java.util.ServiceLoader service providers} can be packaged in
 * the form of <i>service modules</i> and <i>service provider modules</i>
 * for deployment. A JAM module can request <i>service provider</i>
 * of a specific <i>service</i> through
 * the {@link java.util.ServiceLoader ServiceLoader}, and the
 * {@code ServciceLoader} is able to find service providers from the
 * repositories in the Java Module System.
 *
 * <h3>Modules factory class</h3>
 * The {@link java.module.Modules Modules} class is the factory class for the
 * JAM module system. This class provides a set of static factory methods to
 * obtain the {@code ModuleSystem},
 * {@code ModuleDefinition}, and {@code Repository} for JAM modules.
 * <p>
 * Applications can obtain the
 * {@code ModuleSystem} object of the JAM module system by calling the
 * {@link java.module.Modules#getModuleSystem() <tt>getModuleSystem</tt>} method.
 * Applications can obtain the
 * {@code ModuleDefinition} objects of the JAM modules by calling the
 * {@link java.module.Modules#newModuleDefinition(byte[], ModuleContent,Repository,boolean)
 * <tt>newModuleDefinition</tt>} factory method.
 * <p>
 * The JAM module system provides two concrete repository implementations:
 * <i>Local repository</i> and <i>URL repository</i>. The former can be
 * obtained by calling one of the
 * {@link java.module.Modules#newLocalRepository(String, File, Map, Repository)
 * <tt>newLocalRepository</tt>} factory methods, while the latter can be
 * obtained by calling one of the
 * {@link java.module.Modules#newURLRepository(String, URL, Map, Repository)
 * <tt>newURLRepository</tt>} factory methods.
 * <p>
 * <blockquote>
 * <table cellspacing=1 summary="Description of the JAM Module System">
 * <tr>
 * <th><p align="left">JAM module system API</p></th>
 * <th><p align="left">Description</p></th>
 * </tr>
 * <tr>
 * <td> <tt>{@link java.module.ImportOverridePolicy}</tt> </td>
 * <td> Import override policy in the JAM module system.</td>
 * </tr>
 * <tr>
 * <td> <tt>{@link java.module.ImportPolicy}</tt> </td>
 * <td> Import policy of a module definition in the JAM module system.</td>
 * </tr>
 * <tr>
 * <td> <tt>{@link java.module.ModuleInitializer}</tt> </td>
 * <td> Module initializer of a module instance in the JAM module system.</td>
 * </tr>
 * <tr>
 * <td> <tt>{@link java.module.Modules}</tt> </td>
 * <td> Factory methods which are specifically for the JAM module system.</td>
 * </tr>
 * </table>
 * </blockquote>
 * <p> Except these APIs above, all the other APIs defined in the java.module package are for the framework.

 * <p> Unless otherwise noted, passing a <tt>null</tt> argument to a
 * constructor or method in any class or interface in this package will cause
 * a {@link java.lang.NullPointerException NullPointerException} to be thrown.
 *
 * <p> The java.module API is thread-safe.
 *
 * @since 1.7
 */
package java.module;
