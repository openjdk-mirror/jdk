/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.module.*;
import java.security.AccessController;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import sun.module.JamUtils;
import sun.module.repository.RepositoryConfig;
import sun.security.action.GetPropertyAction;
import sun.tools.jar.CommandLine;

/**
 * Java Modules Repository Management Tool
 * @since 1.7
 */
public class JRepo {
    public static final boolean DEBUG;

    static {
        DEBUG = (AccessController.doPrivileged(new GetPropertyAction("sun.module.tools.debug")) != null);
    }

    private static void debug(String s) {
        System.err.println(s);
    }

    /** For printing user output. */
    private final Messenger msg;

    /** Optional command line argument used by {@code list()}. */
    private String moduleName;

    /** Map from this tool's command names to the commands themselves. */
    private static final Map<String, Command> commands = new LinkedHashMap<String, Command>();

    /** Usage message created from each command's usage. */
    private static String usage = null;

    /** Format for module name & version. */
    private static final String MAIFormat = "%-20s %-20s";

    /** Format for additional/verbose module information. */
    private static final String MAIFormatVerbose = " %-9s %-7s %-17s %s";

    /** String containing column headings for name & version. */
    private static final String maiHeading;

    /** String containing column headings for additional/verbose module information. */
    private static final String maiHeadingVerbose;

    /** Indicates that parent repositories should be used by a command. */
    private static final Flag parentFlag = new Flag('p');

    /** Indicates that command output should be verbose. */
    private static final Flag verboseFlag = new Flag('v');

    /** Location of repository; if not given uses system repository. */
    private static final RepositoryFlag repositoryFlag = new RepositoryFlag();

    /** Contains the flags that are common to all commands. */
    private static final HashMap<Character, Flag> commonFlags = new HashMap<Character, Flag>();

    static {
        repositoryFlag.register(commonFlags);
        verboseFlag.register(commonFlags);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.printf(MAIFormat, "Name", "Version"); // XXX i18n
        maiHeading = sw.toString();

        sw = new StringWriter();
        pw = new PrintWriter(sw);
        pw.printf(MAIFormat, "Name", "Version"); // XXX i18n
        pw.printf(MAIFormatVerbose, "Platform", "Arch", "Modified", "Filename"); // XXX i18n
        maiHeadingVerbose = sw.toString();
    }

    public JRepo(PrintStream out, PrintStream err, BufferedReader reader) {
        msg = new Messenger("jrepo", out, err, reader);
        synchronized(commands) {
            if (commands.isEmpty()) {
                new ListCommand().register(commands);
                new InstallCommand().register(commands);
                new UninstallCommand().register(commands);
            }
        }
        reset();
    }

    public static void main(String[] args) {
        JRepo jrepo = new JRepo(
            System.out, System.err,
            new BufferedReader(new InputStreamReader(System.in)));
        System.exit(jrepo.run(args) ? 0 : 1);
    }

    public synchronized boolean run(String[] args) {
        reset();

        if (DEBUG) { for (String s : args) debug("arg: '" + s + "'"); }
        Command cmd = parseArgs(args);
        if (DEBUG && cmd != null) debug("running " + cmd);
        if (cmd == null) {
            return false;
        }

        Repository repo = null;
        try {
             repo = getRepository();
             repo.shutdownOnExit(true);
        } catch (IOException ex) {
            msg.fatalError(ex.getMessage());
            return false;
        }

        boolean rc = cmd.run(repo, msg);
        if (DEBUG) debug(cmd + " returned " + rc);
        return rc;

    }

    /**
     * Gets the repository based on command line flags.
     * @return Reposistory based on the value from {@code repositoryFlag} if
     * that is non-null, else the system repository.
     */
    private Repository getRepository() throws IOException {
        Repository rc = null;

        String repositoryLocation = repositoryFlag.getLocation();

        if (repositoryLocation == null) {
            rc = Repository.getSystemRepository();
        } else {
            // If repositoryLocation is a URL use URLRepository else LocalRepository.
            try {
                URL u = new URL(repositoryLocation);
                rc = Modules.newURLRepository(
                    RepositoryConfig.getSystemRepository(),"jrepo", u);
            } catch (MalformedURLException ex) {
                File f = new File(repositoryLocation);
                if (f.exists() && f.canRead()) {
                    rc = Modules.newLocalRepository(
                        RepositoryConfig.getSystemRepository(),
                        "jrepo",
                        f.getCanonicalFile());
                } else {
                    throw new IOException("Cannot access repository at " + repositoryLocation);
                }
            }
        }
        return rc;
    }

