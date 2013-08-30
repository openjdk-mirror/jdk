#
# Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
# Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Oracle designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
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
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#

BUILDDIR = ../../../../..
PACKAGE = sun.security.krb5.internal.ccache
PRODUCT = sun
include $(BUILDDIR)/common/Defs.gmk

#
# Java Files
#
AUTO_FILES_JAVA_DIRS = sun/security/krb5/internal/ccache

ifeq ($(PLATFORM), $(filter $(PLATFORM),linux solaris))
FILES_export = sun/security/krb5/internal/ccache/FileCredentialsCache.java
FILES_c = krb5ccache.c
LIBRARY = j2krb5

vpath %.c \
  $(PLATFORM_SRC)/native/sun/security/krb5/internal/ccache

include $(BUILDDIR)/common/Library.gmk

OTHER_CFLAGS += $(KRB5_CFLAGS)
OTHER_LDLIBS = $(LIBDL) $(JVMLIB) $(KRB5_LIBS)
endif # PLATFORM
