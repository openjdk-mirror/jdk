/*
 * Copyright 2009 Sun Microsystems, Inc.  All Rights Reserved.
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

package sun.awt.peer.cacio.managed;

import sun.awt.peer.cacio.*;
import sun.security.action.*;

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.security.*;
import java.util.logging.*;

public class FocusManager {

    private static Logger logger = Logger.getLogger(FocusManager.class.getName());

    private static FocusManager instance;
    private static final Class focusManagerCls;

    /**
     * Load the specified FocusManager Implementation, or default to
     * FocusManager in case the property "cacio.focusmgr" has not been set.
     */
    static {
	String focusMgrClsName = AccessController.doPrivileged(new GetPropertyAction("cacio.focusmgr"));
	Class cls = FocusManager.class;
	try {
	    if (focusMgrClsName != null) {
		cls = Class.forName(focusMgrClsName);
	    }
	} catch (ClassNotFoundException e) {
	    logger.log(Level.SEVERE, "Unable to load FocusManager implementation", e);
	}

	focusManagerCls = cls;
    }

    /**
     * @return The FocusManager instance for the current "context"
     */
    static FocusManager getInstance() {
	if (instance == null) {
	    try {
		instance = (FocusManager) focusManagerCls.newInstance();
	    } catch (ReflectiveOperationException e) {
		logger.log(Level.SEVERE, "Unable to create FocusManager instance", e);
	    }
	}
	return instance.getContextInstance();
    }

    private ManagedWindow focusedWindow;

    public FocusManager() {
    }

    /**
     * The default-implementation is classloader scoped, as it stores the
     * FocusManager in a static field. Subclasses that wish to change the scope
     * (e.g. to AppContext scope) have to override this method.
     * 
     * @return FocusManager instance for the current "context".
     */
    protected FocusManager getContextInstance() {
	return instance;
    }

    ManagedWindow getFocusedWindow() {
	return focusedWindow;
    }

    void setVisible(ManagedWindow w, boolean v) {
	if (v) {
	    setFocusedWindow(w);
	} else {
	    setFocusedWindow(null);
	}
    }

    void mousePressed(ManagedWindow w) {
	if (w != focusedWindow) {
	    setFocusedWindow(w);
	}
    }

    void setFocusedWindow(ManagedWindow w) {
	setFocusedWindowNoEvent(w);
	focusLost(focusedWindow, w);
	focusGained(w, focusedWindow);
    }

    void setFocusedWindowNoEvent(ManagedWindow w) {
	focusedWindow = w;
    }

    private void focusLost(ManagedWindow w, ManagedWindow lostTo) {
	if (w != null) {
	    CacioComponent cacioComp = w.getCacioComponent();
	    Component c = cacioComp.getAWTComponent();
	    Component opposite = getAWTComponent(lostTo);
	    FocusEvent fe = new FocusEvent(c, FocusEvent.FOCUS_LOST, false, opposite);
	    cacioComp.handlePeerEvent(fe);
	}
    }

    private void focusGained(ManagedWindow w, ManagedWindow lost) {
	if (w != null) {
	    CacioComponent cacioComp = w.getCacioComponent();
	    Component c = cacioComp.getAWTComponent();
	    Component opposite = getAWTComponent(lost);
	    FocusEvent fe = new FocusEvent(c, FocusEvent.FOCUS_GAINED, false, opposite);
	    cacioComp.handlePeerEvent(fe);
	}
    }

    private Component getAWTComponent(ManagedWindow w) {
	Component c;
	if (w != null) {
	    CacioComponent cacio = w.getCacioComponent();
	    c = cacio.getAWTComponent();
	} else {
	    c = null;
	}
	return c;
    }
}
