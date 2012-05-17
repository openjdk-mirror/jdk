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

import sun.awt.FontConfiguration;
import sun.font.FcFontConfiguration;
import sun.font.SunFontManager;

public class HaikuFontManager extends SunFontManager {

	private String[] defaultPlatformFont;
	private String fontPath;

	private native String nativeGetFontPath();

    @Override
    protected FontConfiguration createFontConfiguration() {
        FontConfiguration fontConfig = new HaikuFontConfiguration(this);
        fontConfig.init();
        return fontConfig;
    }

    @Override
    public FontConfiguration createFontConfiguration(boolean preferLocaleFonts,
			boolean preferPropFonts)
    {
        return new HaikuFontConfiguration(this, preferLocaleFonts,
        	preferPropFonts);
    }

    @Override
    public synchronized String[] getDefaultPlatformFont() {
        if (defaultPlatformFont == null) {
            defaultPlatformFont = new String[2];
            defaultPlatformFont[0] = "DejaVu Sans";
			defaultPlatformFont[1] =
				"/boot/system/data/fonts/ttfonts/DejaVuSans.ttf";
        }
        return defaultPlatformFont;
    }

    @Override
    public String getFontPath(boolean noType1Fonts) {
        if (fontPath == null)
        	fontPath = nativeGetFontPath();
        return fontPath;
    }
}
