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
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.peer.*;
import java.io.IOException;
import java.util.*;

import sun.awt.datatransfer.*;
import sun.awt.dnd.*;
import sun.lwawt.*;

public final class HaikuDragSourceContextPeer extends SunDragSourceContextPeer {

    private static final HaikuDragSourceContextPeer instance =
        new HaikuDragSourceContextPeer(null);

    private native boolean nativeStartDrag(long nativeWindow, String[] mimeTypes,
        byte[][] data);

    private HaikuDragSourceContextPeer(DragGestureEvent dge) {
        super(dge);
    }

    public static HaikuDragSourceContextPeer createDragSourceContextPeer(
            DragGestureEvent dge) throws InvalidDnDOperationException {
        // TODO consider the case in which a new drag operation is started
        // while another is ongoing. Where does the drag end event get
        // delivered and what effect does it have?
        instance.setTrigger(dge);
        return instance;
    }

    protected void startDrag(Transferable transferable, long[] formats,
            Map formatMap) {
        DragGestureEvent trigger = getTrigger();

        Component component = trigger.getComponent();
        ComponentPeer peer = component.getPeer();

        // For a lightweight component traverse up the hierarchy to the
        // first heavyweight.
        if (component.isLightweight()) {
            for (Component parent = component.getParent(); parent != null;
                    parent = parent.getParent()) {
                if (parent.isLightweight() == false) {
                    peer = parent.getPeer();
                    break;
                }
            }
        }

        if (!(peer instanceof LWComponentPeer)) {
            throw new IllegalArgumentException(
                "DragSource's peer must be a LWComponentPeer.");
        }

        LWComponentPeer lwPeer = (LWComponentPeer)peer;
        HaikuPlatformWindow platformWindow = (HaikuPlatformWindow)
            lwPeer.getPlatformWindow();
        long nativeWindow = platformWindow.getLayerPtr();

        ArrayList<String> mimeTypes = new ArrayList<String>();
        ArrayList<byte[]> mimeData = new ArrayList<byte[]>();

        HaikuDataTransferer transferer = (HaikuDataTransferer)
            DataTransferer.getInstance();
        Map<Long, DataFlavor> map = (Map<Long, DataFlavor>)formatMap;

        if (map.keySet().size() > 0) {
            for (long format : map.keySet()) {
                DataFlavor flavor = map.get(format);

                try {
                    byte[] bytes = transferer.
                        translateTransferable(transferable, flavor, format);
                    mimeTypes.add(transferer.getNativeForFormat(format));
                    mimeData.add(bytes);
                } catch (IOException e) {
                    // Fix 4696186: don't print exception if data with
                    // javaJVMLocalObjectMimeType failed to serialize.
                    // May remove this if-check when 5078787 is fixed.
                    if (!(flavor.isMimeTypeEqual(
                            DataFlavor.javaJVMLocalObjectMimeType) &&
                            e instanceof java.io.NotSerializableException)) {
                        e.printStackTrace();
                    }
                }
            }
        }

        String[] typeArray = mimeTypes.toArray(new String[mimeTypes.size()]);
        byte[][] dataArray = mimeData.toArray(new byte[mimeData.size()][]);

        boolean result = nativeStartDrag(nativeWindow, typeArray, dataArray);
        SunDropTargetContextPeer.setCurrentJVMLocalSourceTransferable(transferable);

        // We have no real way of knowing when the drag is done, so we
        // it's done as soon as we start...
        dragDropFinished(true, DnDConstants.ACTION_COPY, 0, 0);
    }

    protected void setNativeCursor(long nativeContext, Cursor cursor,
            int cursorType) {
        // We can't reset the cursor after the drag is done... so there's
        // no point in doing this.
    }
}
