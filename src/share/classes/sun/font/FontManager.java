/*
 * Copyright 2003-2008 Sun Microsystems, Inc.  All Rights Reserved.
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
package sun.font;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.swing.plaf.FontUIResource;

import sun.java2d.SunGraphicsEnvironment;

/**
 * Interface between Java Fonts (java.awt.Font) and the underlying
 * font files/native font resources and the Java and native font scalers.
 */
public interface FontManager {
   
    @Deprecated
    final String osName = System.getProperty("os.name", "unknownOS");
    
    /**
     * Referenced by code in the JDK which wants to test for the
     * minimum char code for which layout may be required.
     * Note that even basic latin text can benefit from ligatures,
     * eg "ffi" but we presently apply those only if explicitly
     * requested with TextAttribute.LIGATURES_ON.
     * The value here indicates the lowest char code for which failing
     * to invoke layout would prevent acceptable rendering.
     */
    public static final int MIN_LAYOUT_CHARCODE = 0x0300;

    /**
     * Referenced by code in the JDK which wants to test for the
     * maximum char code for which layout may be required.
     * Note this does not account for supplementary characters
     * where the caller interprets 'layout' to mean any case where
     * one 'char' (ie the java type char) does not map to one glyph
     */
    public static final int MAX_LAYOUT_CHARCODE = 0x206F;
    
    public static final int FONTFORMAT_NONE = -1;
    public static final int FONTFORMAT_TRUETYPE = 0;
    public static final int FONTFORMAT_TYPE1 = 1;
    public static final int FONTFORMAT_T2K = 2;
    public static final int FONTFORMAT_TTC = 3;
    public static final int FONTFORMAT_COMPOSITE = 4;
    public static final int FONTFORMAT_NATIVE = 5;

    public static final int NO_FALLBACK = 0;
    public static final int PHYSICAL_FALLBACK = 1;
    public static final int LOGICAL_FALLBACK = 2;

    public static final int QUADPATHTYPE = 1;
    public static final int CUBICPATHTYPE = 2;

    //public static final short US_LCID = 0x0409;  // US English - default
    
    /**
     * For backward compatibilty only, this will to be removed soon, don't use
     * in new code.
     */
    @Deprecated
    public static final boolean IS_WINDOWS = osName.startsWith("Windows");
    
    /**
     * For backward compatibilty only, this will to be removed soon, don't use
     * in new code.
     */
    @Deprecated
    public static final boolean IS_LINUX = osName.startsWith("Linux");
    
    /**
     * For backward compatibilty only, this will to be removed soon, don't use
     * in new code.
     */
    @Deprecated
    public static final boolean IS_SOLARIS = osName.startsWith("SunOS");
    
    /**
     * For backward compatibilty only, this will to be removed soon, don't use
     * in new code.
     */
    @Deprecated
    public static final boolean IS_SOLARIS_8 =
        (IS_SOLARIS ? System.getProperty("os.version", "unk").startsWith("5.8")
                    : false);
    
    /**
     * For backward compatibilty only, this will to be removed soon, don't use
     * in new code.
     */
    @Deprecated
    public static final boolean USE_T2K =
        System.getProperty("sun.java2d.font.scaler", "no").equals("t2k");
    
    /**
     * For backward compatibilty only, this will to be removed soon, don't use
     * in new code.
     */
    @Deprecated
    public static final boolean IS_SOLARIS_9 =
        (IS_SOLARIS ? System.getProperty("os.version", "hanky").startsWith("5.8")
                    : false);
    
    /**
     * For backward compatibilty only, this will to be removed soon, don't use
     * in new code.
     */
    // Note that this flag always return true and is not computed
    // like the others. This makes sense of OpenJDK, but Sun internal code
    // may break.
    @Deprecated
    public static final boolean IS_OPENJDK = true;
    
    /**
     * Return true if Font Debugging is enable. 
     */
    public boolean debugFonts();
    
    /**
     * Get a list of installed fonts in the requested {@link Locale}.
     * The list contains the fonts Family Names.
     * If Locale is null, the default locale is used.
     * 
     * @param requestedLocale, if null the default locale is used.
     * @return list of installed fonts in the system.
     */
    public String[] getInstalledFontFamilyNames(Locale requestedLocale);

