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

package sun.module.core;

import java.lang.reflect.Constructor;
import java.io.IOException;
import java.module.*;
import java.module.annotation.ModuleInitializerClass;
import java.module.annotation.ImportPolicyClass;
import java.module.annotation.AllowShadowing;
import java.net.URL;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.CodeSigner;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.*;

/**
 * This class represents the reified module instance in the module system.
 * <p>
 * @see java.lang.ClassLoader
 * @see java.module.ModuleDefinition
 * @see java.module.ModuleSystemPermission
 * @since 1.7
 */
final class ModuleImpl extends Module {

    final static boolean DEBUG = ModuleSystemImpl.DEBUG || false;

    private final ModuleSystemImpl moduleSystem;

    private final ModuleDefinition moduleDef;

    private final ModuleContent content;

    private final String moduleString;

    // state of this module
    private volatile State state;

    // List of imported module providers that are added
    // during the resolving process.
    private List<Module> importedModules;

    // Set of modules that import the current module.
    private Set<Module> importingModules;

    // ClassLoader of this module
    private ModuleLoader loader;

    // Imported modules that the module classloader delegates to
    private List<Module> importedModulesInLoader;

    // Module initializer of this module
    private ModuleInitializer moduleInitializer;

    // If initialization fails, the reason of the failure
    private ModuleInitializationException initException;

    // Used to signal completion of the initialization process
    private final Object initSignal = new Object();

