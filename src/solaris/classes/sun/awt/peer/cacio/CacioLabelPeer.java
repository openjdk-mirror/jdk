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

import java.awt.Label;
import java.awt.peer.LabelPeer;

import javax.swing.JLabel;

class CacioLabelPeer extends CacioComponentPeer<Label, JLabel> implements LabelPeer {

    /**
     * Creates a new <code>SwingLabelPeer</code> for the specified AWT label.
     * 
     * @param label the AWT label
     * @param pwf the platform window factory
     */
    public CacioLabelPeer(Label label, PlatformWindowFactory pwf) {
        super(label, pwf);
    }

    @Override
    JLabel initSwingComponent() {
        Label label = getAWTComponent();
        JLabel swingLabel = new JLabel();
        swingLabel.setText(label.getText());
        swingLabel.setOpaque(true);
        return swingLabel;
    }

    @Override
    void postInitSwingComponent() {
        super.postInitSwingComponent();
        // TODO: Maybe make the AWT component type generic.
        setAlignment(getAWTComponent().getAlignment());
    }

    /**
     * Sets the text of the label. This is implemented to set the text on the
     * Swing label.
     *
     * @param text the text to be set
     */
    @Override
    public void setText(String text) {
        getSwingComponent().setText(text);
    }

    /**
     * Sets the horizontal alignment of the label. This is implemented to
     * set the alignment on the Swing label.
     *
     * @param alignment the horizontal alignment
     *
     * @see Label#LEFT
     * @see Label#RIGHT
     * @see Label#CENTER
     */
    @Override
    public void setAlignment(int alignment) {
        JLabel swingLabel = getSwingComponent();
        switch (alignment) {
            case Label.RIGHT:
                swingLabel.setHorizontalAlignment(JLabel.RIGHT);
                break;
            case Label.CENTER:
                swingLabel.setHorizontalAlignment(JLabel.CENTER);
                break;
            case Label.LEFT:
            default:
                swingLabel.setHorizontalAlignment(JLabel.LEFT);
                break;
        }
    }

    /**
     * Swing labels are focusable, but AWT labels are not.
     */
    @Override
    public boolean isFocusable() {
        return false;
    }
}
