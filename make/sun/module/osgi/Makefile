#
# Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Sun designates this
# particular file as subject to the "Classpath" exception as provided
# by Sun in the LICENSE file that accompanied this code.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
# CA 95054 USA or visit www.sun.com if you need additional information or
# have any questions.
#

#
# Makefile for building OSGi repository
#

BUILDDIR = ../../..
PACKAGE = sun.module.osgi 
PRODUCT = sun
include $(BUILDDIR)/common/Defs.gmk

ifndef OPENJDK 
  # OSGi repository depends on the OSGi framework API
  #
  # Temporary workaround: osgi-repo.jar will be built only
  # if OSGI_FRAMEWORK_JAR and FELIX_JAR are set
  MODULES_LIB = $(CLOSED_SHARE_SRC)/lib/module
  ifndef OSGI_FRAMEWORK_JAR
     OSGI_FRAMEWORK_JAR = $(MODULES_LIB)/org.osgi.core-1.0.1.jar
  endif
  ifndef FELIX_JAR
     FELIX_JAR = $(MODULES_LIB)/felix.jar
  endif
endif

OSGI_REPO_DEST = $(LIBDIR)/osgi-repo.jar

EXISTS := $(call FileExists,$(OSGI_FRAMEWORK_JAR),,)
ifneq ("$(EXISTS)", "NO_FILE_EXISTS")
  TARGET = classes $(OSGI_REPO_DEST)
endif

ifdef OSGI_FRAMEWORK_JAR
  #
  # Files to compile
  #
  AUTO_FILES_JAVA_DIRS =  sun/module/osgi
  #
  # Temporary workaround to include the OSGi framework
  # and Felix OSGi container for compilation.
  OTHER_JAVACFLAGS = -cp $(OSGI_FRAMEWORK_JAR):$(FELIX_JAR)
endif

#
# Rules.
#
include $(BUILDDIR)/common/Classes.gmk

all: $(TARGET)

#
# Extra rule to build osgi-repo.jar
#

$(OSGI_REPO_DEST): $(LIBDIR) $(FILES_class) 
	$(BOOT_JAR_CMD) -cf $(OSGI_REPO_DEST) \
               -C $(CLASSBINDIR) sun/module/osgi \
		    $(JAR_JFLAGS)
	$(RM) -rf $(CLASSBINDIR)/sun/module/osgi
	@$(java-vm-cleanup)

clean clobber::
	$(RM) $(OSGI_REPO_DEST)