    ModuleImpl(ModuleSystemImpl moduleSystem, final ModuleDefinition moduleDef) {
        this.moduleSystem = moduleSystem;
        this.moduleDef = moduleDef;
        this.moduleString = toString(moduleDef);
        this.content = java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<ModuleContent>() {
                    public ModuleContent run() {
                        return moduleDef.getModuleContent();
                    }
                });
        this.state = State.NEW;
    }

    /**
     * State of module instance.
     */
    private static enum State {

        /**
         * New module instance created.
         */
        NEW,

        /**
         * Located the ModuleDefinitions of our direct imports
         */
        FOUND_DIRECT_IMPORTS,

        /**
         * Located the ModuleDefinitions of all (direct and indirect) imports
         */
        FOUND_ALL_IMPORTS,

        /**
         * Shallow validation succeeded.
         */
        VALIDATED,

        /**
         * Execute the module initializer.
         */
        EXECUTE_INITIALIZER,

        /**
         * Execution of the module initializer succeeded.
         */
        INITIALIZER_COMPLETE,

        /**
         * The module instance is ready.
         */
        READY,

        /**
         * The module instance is in erroneous state.
         */
        ERROR
    }

    boolean isFindingDirectImports() {
        return state == State.NEW;
    }

    boolean isExecutingInitializer() {
        return state == State.EXECUTE_INITIALIZER;
    }

    @Override
    public ModuleDefinition getModuleDefinition() {
        return moduleDef;
    }

    @Override
    public ClassLoader getClassLoader() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("getClassLoader"));
        }

        // Returns the module class loader only when the module instance
        // has been fully initialized.
        //
        // The module instance is ready for class loading as soon as it
        // reaches INITIALIZER_COMPLETE.
        if (state.compareTo(State.INITIALIZER_COMPLETE) < 0) {
            throw new IllegalStateException("Module instance " + moduleString
                        + " has not been fully initialized.");
        }

        return getModuleLoader();
    }

    @Override
    public List<Module> getImportedModules() {
        if (importedModules == null) {
            throw new IllegalStateException("Imported modules list has not been created yet in module " + moduleString);
        }

        return Collections.unmodifiableList(importedModules);
    }

    /**
     * Returns an unmodifiable list of module instances that imports this module
     * instance.
     */
    Set<Module> getImportingModules() {
        if (importingModules == null) {
            throw new IllegalStateException("Importing modules list has not been created yet in module " + moduleString);
        }
        return Collections.unmodifiableSet(importingModules);
    }

    /**
     * Adds a module instance that imports this module instance.
     */
    void addImportingModule(Module module) {
        if (importingModules == null) {
            throw new IllegalStateException("Importing modules list has not been created yet in module " + moduleString);
        }
        importingModules.add(module);
    }

    /**
     * Removes a module instance that imports this module instance.
     */
    void removeImportingModule(Module module) {
        // This may be called when a module instance gets into ERROR state,
        // before the importing module list is created.
        if (importingModules != null) {
            importingModules.remove(module);
        }
    }

    /**
     * Removes all module instances that import this module instance.
     */
    void removeImportingModules() {
        if (importingModules != null) {
            importingModules.clear();
        }
    }

    @Override
    public boolean supportsDeepValidation() {
        try {
            // Find the transitive closure of this module through imported dependency
            Set<Module> closure = ModuleUtils.findImportedModulesClosure(this);

            // Deep validation is supported only if the member classes in all
            // modules in the transitive closure are known.
            for (Module m : closure) {
                ModuleDefinition md = m.getModuleDefinition();

                // XXX: Workaround for virtual module for now ... until JSR 294
                // support arrives
                if (md.getName().equals("java.classpath")) {
                    throw new UnsupportedOperationException();
                } else if (md.getRepository() == Repository.getBootstrapRepository())  {
                    continue;
                }

                md.getMemberClasses();
            }

            return true;
        } catch (UnsupportedOperationException uoe) {
            return false;
        }
    }

    @Override
    public void deepValidate() throws ModuleInitializationException {
        Set<String> classNamespace = new HashSet<String>();
        Set<String> questionableClasses = new HashSet<String>();

        for (Module m : ModuleUtils.findImportedModulesClosure(this)) {
            try {
                ModuleDefinition md = m.getModuleDefinition();

                // XXX: Workaround for virtual module for now ... until JSR 294
                // support arrives
                if (md.getName().equals("java.classpath")) {
                    throw new UnsupportedOperationException();
                } else if (md.getRepository() == Repository.getBootstrapRepository())  {
                    continue;
                }

                Set<String> memberClasses = md.getMemberClasses();

                for (String clazz : memberClasses) {
                    if (classNamespace.contains(clazz)) {
                        // The member class already exists in the namespace.
                        // There is a potential type consistency conflict.
                        questionableClasses.add(clazz);
                    }
                }

                // Add the member classes to the namespace
                classNamespace.addAll(memberClasses);

            } catch (UnsupportedOperationException uoe) {
                    throw new ModuleInitializationException("Module " + moduleString
                        + " cannot be deep validated because member classes in "
                        + m.getModuleDefinition().toString()
                        + "in the dependency transitive closure are unknown.");
            }
        }

        // XXX: we may want to consider creating a new exception class so the
        // type conflict information could be reported more precisely through
        // exception.
        if (questionableClasses.size() > 0)
            throw new ModuleInitializationException("Module "
                        + moduleString + " has potential type consistency conflict.");
    }

    /**
     * Wait for initialization of this module to complete.
     * Blocks until the state is either READY or ERROR.
     */
    void awaitInitialization() throws ModuleInitializationException, InterruptedException {
        // check state first, avoid synchronization if possible
        if (state == State.READY) {
            return;
        }
        if (state == State.ERROR) {
            // already failed before, throw again
            throw new ModuleInitializationException
                (initException.getMessage(), initException);
        }
        while (true) {
            synchronized (initSignal) {
                if (state == State.READY) {
                    return;
                }
                if (state == State.ERROR) {
                    if (initException != null) {
                        throw initException;
                    }
                    // should never be reached
                    fail(null, "Unknown error during initialization of module " + moduleString);
                    // not reached
                }
                initSignal.wait();
            }
        }
    }

    boolean initializationComplete() {
        return (state == State.READY) || (state == State.ERROR);
    }

    boolean inError() {
        return (state == State.ERROR);
    }

    private void signalCompletion() {
        synchronized (initSignal) {
            initSignal.notifyAll();
        }
    }

    void fail(Throwable t, String msg) throws ModuleInitializationException {
        if (t instanceof ThreadDeath) {
            throw (ThreadDeath)t;
        }
        ModuleInitializationException e;
        if (t instanceof ModuleInitializationException) {
            e = (ModuleInitializationException)t;
        } else {
            e = new ModuleInitializationException(msg, t);
        }
        if (initException == null) {
            initException = e;
        }
        if (state != State.ERROR) {
            state = State.ERROR;
            if (importedModules != null) {
                for (Module m : importedModules) {
                    if (m instanceof ModuleImpl) {
                        ModuleImpl mi = (ModuleImpl) m;
                        mi.removeImportingModule(this);
                    }
                    else {
                        // Imported module instance is from other module system.
                        //
                        // TBD: May need a way to unregister an importer from a
                        // module system in other module system.
                    }
                }
            }
            // Reset imported and importing module instances
            importedModules = null;
            importingModules = null;
            loader = null;
            importedModulesInLoader = null;

            // If module initializer's initialize() has been called, inovoke
            // module initializer's release().
            if (moduleInitializer != null) {
                callReleaseOnModuleInitializer();
                moduleInitializer = null;
            }

            // Send MODULE_INITIALIZATION_EXCEPTION event
            ModuleSystemEvent evt = new ModuleSystemEvent(moduleSystem,
                                        ModuleSystemEvent.Type.MODULE_INITIALIZATION_EXCEPTION,
                                        null, getModuleDefinition(), e);
            moduleSystem.sendEvent(evt);

            signalCompletion();
        }
        throw e;
    }

    boolean initStep() {
        try {
            return nextStep();
        } catch (ThreadDeath e) {
            throw e;
        } catch (Throwable e) {
            if (DEBUG) {
                System.out.println("XXX module initialization failed: " + moduleString);
                e.printStackTrace();
            }
            try {
                fail(e, "Cannot initialize module " + moduleString);
            } catch (ModuleInitializationException ee) {
                // call fail to make sure we are in error state,
                // but ignore the exception
            }
            return true;
        }
    }

    private boolean nextStep() throws ModuleInitializationException {
        boolean r;

        switch (state) {
        case READY:
            return true;

        case NEW:
            // Trigger the module content to be downloaded if the
            // module definition is not local.
            try {
                content.getCodeSigners();
            } catch (IOException e) {
                fail(e, "Module " + moduleString + "s content is not available.");
            }

            findDirectImports();
            state = State.FOUND_DIRECT_IMPORTS;
            nextStep(); // recursive call
            return true;

        case FOUND_DIRECT_IMPORTS:
            // Check if all imported module instances in the transitive closure
            // are in the state FOUND_DIRECT_IMPORTS or later.
            r = checkDependencies(State.FOUND_DIRECT_IMPORTS);
            if (r) {
                state = State.FOUND_ALL_IMPORTS;
                nextStep(); // recursive call
            }
            return r;

        case FOUND_ALL_IMPORTS:
            validate();
            state = State.VALIDATED;
            nextStep(); // recursive call
            return true;

        case VALIDATED:
            // Check if all imported module instances in the transitive closure
            // are in the state VALIDATED or later.
            r = checkDependencies(State.VALIDATED);
            if (r) {
                state = State.EXECUTE_INITIALIZER;
                nextStep(); // recursive call
            }
            return r;

        case EXECUTE_INITIALIZER:
            callInitializeOnModuleInitializer();

            state = State.INITIALIZER_COMPLETE;

            if (loader != null) {
                // If a module class loader has been created, set the
                // imported modules to delegate in the loader -
                // reexports have been expanded.
                loader.setModule(this, importedModulesInLoader);
                importedModulesInLoader = null;
            } else {
                // Otherwise, wait until getClassLoader() is
                // invoked to create a module class loader.
            }

            nextStep(); // recursive call
            return true;

        case INITIALIZER_COMPLETE:
            // Check if all imported module instances in the transitive closure
            // are in the state INITIALIZER_COMPLETE or later.
            r = checkDependencies(State.INITIALIZER_COMPLETE);
            if (r) {
                state = State.READY;

                for (Module m : getImportedModules()) {
                    if (m instanceof ModuleImpl) {
                        ModuleImpl mi = (ModuleImpl) m;
                        mi.addImportingModule(this);
                    }
                    else {
                        // Imported module is from other module system.
                        //
                        // TBD: May need a way to register importer with
                        // module instance from a foreign module system.
                    }
                }

                // Send MODULE_INITIALIZED event
                ModuleSystemEvent evt = new ModuleSystemEvent(moduleSystem,
                                            ModuleSystemEvent.Type.MODULE_INITIALIZED,
                                            this, null, null);
                moduleSystem.sendEvent(evt);

                signalCompletion();
            }
            return r;

        case ERROR:
        default:
            // should never occur
            fail(null, "Unexpected module state in module " + moduleString + ": " + state);
            return false; // not reached
        }
    }

    /**
     * Creates module class loader lazily.
     */
    private ModuleLoader getModuleLoader()  {
        if (loader == null) {
            final CodeSource cs = getCodeSource();

            loader = java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<ModuleLoader>() {
                    public ModuleLoader run() {
                        return new ModuleLoader(moduleDef, content, cs);
                    }
                });

            // If the loader has not been created and the module
            // state is already INITIALIZER_COMPLETE or later,
            // then need to set up the module class loader to
            // be capable of loading member classes from the module
            // instance and loading exported classes from the
            // imported module modules.
            if (state.compareTo(State.INITIALIZER_COMPLETE) >= 0) {
                loader.setModule(this, importedModulesInLoader);
                importedModulesInLoader = null;
            }
        }
        return loader;
    }

    /**
     * Finds module instances of all direct imports
     */
    private void findDirectImports() throws ModuleInitializationException {
        importedModules = new ArrayList<Module>();
        importingModules = new HashSet<Module>();

        List<ImportDependency> importDependencies = moduleDef.getImportDependencies();
        if (DEBUG) {
            System.out.println("Import dependency: " + importDependencies);
        }

        // Build version constraint map from the ImportDependencies
        Map<ImportDependency,VersionConstraint> versionConstraints = new HashMap<ImportDependency,VersionConstraint>();
        for (ImportDependency dep : importDependencies) {
            if ((dep instanceof ModuleDependency) == false) {
                // XXX: supports module dependency only at this point
                fail(null, "Module " + moduleString + " has unsupported import dependency: "
                     + dep);
            }
            if (versionConstraints.put(dep, dep.getVersionConstraint()) != null) {
                fail(null, "Module " + moduleString + " has duplicate import dependency: "
                     + dep);
            }
        }
        versionConstraints = Collections.unmodifiableMap(versionConstraints);

        // Invoke import override policy
        versionConstraints = callImportOverridePolicy(versionConstraints);

        // Invoke custom import policy
        Map<ImportDependency,VersionConstraint> result = callImportPolicy(versionConstraints);

        // Get a Module instance for each imported ModuleDefinition
        Repository repository = moduleDef.getRepository();
        for (ImportDependency dep : importDependencies) {
            // Retreives overriden version constraint for the dependency
            VersionConstraint vc = result.get(dep);

            // Resolution MUST ignore an optional import dependency if the
            // corresponding version constraint is null.
            //
            if (dep.isOptional() && vc == null) {
                continue;
            }

            ModuleDefinition importedMD = repository.find(dep.getName(), vc);

            if (importedMD == null) {
                if (dep.isOptional()) {
                    // Resolution MUST ignore an optional import dependency if
                    // the corresponding version constraint is not null and no
                    // module definition in the repository can satisfy the
                    // import dependency.
                    //
                    continue;
                } else {
                    // Resolution MUST fail if no module definition in the
                    // repository can satisfy a mandatory import dependency
                    // with the corresponding version constraint.
                    fail(null,  moduleString
                         + ": no module definition in the repository can satisfy the import dependency "
                         + dep);
                }
            }

            ModuleSystem importedModuleSystem = importedMD.getModuleSystem();
            if (importedModuleSystem == moduleSystem) {
                // Get the raw module instance from the module system.
                // If it has not been initialized yet, it is automatically
                // enqueued.
                Module m = moduleSystem.getModuleInstance(importedMD);
                importedModules.add(m);
            } else {
                // Different module system. Get the fully initialized module.
                Module m = importedModuleSystem.getModule(importedMD);
                importedModules.add(m);
            }
        }
    }

    // Invoke ImportOverridePolicy
    private Map<ImportDependency,VersionConstraint> callImportOverridePolicy
                        (Map<ImportDependency,VersionConstraint> originalConstraints)
            throws ModuleInitializationException {
        ImportOverridePolicy overridePolicy = Modules.getImportOverridePolicy();
        if (overridePolicy == null) {
            return originalConstraints;
        }

        // Invoke the ImportOverridePolicy.narrow() method.
        //
        Map<ImportDependency,VersionConstraint> newConstraints
            = overridePolicy.narrow(moduleDef, originalConstraints);

        // If nothing has been overridden, simply return.
        if (newConstraints == originalConstraints) {
            return originalConstraints;
        }

        //
        // Check to ensure the result is acceptable.
        //

        // Just to be safe - make a copy of the result first to prevent
        // the import override policy object from changing the result under
        // our feets since the object could hold a reference of the result.
        //
        newConstraints = Collections.<ImportDependency,VersionConstraint>unmodifiableMap(newConstraints);

        // Resolution MUST fail if the set of import dependencies in the
        // originalConstraints is different than the set of import
        // dependencies in the newConstraints.
        //
        Set<ImportDependency> keySet1 = originalConstraints.keySet();
        Set<ImportDependency> keySet2 = newConstraints.keySet();
        if (keySet1.containsAll(keySet2) == false
            || keySet2.containsAll(keySet1) == false)  {
            fail(null, "Import override policy error in module " + moduleString
                 + ": the returned map does not contain the expected set of import "
                 + "dependencies");
        }

        for (ImportDependency dep : keySet1) {
            VersionConstraint originalConstraint = originalConstraints.get(dep);
            VersionConstraint newConstraint = newConstraints.get(dep);

            // Resolution MUST fail if the corresponding version constraint in
            // newConstraint is null and the import dependency is mandatory.
            //
            if (newConstraint == null && dep.isOptional() == false) {
                fail(null, "Import override policy error in module " + moduleString
                    + ": overridden version constraint is missing "
                    + " for import dependency " + dep);
            }

            // Resolution MUST fail if the corresponding version constraint in
            // newConstraint is outside the ranges of the version constraint for
            // the same import dependency in originalConstraint.
            //
            if (originalConstraint.contains(newConstraint) == false) {
                fail(null, "Import override policy error in module " + moduleString
                    + ": overridden version constraint " + newConstraint
                    + " is outside the boundary of " + originalConstraint
                    + " for import dependency " + dep);
            }
        }
        return newConstraints;
    }

    // Invoke custom import policy
    private Map<ImportDependency,VersionConstraint> callImportPolicy
            (final Map<ImportDependency,VersionConstraint> constraints)
            throws ModuleInitializationException {
        ImportPolicyClass importPolicyClass = moduleDef.getAnnotation(ImportPolicyClass.class);
        if (importPolicyClass == null) {
            return constraints;
        }

        final String importPolicyName = importPolicyClass.value();
        try {
            // Set the context class loader to null before calling import
            // policy.
            Thread.currentThread().setContextClassLoader(null);

            // Create the module class loader if one has not existed yet
            final ModuleLoader cl = getModuleLoader();

            // Load the import policy class using its module class
            // loader under a restricted access control context. This is
            // necessary because the static initializer of the class will be
            // called if it exists.
            final Constructor<? extends ImportPolicy> ctor = AccessController.doPrivileged(
                new PrivilegedExceptionAction<Constructor<? extends ImportPolicy> >() {
                    public Constructor<? extends ImportPolicy> run() throws Exception {
                        Class<? extends ImportPolicy> clazz =
                                Class.forName(importPolicyName, true, cl).asSubclass(ImportPolicy.class);
                        return clazz.getConstructor();
                }
            }, cl.getRestrictedAccessControlContext());

            // Must set constructor accessible as the import policy
            // may not be a public class.
            ctor.setAccessible(true);

            // Construct the import policy and invoke its getImports() method
            // under the restricted access control context.
            final Module module = this;
            Map<ImportDependency,VersionConstraint> newConstraints = AccessController.doPrivileged(
                new PrivilegedExceptionAction<Map<ImportDependency,VersionConstraint> >() {
                    public Map<ImportDependency,VersionConstraint> run() throws Exception {
                        ImportPolicy importPolicy = ctor.newInstance();
                        return importPolicy.getImports(moduleDef, constraints, DefaultImportPolicy.INSTANCE);
                }
            }, cl.getRestrictedAccessControlContext());

            // If nothing has been overridden, simply return
            if (newConstraints == constraints) {
                return constraints;
            }

            //
            // Check to ensure the result is acceptable.
            //

            // Just to be safe - make a copy of the result first to prevent
            // the import policy object from changing the result under
            // our feets since the object could hold a reference of the result.
            newConstraints = Collections.<ImportDependency,VersionConstraint>unmodifiableMap(newConstraints);

            Set<ImportDependency> keySet1 = new HashSet<ImportDependency>(moduleDef.getImportDependencies());
            Set<ImportDependency> keySet2 = newConstraints.keySet();

            // Resolution MUST fail if the set of import dependencies in the
            // result is different than the set of declared import
            // dependencies.
            //
            if (!(keySet1.containsAll(keySet2)
                  && keySet2.containsAll(keySet1)))  {
                    fail(null, "Import policy error in module " + moduleString
                        + ": the returned map does not contain the expected set of import dependencies");
            }

            for (ImportDependency dep : keySet1) {
                VersionConstraint vc = newConstraints.get(dep);

                // Resolution MUST fail if the corresponding version constraint in
                // the result is null and the import dependency is mandatory.
                //
                if (vc == null) {
                    if (dep.isOptional() == false) {
                        fail(null, "Import policy error in module " + moduleString
                             + ": overridden version constraint is missing for import dependency " + dep);
                    } else {
                        continue;
                    }
                }

                // Resolution MUST fail if the corresponding version constraint in
                // the result is outside the declared ranges of the version constraint
                // for the same import dependency.
                //
                if (dep.getVersionConstraint().contains(vc) == false) {
                    fail(null, "Import policy error in module " + moduleString
                    + ": overridden version constraint " + vc
                    + " is outside the boundary of " + dep.getVersionConstraint()
                    + " for import dependency " + dep);
                }
            }

            return newConstraints;

        } catch (PrivilegedActionException e) {
            if (e.getCause() instanceof ClassNotFoundException) {
                fail(e.getCause(), "Cannot load import policy class "
                     + importPolicyName + " in module " + moduleString);
            } else if (e.getCause() instanceof NoSuchMethodException) {
                fail(e.getCause(), "Import policy class " + importPolicyName
                     + " in module " + moduleString
                     + " does not have zero-argument public constructor.");
            } else if (e.getCause() instanceof ClassCastException) {
                fail(e.getCause(), importPolicyName + " does not implement java.module.ImportPolicy in module " + moduleString);
            } else {
                // print exception if the initializer throws any
                fail(e.getCause(), "Import policy exception in module " + moduleString);
            }
        } finally {
            // Set the context class loader to null after calling module
            // initializer.
            Thread.currentThread().setContextClassLoader(null);
        }

        return null;
    }

    // Default import policy implementation
    private static class DefaultImportPolicy implements ImportPolicy {

        private static ImportPolicy INSTANCE = new DefaultImportPolicy();

        private DefaultImportPolicy() {
            // empty
        }

        public Map<ImportDependency,VersionConstraint> getImports(ModuleDefinition moduleDef,
                Map<ImportDependency,VersionConstraint> constraints, ImportPolicy defaultImportPolicy)
                throws ModuleInitializationException {
            String moduleString = moduleDef.getName() + " v" + moduleDef.getVersion();
            Map<ImportDependency,VersionConstraint> result = new HashMap<ImportDependency,VersionConstraint>();
            Repository rep = moduleDef.getRepository();
            List<ImportDependency> importDependencies = moduleDef.getImportDependencies();
            for (ImportDependency dep : importDependencies) {
                if ((dep instanceof ModuleDependency) == false) {
                    throw new ModuleInitializationException
                        ("Default import policy error in module " + moduleString
                         + ": unsupported import dependency " + dep);
                }

                ModuleDependency moduleDep = (ModuleDependency) dep;

                String name = moduleDep.getName();
                VersionConstraint constraint = constraints.get(moduleDep);
                if (constraint == null) {
                    throw new ModuleInitializationException
                        ("Default import policy error in module " + moduleString
                         + ": overridden version constraint is missing for import dependency " + moduleDep);
                }
                ModuleDefinition importedMD = rep.find(name, constraint);
                if (DEBUG) System.out.println("Imported module definition: " + importedMD);
                if (importedMD == null) {
                    if (moduleDep.isOptional() == false) {
                        throw new ModuleInitializationException
                            ("Default import policy error in module " + moduleString
                            + ": imported module " + name + " "
                             + moduleDep.getVersionConstraint() + " is not found");
                    }
                    if (DEBUG) {
                        System.out.println("Optional import is not satisfied: "
                                           + name + " " + moduleDep.getVersionConstraint());
                    }
                    continue;
                }
                result.put(moduleDep, importedMD.getVersion().toVersionConstraint());
            }
            return Collections.unmodifiableMap(result);
        }
    }

    /**
     * Checks the type consistency requirements with this module
     * instance and its imported module instances.
     */
    private void validate() throws ModuleInitializationException {
        // deal with re-exports
        importedModulesInLoader = new ArrayList<Module>();
        ModuleUtils.expandReexports(this, importedModulesInLoader);

        // Perform shallow validation
        shallowValidate(importedModulesInLoader);

        if (DEBUG) System.out.println("Validation succeeded: module " + moduleString);
    }

    /**
     * Checks all dependencies of the specified Module.
     * <p>
     * Traverses the graph of all directly or indirectly imported modules.
     * If any are in ERROR state, call fail() and throw a
     * ModuleInitializationException.
     * <p>
     * If all modules are at least in state "requiredState" return true,
     * otherwise return false.
     * <p>
     * Note that we need to continue graph traversal even if we already
     * found a module that is not ready in order to find modules in ERROR
     * state.
     */
    private boolean checkDependencies(State requiredState) throws ModuleInitializationException {
        return checkDependencies(requiredState, this, true, new HashSet<ModuleImpl>());
    }

    private boolean checkDependencies(State requiredState, Module module,
            boolean ready, Set<ModuleImpl> checkedModules)
            throws ModuleInitializationException {
        if (checkedModules.contains(module)) {
            return ready;
        }
        if (module instanceof ModuleImpl == false) {
            // module instances returned from a foreign module system
            // are always fully initialized, i.e. READY.
            return true;
        }
        ModuleImpl moduleImpl = (ModuleImpl) module;
        State implState = moduleImpl.state;
        if (implState == State.READY) {
            // module instance has already been fully initialized, no
            // need to check further.
            return true;
        }
        if (implState == State.ERROR) {
            fail(null, "Cannot initialize imported module "
                       + toString(module.getModuleDefinition())
                       + " in module " + moduleString);
            return false; // not reached
        }
        if (implState.compareTo(requiredState) < 0) {
            ready = false;
        }
        checkedModules.add(moduleImpl);
        if (implState.compareTo(State.FOUND_DIRECT_IMPORTS) < 0) {
            // imports of the module not yet initialized, must skip and assume
            // not yet ready.
            return false;
        }
        for (Module importedModule : module.getImportedModules()) {
            ready &= checkDependencies(requiredState, importedModule, ready, checkedModules);
        }
        return ready;
    }

    private static String toString(ModuleDefinition md) {
        return md.getName() + " v" + md.getVersion();
    }

    /**
     * Perform shallow validation in the JAM module system.
     *
     * @param importedModules list of (transitive) imported module instances
     * @throws ModuleInitializationException if shallow validation fails.
     */
    private void shallowValidate(List<Module> importedModules) throws ModuleInitializationException {
        if (importedModules.contains(this)) {
            fail(null, "Validation: module " + moduleString + " is not allowed to import itself");
        }
        Set<String> myPackages = (moduleDef.getAnnotation(AllowShadowing.class) != null)
            ? Collections.<String>emptySet() : getPackages(moduleDef.getMemberPackageDefinitions());

        List<Set<String>> exportedPackages = new ArrayList<Set<String>>();
        for (Module importedModule : importedModules) {
            ModuleDefinition importedMD = importedModule.getModuleDefinition();
            Set<String> moduleExported = getPackages(importedMD.getExportedPackageDefinitions());
            if (Collections.disjoint(myPackages, moduleExported) == false) {
                moduleExported.retainAll(myPackages);
                fail(null, "Validation: module " + moduleString + " and imported module "
                    + toString(importedMD) + " both define packages "
                    + moduleExported);
                // not reached
            }
            int i = 0;
            for (Set<String> otherExported : exportedPackages) {
                if (Collections.disjoint(otherExported, moduleExported)) {
                    i++;
                    continue;
                }
                moduleExported.retainAll(otherExported);
                ModuleDefinition otherMD = importedModules.get(i).getModuleDefinition();
                fail(null, "Validation: modules " + toString(otherMD) + " and "
                    + toString(importedMD) + " imported by " + moduleString
                    + " both define packages " + moduleExported);
                // not reached
            }
            exportedPackages.add(moduleExported);
        }
    }

    private static Set<String> getPackages(Collection<PackageDefinition> packageDefs) {
        Set<String> packages = new HashSet<String>();
        for (PackageDefinition packageDef : packageDefs) {
            packages.add(packageDef.getName());
        }
        return packages;
    }

    /*
     * Constructor an module initializer object and invoke its initialize
     * method if the module initializer class exist.
     */
    void callInitializeOnModuleInitializer()
            throws ModuleInitializationException {
        ModuleInitializerClass moduleInitializerClass = moduleDef.getAnnotation(ModuleInitializerClass.class);
        if (moduleInitializerClass == null) {
            return;
        }

        final String moduleInitializerName = moduleInitializerClass.value();
        try {
            // Set the context class loader to null before calling module
            // initializer.
            Thread.currentThread().setContextClassLoader(null);

            // Create the module class loader if one has not existed yet
            final ModuleLoader cl = getModuleLoader();

            // Load the module initializer class using its module class
            // loader under a restricted access control context. This is
            // necessary because the static initializer of the class will be
            // called if it exists.
            final Constructor<? extends ModuleInitializer> ctor = AccessController.doPrivileged(
                new PrivilegedExceptionAction<Constructor<? extends ModuleInitializer> >() {
                    public Constructor<? extends ModuleInitializer> run() throws Exception {
                        Class<? extends ModuleInitializer> clazz =
                                Class.forName(moduleInitializerName, true, cl).asSubclass(ModuleInitializer.class);
                        return clazz.getConstructor();
                }
            }, cl.getRestrictedAccessControlContext());

            // Must set constructor accessible as the module initializer
            // may not be a public class.
            ctor.setAccessible(true);

            // Construct the module initializer and invoke its initialize() method
            // under the restricted access control context.
            moduleInitializer = AccessController.doPrivileged(
                new PrivilegedExceptionAction<ModuleInitializer>() {
                    public ModuleInitializer run() throws Exception {
                        ModuleInitializer initializer = ctor.newInstance();
                        initializer.initialize(moduleDef);
                        return initializer;
                }
            }, cl.getRestrictedAccessControlContext());

        } catch (PrivilegedActionException e) {
            if (e.getCause() instanceof ClassNotFoundException) {
                fail(e.getCause(), "Cannot load module initializer class "
                     + moduleInitializerName + " in module " + moduleString);
            } else if (e.getCause() instanceof NoSuchMethodException) {
                fail(e.getCause(), "Module initializer class " + moduleInitializerName
                     + " in module " + moduleString
                     + " does not have zero-argument public constructor.");
            } else if (e.getCause() instanceof ClassCastException) {
                fail(e.getCause(), moduleInitializerName + " does not implement java.module.ModuleInitializer in module " + moduleString);
            } else {
                // print exception if the initializer throws any
                fail(e.getCause(), "Module initializer exception in module " + moduleString);
            }
        } finally {
            // Set the context class loader to null after calling module
            // initializer.
            Thread.currentThread().setContextClassLoader(null);
        }
    }

    /**
     * Invoke the release method of the module initializer object if it exists.
     */
    void callReleaseOnModuleInitializer() {
        if (moduleInitializer == null) {
            return;
        }

        try {
            // Set the context class loader to null before calling module
            // initializer.
            Thread.currentThread().setContextClassLoader(null);

            final ModuleInitializer initializer = moduleInitializer;

            // Invoke the module initializer's release() method under the
            // restricted access control context.
            AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                public Object run() throws Exception {
                    initializer.release();
                    return null;
                }
            }, getModuleLoader().getRestrictedAccessControlContext());

        } catch (PrivilegedActionException pae) {
            // print exception if the initializer throws any
            pae.getCause().printStackTrace();
        } catch (Throwable e) {
            // print exception if the initializer throws any
            e.printStackTrace();
        } finally {
            // Set the context class loader to null after calling module
            // initializer.
            Thread.currentThread().setContextClassLoader(null);

            // Make the module initializer object unreachable from the
            // module instance for GC.
            moduleInitializer = null;
        }
    }

    /**
     * Returns the code source for the classes in this module instance.
     */
    private CodeSource getCodeSource() {
        try {
            // constructs module URL and module's code source
            StringBuilder sb = new StringBuilder();
            sb.append("module:");
            sb.append(moduleDef.getRepository().getName());
            if (content.getLocation() != null) {
                sb.append("/");
                sb.append(content.getLocation().toString());
            }
            sb.append("!/");
            sb.append(moduleDef.getName());
            sb.append("/");
            sb.append(moduleDef.getVersion());

            URL moduleURL = new URL(sb.toString());

            // This is currently the very first call the module system would
            // call into ModuleContent in a module definition. In the
            // case of URLRepository, this would trigger the JAM file to be
            // downloaded and the module metadata is compared (and potentially
            // throws exception if there is a mismatch between the
            // MODULE.METADATA file and that in the JAM file.
            List<CodeSigner> codeSigners = new ArrayList<CodeSigner>(content.getCodeSigners());
            return new CodeSource(moduleURL, codeSigners.toArray(new CodeSigner[codeSigners.size()]));
        } catch (IOException e) {
            throw new AssertionError("Internal module error: cannot construct module's code source: " + e);
        }
    }
}
