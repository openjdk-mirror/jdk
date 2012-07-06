/*
 * Copyright (c) 2003, 2008, Oracle and/or its affiliates. All rights reserved.
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

package sun.print;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import sun.print.IPPPrintService;
import sun.print.CustomMediaSizeName;
import sun.print.CustomMediaTray;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaTray;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.Attribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.standard.PrinterName;


public class CUPSPrinter  {
    private static final String debugPrefix = "CUPSPrinter>> ";
    private static final double PRINTER_DPI = 72.0;
    private boolean initialized;
    private static native String getCupsServer();
    private static native int getCupsPort();
    private static native boolean canConnect(String server, int port);
    private static native boolean initIDs();
    // These functions need to be synchronized as
    // CUPS does not support multi-threading.
    private static synchronized native String[] getMedia(String printer);
    private static synchronized native float[] getPageSizes(String printer);
    //public static boolean useIPPMedia = false; will be used later

    private MediaPrintableArea[] cupsMediaPrintables;
    private MediaSizeName[] cupsMediaSNames;
    private CustomMediaSizeName[] cupsCustomMediaSNames;
    private MediaTray[] cupsMediaTrays;

    public  int nPageSizes = 0;
    public  int nTrays = 0;
    private  String[] media;
    private  float[] pageSizes;
    private String printer;

    private static boolean libFound;
    private static String cupsServer = null;
    private static int cupsPort = 0;

    static {
        libFound = false;
    }


    CUPSPrinter (String printerName) {
        if (printerName == null) {
            throw new IllegalArgumentException("null printer name");
        }
        printer = printerName;
        cupsMediaSNames = null;
        cupsMediaPrintables = null;
        cupsMediaTrays = null;
        initialized = false;
    }


    /**
     * Returns array of MediaSizeNames derived from PPD.
     */
    public MediaSizeName[] getMediaSizeNames() {
        initMedia();
        return cupsMediaSNames;
    }


    /**
     * Returns array of Custom MediaSizeNames derived from PPD.
     */
    public CustomMediaSizeName[] getCustomMediaSizeNames() {
        initMedia();
        return cupsCustomMediaSNames;
    }


    /**
     * Returns array of MediaPrintableArea derived from PPD.
     */
    public MediaPrintableArea[] getMediaPrintableArea() {
        initMedia();
        return cupsMediaPrintables;
    }

    /**
     * Returns array of MediaTrays derived from PPD.
     */
    public MediaTray[] getMediaTrays() {
        initMedia();
        return cupsMediaTrays;
    }


    /**
     * Initialize media by translating PPD info to PrintService attributes.
     */
    private synchronized void initMedia() {
            return;
    }

    /**
     * Get CUPS default printer using IPP.
     */
    public static String getDefaultPrinter() {
        return null;
    }


    /**
     * Get list of all CUPS printers using IPP.
     */
    public static String[] getAllPrinters() {
        return null;
    }

    /**
     * Returns CUPS server name.
     */
    public static String getServer() {
        return cupsServer;
    }

    /**
     * Returns CUPS port number.
     */
    public static int getPort() {
        return cupsPort;
    }

    /**
     * Detects if CUPS is running.
     */
    public static boolean isCupsRunning() {
            return false;
    }


}
