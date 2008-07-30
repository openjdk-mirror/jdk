/*
 * Copyright 1997-2007 Sun Microsystems, Inc.  All Rights Reserved.
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

package sun.java2d;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import sun.awt.AppContext;
import sun.awt.DisplayChangedListener;
import sun.awt.FontConfiguration;
import sun.awt.SunDisplayChanger;
import sun.font.CompositeFontDescriptor;
import sun.font.Font2D;
import sun.font.FontManager;
import sun.font.NativeFont;

/**
 * This is an implementation of a GraphicsEnvironment object for the
 * default local GraphicsEnvironment.
 *
 * @see GraphicsDevice
 * @see GraphicsConfiguration
 */

public abstract class SunGraphicsEnvironment extends GraphicsEnvironment
    implements DisplayChangedListener {

    public static boolean isLinux;
    public static boolean isSolaris;
    public static boolean isWindows;
    public static boolean noType1Font;
    private static Font defaultFont;
    public static boolean debugFonts = false;
    protected static Logger logger = null;
    public static String jreLibDirName;
    public static String jreFontDirName;

    /* loadedAllFontFiles is set to true when all fonts on the font path are
     * actually opened, validated and registered. This always implies
     * discoveredAllFonts is true.
     */
    private boolean loadedAllFontFiles = false;

    public static String eudcFontFileName; /* Initialised only on windows */

    static {
        java.security.AccessController.doPrivileged(
                                    new java.security.PrivilegedAction() {
            public Object run() {

                jreLibDirName = System.getProperty("java.home","") +
                    File.separator + "lib";
                jreFontDirName = jreLibDirName + File.separator + "fonts";

                String debugLevel =
                    System.getProperty("sun.java2d.debugfonts");

                if (debugLevel != null && !debugLevel.equals("false")) {
                    debugFonts = true;
                    logger = Logger.getLogger("sun.java2d");
                    if (debugLevel.equals("warning")) {
                        logger.setLevel(Level.WARNING);
                    } else if (debugLevel.equals("severe")) {
                        logger.setLevel(Level.SEVERE);
                    }
                }
                return null;
            }
        });
    };

    public SunGraphicsEnvironment() {
        java.security.AccessController.doPrivileged(
                                    new java.security.PrivilegedAction() {
            public Object run() {
                String osName = System.getProperty("os.name");
                if ("Linux".equals(osName)) {
                    isLinux = true;
                } else if ("SunOS".equals(osName)) {
                    isSolaris = true;
                } else if ("Windows".equals(osName)) {
                    isWindows = true;
                }

                noType1Font = "true".
                    equals(System.getProperty("sun.java2d.noType1Font"));


                /* Register the JRE fonts so that the native platform can
                 * access them. This is used only on Windows so that when
                 * printing the printer driver can access the fonts.
                 */
                registerJREFontsWithPlatform(jreFontDirName);

                getPlatformFontPathFromFontConfig();


                /* Establish the default font to be used by SG2D etc */
                defaultFont = new Font(Font.DIALOG, Font.PLAIN, 12);

                return null;
            }
        });
    }

    protected GraphicsDevice[] screens;

    /**
     * Returns an array of all of the screen devices.
     */
    public synchronized GraphicsDevice[] getScreenDevices() {
        GraphicsDevice[] ret = screens;
        if (ret == null) {
            int num = getNumScreens();
            ret = new GraphicsDevice[num];
            for (int i = 0; i < num; i++) {
                ret[i] = makeScreenDevice(i);
            }
            screens = ret;
        }
        return ret;
    }

    protected abstract int getNumScreens();
    protected abstract GraphicsDevice makeScreenDevice(int screennum);

    /**
     * Returns the default screen graphics device.
     */
    public GraphicsDevice getDefaultScreenDevice() {
        return getScreenDevices()[0];
    }

    /**
     * Returns a Graphics2D object for rendering into the
     * given BufferedImage.
     * @throws NullPointerException if BufferedImage argument is null
     */
    public Graphics2D createGraphics(BufferedImage img) {
        if (img == null) {
            throw new NullPointerException("BufferedImage cannot be null");
        }
        SurfaceData sd = SurfaceData.getPrimarySurfaceData(img);
        return new SunGraphics2D(sd, Color.white, Color.black, defaultFont);
    }

     /**
     * Returns all fonts available in this environment.
     */
    public Font[] getAllFonts() {
        FontManager fm = FontManager.getInstance();
        Font[] installedFonts = fm.getAllInstalledFonts();
        Font[] created = fm.getCreatedFonts();
        if (created == null || created.length == 0) {
            return installedFonts;
        } else {
            int newlen = installedFonts.length + created.length;
            Font [] fonts = java.util.Arrays.copyOf(installedFonts, newlen);
            System.arraycopy(created, 0, fonts,
                             installedFonts.length, created.length);
            return fonts;
        }
    }

    /**
     * Default locale can be changed but we need to know the initial locale
     * as that is what is used by native code. Changing Java default locale
     * doesn't affect that.
     * Returns the locale in use when using native code to communicate
     * with platform APIs. On windows this is known as the "system" locale,
     * and it is usually the same as the platform locale, but not always,
     * so this method also checks an implementation property used only
     * on windows and uses that if set.
     */
    private static Locale systemLocale = null;
    public static Locale getSystemStartupLocale() {
        if (systemLocale == null) {
            systemLocale = (Locale)
                java.security.AccessController.doPrivileged(
                                    new java.security.PrivilegedAction() {
            public Object run() {
                /* On windows the system locale may be different than the
                 * user locale. This is an unsupported configuration, but
                 * in that case we want to return a dummy locale that will
                 * never cause a match in the usage of this API. This is
                 * important because Windows documents that the family
                 * names of fonts are enumerated using the language of
                 * the system locale. BY returning a dummy locale in that
                 * case we do not use the platform API which would not
                 * return us the names we want.
                 */
                String fileEncoding = System.getProperty("file.encoding", "");
                String sysEncoding = System.getProperty("sun.jnu.encoding");
                if (sysEncoding != null && !sysEncoding.equals(fileEncoding)) {
                    return Locale.ROOT;
                }

                String language = System.getProperty("user.language", "en");
                String country  = System.getProperty("user.country","");
                String variant  = System.getProperty("user.variant","");
                return new Locale(language, country, variant);
            }
        });
        }
        return systemLocale;
    }

    public String[] getAvailableFontFamilyNames(Locale requestedLocale) {
        FontManager fm = FontManager.getInstance();
        String[] installed = fm.getInstalledFontFamilyNames(requestedLocale);
        /* Use a new TreeMap as used in getInstalledFontFamilyNames
         * and insert all the keys in lower case, so that the sort order
         * is the same as the installed families. This preserves historical
         * behaviour and inserts new families in the right place.
         * It would have been marginally more efficient to directly obtain
         * the tree map and just insert new entries, but not so much as
         * to justify the extra internal interface.
         */
        TreeMap<String, String> map = fm.getCreatedFontFamilyNames();
        if (map == null || map.size() == 0) {
            return installed;
        } else {
            for (int i=0; i<installed.length; i++) {
                map.put(installed[i].toLowerCase(requestedLocale),
                        installed[i]);
            }
            String[] retval =  new String[map.size()];
            Object [] keyNames = map.keySet().toArray();
            for (int i=0; i < keyNames.length; i++) {
                retval[i] = (String)map.get(keyNames[i]);
            }
            return retval;
        }
    }

    public String[] getAvailableFontFamilyNames() {
        return getAvailableFontFamilyNames(Locale.getDefault());
    }

    public static class TTFilter implements FilenameFilter {
        public boolean accept(File dir,String name) {
            /* all conveniently have the same suffix length */
            int offset = name.length()-4;
            if (offset <= 0) { /* must be at least A.ttf */
                return false;
            } else {
                return(name.startsWith(".ttf", offset) ||
                       name.startsWith(".TTF", offset) ||
                       name.startsWith(".ttc", offset) ||
                       name.startsWith(".TTC", offset));
            }
        }
    }

    public static class T1Filter implements FilenameFilter {
        public boolean accept(File dir,String name) {
            if (noType1Font) {
                return false;
            }
            /* all conveniently have the same suffix length */
            int offset = name.length()-4;
            if (offset <= 0) { /* must be at least A.pfa */
                return false;
            } else {
                return(name.startsWith(".pfa", offset) ||
                       name.startsWith(".pfb", offset) ||
                       name.startsWith(".PFA", offset) ||
                       name.startsWith(".PFB", offset));
            }
        }
    }

     public static class TTorT1Filter implements FilenameFilter {
        public boolean accept(File dir, String name) {

            /* all conveniently have the same suffix length */
            int offset = name.length()-4;
            if (offset <= 0) { /* must be at least A.ttf or A.pfa */
                return false;
            } else {
                boolean isTT =
                    name.startsWith(".ttf", offset) ||
                    name.startsWith(".TTF", offset) ||
                    name.startsWith(".ttc", offset) ||
                    name.startsWith(".TTC", offset);
                if (isTT) {
                    return true;
                } else if (noType1Font) {
                    return false;
                } else {
                    return(name.startsWith(".pfa", offset) ||
                           name.startsWith(".pfb", offset) ||
                           name.startsWith(".PFA", offset) ||
                           name.startsWith(".PFB", offset));
                }
            }
        }
    }

    /* The majority of the register functions in this class are
     * registering platform fonts in the JRE's font maps.
     * The next one is opposite in function as it registers the JRE
     * fonts as platform fonts. If subsequent to calling this
     * your implementation enumerates platform fonts in a way that
     * would return these fonts too you may get duplicates.
     * This function is primarily used to install the JRE fonts
     * so that the native platform can access them.
     * It is intended to be overridden by platform subclasses
     * Currently minimal use is made of this as generally
     * Java 2D doesn't need the platform to be able to
     * use its fonts and platforms which already have matching
     * fonts registered (possibly even from other different JRE
     * versions) generally can't be guaranteed to use the
     * one registered by this JRE version in response to
     * requests from this JRE.
     */
    protected void registerJREFontsWithPlatform(String pathName) {
        return;
    }

    /* Called from FontManager - has Solaris specific implementation */
    /**
     * Determines whether the given font is a logical font.
     */
    public static boolean isLogicalFont(Font f) {
        return FontConfiguration.isLogicalFontFamilyName(f.getFamily());
    }

    /**
     * Return the bounds of a GraphicsDevice, less its screen insets.
     * See also java.awt.GraphicsEnvironment.getUsableBounds();
     */
    public static Rectangle getUsableBounds(GraphicsDevice gd) {
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        Rectangle usableBounds = gc.getBounds();

        usableBounds.x += insets.left;
        usableBounds.y += insets.top;
        usableBounds.width -= (insets.left + insets.right);
        usableBounds.height -= (insets.top + insets.bottom);

        return usableBounds;
    }

    /**
     * This method is provided for internal and exclusive use by Swing.
     * This method should no longer be called, instead directly call
     * FontManager.fontSupportsDefaultEncoding(Font).
     * This method will be removed once Swing is updated to no longer
     * call it.
     */
    public static boolean fontSupportsDefaultEncoding(Font font) {
        return FontManager.fontSupportsDefaultEncoding(font);
    }

    public static void useAlternateFontforJALocales() {
        FontManager.useAlternateFontforJALocales();
    }

    /* If (as we do on X11) need to set a platform font path,
     * then the needed path may be specified by the platform
     * specific FontConfiguration class & data file. Such platforms
     * (ie X11) need to override this method to retrieve this information
     * into suitable data structures.
     */
    protected void getPlatformFontPathFromFontConfig() {
    }

    /**
     * From the DisplayChangedListener interface; called
     * when the display mode has been changed.
     */
    public void displayChanged() {
        // notify screens in device array to do display update stuff
        for (GraphicsDevice gd : getScreenDevices()) {
            if (gd instanceof DisplayChangedListener) {
                ((DisplayChangedListener) gd).displayChanged();
            }
        }

        // notify SunDisplayChanger list (e.g. VolatileSurfaceManagers and
        // SurfaceDataProxies) about the display change event
        displayChanger.notifyListeners();
    }

    /**
     * Part of the DisplayChangedListener interface:
     * propagate this event to listeners
     */
    public void paletteChanged() {
        displayChanger.notifyPaletteChanged();
    }

    /**
     * Returns true when the display is local, false for remote displays.
     *
     * @return true when the display is local, false for remote displays
     */
    public abstract boolean isDisplayLocal();

    /*
     * ----DISPLAY CHANGE SUPPORT----
     */

    protected SunDisplayChanger displayChanger = new SunDisplayChanger();

    /**
     * Add a DisplayChangeListener to be notified when the display settings
     * are changed.
     */
    public void addDisplayChangedListener(DisplayChangedListener client) {
        displayChanger.add(client);
    }

    /**
     * Remove a DisplayChangeListener from Win32GraphicsEnvironment
     */
    public void removeDisplayChangedListener(DisplayChangedListener client) {
        displayChanger.remove(client);
    }

    /*
     * ----END DISPLAY CHANGE SUPPORT----
     */
}
