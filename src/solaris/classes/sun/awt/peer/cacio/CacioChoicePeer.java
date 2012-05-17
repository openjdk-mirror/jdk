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

import java.awt.Choice;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.peer.ChoicePeer;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.MutableComboBoxModel;

class CacioChoicePeer extends CacioComponentPeer<Choice, JComboBox>
                      implements ChoicePeer {

    private class TrampolineListener implements ItemListener {

        public void itemStateChanged(ItemEvent e) {
            // It appears that AWT doesn't send DESELECTED events.
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                return;
            }
            ItemListener[] listeners = getAWTComponent().getItemListeners();
            if (listeners != null && listeners.length > 0) {
                JComboBox cb = getSwingComponent();
                Choice comp = getAWTComponent();
                for (int i = 0; i < listeners.length; i++) {
                    ItemEvent ev = new ItemEvent(comp, e.getID(), cb.getSelectedItem(),
                                                 e.getStateChange());
                    listeners[i].itemStateChanged(ev);
                }
            }
        }

    }

    CacioChoicePeer(Choice awtComp, PlatformWindowFactory pfw) {
        super(awtComp, pfw);
    }

    @Override
    JComboBox initSwingComponent() {

        JComboBox comboBox = new JComboBox();
        return comboBox;
    }

    @Override
    void postInitSwingComponent() {
        super.postInitSwingComponent();
        // Put all pre-existing items of the AWT Choice into the JComboBox.
        Choice c = getAWTComponent();
        int size = c.getItemCount();
        for (int i = 0; i < size; i++) {
            getModel().addElement(c.getItem(i));
        }
        // Setup trampoline listener.
        getSwingComponent().addItemListener(new TrampolineListener());
    }

    private MutableComboBoxModel getModel() {
        ComboBoxModel m = getSwingComponent().getModel();
        MutableComboBoxModel mm = (MutableComboBoxModel) m;
        return mm;
    }

    @Override
    public void add(String item, int index) {
        getModel().insertElementAt(item, index);
    }

    @Override
    public void remove(int index) {
        getModel().removeElementAt(index);
    }

    @Override
    public void removeAll() {
        MutableComboBoxModel m = getModel();
        int size = m.getSize();
        for (int item = size - 1; item >= 0; item--) {
            m.removeElementAt(item);
        }
    }

    @Override
    public void select(int index) {
        getSwingComponent().setSelectedIndex(index);
    }

}
