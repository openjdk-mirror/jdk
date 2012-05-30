/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.hawt;

import sun.awt.SunToolkit;
import sun.lwawt.LWToolkit;

import java.awt.*;
import java.awt.event.*;
import java.awt.peer.MenuItemPeer;

public class HaikuMenuItem extends HaikuMenuComponent implements MenuItemPeer {

    private native long nativeCreateMenuItem(long menuItemPtr,
        boolean separator);
    private native void nativeSetLabel(long itemPtr, String label);
    private native void nativeSetEnabled(long itemPtr, boolean enabled);

    public HaikuMenuItem(MenuItem target) {
        super(target);
        initialize(target);
    }

    protected void initialize(MenuItem target) {
        if (!isSeparator()) {
            setLabel(target.getLabel());
        }
    }

    private boolean isSeparator() {
        String label = ((MenuItem)getTarget()).getLabel();
        return (label != null && label.equals("-"));
    }

    @Override
    protected long createModel() {
        HaikuMenuComponent parent = (HaikuMenuComponent)
        	LWToolkit.targetToPeer(getTarget().getParent());
        return nativeCreateMenuItem(parent.getModel(), isSeparator());
    }

    @Override
    public void setLabel(String label) {
    	nativeSetLabel(getModel(), label);
    }

    @Override
    public void setEnabled(boolean enabled) {
        nativeSetEnabled(getModel(), enabled);
    }

    void handleAction(final long when, final int modifiers, boolean checked) {
        SunToolkit.executeOnEventHandlerThread(getTarget(), new Runnable() {
            public void run() {
                final String cmd = ((MenuItem)getTarget()).getActionCommand();
                final ActionEvent event = new ActionEvent(getTarget(), ActionEvent.ACTION_PERFORMED, cmd, when, modifiers);
                SunToolkit.postEvent(SunToolkit.targetToAppContext(getTarget()), event);
            }
        });
    }
}