    /** Reset instance state to default values. */
    private void reset() {
        moduleName = null;
        parentFlag.reset();
        repositoryFlag.reset();
        verboseFlag.reset();
        for (Command cmd : commands.values()) {
            cmd.reset();
        }
    }


    /** Parse command line arguments.*/
    private Command parseArgs(String[] args) {
        /* Preprocess and expand @file arguments */
        try {
            args = CommandLine.parse(args);
        } catch (FileNotFoundException e) {
            msg.fatalError(msg.formatMsg("error.cant.open", e.getMessage()));
            return null;
        } catch (IOException e) {
            msg.fatalError(msg.formatMsg("caught.exception", e.getMessage()));
            return null;
        }

        if (args.length < 1) {
            usageError();
            return null;
        }

        Command cmd = null;
        for (Map.Entry<String, Command> entry : commands.entrySet()) {
            if (entry.getKey().startsWith(args[0])) {
                cmd = entry.getValue();
                break;
            }
        }
        if (DEBUG) debug("found command " + cmd);
        if (cmd == null) {
            usageError();
            return null;
        }

        try {
            int numFlags = cmd.parseFlags(args, commonFlags);
            args = Arrays.copyOfRange(args, numFlags + 1, args.length);
            try {
                if (cmd.parseArgs(args, msg)) {
                    return cmd;
                } else {
                    usageError();
                    return null;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                usageError();
            }
        } catch (IllegalArgumentException ex) {
            msg.error(ex.getMessage());
            usageError();
            return null;
        }
        return cmd;
    }


    /** Returns a user-grokkable description of the repository. */
    private String getRepositoryText(Repository repo) {
        String rc;
        URL u = repo.getSourceLocation();
        if (u == null) {
            rc = "Bootstrap repository";
        } else {
            rc = "Repository " + u.toExternalForm();
        }
        return rc;
    }

    private String getMAIText(ModuleArchiveInfo mai) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.printf(MAIFormat, mai.getName(), mai.getVersion());
        if (verboseFlag.isEnabled()) {
            long t = mai.getLastModified();
            String lastMod = null;
            if (t != 0) {
                lastMod = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(t));
            }
            pw.printf(MAIFormatVerbose,
                mai.getPlatform() == null ? "generic" : mai.getPlatform(),
                mai.getArch() == null ? "generic" : mai.getArch(),
                lastMod == null ? "n/a" : lastMod,
                mai.getFileName() == null ? "n/a" : mai.getFileName()
                );
        }
        return sw.toString();
    }

    void usageError() {
        usageError(msg);
    }

    static void usageError(Messenger msg) {
        if (usage == null) {
            StringBuilder ub = new StringBuilder(
                "Usage: jrepo <command>\nwhere <command> includes:");
            for (Command c : commands.values()) {
                String u = c.usage();
                if (u != null) {
                    ub.append("\n    ").append(c.usage());
                }
            }
            usage = ub.toString();
        }
        msg.error(usage);
    }

    /**
     * Represents a flag given on the command line.  Flags always are 2
     * characters long, and start with a '-'.  They can have additional
     * associated arguments (cf RepositoryFlag).
     */
    private static class Flag {
        private final char name;

        private boolean enabled = false;

        private static final Map<Character, Flag> flags = new HashMap<Character, Flag>();

        Flag(char name) {
            this.name = name;
            flags.put(new Character(name), this);
        }

        void register(Map<Character, Flag> registry) {
            registry.put(new Character(name), this);
        }

        char getName() {
            return name;
        }

        /** @return the number of arguments consumed by this Flag. */
        int set(String[] args, int pos) {
            enabled = true;
            return 1;
        }

        void reset() {
            enabled = false;
        }

        boolean isEnabled() {
            return enabled;
        }

        static Flag get(char c) {
            return flags.get(new Character(c));
        }
    }

    private static class RepositoryFlag extends Flag {
        String location = null;

        RepositoryFlag() {
            super('r');
        }

        int set(String[] args, int pos) {
            int rc = super.set(args, pos);
            location = args[pos + 1];
            return rc + 1;
        }

        String getLocation() {
            return location;
        }

        void reset() {
            super.reset();
            location = null;
        }
    }

    /*
     * Command types: An abstract base class, plus one concrete class for
     * each Command.
     */

    /*
     * Represents a Command.
     */
    private static abstract class Command {
        private final String name;

        Command(String name) {
            this.name = name;
        }

        /** Adds this command to the given registry. */
        void register(Map<String, Command> registry) {
            registry.put(name, this);
        }

        void reset() {
            // Empty; subclasses can implement
        }

        // Flags differ from arguments in that they have the form "-X [opt]".
        // Flags appear in a command line before arguments.

        /** Parses the arguments particular to this command. */
        abstract boolean parseArgs(String[] args, Messenger msg);

        /**
         * Parse the Flags for this command.
         * @return number of flags found.
         * @throws IllegalArgumentException if an invalid flag is given.
         */
        int parseFlags(String[] args, Map<Character, Flag> flags) {
            int rc = 0;
            int i = 0;
            while (i < args.length) {
                String s = args[i];
                if (s.length() == 2 && s.charAt(0) == '-') {
                    Flag f = flags.get(s.charAt(1));
                    if (f != null) {
                        int numConsumed = f.set(args, i);
                        i += numConsumed;  // Increases at each iteration.
                        rc += numConsumed; // Increases only when the arg is a flag.
                    } else {
                        throw new IllegalArgumentException("unrecognized flag: " + args[i]);
                    }
                } else {
                    i++;
                }
            }
            return rc;
        }

        /** Represents the actual behavior of the command. */
        abstract boolean run(Repository repo, Messenger msg);

        /** Returns a usage string describing this command, or null if the
         * command is a synonym for another command.
         */
        abstract String usage();

        public String toString() { return name; }

        /**
         * RepositoryVisitor types walk a parent chain of repositories, invoking
         * {@code doit} in each one.  The abstract base class provides the recursion;
         * each concrete subclass provides the per-repository behavior.  The
         * recursion is such that the bootstrap repository is visited first,
         * and the system repository is last.
         */
        abstract class RepositoryVisitor {
            abstract void doit(Repository repo, Messenger msg);

            void doBefore(Messenger msg) { }

            void doAfter(Messenger msg) { }

            final void run(Repository repo, Messenger msg) {
                visit(repo, msg);
            }

            private final void visit(Repository repo, Messenger msg) {
                Repository parent = parentFlag.isEnabled() ? repo.getParent() : null;
                if (parent != null) {
                    visit(parent, msg);
                }
                doBefore(msg);
                doit(repo, msg);
                doAfter(msg);
            }
        }
    }

    /** Installs a JAM into a repository. */
    private class InstallCommand extends Command {
        private String jamName;

        InstallCommand() {
            super("install");
        }

        boolean parseArgs(String[] args, Messenger msg) {
            boolean rc = false;
            if (!parentFlag.isEnabled()
                    && repositoryFlag.getLocation() != null
                    && args.length == 1) {
                jamName = args[0];
                rc = true;
            }
            return rc;
        }

        boolean run(Repository repo, Messenger msg) {
            if (repo != null) {
                String jamURL = null;
                File f = new File(jamName);
                if (f.canRead()) {
                    try {
                        String path = f.getCanonicalPath();
                        // Ensure that path starts with a "/" (it does not on
                        // some systems, e.g. Windows).
                        if (!path.startsWith("/")) {
                            path = "/" + path;
                        }
                        jamURL = "file://" + path;
                    } catch (IOException ex) {
                        msg.error("Cannot install " + jamName + ": " + ex.getMessage());
                        return false;
                    }
                } else {
                    jamURL = jamName;
                }
                try {
                    ModuleArchiveInfo mai = repo.install(new URL(jamURL));
                    if (verboseFlag.isEnabled()) {
                        msg.println("Installed " + jamName + ": " + getMAIText(mai));
                    }
                    return true;
                } catch (MalformedURLException ex) {
                    msg.error("Cannot install " + jamName + ": no such file, or malformed URL");
                } catch (IOException ex) {
                    msg.error("Cannot install " + jamName + ": " + ex.getMessage());
                }
            }
            return false;
        }

        String usage() {
            return "install [-v] -r repositoryLocation jamFile | jamURL\n"
                +  "        installs a module into a repository";
        }
    }

    /** Uninstalls a module from a repository. */
    private class UninstallCommand extends Command {
        @SuppressWarnings("unchecked")
        private final Map<Character, Flag> myFlags = (Map<Character, Flag>) commonFlags.clone();

        private Flag forceFlag = new Flag('f');

        private Flag interactiveFlag = new Flag('i');

        private String moduleName;

        private Version version;

        private String platformBinding;

        UninstallCommand() {
            super("uninstall");
            forceFlag.register(myFlags);
            interactiveFlag.register(myFlags);
        }

        void reset() {
            version = null;
            platformBinding = null;
            forceFlag.reset();
            interactiveFlag.reset();
        }

        int parseFlags(String[] args, Map<Character, Flag> flags) {
            return super.parseFlags(args, myFlags);
        }

        boolean parseArgs(String[] args, Messenger msg) {
            if (repositoryFlag.getLocation() == null) {
                return false;
            }

            if (forceFlag.isEnabled() && interactiveFlag.isEnabled()) {
                // Doesn't make sense for these to both be enabled
                msg.error("uninstall cannot simultaneously use both -i and -f: choose one or the other"); // XXX i81n
                return false;
            }

            if (args.length >= 1) {
                moduleName = args[0];
                if (args.length >= 2 && args.length < 4) {
                    try {
                        version = Version.valueOf(args[1]);
                    } catch (IllegalArgumentException ex) {
                        msg.error(ex.getMessage());
                        return false;
                    }
                }
                if (args.length == 3) {
                    platformBinding = args[2];
                }
                return true;
            } else {
                return false;
            }
        }

        boolean run(Repository repo, Messenger msg) {
            boolean rc = false;

            if (DEBUG) debug(
                "force=" + forceFlag.isEnabled()
                + " interactive=" + interactiveFlag.isEnabled()
                + " verbose=" + verboseFlag.isEnabled()
                + " repository=" + repositoryFlag.getLocation()
                + " moduleName=" + moduleName
                + " version=" + version
                + " plat/arch=" + platformBinding);

            List<ModuleArchiveInfo> found = new ArrayList<ModuleArchiveInfo>();
            for (ModuleArchiveInfo mai : repo.list()) {
                if (match(mai)) {
                    found.add(mai);
                }
            }
            if (DEBUG) debug("found.size=" + found.size());
            if (found.size() == 1) {
                ModuleArchiveInfo mai = found.get(0);
                rc = uninstall(repo, mai, msg);
            } else if (found.size() == 0) {
                if (verboseFlag.isEnabled()) {
                    msg.error("Could not find a module matching " + getInfo());
                }
            } else { // multiple matches
                if (!forceFlag.isEnabled() && !interactiveFlag.isEnabled()) {
                    msg.error("Cannot uninstall: multiple modules match " + getInfo());
                    if (verboseFlag.isEnabled()) {
                        for (ModuleArchiveInfo mai : found) {
                            msg.error(getMAIText(mai));
                        }
                    }
                } else if (forceFlag.isEnabled()) {
                    if (DEBUG) debug("forced uninstall of multiple matches");
                    rc = true;
                    for (ModuleArchiveInfo mai : found) {
                        if (!uninstall(repo, mai, msg)) {
                            if (DEBUG) debug("uninstall failed for " + getMAIText(mai) + " in " + repo.getSourceLocation());
                            rc = false;
                            break;
                        }
                    }
                } else if (interactiveFlag.isEnabled()) {
                    rc = uninstallInteractive(repo, found, msg);
                }
            }
            return rc;
        }

        String usage() {
            return "uninstall [-v] [-f | -i] -r repositoryLocation moduleName [moduleVersion] [modulePlatformBinding]\n"
                +  "        removes a module from a repository, along with associated files.";
        }

        /** Uninstall the ModuleArchiveInfo from the Repository. */
        private boolean uninstall(Repository repo, ModuleArchiveInfo mai, Messenger msg) {
            boolean rc = false;
            if (DEBUG) debug("Uninstalling " + getMAIText(mai));
            try {
                rc = repo.uninstall(mai);
                if (verboseFlag.isEnabled()) {
                    if (rc) {
                        msg.println("Uninstalled " + getMAIText(mai));
                    } else {
                        msg.error("Failed to uninstall " + getMAIText(mai));
                    }
                }
            } catch (Exception ex) {
                msg.error("Exception while uninstalling " + getInfo()
                          + ": " + ex.getMessage());
            }
            return rc;
        }

        /** Uninstall one of the ModuleArchiveInfos from the Repository. */
        private boolean uninstallInteractive(
                Repository repo,
                List<ModuleArchiveInfo> found,
                Messenger msg) {
            boolean rc = false;

            String spaces = "          ";
            String fmt = "%" + found.size() + "d %s\n";

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println("Multiple matches for module found.  Choose one of the below by index:");
            boolean saveVerbose = verboseFlag.isEnabled();
            verboseFlag.set(new String[0], 0);
            int count = 0;
            for (ModuleArchiveInfo m : found) {
                pw.printf(fmt, count++, getMAIText(m));
            }
            pw.print("\nIndex? ");
            pw.close();
            String choiceMessage = sw.toString();
            boolean done = false;
            while (!done) {
                msg.print(choiceMessage);
                String input = null;
                try {
                    input = msg.readLine().trim();
                    int index = Integer.parseInt(input);
                    if (index >= 0 && index < found.size()) {
                        uninstall(repo, found.get(index), msg);
                        done = true;
                        rc = true;
                    } else {
                        msg.error("Invalid input " + input + "; try again");
                    }
                } catch (NumberFormatException ex) {
                    msg.error("Invalid input '" + input + "'; try again");
                } catch (Exception ex) {
                    if (DEBUG) debug("msg.readLine threw " + ex);
                    done = true;
                }
            }

            // Restore if necessary.
            if (!saveVerbose) {
                verboseFlag.reset();
            }

            return rc;
        }

        /**
         * @return true iff moduleName matches mai.getName(), and if version
         * and platformBinding are set, they also match.
         */
        private boolean match(ModuleArchiveInfo mai) {
            boolean rc = false;
            if (DEBUG) debug("attempting to match " + getMAIText(mai));
            if (moduleName.equals(mai.getName())) {
                if (version == null) {
                    rc = true;
                } else if (version.equals(mai.getVersion())) {
                    if (platformBinding == null) {
                        rc = true;
                    } else {
                        String pb = mai.getPlatform() + "-" + mai.getArch();
                        if (platformBinding.equals(pb)) {
                            rc = true;
                        }
                    }
                }
            }
            return rc;
        }

        private String getInfo() {
            String s = moduleName;
            if (version != null) {
                s += " with version " + version;
            }
            if (platformBinding != null) {
                s += " and platform-binding of " + platformBinding;
            }
            return s;
        }
    }

    /** Prints information about modules found in repositories. */
    private class ListCommand extends Command {
        @SuppressWarnings("unchecked")
        private final Map<Character, Flag> myFlags = (Map<Character, Flag>) commonFlags.clone();

        ListCommand() {
            super("list");
            parentFlag.register(myFlags);
        }

        void reset() {
            parentFlag.reset();
        }

        int parseFlags(String[] args, Map<Character, Flag> flags) {
            return super.parseFlags(args, myFlags);
        }

        boolean parseArgs(String[] args, Messenger msg){
            boolean rc = true;
            if (args.length == 0) {
                moduleName = null;
            } else if (args.length == 1) {
                moduleName = args[0];
            } else {
                rc = false;
            }
            return rc;
        }

        boolean run(Repository repo, Messenger msg) {
            ListRepositoryVisitor visitor = new ListRepositoryVisitor();
            visitor.run(repo, msg);
            boolean found = visitor.wasFound();
            if (verboseFlag.isEnabled() && !found) {
                if (moduleName != null) {
                    msg.error("Could not find module name starting with '" + moduleName + "'");
                } else {
                    msg.error("Could not find any modules");
                }
            }
            return found;
        }

        String usage() {
            return "list [-v] [-p] [-r repositoryLocation] moduleName\n"
                +  "        lists the modules in the repository";
        }

        class ListRepositoryVisitor extends RepositoryVisitor {
            private boolean found = false;

            boolean wasFound() { return found; }

            void doit(Repository repo, Messenger msg) {
                boolean printedHeader = false;
                for (ModuleArchiveInfo mai : repo.list()) {
                    if (moduleName == null || mai.getName().startsWith(moduleName)) {
                        if (!printedHeader) {
                            msg.println(getRepositoryText(repo));
                            msg.println(verboseFlag.isEnabled() ? maiHeadingVerbose : maiHeading);
                            printedHeader = true;
                        }
                        msg.println(getMAIText(mai));
                        found = true;
                    }
                }
            }
        }
    }
}
