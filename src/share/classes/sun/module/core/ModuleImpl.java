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

import java.module.*;
import java.module.annotation.ModuleInitializerClass;
import java.module.annotation.ImportPolicyClass;
import java.module.annotation.AllowShadowing;
import java.security.AccessController;
import java.security.AccessControlContext;
import java.security.ProtectionDomain;
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

    ModuleImpl(ModuleSystemImpl moduleSystem, ModuleDefinition moduleDef) {
        this.moduleSystem = moduleSystem;
        this.moduleDef = moduleDef;
        this.moduleString = toString(moduleDef);
        state = State.NEW;
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

    @Override
    public ModuleDefinition getModuleDefinition() {
        return moduleDef;
    }

    @Override
    public ClassLoader getClassLoader() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("createClassLoader"));
        }

        if (loader == null) {
            throw new NullPointerException("Classloader has not been created yet in module " + moduleString);
        }

        return loader;
    }

    @Override
    public List<Module> getImportedModules() {
        if (importedModules == null) {
            throw new NullPointerException("Imported modules list has not been created yet in module " + moduleString);
        }

        return Collections.unmodifiableList(importedModules);
    }

    /**
     * Returns an unmodifiable list of module instances that imports this module
     * instance.
     */
    Set<Module> getImportingModules() {
        if (importingModules == null) {
            throw new NullPointerException("Importing modules list has not been created yet in module " + moduleString);
        }
        return Collections.unmodifiableSet(importingModules);
    }

    /**
     * Adds a module instance that imports this module instance.
     */
    void addImportingModule(Module module) {
        if (importingModules == null) {
            throw new NullPointerException("Importing modules list has not been created yet in module " + moduleString);
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
            // In order to support deep validation, member classes and exported
            // classes must be available.
            moduleDef.getMemberClasses();
            moduleDef.getExportedClasses();
            return true;
        } catch (UnsupportedOperationException uoe) {
            return false;
        }
    }

    @Override
    public void deepValidate() throws ModuleInitializationException {
        if (supportsDeepValidation() == false) {
            throw new UnsupportedOperationException(moduleDef.getName()
                                + " module cannot be deep validated.");
        }

        // Find the transitive closure of this module through imported dependency
        Set<Module> importedModulesClosure = ModuleUtils.findImportedModulesClosure(this);

        // Continue with deep validation only if all modules in the
        // transitive closure support deep validation.
        for (Module m : importedModulesClosure) {
            if (m.supportsDeepValidation() == false) {
                ModuleDefinition md = m.getModuleDefinition();
                throw new ModuleInitializationException("module " + toString(md)
                    + " in the dependency transitive closure does not support deep validation.");
            }
        }

        Set<String> classNamespace = new HashSet<String>();
        Set<String> questionableClasses = new HashSet<String>();

        for (Module m : importedModulesClosure) {
            ModuleDefinition md = m.getModuleDefinition();

            // XXX: Workaround for virtual module for now ... until JSR 294
            // support arrives
            if (md.getRepository() == Repository.getBootstrapRepository()) {
                continue;
            }

            Set<String> memberClasses = md.getMemberClasses();
            for (String clazz : memberClasses) {
                if (classNamespace.contains(clazz)) {
                    // The member class already exists in the namespace. There is
                    // a potential conflict.
                    questionableClasses.add(clazz);
                } else {
                    // Add the class to the namespace
                    classNamespace.add(clazz);
                }
            }
        }

        // XXX: we may want to consider creating a new exception class so the
        // type conflict information could be reported more precisely through
        // exception.
        if (questionableClasses.size() > 0)
            throw new ModuleInitializationException("The class namespace of module "
                        + moduleString + " has potential type conflict.");
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
            ModuleSystemEvent evt = new ModuleSystemEvent(moduleSystem, getModuleDefinition(), e);
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
            findImports();
            state = State.FOUND_DIRECT_IMPORTS;
            nextStep(); // recursive call
            return true;

        case FOUND_DIRECT_IMPORTS:
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
            r = checkDependencies(State.VALIDATED);
            if (r) {
                state = State.EXECUTE_INITIALIZER;
                nextStep(); // recursive call
            }
            return r;

        case EXECUTE_INITIALIZER:
            callInitializeOnModuleInitializer();
            state = State.INITIALIZER_COMPLETE;
            nextStep(); // recursive call
            return true;

        case INITIALIZER_COMPLETE:
            r = checkDependencies(State.INITIALIZER_COMPLETE);
            if (r) {
                state = State.READY;

                // Set the imported modules to delegate in the loader -
                // reexports have been expanded.
                loader.setImportedModules(importedModulesInLoader);
                importedModulesInLoader = null;

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
                ModuleSystemEvent evt = new ModuleSystemEvent(moduleSystem, ModuleSystemEvent.Type.MODULE_INITIALIZED, this);
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

    private void findImports() throws ModuleInitializationException {
        importedModules = new ArrayList<Module>();
        importingModules = new HashSet<Module>();
        loader = new ModuleLoader(this, moduleDef);

        List<ImportDependency> importDependencies = moduleDef.getImportDependencies();
        if (DEBUG) {
            System.out.println("Import dependency: " + importDependencies);
        }

        // Build version constraint map from the ImportDependencies
        Map<String,VersionConstraint> versionConstraints = new HashMap<String,VersionConstraint>();
        for (ImportDependency dep : importDependencies) {
            if (versionConstraints.put(dep.getName(), dep.getVersionConstraint()) != null) {
                fail(null, "Module " + moduleString + " imports module "
                     + dep.getName() + " more than once.");
            }
        }
        versionConstraints = Collections.unmodifiableMap(versionConstraints);

        // Invoke ImportOverridePolicy
        versionConstraints = callOverridePolicy(versionConstraints);

        // Invoke default or custom import policy
        List<ModuleDefinition> importedMDs = callImportPolicy(versionConstraints);

        // Get a Module instance for each imported ModuleDefinition
        for (ModuleDefinition importedMD : importedMDs) {
            if (importedMD == null) {
                continue;
            }
            ModuleSystem importedModuleSystem = importedMD.getRepository().getModuleSystem();
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
    private Map<String,VersionConstraint> callOverridePolicy
            (Map<String,VersionConstraint> versionConstraints)
            throws ModuleInitializationException {
        ImportOverridePolicy overridePolicy = Modules.getImportOverridePolicy();
        if (overridePolicy == null) {
            return versionConstraints;
        }
        Map<String,VersionConstraint> newConstraints
            = overridePolicy.narrow(moduleDef, versionConstraints);
        // check constraints, unless the override policy just returned
        // the original constraints Map
        if (newConstraints != versionConstraints) {
            // make a copy before checking
            newConstraints = new HashMap<String,VersionConstraint>(newConstraints);
            if (versionConstraints.size() != newConstraints.size()) {
                fail(null, "Import override policy error in module " + moduleString
                     + ": size mismatch in the returned map of imported "
                     + "module names and overridden version constraints: "
                     + versionConstraints.size() + " != " + newConstraints.size());
            }
            for (Map.Entry<String,VersionConstraint> entry : versionConstraints.entrySet()) {
                String moduleName = entry.getKey();
                VersionConstraint constraint = entry.getValue();
                VersionConstraint newConstraint = newConstraints.get(moduleName);
                if (newConstraint == null) {
                    fail(null, "Import override policy error in module " + moduleString
                        + ": overridden version constraint missing in the "
                        + "returned map for module " + moduleName);
                }
                if (constraint.contains(newConstraint) == false) {
                    fail(null, "Import override policy error in module " + moduleString
                        + ": overridden version constraint " + newConstraint
                        + " for imported module " + moduleName
                        + " is outside the boundary of the original "
                        + "version constraint "+ constraint);
                }
            }
        }
        return Collections.unmodifiableMap(newConstraints);
    }

    // Invoke default or custom import policy
    private List<ModuleDefinition> callImportPolicy
            (Map<String,VersionConstraint> versionConstraints)
            throws ModuleInitializationException {
        ImportPolicyClass importClass = moduleDef.getAnnotation(ImportPolicyClass.class);
        String importPolicyName = (importClass != null) ? importClass.value() : null;
        if (importPolicyName == null) {
            return DefaultImportPolicy.INSTANCE.getImports
                (moduleDef, versionConstraints, null);
        }
        Class clazz;
        Object obj;
        try {
            clazz = Class.forName(importPolicyName, true, loader);
            obj = clazz.newInstance();
        } catch (Exception e) {
            fail(e, "Cannot load import policy class in module "
                 + moduleString + ": " + importPolicyName);
            return null; // not reached
        }
        if (obj instanceof ImportPolicy == false) {
            fail(null, clazz.getName() + " does not implement java.module.ImportPolicy in module " + moduleString);
            // not reached
        }
        ImportPolicy importPolicy = (ImportPolicy)obj;
        List<ModuleDefinition> importedMDs = importPolicy.getImports(moduleDef,
            versionConstraints, DefaultImportPolicy.INSTANCE);
        // make copy before checking
        importedMDs = new ArrayList<ModuleDefinition>(importedMDs);
        // Verify that all the returned ModuleDefinitions match the
        // import dependencies in order, name, and version constraints
        // and that no non-optional imports are missing
        int n = importedMDs.size();
        List<ImportDependency> importDependencies = moduleDef.getImportDependencies();
        if (n != importDependencies.size()) {
            fail(null, "Import policy error in module " + moduleString
                 + ": mismatch in number of imported module definition in the returned list: "
                 + n + " != " + importDependencies.size());
        }
        for (int i = 0; i < n; i++) {
            ImportDependency dep = importDependencies.get(i);
            ModuleDefinition md = importedMDs.get(i);
            if (md == null) {
                if (dep.isOptional() == false) {
                    fail(null, "Import policy error in module " + moduleString
                         + ": non-optional imported module definition is missing in the returned list: "
                         + dep.getName() + " " + dep.getVersionConstraint());
                }
                continue;
            }
            String name = dep.getName();
            if (name.equals(md.getName()) == false) {
                fail(null, "Import policy error in module " + moduleString
                    + ": mismatch in the name of imported module definition in the returned list: "
                    + name + " != " + md.getName());
            }
            Version version = md.getVersion();
            VersionConstraint constraint = versionConstraints.get(name);
            if (constraint.contains(version) == false) {
                fail(null, "Import policy error in module " + moduleString
                    + ": " + toString(md)
                    + " in the returned list does not satisfy version constraint " + constraint);
            }
        }
        return importedMDs;
    }

    // Default import policy implementation
    private static class DefaultImportPolicy implements ImportPolicy {

        final static ImportPolicy INSTANCE = new DefaultImportPolicy();

        private DefaultImportPolicy() {
            // empty
        }

        public List<ModuleDefinition> getImports(ModuleDefinition moduleDef,
                Map<String,VersionConstraint> constraints, ImportPolicy defaultImportPolicy)
                throws ModuleInitializationException {
            String moduleString = moduleDef.getName() + " v" + moduleDef.getVersion();
            List<ModuleDefinition> importedMDs = new ArrayList<ModuleDefinition>();
            Repository rep = moduleDef.getRepository();
            List<ImportDependency> importDependencies = moduleDef.getImportDependencies();
            for (ImportDependency dep : importDependencies) {

                String name = dep.getName();
                VersionConstraint constraint = constraints.get(name);
                if (constraint == null) {
                    throw new ModuleInitializationException
                        ("Default import policy error in module " + moduleString
                         + ": overridden version constraint is missing for imported module " + name);
                }
                ModuleDefinition importedMD = rep.find(name, constraint);
                if (DEBUG) System.out.println("Imported module definition: " + importedMD);
                if (importedMD == null) {
                    if (dep.isOptional() == false) {
                        throw new ModuleInitializationException
                            ("Default import policy error in module " + moduleString
                            + ": imported module " + dep.getName() + " "
                             + dep.getVersionConstraint() + " is not found");
                    }
                    if (DEBUG) {
                        System.out.println("Optional import is not satisfied: "
                                           + dep.getName() + " " + dep.getVersionConstraint());
                    }
                    continue;
                }
                importedMDs.add(importedMD);
            }
            return Collections.unmodifiableList(importedMDs);
        }
    }

    private void validate() throws ModuleInitializationException {
        // deal with re-exports
        importedModulesInLoader = new ArrayList<Module>();
        ModuleUtils.expandReexports(this, importedModulesInLoader, true);

        // Perform shallow validation
        shallowValidate(importedModulesInLoader);

        if (DEBUG) System.out.println("Validation succeeded: module " + moduleString);
    }

    /**
     * Check all dependencies of the specified Module.
     *
     * Traverses the graph of all directly or indirectly imported modules.
     * If any are in error state, call fail() and throw a
     * ModuleInitializationException.
     * If all modules are at least in state "requiredState" return true,
     * otherwise return false.
     * Note that we need to continue graph traversal even if we already
     * found a module that is not ready in order to find modules in error
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
            // foreign modules are already fully initialized
            return true;
        }
        ModuleImpl moduleImpl = (ModuleImpl)module;
        State implState = moduleImpl.state;
        if (implState == State.READY) {
            // module already fully initialized, no need to check further.
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
     * Perform shallow validation.
     *
     * @throws ModuleInitializationException if shallow validation
     *         fails.
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

    /**
     * If the module initializer exists, invoke the initialize() method. The
     * action is performed with the permissions possessed by the module's
     * protection domain.
     */
    private void callInitializeOnModuleInitializer()
            throws ModuleInitializationException {
        ModuleInitializerClass moduleInitializerClass = moduleDef.getAnnotation(ModuleInitializerClass.class);
        if (moduleInitializerClass == null) {
            return;
        }
        String moduleInitializerName = moduleInitializerClass.value();
        Class clazz;
        Object obj;
        try {
            clazz = Class.forName(moduleInitializerName, true, loader);
            obj = clazz.newInstance();
        } catch (Exception e) {
            fail(e, "Cannot load module initializer class in module "
                 + moduleString + ": " + moduleInitializerName);
            return; // not reached
        }
        if (obj instanceof ModuleInitializer == false) {
            fail(null, clazz.getName() + " does not implement java.module.ModuleInitializer in module " + moduleString);
            // not reached
        }

        try {
            final ModuleInitializer initializer = (ModuleInitializer)obj;
            final Module module = this;

            // Set up a virtual protection domain exactly as the protection
            // domain of the module.
            ProtectionDomain[] domains = new ProtectionDomain[1];
            domains[0] = initializer.getClass().getProtectionDomain();
            AccessControlContext context = new AccessControlContext(domains);

            // Invoke the module initializer's initialize() method under the
            // virtual protection domain.
            AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                public Object run() throws Exception {
                    initializer.initialize(module);
                    return null;
                }
            }, context);

            // Set moduleInitializer only after initialize() has been
            // invoked successfully.
            moduleInitializer = initializer;
        } catch (PrivilegedActionException e) {
            // print exception if the initializer throws any
            throw new ModuleInitializationException("Module initializer exception in module "
                            + moduleString, e.getCause());
        }
    }

    /**
     * If the module initializer exists, invoke the release() method. The
     * action is performed with the permissions possessed by the module's
     * protection domain.
     */
    void callReleaseOnModuleInitializer() {
        if (moduleInitializer == null) {
            return;
        }

        try {
            final ModuleInitializer initializer = moduleInitializer;
            final Module module = this;

            // Set up a virtual protection domain exactly as the protection
            // domain of the module.
            ProtectionDomain[] domains = new ProtectionDomain[1];
            domains[0] = initializer.getClass().getProtectionDomain();
            AccessControlContext context = new AccessControlContext(domains);

            // Invoke the module initializer's release() method under the
            // virtual protection domain.
            AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                public Object run() throws Exception {
                    initializer.release(module);
                    return null;
                }
            }, context);
        } catch (PrivilegedActionException pae) {
            // print exception if the initializer throws any
            pae.getCause().printStackTrace();
        } catch (Throwable e) {
            // print exception if the initializer throws any
            e.printStackTrace();
        } finally {
            moduleInitializer = null;
        }
    }
}
