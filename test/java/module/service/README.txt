There are two sets of ServiceLoader - related tests:

* Those in test/java/module/service (this directory)
* Those in test/java/module/modinit/mtest/service

test/java/module/service
------------------------

ServiceTest is a base class for the actual testcases.  Each testcase has some
supporting files under the src directory, related as follows:

ClasspathServiceTest.java   src/cpserv
CharsetServiceTest.java     src/charserv
ClientServiceTest.java      src/cliserv
DefaultServiceTest.java     src/defserv
ModuleServiceTest.java      src/modserv
ReexportServiceTest.java    src/rxpserv
RepositoryServiceTest.java  src/reposerv
VersionServiceTest.java     src/verserv

In general, each test creates some jam files, installs them into a repository,
and then launches another JVM that accesses them.  For each test there is a
client, e.g. src/defserv/client/Main.java, which does the actual testing.
Testing is limited to ensuring that the correct providers are loaded at the
in the correct order.

The process of creating JAMs for each test involves compiling corresponding
code under src.  The client and service classes use @Service and
@ServiceProvider annotations.  The code which runs javac to compile these is
in ServiceTest, and invocation of javac includes a -processor directive to
cause use of sun.module.core.ServiceProcessor.

Below is a brief description of each test.  There is more information in each
testcase.  In general, looking at a *Test.java and it's corresponding
src/*/client/Main.java will give a good idea of what the test is doing.

* ClasspathServiceTest: Ensures that a service-provider module can access code
that is on the classpath if the client imports the java.classpath module.

* CharsetServiceTest: Ensures that a service-provider module can be accessed
via a service that is in java.se.core.

* ClientServiceTest: Ensures that a service-provider can be accessed when the
client directly calls ServiceLoader.load.  Contrast this with the expected
case (embodied in other testcases) in which it is the service class itself
which calls ServiceLoader.load.

* DefaultServiceTest: Ensures that default service-providers are available as
expected.

* ModuleServiceTest: This test differs from the others, in that much of it is
devoted to ensuring that sun.module.service.ServiceProcessor is working
correctly.  It expects certain files to have been put into the JAMs with
certain contents (e.g. META-INF/service-index).

* ReexportServiceTest: Ensures that service-providers can be obtained from
modules that are reexported from one to another.

* RepositoryServiceTest: Ensures that services and providers can be in
different repositories; exercises ServiceLoader.load(Class<S>, Repository).

* VersionServiceTest: Ensures that the correct version of a service-provider
is obtained in the presence of multiple versions.


test/java/module/modinit/mtest/service
--------------------------------------

These tests use the RunMTest driver (in test/java/module/modinit); the source
for each test is a single ".mtest" file.  There is a description in each file;
here is a summary:

* SqlDriver: Ensures that a java.se.core can act as a service-module with
service providers in separate service-provider modules.

* PrintServiceLookup: Ensures that java.se.core can act as both a
service-module and a service-provider module when appropriate.

* NullPrintServiceLookup: Same as PrintServiceLookup except invokes a
different call to ServiceLoader.load().

* InstalledLookup: Same as PrintServiceLookup except invokes
ServiceLoader.loadInstalled().


Annotation Processing
---------------------

Several of the tests' super_package.java files use the
java.module.annotation.Services and java.module.annotation.ServiceProcessor
annotations.  These are processed by sun.moule.core.ServiceProcessor.
Javac compilation does not (currently) include that processor by default.  It
is enabled in ServiceTest.compileSources.
