/*
 * Copyright 2008-2009 Sun Microsystems, Inc.  All Rights Reserved.
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

package sun.awt.peer.cacio;

import java.awt.Dialog;
import java.awt.Window;
import java.awt.peer.DialogPeer;
import java.awt.peer.WindowPeer;
import java.util.List;

import javax.swing.JRootPane;

import sun.awt.*;

class CacioDialogPeer extends CacioWindowPeer implements DialogPeer {

    public CacioDialogPeer(Dialog awtC, PlatformWindowFactory pwf) {
        super(awtC, pwf);
        setResizable(awtC.isResizable());
        setTitle(awtC.getTitle());
    }

    public void setResizable(boolean resizable) {

        getToplevelWindow().setResizable(resizable);

    }

    public void setTitle(String title) {

        getToplevelWindow().setTitle(title);
        
    }

    public void blockWindows(List<Window> windows) {
        for (Window window : windows) {
            WindowPeer peer = (WindowPeer)AWTAccessor.getComponentAccessor().getPeer(window);
            if (peer != null) {
              peer.setModalBlocked((Dialog)getAWTComponent(), true);
            }
        }
    }

    @Override
    protected int getRootPaneDecorationStyle() {
        if ((! isDecorateDialogs())
            || ((Dialog) getAWTComponent()).isUndecorated()) {
            return JRootPane.NONE;
        } else {
            return JRootPane.PLAIN_DIALOG;
        }
    }

}
