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
import java.awt.event.*;
import java.awt.image.*;
import java.awt.peer.TrayIconPeer;
import java.awt.TrayIcon.MessageType;

import javax.swing.*;

public class HaikuTrayIcon implements TrayIconPeer {

    private TrayIcon target;
    private PopupMenu popup;

    private Dimension iconSize;
    private HaikuDrawable icon;
    private long nativeTrayIcon;

    private IconObserver observer = new IconObserver();

    private Frame popupParent = new Frame();

    private static final int NO_ICON = 1;
    private static final int INFO_ICON = 2;
    private static final int WARNING_ICON = 3;
    private static final int ERROR_ICON = 4;

    private native long nativeCreate(long nativeDrawable);
    private native void nativeSetToolTip(long nativeTrayIcon, String toolTip);
    private native void nativeDisplayMessage(String caption, String text,
        int messageType);
    private native void nativeUpdate(long nativeTrayIcon);
    private native void nativeDispose(long nativeTrayIcon);

    HaikuTrayIcon(TrayIcon target) {
        this.target = target;
        this.popup = target.getPopupMenu();
        updateImage();

        nativeTrayIcon = nativeCreate(icon.getNativeDrawable());
    }

    @Override
    public void dispose() {
        nativeDispose(nativeTrayIcon);
        icon.dispose();
        HaikuToolkit.targetDisposedPeer(target, this);
        target = null;
    }

    @Override
    public void setToolTip(String tooltip) {
        nativeSetToolTip(nativeTrayIcon, tooltip);
    }

    @Override
    public void showPopupMenu(final int x, final int y) {
        HaikuToolkit.executeOnEventHandlerThread(target, new Runnable() {
            public void run() {
                PopupMenu newPopup = ((TrayIcon)target).getPopupMenu();
                if (popup != newPopup) {
                    if (popup != null) {
                        popupParent.remove(popup);
                    }
                    if (newPopup != null) {
                        popupParent.add(newPopup);
                    }
                    popup = newPopup;
                }
                if (popup != null) {
                    popup.show(popupParent, x, y);
                }
            }
        });
    }

    @Override
    public void updateImage() {
        Image image = target.getImage();
        if (image == null) {
            return;
        }

        if (icon == null) {
        	iconSize = SystemTray.getSystemTray().getTrayIconSize();
        	icon = new HaikuDrawable(iconSize.width, iconSize.height);
        }

        boolean autosize = ((TrayIcon)target).isImageAutoSize();
        int width = autosize ? iconSize.width : image.getWidth(observer);
        int height = autosize ? iconSize.height : image.getHeight(observer);

        Graphics2D graphics = icon.createGraphics();
        graphics.drawImage(image, 0, 0, width, height, Color.WHITE, observer);

        graphics.dispose();
        // Wait for IconObserver?
        //nativeUpdate(nativeTrayIcon);
    }

    class IconObserver implements ImageObserver {
        public boolean imageUpdate(Image image, int flags, int x, int y,
                int width, int height) {
            if (image != ((TrayIcon)target).getImage()) {
                return false;
            }

            if ((flags & (ImageObserver.FRAMEBITS | ImageObserver.ALLBITS |
                    ImageObserver.WIDTH | ImageObserver.HEIGHT)) != 0) {
                nativeUpdate(nativeTrayIcon);
            }
            return (flags & ImageObserver.ALLBITS) == 0;
        }
    }

    @Override
    public void displayMessage(final String caption, final String text,
            final String messageType) {
        int type = messageTypeForName(messageType);
        nativeDisplayMessage(caption, text, type);
    }

    private static int messageTypeForName(String messageType) {
        if (messageType.equals("ERROR")) {
            return ERROR_ICON;
        } else if (messageType.equals("WARNING")) {
            return WARNING_ICON;
        } else if (messageType.equals("INFO")) {
        	return INFO_ICON;
        } else {
            return NO_ICON;
        }
    }
}