    /**
     * Returns all fonts installed in this environment.
     */
    public Font[] getAllInstalledFonts();

    /*
     * This is the Physical font used when some other font on the system
     * can't be located. There has to be at least one font or the font
     * system is not useful and the graphics environment cannot sustain
     * the Java platform.
     */
    public PhysicalFont getDefaultPhysicalFont();

    /**
     * Register a new font. Please, note that {@code null} is not a valid
     * argument, and it's caller's responsibility to ensure that, but to keep
     * compatibility, if {@code null} is passed as an argument, {@code false}
     * is returned, and no {@link NullPointerException} 
     * is thrown.
     * 
     * As additional note, an implementation should ensure that this font
     * cannot override existing installed fonts.
     *
     * @param font
     * @return {@code true} is the font is successfully registered,
     * {@code false} otherwise.
     */
    public boolean registerFont(Font font);

    /**
     * Register fall back fonts.
     */
    public void registerFontsInDir(String dirName);

    /**
     * Return the file name of the Font associated to this fontName.
     */
    public String getFileNameForFontName(String fontName);

    /**
     * Return a list of registered fonts.
     * @return a list of registered fonts.
     */
    public Font2D[] getRegisteredFonts();

    /**
     * The client supplies a name and a style.
     * The name could be a family name, or a full name.
     * A font may exist with the specified style, or it may
     * exist only in some other style. For non-native fonts the scaler
     * may be able to emulate the required style.
     */
    public Font2D findFont2D(String name, int style, int fallback);

    /**
     * This is called by Swing passing in a fontconfig family name
     * such as "sans". In return Swing gets a FontUIResource instance
     * that has queried fontconfig to resolve the font(s) used for this.
     * Fontconfig will if asked return a list of fonts to give the largest
     * possible code point coverage.
     */
    // TODO: move out of the interface
    public FontUIResource getFontConfigFUIR(String fcFamily, int style, int size);

    /**
     * This method is provided for internal and exclusive use by Swing.
     *
     * It may be used in conjunction with fontSupportsDefaultEncoding(Font)
     * In the event that a desktop properties font doesn't directly
     * support the default encoding, (ie because the host OS supports
     * adding support for the current locale automatically for native apps),
     * then Swing calls this method to get a font which  uses the specified
     * font for the code points it covers, but also supports this locale
     * just as the standard composite fonts do.
     * Note: this will over-ride any setting where an application
     * specifies it prefers locale specific composite fonts.
     * The logic for this, is that this method is used only where the user or
     * application has specified that the native L&F be used, and that
     * we should honour that request to use the same font as native apps use.
     *
     * The behaviour of this method is to construct a new composite
     * Font object that uses the specified physical font as its first
     * component, and adds all the components of "dialog" as fall back
     * components.
     * The method currently assumes that only the size and style attributes
     * are set on the specified font. It doesn't copy the font transform or
     * other attributes because they aren't set on a font created from
     * the desktop. This will need to be fixed if use is broadened.
     *
     * Operations such as Font.deriveFont will work properly on the
     * font returned by this method for deriving a different point size.
     * Additionally it tries to support a different style by calling
     * getNewComposite() below. That also supports replacing slot zero
     * with a different physical font but that is expected to be "rare".
     * Deriving with a different style is needed because its been shown
     * that some applications try to do this for Swing FontUIResources.
     * Also operations such as new Font(font.getFontName(..), Font.PLAIN, 14);
     * will NOT yield the same result, as the new underlying CompositeFont
     * cannot be "looked up" in the font registry.
     * This returns a FontUIResource as that is the Font sub-class needed
     * by Swing.
     * Suggested usage is something like :
     * FontUIResource fuir;
     * Font desktopFont = getDesktopFont(..);
     * // NOTE even if fontSupportsDefaultEncoding returns true because
     * // you get Tahoma and are running in an English locale, you may
     * // still want to just call getCompositeFontUIResource() anyway
     * // as only then will you get fallback fonts - eg for CJK.
     * if (FontManager.fontSupportsDefaultEncoding(desktopFont)) {
     *   fuir = new FontUIResource(..);
     * } else {
     *   fuir = FontManager.getCompositeFontUIResource(desktopFont);
     * }
     * return fuir;
     */
    // TODO: move out of the interface
    public FontUIResource getCompositeFontUIResource(Font font);
    
