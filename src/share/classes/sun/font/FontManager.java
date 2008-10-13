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
    
}
