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
import java.util.Locale;
import java.util.TreeMap;

import javax.swing.plaf.FontUIResource;

/**
 * Interface between Java Fonts (java.awt.Font) and the underlying
 * font files/native font resources and the Java and native font scalers.
 */
public interface FontManager {
   
    // These constants are used in findFont().
    public static final int NO_FALLBACK = 0;
    public static final int PHYSICAL_FALLBACK = 1;
    public static final int LOGICAL_FALLBACK = 2;

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
     * The client supplies a name and a style.
     * The name could be a family name, or a full name.
     * A font may exist with the specified style, or it may
     * exist only in some other style. For non-native fonts the scaler
     * may be able to emulate the required style.
     */
    public Font2D findFont2D(String name, int style, int fallback);

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
     * This is called by Swing passing in a fontconfig family name
     * such as "sans". In return Swing gets a FontUIResource instance
     * that has queried fontconfig to resolve the font(s) used for this.
     * Fontconfig will if asked return a list of fonts to give the largest
     * possible code point coverage.
     */
    // TODO: Only used internally by Swing. Maybe move out of the interface.
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
    // TODO: Only used internally by Swing. Maybe move out of the interface.
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
    // TODO: Only used internally by Swing. Maybe move out of the interface.
    public boolean fontSupportsDefaultEncoding(Font font);
    
}
