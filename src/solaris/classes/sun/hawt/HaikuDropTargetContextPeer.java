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
import javax.swing.*;
import sun.awt.dnd.*;

final class HaikuDropTargetContextPeer extends SunDropTargetContextPeer {

    // When we're dropped on we put the native pointer to the
    // message here
    private long nativeMessage = 0;
    private Component target;

    private native void nativeDisposeMessage(long nativeMessage);

    private HaikuDropTargetContextPeer(long nativeMessage, Component target) {
        super();
        
        this.nativeMessage = nativeMessage;
        this.target = target;
    }

    private static HaikuDropTargetContextPeer createDropTargetContextPeer(
            long nativeMessage, int x, int y, Component target) {
        HaikuDropTargetContextPeer peer = new HaikuDropTargetContextPeer(
            nativeMessage, target);
        // 1. Get actions, formats, etc from native message.
        // 2. Post enter event.
        // 3. Return peer.
        
        return peer;
    }

    private void handleMotion(int x, int y) {
    	// 1. Get (cache?) actions, formats, etc from native message.
    	// 2. Post motion event.
    }

    private void handleExit() {
        handleExitMessage(target, nativeMessage);
    }

    protected Object getNativeData(long format) {
        // 1. Get data from native message
        return null;
    }

    protected void doDropDone(boolean success, int dropAction, boolean isLocal) {
    	// TODO need to dispose in other places too
        nativeDisposeMessage(nativeMessage);
    }
}
