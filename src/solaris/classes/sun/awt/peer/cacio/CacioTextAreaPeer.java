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

import java.awt.AWTEvent;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Paint;
import java.awt.TextArea;
import java.awt.event.PaintEvent;
import java.awt.im.InputMethodRequests;
import java.awt.peer.ContainerPeer;
import java.awt.peer.TextAreaPeer;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

class CacioTextAreaPeer extends CacioComponentPeer<TextArea, JScrollPane> implements TextAreaPeer {

    private JTextArea textArea;

    public CacioTextAreaPeer(TextArea awtC, PlatformWindowFactory pwf) {
        super(awtC, pwf);
    }

    @Override
    JScrollPane initSwingComponent() {
        TextArea awtTextArea = getAWTComponent();
        textArea = new JTextArea();
        int sbv = awtTextArea.getScrollbarVisibility();
        int hsp;
        int vsp;
        if (sbv == TextArea.SCROLLBARS_NONE) {
            hsp = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER;
            vsp = JScrollPane.VERTICAL_SCROLLBAR_NEVER;
        } else if (sbv == TextArea.SCROLLBARS_HORIZONTAL_ONLY) {
            hsp = JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS;
            vsp = JScrollPane.VERTICAL_SCROLLBAR_NEVER;
        } else if (sbv == TextArea.SCROLLBARS_VERTICAL_ONLY) {
            hsp = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER;
            vsp = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS;
        } else {
            hsp = JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS;
            vsp = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS;
        }
        JScrollPane sp = new JScrollPane(textArea, vsp, hsp);
        return sp;
    }

    @Override
    void postInitSwingComponent() {
        super.postInitSwingComponent();
        setText(getAWTComponent().getText());
    }

    @Override
    public Dimension getMinimumSize(int rows, int columns) {
        return getPreferredSize(rows, columns);
    }

    @Override
    public Dimension getPreferredSize(int rows, int columns) {
        Font f = textArea.getFont();
        FontMetrics fm = textArea.getFontMetrics(f);
        int w = fm.charWidth('m') * columns;
        int h = fm.getHeight() * rows;
        Dimension spSize = getSwingComponent().getMinimumSize();
        spSize.width += w;
        spSize.height += h;
        return spSize;
    }

    @Override
    public void insert(String text, int pos) {
        getTextArea().insert(text, pos);
    }

    @Override
    public void replaceRange(String text, int start, int end) {
        getTextArea().replaceRange(text, start, end);
    }

    @Override
    public int getCaretPosition() {
        return getTextArea().getCaretPosition();
    }

    @Override
    public InputMethodRequests getInputMethodRequests() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getSelectionEnd() {
        return getTextArea().getSelectionEnd();
    }

    @Override
    public int getSelectionStart() {
        return getTextArea().getSelectionStart();
    }

    @Override
    public String getText() {
        return getTextArea().getText();
    }

    @Override
    public void select(int selStart, int selEnd) {
        getTextArea().select(selStart, selEnd);
    }

    @Override
    public void setCaretPosition(int pos) {
        getTextArea().setCaretPosition(pos);
    }

    @Override
    public void setEditable(boolean editable) {
        getTextArea().setEditable(editable);
    }

    @Override
    public void setText(String text) {
        getTextArea().setText(text);
    }

    private JTextArea getTextArea() {
        return textArea;
    }

    @Override
    void setEnabledImpl(boolean enable) {
        super.setEnabledImpl(enable);
        getTextArea().setEnabled(enable);
    }
}
