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

import java.awt.Scrollbar;
import java.awt.peer.ScrollbarPeer;
import javax.swing.JScrollBar;

class CacioScrollBarPeer extends CacioComponentPeer<Scrollbar,JScrollBar>
                         implements ScrollbarPeer {

    CacioScrollBarPeer(Scrollbar sb, PlatformWindowFactory pwf) {
        super(sb, pwf);
    }

    @Override
    JScrollBar initSwingComponent() {
        Scrollbar sb = getAWTComponent();
        int orientation = sb.getOrientation();
        int swingOrientation;
        if (orientation == Scrollbar.HORIZONTAL) {
            swingOrientation = JScrollBar.HORIZONTAL;
        } else {
            assert orientation == Scrollbar.VERTICAL;
            swingOrientation = JScrollBar.VERTICAL;
        }
        return new JScrollBar(swingOrientation);
    }

    @Override
    void postInitSwingComponent() {
        super.postInitSwingComponent();
        Scrollbar sb = getAWTComponent();
        setValues(sb.getValue(), sb.getVisibleAmount(), sb.getMinimum(),
                  sb.getMaximum());
        setLineIncrement(sb.getUnitIncrement());
        setPageIncrement(sb.getBlockIncrement());
    }

    public void setValues(int value, int visible, int minimum, int maximum) {
        getSwingComponent().setValues(value, visible, minimum, maximum);
    }

    public void setLineIncrement(int l) {
        getSwingComponent().setUnitIncrement(l);
    }

    public void setPageIncrement(int l) {
        getSwingComponent().setBlockIncrement(l);
    }

}
