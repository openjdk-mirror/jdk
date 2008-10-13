package sun.font;

import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class FontUtilities {

    /**
     * For backward compatibilty only, this will to be removed soon, don't use
     * in new code.
     */
    @Deprecated
    public static boolean IS_WINDOWS;
    
    /**
     * For backward compatibilty only, this will to be removed soon, don't use
     * in new code.
     */
    @Deprecated
    public static boolean IS_LINUX;
    
    /**
     * For backward compatibilty only, this will to be removed soon, don't use
     * in new code.
     */
    @Deprecated
    public static boolean IS_SOLARIS;
    
    /**
     * For backward compatibilty only, this will to be removed soon, don't use
     * in new code.
     */
    @Deprecated
    public static boolean IS_OPEN_SOLARIS;
    
    /**
     * For backward compatibilty only, this will to be removed soon, don't use
     * in new code.
     */
    @Deprecated
    public static boolean IS_SOLARIS_8;
    
    /**
     * For backward compatibilty only, this will to be removed soon, don't use
     * in new code.
     */
    @Deprecated
    public static boolean USE_T2K;
    
    /**
     * For backward compatibilty only, this will to be removed soon, don't use
     * in new code.
     */
    @Deprecated
    public static boolean IS_SOLARIS_9;
    
    /**
     * For backward compatibilty only, this will to be removed soon, don't use
     * in new code.
     */
    @Deprecated
    public static boolean IS_OPENJDK;
    
    // Note: This mirrors FontManagerBase.lucidaFileName for now, we don't
    // want to reference this directly though, to avoid initialization of
    // FontManagerBase related classes when we don't use FontManager base at
    // all.
    private static final String LUCIDA_FILE_NAME = "LucidaSansRegular.ttf";

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
    
    private static boolean debugFonts = false;
    private static Logger logger = null;
    private static boolean logging;
    
    private static Method getFont2DMethod;
    private static Field font2DHandleField;

    static {
        
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

        if (debugFonts) {
            logger = Logger.getLogger("sun.java2d", null);
            logging = logger.getLevel() != Level.OFF;
        }

    }

    // This static initializer block figures out the OS constants.
    static {

        java.security.AccessController
                .doPrivileged(new java.security.PrivilegedAction() {

                    public Object run() {
                        String osName = System.getProperty("os.name",
                                "unknownOS");
                        IS_SOLARIS = osName.startsWith("SunOS");

                        IS_LINUX = osName.startsWith("Linux");

                        String t2kStr = System
                                .getProperty("sun.java2d.font.scaler");
                        if (t2kStr != null) {
                            USE_T2K = "t2k".equals(t2kStr);
                        }
                        if (IS_SOLARIS) {
                            String version = System.getProperty("os.version",
                                    "0.0");
                            IS_SOLARIS_8 = version.startsWith("5.8");
                            IS_SOLARIS_9 = version.startsWith("5.9");
                            try {
                                float ver = Float.parseFloat(version);
                                if (ver > 5.10f) {
                                    File f = new File("/etc/release");
                                    FileInputStream fis = new FileInputStream(f);
                                    InputStreamReader isr = new InputStreamReader(
                                            fis, "ISO-8859-1");
                                    BufferedReader br = new BufferedReader(isr);
                                    String line = br.readLine();
                                    if (line.indexOf("OpenSolaris") >= 0) {
                                        IS_OPEN_SOLARIS = true;
                                    }
                                    fis.close();
                                }
                            } catch (Exception e) {
                            }
                        } else {
                            IS_WINDOWS = osName.startsWith("Windows");
                        }
                        String jreLibDirName =
                            System.getProperty("java.home","") + File.separator + "lib";
                        String jreFontDirName = jreLibDirName + File.separator + "fonts";
                        File lucidaFile =
                            new File(jreFontDirName + File.separator + LUCIDA_FILE_NAME);
                        IS_OPENJDK = !lucidaFile.exists();

                    return null;
                    };
                });
    }

    /**
     * Calls the private getFont2D() method in java.awt.Font objects.
     *
     * @param font the font object to call
     *
     * @return the Font2D object returned by Font.getFont2D()
     */
    public static Font2D getFont2D(Font font) {

        try {
            if (getFont2DMethod == null) {
                Class<Font> fontCls = Font.class;
                getFont2DMethod = fontCls.getDeclaredMethod("getFont2D");
                getFont2DMethod.setAccessible(true);
            }
            Font2D font2D = (Font2D) getFont2DMethod.invoke(font);
            return font2D;
        } catch (Exception ex) {
            InternalError err = new InternalError();
            err.initCause(ex);
            throw err;
        }
    }

    public static void setFont2D(Font font, Font2DHandle handle) {
        try  {
            if (font2DHandleField == null) {
                Class<Font> fontCls = Font.class;
                font2DHandleField = fontCls.getDeclaredField("font2DHandle");
                font2DHandleField.setAccessible(true);
            }
            font2DHandleField.set(font, handle);
        } catch (Exception ex) {
            InternalError err = new InternalError();
            err.initCause(ex);
            throw err;
        }
    }

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
    public static boolean isComplexText(char [] chs, int start, int limit) {

        for (int i = start; i < limit; i++) {
            if (chs[i] < MIN_LAYOUT_CHARCODE) {
                continue;
            }
            else if (isNonSimpleChar(chs[i])) {
                return true;
            }
        }
        return false;
    }

    /* This is almost the same as the method above, except it takes a
     * char which means it may include undecoded surrogate pairs.
     * The distinction is made so that code which needs to identify all
     * cases in which we do not have a simple mapping from
     * char->unicode character->glyph can be be identified.
     * For example measurement cannot simply sum advances of 'chars',
     * the caret in editable text cannot advance one 'char' at a time, etc.
     * These callers really are asking for more than whether 'layout'
     * needs to be run, they need to know if they can assume 1->1
     * char->glyph mapping.
     */
    public static boolean isNonSimpleChar(char ch) {
        return
            isComplexCharCode(ch) ||
            (ch >= CharToGlyphMapper.HI_SURROGATE_START &&
             ch <= CharToGlyphMapper.LO_SURROGATE_END);
    }

    /* If the character code falls into any of a number of unicode ranges
     * where we know that simple left->right layout mapping chars to glyphs
     * 1:1 and accumulating advances is going to produce incorrect results,
     * we want to know this so the caller can use a more intelligent layout
     * approach. A caller who cares about optimum performance may want to
     * check the first case and skip the method call if its in that range.
     * Although there's a lot of tests in here, knowing you can skip
     * CTL saves a great deal more. The rest of the checks are ordered
     * so that rather than checking explicitly if (>= start & <= end)
     * which would mean all ranges would need to be checked so be sure
     * CTL is not needed, the method returns as soon as it recognises
     * the code point is outside of a CTL ranges.
     * NOTE: Since this method accepts an 'int' it is asssumed to properly
     * represent a CHARACTER. ie it assumes the caller has already
     * converted surrogate pairs into supplementary characters, and so
     * can handle this case and doesn't need to be told such a case is
     * 'complex'.
     */
    public static boolean isComplexCharCode(int code) {

        if (code < MIN_LAYOUT_CHARCODE || code > MAX_LAYOUT_CHARCODE) {
            return false;
        }
        else if (code <= 0x036f) {
            // Trigger layout for combining diacriticals 0x0300->0x036f
            return true;
        }
        else if (code < 0x0590) {
            // No automatic layout for Greek, Cyrillic, Armenian.
             return false;
        }
        else if (code <= 0x06ff) {
            // Hebrew 0590 - 05ff
            // Arabic 0600 - 06ff
            return true;
        }
        else if (code < 0x0900) {
            return false; // Syriac and Thaana
        }
        else if (code <= 0x0e7f) {
            // if Indic, assume shaping for conjuncts, reordering:
            // 0900 - 097F Devanagari
            // 0980 - 09FF Bengali
            // 0A00 - 0A7F Gurmukhi
            // 0A80 - 0AFF Gujarati
            // 0B00 - 0B7F Oriya
            // 0B80 - 0BFF Tamil
            // 0C00 - 0C7F Telugu
            // 0C80 - 0CFF Kannada
            // 0D00 - 0D7F Malayalam
            // 0D80 - 0DFF Sinhala
            // 0E00 - 0E7F if Thai, assume shaping for vowel, tone marks
            return true;
        }
        else if (code < 0x1780) {
            return false;
        }
        else if (code <= 0x17ff) { // 1780 - 17FF Khmer
            return true;
        }
        else if (code < 0x200c) {
            return false;
        }
        else if (code <= 0x200d) { //  zwj or zwnj
            return true;
        }
        else if (code >= 0x202a && code <= 0x202e) { // directional control
            return true;
        }
        else if (code >= 0x206a && code <= 0x206f) { // directional control
            return true;
        }
        return false;
    }

    public static Logger getLogger() {
        return logger;
    }
    
    public static boolean isLogging() {     
        return logging;
    }
    
    public static boolean debugFonts() {
        return debugFonts;
    }
    

}