    /**
     * This method is provided for internal and exclusive use by Swing.
     * Return true if the underlying font is a TrueType or OpenType font
     * is a composite font.
     * This ensures that Swing components will always benefit from the
     * fall back fonts.
     * 
     * @param font representing a physical font.
     * @return true if the underlying font is a composite font.
     * that claims to
     */
    // TODO: move out of the interface
    public boolean fontSupportsDefaultEncoding(Font font);
    
    /**
     * This method doesn't check if alternates are selected in this app
     * context. Its used by the FontMetrics caching code which in such
     * a case cannot retrieve a cached metrics solely on the basis of
     * the Font.equals() method since it needs to also check if the Font2D
     * is the same.
     */
    public boolean maybeUsingAlternateCompositeFonts();
    
    /**
     * If usingPerAppContextComposites is true, we are in "applet"
     * (eg browser) enviroment and at least one context has selected
     * an alternate composite font behaviour.
     */
    public boolean usingPerAppContextComposites();
    
    /**
     * TODO
     * @param family
     * @param style
     * @param handle
     * @return
     */
    public Font2DHandle getNewComposite(String family, int style,
                                        Font2DHandle handle);
    /**
     * If there is anything in the text which triggers a case
     * where char->glyph does not map 1:1 in straightforward
     * left->right ordering, then this method returns true.
     * Scripts which might require it but are not treated as such
     * due to JDK implementations will not return true.
     * ie a 'true' return is an indication of the treatment by
     * the implementation.
     * Whether supplementary characters should be considered is dependent
     * on the needs of the caller. Since this method accepts the 'char' type
     * then such chars are always represented by a pair. From a rendering
     * perspective these will all (in the cases I know of) still be one
     * unicode character -> one glyph. But if a caller is using this to
     * discover any case where it cannot make naive assumptions about
     * the number of chars, and how to index through them, then it may
     * need the option to have a 'true' return in such a case.
     */
    public boolean isComplexText(char [] chs, int start, int limit);
    
    /**
     * Indicates a preference for locale-specific fonts in the mapping of
     * logical fonts to physical fonts. Calling this method indicates that font
     * rendering should primarily use fonts specific to the primary writing
     * system (the one indicated by the default encoding and the initial
     * default locale). For example, if the primary writing system is
     * Japanese, then characters should be rendered using a Japanese font
     * if possible, and other fonts should only be used for characters for
     * which the Japanese font doesn't have glyphs.
     * <p>
     * The actual change in font rendering behavior resulting from a call
     * to this method is implementation dependent; it may have no effect at
     * all, or the requested behavior may already match the default behavior.
     * The behavior may differ between font rendering in lightweight
     * and peered components.  Since calling this method requests a
     * different font, clients should expect different metrics, and may need
     * to recalculate window sizes and layout. Therefore this method should
     * be called before user interface initialisation.
     * 
     * @see #preferProportionalFonts()
     * @since 1.5
     */
    public void preferLocaleFonts();
    
    /**
     * preferLocaleFonts() and preferProportionalFonts() are called to inform
     * that the application could be using an alternate set of composite
     * fonts, and so the implementation should try to create a CompositeFonts
     * with this directive in mind.
     * 
     * @see #preferLocaleFonts()
     */
    public void preferProportionalFonts();
    
    /**
     * TODO
     * @param font
     * @return
     */
    public Font2D getFont2D(Font font);
    
    /**
     * TODO
     * @param fontFile
     * @param fontFormat
     * @param isCopy
     * @return
     */
    public Font2D createFont2D(File fontFile, int fontFormat,
                               boolean isCopy) throws FontFormatException;
    
    /**
     * TODO
     */
    public PhysicalFont registerFontFile(String fileName,
                                         String[] nativeNames,
                                         int fontFormat,
                                         boolean useJavaRasterizer,
                                         int fontRank);
    
