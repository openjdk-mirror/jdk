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

import java.awt.Adjustable;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.ScrollPane;
import java.awt.peer.ComponentPeer;
import java.awt.peer.ScrollPanePeer;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

import sun.awt.*;

class CacioScrollPanePeer extends CacioContainerPeer<ScrollPane, JPanel>
                          implements ScrollPanePeer {


    private JScrollBar verticalSB;
    private JScrollBar horizontalSB;

    private int childWidth, childHeight;

    private int viewX, viewY;
    private int viewWidth, viewHeight;

    CacioScrollPanePeer(ScrollPane awtC, PlatformWindowFactory pwf) {
        super(awtC, pwf);
    }

    @Override
    JPanel initSwingComponent() {
        JPanel jsp = new JPanel();
        verticalSB = new JScrollBar(JScrollBar.VERTICAL);
        horizontalSB = new JScrollBar(JScrollBar.HORIZONTAL);
        jsp.add(verticalSB);
        jsp.add(horizontalSB);
        jsp.setLayout(null); // We do the layout in the peer directly.
        return jsp;
    }

    @Override
    public int getHScrollbarHeight() {
        return horizontalSB.getHeight();
    }

    @Override
    public int getVScrollbarWidth() {
        return verticalSB.getWidth();
    }

    @Override
    public void setScrollPosition(int x, int y) {
        verticalSB.setValue(y);
        horizontalSB.setValue(x);
    }

    @Override
    public void childResized(int w, int h) {
        childWidth = w;
        childHeight = h;
        verticalSB.setMinimum(0);
        verticalSB.setMaximum(w);
        horizontalSB.setMinimum(0);
        horizontalSB.setMaximum(h);
    }

    @Override
    public void setUnitIncrement(Adjustable arg0, int arg1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setValue(Adjustable arg0, int arg1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void layout() {
        viewWidth = layoutVerticalScrollbar();
        viewHeight = layoutHorizontalScrollbar();
        ScrollPane sp = getAWTComponent();

        if (sp.getComponentCount() > 0) {
            Component view = sp.getComponent(0);
            ComponentPeer peer = AWTAccessor.getComponentAccessor().getPeer(view);
            if (peer instanceof CacioComponentPeer) {
                CacioComponentPeer ccp = (CacioComponentPeer) peer;
                ccp.setViewport(viewX, viewY, viewWidth, viewHeight);
            }
        }
        getSwingComponent().validate();
    }

    @Override
    public Dimension getPreferredSize() {

        Dimension preferredSize = null;
        JPanel component = getSwingComponent();

        if (component != null) {
            preferredSize = getSwingComponent().getSize();

        } else {
            preferredSize = super.getPreferredSize();
        }
        
        return preferredSize;
    }

    private int layoutVerticalScrollbar() {
        int vw;
        if (needVerticalScrollbar()) {
            verticalSB.setVisible(true);
            Dimension vsbSize = verticalSB.getPreferredSize();
            Dimension spSize = getAWTComponent().getSize();
            verticalSB.setBounds(spSize.width - vsbSize.width, 0,
                                 vsbSize.width, spSize.height);
            vw = spSize.width - vsbSize.width;
        } else {
            verticalSB.setVisible(false);
            vw = getAWTComponent().getWidth();
        }
        return vw;
    }

    private boolean needVerticalScrollbar() {
        int policy = getAWTComponent().getScrollbarDisplayPolicy();
        return policy == ScrollPane.SCROLLBARS_ALWAYS
               || (policy != ScrollPane.SCROLLBARS_NEVER
                   && childHeight > getAWTComponent().getHeight());
    }

    private int layoutHorizontalScrollbar() {
        int vh;
        if (needHorizontalScrollbar()) {
            horizontalSB.setVisible(true);
            Dimension hsbSize = horizontalSB.getPreferredSize();
            Dimension spSize = getAWTComponent().getSize();
            if (verticalSB.isVisible()) {
                spSize.width -= verticalSB.getWidth();
                int vsbH = verticalSB.getHeight() - hsbSize.height;
                verticalSB.setSize(new Dimension(verticalSB.getWidth(), vsbH));
            }
            horizontalSB.setBounds(0, spSize.height - hsbSize.height,
                                   spSize.width, hsbSize.height);
            vh = spSize.height - hsbSize.height;
        } else {
            verticalSB.setVisible(false);
            vh = getAWTComponent().getHeight();
        }
        return vh;
    }

    private boolean needHorizontalScrollbar() {
        int policy = getAWTComponent().getScrollbarDisplayPolicy();
        return policy == ScrollPane.SCROLLBARS_ALWAYS
               || (policy != ScrollPane.SCROLLBARS_NEVER
                   && childWidth > getAWTComponent().getWidth());
    }
}
