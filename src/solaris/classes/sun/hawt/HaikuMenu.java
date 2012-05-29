/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.hawt;

import java.awt.*;
import java.awt.peer.*;

public class HaikuMenu extends HaikuMenuComponent implements MenuPeer {

    private native long nativeCreateMenu();
    private native void nativeSetLabel(long menuPtr, String label);
    private native void nativeSetEnabled(boolean enabled);
    private native void nativeAddSeparator(long menuPtr);
    private native void nativeAddItem(long menuPtr, long itemPtr);
    private native void nativeAddMenu(long menuPtr, long itemPtr);
    private native void nativeDeleteItem(long menuPtr, int index);

    public HaikuMenu(Menu target) {
        super(target);
        initialize(target);
    }

    protected void initialize(MenuItem target) {
        setLabel(target.getLabel());
        setEnabled(target.isEnabled());
    }

    @Override
    protected long createModel() {
        return nativeCreateMenu();
    }

    @Override
    public void addItem(MenuItem item) {
    	MenuComponentPeer peer = item.getPeer();
    	if (peer instanceof HaikuMenu)
    	    nativeAddMenu(getModel(), ((HaikuMenu)peer).getModel());
    	else
            nativeAddItem(getModel(), ((HaikuMenuItem)peer).getModel());
    }

    @Override
    public void delItem(int index) {
        nativeDeleteItem(getModel(), index);
    }

    @Override
    public void addSeparator() {
        nativeAddSeparator(getModel());
    }

    @Override
    public void setLabel(String label) {
    	nativeSetLabel(getModel(), label);
    }

    @Override
    public void setEnabled(boolean enabled) {
        nativeSetEnabled(enabled);
    }

}
