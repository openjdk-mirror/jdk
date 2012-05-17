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

import java.awt.Frame;
import java.awt.MenuBar;
import java.awt.Rectangle;
import java.awt.peer.FramePeer;

import java.awt.peer.MenuBarPeer;
import javax.swing.JMenuBar;
import javax.swing.JRootPane;

class CacioFramePeer extends CacioWindowPeer implements FramePeer {

    public CacioFramePeer(Frame awtC, PlatformWindowFactory pwf) {
        super(awtC, pwf);
        // TODO Auto-generated constructor stub
    }

    public int getState() {

        return getToplevelWindow().getState();

    }

    public void setState(int state) {

        getToplevelWindow().setState(state);

    }

    public void setMaximizedBounds(Rectangle bounds) {

        getToplevelWindow().setMaximizedBounds(bounds);
        
    }

    public void setMenuBar(MenuBar mb) {

        MenuBarPeer mbp = (MenuBarPeer) mb.getPeer();
        if (mbp == null) {
            mb.addNotify();
            mbp = (MenuBarPeer) mb.getPeer();
        }
        assert mbp instanceof CacioMenuBarPeer;
        JMenuBar jmb = ((CacioMenuBarPeer) mbp).getSwingMenu();
        JRootPane rp = getSwingComponent();
        rp.setJMenuBar(jmb);
    }

    public void setResizable(boolean resizable) {

        getToplevelWindow().setResizable(resizable);

    }

    public void setTitle(String title) {

        getToplevelWindow().setTitle(title);
        
    }

    public Rectangle getBoundsPrivate() {

        // TODO: Implement this correctly.
        System.out.println("IMPLEMENT ME: CacioFramePeer.getBoundsPrivate");
        return null;

    }

    public void setBoundsPrivate(int x, int y, int width, int height) {

        // TODO: Implement this correctly.
        System.out.println("IMPLEMENT ME: CacioFramePeer.setBoundsPrivate");

    }

    @Override
    protected int getRootPaneDecorationStyle() {
        if ((! isDecorateWindows())
            || ((Frame) getAWTComponent()).isUndecorated()) {
            return JRootPane.NONE;
        } else {
            return JRootPane.FRAME;
        }
    }

}
