#
# Copyright 2006-2007 Sun Microsystems, Inc.  All Rights Reserved.
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

BUILDDIR = ../..
PACKAGE = java.module
PRODUCT = java
include $(BUILDDIR)/common/Defs.gmk

OTHER_JAVACFLAGS = -Xlint:all

#
# Files to compile
#
AUTO_FILES_JAVA_DIRS = \
    java/module \
    sun/module
AUTO_JAVA_PRUNE = osgi

#
# Directories
#
PROPS_SRC   = $(TOPDIR)/src/share/lib/module/module.properties
PROPS_BUILD = $(LIBDIR)/module/module.properties

VISIBILITY_POLICY_SRC =	$(TOPDIR)/src/share/lib/module/visibility.policy
VISIBILITY_POLICY_BUILD = $(LIBDIR)/module/visibility.policy

IMPORT_OVERRIDE_POLICY_SRC = $(TOPDIR)/src/share/lib/module/import.override.policy
IMPORT_OVERRIDE_POLICY_BUILD = $(LIBDIR)/module/import.override.policy

REPOSITORY_PROPS_SRC = $(TOPDIR)/src/share/lib/module/repository.properties
REPOSITORY_PROPS_BUILD = $(LIBDIR)/module/repository.properties

SYSTEM_EXTENSION_DIR = $(LIBDIR)/module/ext

#
# Resources
#
REPOSITORY_METADATA_XML = $(PKGDIR)/repository-metadata-schema.xml

#
# Rules
#
include $(BUILDDIR)/common/Rules.gmk

all: classes repository_metadata_schema_xml

build: properties visibilitypolicy importoverridepolicy repositoryprops systemextensiondir

install: all

properties: classes  $(PROPS_BUILD)

visibilitypolicy: classes  $(VISIBILITY_POLICY_BUILD)

importoverridepolicy: classes  $(IMPORT_OVERRIDE_POLICY_BUILD)

repositoryprops: classes $(REPOSITORY_PROPS_BUILD)

.PHONY: systemextensiondir
systemextensiondir:
	$(MKDIR) -p $(SYSTEM_EXTENSION_DIR)

$(PROPS_BUILD): $(PROPS_SRC)
	$(install-file)

$(VISIBILITY_POLICY_BUILD): $(VISIBILITY_POLICY_SRC)
	$(install-file)

$(IMPORT_OVERRIDE_POLICY_BUILD): $(IMPORT_OVERRIDE_POLICY_SRC)
	$(install-file)

$(REPOSITORY_PROPS_BUILD): $(REPOSITORY_PROPS_SRC)
	$(install-file)

clean clobber:: .delete.classlist
	$(RM) -r $(CLASSBINDIR)/java/module
	$(RM) $(PROPS_BUILD) $(VISIBILITY_POLICY_BUILD) $(IMPORT_OVERRIDE_POLICY_BUILD) $(REPOSITORY_PROPS_BUILD)

repository_metadata_schema_xml: $(CLASSBINDIR)/$(REPOSITORY_METADATA_XML)

$(CLASSBINDIR)/%.xml: $(SHARE_SRC)/classes/%.xml
	$(install-file)

.PHONY: repository_metadata_schema_xml
