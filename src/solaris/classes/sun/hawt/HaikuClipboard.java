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

import java.awt.datatransfer.*;
import java.io.IOException;
import java.util.*;

import sun.awt.datatransfer.*;

public class HaikuClipboard extends SunClipboard {

    private native String[] nativeGetFormats();
    private native byte[] nativeGetData(String format) throws IOException;
    private native long nativeLockAndClear();
    private native void nativeSetData(long nativeClipboard, byte[] data,
        String format);
    private native void nativeUnlock(long nativeClipboard);

    public HaikuClipboard(String name) {
        super(name);
    }

    @Override
    public long getID() {
        return 0;
    }

    @Override
    protected void clearNativeContext() {
    }

    @Override
    protected void setContentsNative(Transferable contents) {

        // Don't use delayed Clipboard rendering for the Transferable's data.
        // If we did that, we would call Transferable.getTransferData on
        // the Toolkit thread, which is a security hole.
        //
        // Get all of the target formats into which the Transferable can be
        // translated. Then, for each format, translate the data and post
        // it to the Clipboard.
        HaikuDataTransferer transferer = (HaikuDataTransferer)
            DataTransferer.getInstance();
        Map<Long, DataFlavor> formatMap = transferer.
            getFormatsForTransferable(contents, flavorMap);
        
        if (formatMap.keySet().size() > 0) {
        	long nativeClipboard = nativeLockAndClear();
            for (long format : formatMap.keySet()) {
                DataFlavor flavor = formatMap.get(format);

                try {
                    byte[] bytes = transferer.
                        translateTransferable(contents, flavor, format);
                    nativeSetData(nativeClipboard, bytes,
                        transferer.getNativeForFormat(format));
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
            nativeUnlock(nativeClipboard);
        }
    }

    @Override
    protected long[] getClipboardFormats() {
        String[] nativeFormats = nativeGetFormats();
        if (nativeFormats == null)
            nativeFormats = new String[0];

        long[] temporary = new long[nativeFormats.length];
        HaikuDataTransferer transferer =
            (HaikuDataTransferer)DataTransferer.getInstance();
        int length = 0;
        for (String nativeFormat : nativeFormats)
            temporary[length++] = transferer.getFormatForNativeAsLong(
                nativeFormat);

        long[] formats = new long[length];
        System.arraycopy(temporary, 0, formats, 0, length);
        return formats;
    }

    @Override
    protected byte[] getClipboardData(long format) throws IOException {
        HaikuDataTransferer transferer = (HaikuDataTransferer)
            DataTransferer.getInstance();
        String nativeFormat = transferer.getNativeForFormat(format);
        return nativeGetData(nativeFormat);
    }

    @Override
    protected void registerClipboardViewerChecked() {
        // todo
    }

    @Override
    protected void unregisterClipboardViewerChecked() {
        // todo
    }
}
