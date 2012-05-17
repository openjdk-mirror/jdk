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

package sun.awt.peer.cacio;

import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.peer.MenuItemPeer;
import javax.swing.JMenuItem;

class CacioMenuItemPeer extends CacioMenuComponentPeer<MenuItem,JMenuItem>
                        implements MenuItemPeer {

    private class ProxyListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            ActionListener[] l = getAWTMenu().getActionListeners();
            if (l != null && l.length > 0) {
                MenuItem i = getAWTMenu();
                for (int idx = 0; idx < l.length; idx++) {
                    ActionEvent ev = new ActionEvent(i, idx,
                                                     e.getActionCommand());
                    l[idx].actionPerformed(ev);
                }
            }
        }

    }

    CacioMenuItemPeer(MenuItem i) {
        this(i, new JMenuItem());
    }

    CacioMenuItemPeer(MenuItem i, JMenuItem jmi) {
        super(i, jmi);
    }

    @Override
    void postInitSwingComponent() {
        setLabel(getAWTMenu().getLabel());
        setEnabled(getAWTMenu().isEnabled());
        if (needActionProxy()) {
            getSwingMenu().addActionListener(new ProxyListener());
        }
    }

    boolean needActionProxy() {
        return true;
    }

    public void setLabel(String label) {
        getSwingMenu().setText(label);
    }

    public void setEnabled(boolean e) {
        getSwingMenu().setEnabled(e);
    }

}
