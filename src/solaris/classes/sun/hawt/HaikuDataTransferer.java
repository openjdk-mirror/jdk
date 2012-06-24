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
import java.awt.image.*;
import sun.awt.image.ImageRepresentation;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.*;

import java.awt.datatransfer.*;
import sun.awt.datatransfer.*;

public class HaikuDataTransferer extends DataTransferer {

    // This is how we generate our unique longs for natives.  
    private static long index = 0;

    private static final Map<Long, String> formatToNative;
    private static final Map<String, Long> nativeToFormat;

    private static HaikuDataTransferer transferer;
    private ToolkitThreadBlockedHandler handler;

    public final long FORMAT_INVALID   = addNative("");
    public final long FORMAT_STRING    = addNative("text/plain");
    public final long FORMAT_URI_LIST  = addNative("text/uri-list");
    public final long FORMAT_JPEG      = addNative("image/jpeg");
    public final long FORMAT_PNG       = addNative("image/png");
    public final long FORMAT_TIFF      = addNative("image/tiff");

    // We use this invalid mime-type to denote files received
    // in a "refs" field, as received from tracker or similar.
    // For transfering to and from native we use a UTF-8 string
    // of filenames separated by colons.
    public final long FORMAT_FILE_LIST = addNative("refs");

    static {
        Map<Long, String> formatMap = new HashMap<>();
        Map<String, Long> nativeMap = new HashMap<>();
        
        formatToNative = Collections.synchronizedMap(formatMap);
        nativeToFormat = Collections.synchronizedMap(nativeMap);
    }

    private HaikuDataTransferer() {
    }

    public static synchronized HaikuDataTransferer getInstanceImpl() {
        if (transferer == null)
            transferer = new HaikuDataTransferer();
        return transferer;
    }

    private Long addNative(String str) {
        Long format = nativeToFormat.get(str);
        if (format != null)
            return format;
        
        format = Long.valueOf(index++);
        formatToNative.put(format, str);
        nativeToFormat.put(str, format);
        return format;
    }

    @Override
    public String getDefaultUnicodeEncoding() {
        return "UTF-8";
    }

    @Override
    public boolean isLocaleDependentTextFormat(long format) {
        // TODO: find out what this actually means
        return format == FORMAT_STRING;
    }

    @Override
    public boolean isFileFormat(long format) {
        return format == FORMAT_FILE_LIST;
    }

    @Override
    public boolean isImageFormat(long format) {
        return format == FORMAT_JPEG || format == FORMAT_PNG ||
            format == FORMAT_TIFF;
    }

    @Override
    protected boolean isURIListFormat(long format) {
        String nat = getNativeForFormat(format);
        if (nat == null) {
            return false;
        }
        try {
            DataFlavor df = new DataFlavor(nat);
            if (df.getPrimaryType().equals("text") && df.getSubType().equals("uri-list")) {
                return true;
            }
        } catch (Exception e) {
            // Not a MIME format.
        }
        return false;
    }

    @Override
    synchronized protected Long getFormatForNativeAsLong(String str) {
        return addNative(str);
    }

    @Override
    protected String getNativeForFormat(long format) {
        return formatToNative.get(format);
    }

    @Override
    public ToolkitThreadBlockedHandler getToolkitThreadBlockedHandler() {
        if (handler == null) {
            handler = new HaikuToolkitThreadBlockedHandler();
        }
        return handler;
    }

    @Override
    protected byte[] imageToPlatformBytes(Image image, long format)
            throws IOException {
        String mimeType = null;
        if (format == FORMAT_PNG) {
            mimeType = "image/png";
        } else if (format == FORMAT_JPEG) {
            mimeType = "image/jpeg";
        } else {
            // Check if an image MIME format.
            try {
                String nat = getNativeForFormat(format);
                DataFlavor df = new DataFlavor(nat);
                String primaryType = df.getPrimaryType();
                if ("image".equals(primaryType)) {
                    mimeType = df.getPrimaryType() + "/" + df.getSubType();
                }
            } catch (Exception e) {
                // Not an image MIME format.
            }
        }
        if (mimeType != null) {
            return imageToStandardBytes(image, mimeType);
        } else {
            String nativeFormat = getNativeForFormat(format);
            throw new IOException("Translation to " + nativeFormat +
                                  " is not supported.");
        }
    }

    @Override
    protected String[] dragQueryFile(final byte[] bytes) {
        // Bytes is (hopefully) a UTF-8 string of paths separated by colons.
        String paths = new String(bytes, Charset.forName("UTF-8"));
        return paths.split(":");
    }

    /**
     * Translates either a byte array or an input stream which contain
     * platform-specific image data in the given format into an Image.
     */
    @Override
    protected Image platformImageBytesOrStreamToImage(InputStream stream,
            byte[] bytes, long format) throws IOException {
        String mimeType = null;
        if (format == FORMAT_PNG) {
            mimeType = "image/png";
        } else if (format == FORMAT_JPEG) {
            mimeType = "image/jpeg";
        } else {
            // Check if an image MIME format.
            try {
                String nat = getNativeForFormat(format);
                DataFlavor df = new DataFlavor(nat);
                String primaryType = df.getPrimaryType();
                if ("image".equals(primaryType)) {
                    mimeType = df.getPrimaryType() + "/" + df.getSubType();
                }
            } catch (Exception e) {
                // Not an image MIME format.
            }
        }
        if (mimeType != null) {
            return standardImageBytesOrStreamToImage(stream, bytes, mimeType);
        } else {
            String nativeFormat = getNativeForFormat(format);
            throw new IOException("Translation from " + nativeFormat +
                                  " is not supported.");
        }
    }

    @Override
    protected ByteArrayOutputStream convertFileListToBytes(
            ArrayList<String> fileList) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (int i = 0; i < fileList.size(); i++) {
            byte[] bytes = fileList.get(i).getBytes("UTF-8");
            bos.write(bytes, 0, bytes.length);
            // Paths are separated by colons
            bos.write(58);
        }
        return bos;
    }
}

class HaikuToolkitThreadBlockedHandler implements ToolkitThreadBlockedHandler {

    @Override
    public void lock() {
    }

    @Override
    public void unlock() {
    }

    @Override
    public void enter() {
    }

    @Override
    public void exit() {
    }
}