    /**
     * Return true if this font was already registered.
     */
    public boolean isRegisteredFontFile(String fileName);
    
    /**
     * Return the PhysicalFont associated with the given fileName.
     */
    public PhysicalFont getRegisteredFontFile(String fileName);
    
    /**
     * TODO
     * @param fileNameKey
     * @return
     */
    public PhysicalFont initialiseDeferredFont(String fileName);
    
    /**
     * TODO
     */
    public boolean isDeferredFont(String fileName);
    
    /**
     * TODO
     * FIXME: this should not be here. It's used by FontConfigManager
     * @param name
     * @param style
     * @return
     */
    public PhysicalFont findJREDeferredFont(String name, int style);
    
    /**
     * TODO
     * FIXME: should that be moved elsewhere?
     * Original javadoc: Workaround for apps which are dependent on a font
     * metrics bug in JDK 1.1. This is an unsupported win32 private setting.
     */
    public boolean usePlatformFontMetrics();

    /**
     * Return true if logging is enabled.
     * You can then use {@link #getLogger()} to get a valid logger object. 
     */
    public boolean isLogging();
    
    /**
     * Return a valid logger for FontManager or null if logger is not enabled.
     */
    public Logger getLogger();

    /**
     * TODO
     * @return
     */
    public FilenameFilter getTrueTypeFilter();

    /**
     * TODO
     * @return
     */
    public FilenameFilter getType1Filter();
    
    /**
     * Return true if the application should use alternative fonts from Japanese
     * locales. 
     * @return
     */
    public boolean usingAlternateFontforJALocales();
    
    /**
     * Modifies the behaviour of a subsequent call to preferLocaleFonts()
     * to use Mincho instead of Gothic for dialoginput in JA locales
     * on windows. Not needed on other platforms. Used by SunGraphicEnvironment.
     * @see SunGraphicsEnvironment
     */
    public void useAlternateFontforJALocales();
    
    /**
     * Return the End-User-Define-Character used or null is no one is defined.
     * This is usually useful for non Western Language implementation.
     */
    public TrueTypeFont getEUDCFont();
    
    /**
     * TODO.
     */
    public boolean isComplexCharCode(int code);
    
    /**
     * TODO.
     * @param ch
     * @return
     */
    public boolean isNonSimpleChar(char ch);
    
    /**
     * TODO
     * @return
     */
    public FontScaler getNullScaler();
   
    public FontScaler getScaler(Font2D font, int indexInCollection,
                                boolean supportsCJK, int filesize);
    
    /**
     * This is called when font is determined to be invalid/bad.
     * It designed to be called (for example) by the font scaler
     * when in processing a font file it is discovered to be incorrect.
     * This is different than the case where fonts are discovered to
     * be incorrect during initial verification, as such fonts are
     * never registered.
     * Handles to this font held are re-directed to a default font.
     * This default may not be an ideal substitute buts it better than
     * crashing This code assumes a PhysicalFont parameter as it doesn't
     * make sense for a Composite to be "bad".
     */;
   public void deRegisterBadFont(Font2D font2D);
   
   /**
    * Return an array of created Fonts, or null, if no fonts were created yet.
    */
   public Font[] getCreatedFonts();
   
   /**
    * Similar to getCreatedFonts, but returns a TreeMap of fonts by family name.
    */
   public TreeMap<String, String> getCreatedFontFamilyNames();
   
   /**
    * Returns face name for default font, or null if
    * no face names are used for CompositeFontDescriptors
    * for this platform.
    */
   public String getDefaultFontFaceName();
   
   /**
    * Returns file name for default font, either absolute
    * or relative as needed by registerFontFile.
    */
   public String getDefaultFontFile();
   
   public FontConfigManager getFontConfigManager();
   
   /**
    * 
    * @param font
    */
   public void addToPool(FileFont font);

   /**
   * Used by windows printing to assess if a font is likely to
   * be layout compatible with JDK
   * TrueType fonts should be, but if they have no GPOS table,
   * but do have a GSUB table, then they are probably older
   * fonts GDI handles differently.
   */
  public boolean textLayoutIsCompatible(Font font);
}
