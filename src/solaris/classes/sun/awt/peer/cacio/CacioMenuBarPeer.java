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

import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.peer.MenuBarPeer;
import java.awt.peer.MenuPeer;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

class CacioMenuBarPeer extends CacioMenuComponentPeer<MenuBar,JMenuBar>
                       implements MenuBarPeer {


    private Menu helpMenu;

    CacioMenuBarPeer(MenuBar mb) {
        super(mb, new JMenuBar());
    }

    @Override
    void postInitSwingComponent() {

        MenuBar mb = getAWTMenu();
        int menuCount = mb.getMenuCount();
        for (int i = 0; i < menuCount; i++) {
            Menu m = mb.getMenu(i);
            addMenu(mb.getMenu(i));
        }
        Menu helpMenu = mb.getHelpMenu();
        if (helpMenu != null) {
            addHelpMenu(helpMenu);
        }
    }

    public void addMenu(Menu m) {
        JMenuBar jmb = getSwingMenu();
        // If we have a help menu, add new menus add the last - 1 position,
        // otherwise we append at the end.
        if (helpMenu != null) {
            jmb.add(getSwingMenu(m), jmb.getComponentCount() - 1);
        } else {
            jmb.add(getSwingMenu(m));
        }
        // Force re-layout.
        jmb.revalidate();
    }

    public void addHelpMenu(Menu m) {
        // Remove old help menu, if there is one.
        JMenuBar jmb = getSwingMenu();
        if (helpMenu != null) {
            jmb.remove(getSwingMenu(helpMenu));
        }

        // Add new help menu.
        helpMenu = m;
        jmb.add(getSwingMenu(m));
        // Force re-layout.
        jmb.revalidate();
    }

    private JMenu getSwingMenu(Menu m) {
        MenuPeer mp = (MenuPeer) m.getPeer();
        if (mp == null) {
            m.addNotify();
            mp = (MenuPeer) m.getPeer();
        }
        assert mp instanceof CacioMenuPeer;
        return (JMenu) ((CacioMenuPeer) mp).getSwingMenu();
    }

    public void delMenu(int index) {
        getSwingMenu().remove(index);
    }

}
