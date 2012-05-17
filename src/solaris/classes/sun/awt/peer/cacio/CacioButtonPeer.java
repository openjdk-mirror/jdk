/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
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

import java.awt.Button;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.peer.ButtonPeer;

import javax.swing.JButton;

class CacioButtonPeer extends CacioComponentPeer<Button, JButton>
                      implements ButtonPeer {

    /**
     * Listens for ActionEvents on the Swing button and triggers corresponding
     * ActionEvents on the AWT button.
     *
     * @author Roman Kennke (kennke@aicas.com)
     */
    class SwingButtonListener implements ActionListener {

        /**
         * Receives notification when an action was performend on the button.
         * 
         * @param event
         *            the action event
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            Button b = getAWTComponent();
            ActionListener[] l = b.getActionListeners();
            ActionEvent ev = new ActionEvent(b, ActionEvent.ACTION_PERFORMED, b
                    .getActionCommand());
            // This sends both the new and old style events correctly.
            handlePeerEvent(ev);
        }
      
    }

    /**
     * Constructs a new SwingButtonPeer.
     * 
     * @param theButton
     *            the AWT button for this peer
     */
    public CacioButtonPeer(Button theButton, PlatformWindowFactory pwf) {
        super(theButton, pwf);
    }

    @Override
    JButton initSwingComponent() {
        JButton jbutton = new JButton();
        Button theButton = getAWTComponent();
        jbutton.setText(theButton.getLabel());
        jbutton.addActionListener(new SwingButtonListener());
        jbutton.setMargin(new Insets(0, 0, 0, 0));
        return jbutton;
    }

    /**
     * Sets the label of the button. This call is forwarded to the setText method
     * of the managed Swing button.
     *
     * @param label the label to set
     */
    @Override
    public void setLabel(String label)
    {
      getSwingComponent().setText(label);
    }

}
