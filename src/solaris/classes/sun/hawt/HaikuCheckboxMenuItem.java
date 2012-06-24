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

import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.peer.CheckboxMenuItemPeer;

import sun.awt.SunToolkit;
import sun.lwawt.LWToolkit;

public class HaikuCheckboxMenuItem extends HaikuMenuItem implements CheckboxMenuItemPeer {

    private native long nativeCreateCheckboxMenuItem(long menuItemPtr);
    private native void nativeSetState(long modelPtr, boolean state);

    HaikuCheckboxMenuItem(CheckboxMenuItem target) {
        super(target);
        setState(target.getState());
    }

    @Override
    protected long createModel() {
        HaikuMenuComponent parent = (HaikuMenuComponent)
        	LWToolkit.targetToPeer(getTarget().getParent());
        return nativeCreateCheckboxMenuItem(parent.getModel());
    }

    @Override
    public void setState(boolean state) {
        nativeSetState(getModel(), state);
    }

    @Override
    public void handleAction(long when, int modifiers, final boolean state) {
        super.handleAction(when, modifiers, state);
        final CheckboxMenuItem target = (CheckboxMenuItem)getTarget();
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                target.setState(state);
            }
        });
        ItemEvent event = new ItemEvent(target, ItemEvent.ITEM_STATE_CHANGED, target.getLabel(), state ? ItemEvent.SELECTED : ItemEvent.DESELECTED);
        SunToolkit.postEvent(SunToolkit.targetToAppContext(getTarget()), event);
    }

}
