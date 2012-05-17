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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.PaintEvent;
import java.awt.peer.ComponentPeer;
import java.awt.peer.ContainerPeer;
import javax.swing.JComponent;

import sun.awt.*;

abstract class CacioContainerPeer<AWTComponentType extends Component, SwingComponentType extends JComponent>
    extends CacioComponentPeer<AWTComponentType, SwingComponentType>
    implements ContainerPeer {

    /**
     * Indicates if this component is currently layouted or not.
     */
    private boolean isLayouting;

    public CacioContainerPeer(AWTComponentType awtC, PlatformWindowFactory pwf) {
        super(awtC, pwf);
    }

    @Override
    boolean isLayouting() {
        return isLayouting;
    }

    public void beginLayout() {

        // Skip all painting till endLayout().
        isLayouting = true;

    }

    public void endLayout() {

        if (! getPaintArea().isEmpty()
            && !AWTAccessor.getComponentAccessor().getIgnoreRepaint(getAWTComponent())) {

            // If not waiting for native painting repaint damaged area.
            handlePeerEvent(new PaintEvent(getAWTComponent(), PaintEvent.PAINT,
                                           new Rectangle()));
        }
        isLayouting = false;
    }

    public void beginValidate() {

        // This can be used for optimization (e.g. defer painting while
        // layouting). Nothing to do here for now.

    }

    public void endValidate() {

        // This can be used for optimization (e.g. defer painting while
        // layouting). Nothing to do here for now.

    }

    @Override
    public Insets getInsets() {

        return platformWindow.getInsets();
    }

    public boolean isRestackSupported() {

        return platformWindow.isRestackSupported();

    }

    public void restack() {

        platformWindow.restack();

    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        // The font must propagate through the hierarchy until some component
        // defines its own font.
        Container c = (Container) getAWTComponent();
        int count = c.getComponentCount();
        for (int i = 0; i < count; i++) {
            Component comp = c.getComponent(i);
            if (AWTAccessor.getComponentAccessor().getFont(comp) == null) {
                ComponentPeer peer = comp.getPeer();
                if (peer instanceof CacioComponentPeer) {
                    CacioComponentPeer ccp = (CacioComponentPeer) peer;
                    if (ccp.getFont() != font) {
                        ccp.setFont(font);
                    }
                }
            }
        }
    }

    @Override
    public void setForeground(Color fg) {
        super.setForeground(fg);
        // The foreground must propagate through the hierarchy until some
        // component defines its own font.
        Container c = (Container) getAWTComponent();
        int count = c.getComponentCount();
        for (int i = 0; i < count; i++) {
            Component comp = c.getComponent(i);
            if (AWTAccessor.getComponentAccessor().getForeground(comp) == null) {
                ComponentPeer peer = comp.getPeer();
                if (peer instanceof CacioComponentPeer) {
                    CacioComponentPeer ccp = (CacioComponentPeer) peer;
                    if (ccp.getForeground() != fg) {
                        ccp.setForeground(fg);
                    }
                }
            }
        }
    }


    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        // The background must propagate through the hierarchy until some
        // component defines its own background.
        Container c = (Container) getAWTComponent();
        int count = c.getComponentCount();
        for (int i = 0; i < count; i++) {
            Component comp = c.getComponent(i);
            if (AWTAccessor.getComponentAccessor().getBackground(comp) == null) {
                ComponentPeer peer = comp.getPeer();
                if (peer instanceof CacioComponentPeer) {
                    CacioComponentPeer ccp = (CacioComponentPeer) peer;
                    if (ccp.getBackground() != bg) {
                        ccp.setBackground(bg);
                    }
                }
            }
        }
    }

    @Override
    public void setEnabled(boolean enable) {

        // Check ancestors and only enable subtree if all the ancestors are
        // enabled.
        if (enable && ! isParentsEnabled()) {
            return;
        }
        setEnabledImpl(enable);
    }

    @Override
    void setEnabledImpl(boolean enable) {
        super.setEnabledImpl(enable);
        // The property must propagate through the hierarchy until some
        // component defines its own font.
        Container c = (Container) getAWTComponent();
        int count = c.getComponentCount();
        for (int i = 0; i < count; i++) {
            Component comp = c.getComponent(i);
            if ((! enable) || AWTAccessor.getComponentAccessor().isEnabled(comp)) {
                ComponentPeer peer = comp.getPeer();
                if (peer instanceof CacioComponentPeer) {
                    CacioComponentPeer ccp = (CacioComponentPeer) peer;
                    if (ccp.isEnabled() != enable) {
                        ccp.setEnabledImpl(enable);
                    }
                }
            }
        }
    }

    @Override
    protected void peerPaint(Graphics g, boolean update) {
        if (! update) {
            super.peerPaint(g, update);
        }
    }

}
