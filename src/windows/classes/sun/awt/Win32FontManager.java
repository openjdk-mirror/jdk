

package sun.awt;

import java.io.File;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import sun.font.DefaultFontManager;
import sun.font.FontManager;

public class Win32FontManager extends DefaultFontManager {

    /* Unlike the shared code version, this expects a base file name -
     * not a full path name.
     * The font configuration file has base file names and the FontConfiguration
     * class reports these back to the GraphicsEnvironment, so these
     * are the componentFileNames of CompositeFonts.
     */
    protected void registerFontFile(String fontFileName, String[] nativeNames,
                                    int fontRank, boolean defer) {

        // REMIND: case compare depends on platform
        if (registeredFontFiles.contains(fontFileName)) {
            return;
        }
        registeredFontFiles.add(fontFileName);

        int fontFormat;
        if (ttFilter.accept(null, fontFileName)) {
            fontFormat = FontManager.FONTFORMAT_TRUETYPE;
        } else if (t1Filter.accept(null, fontFileName)) {
            fontFormat = FontManager.FONTFORMAT_TYPE1;
        } else {
            /* on windows we don't use/register native fonts */
            return;
        }

        if (fontPath == null) {
            fontPath = getPlatformFontPath(noType1Font);
        }

        /* Look in the JRE font directory first.
         * This is playing it safe as we would want to find fonts in the
         * JRE font directory ahead of those in the system directory
         */
        String tmpFontPath = jreFontDirName+File.pathSeparator+fontPath;
        StringTokenizer parser = new StringTokenizer(tmpFontPath,
                                                     File.pathSeparator);

        boolean found = false;
        try {
            while (!found && parser.hasMoreTokens()) {
                String newPath = parser.nextToken();
                File theFile = new File(newPath, fontFileName);
                if (theFile.canRead()) {
                    found = true;
                    String path = theFile.getAbsolutePath();
                    if (defer) {
                        registerDeferredFont(fontFileName, path,
                                             nativeNames,
                                             fontFormat, true,
                                             fontRank);
                    } else {
                        registerFontFile(path, nativeNames,
                                         fontFormat, true,
                                         fontRank);
                    }
                    break;
                }
            }
        } catch (NoSuchElementException e) {
            System.err.println(e);
        }
        if (!found) {
            addToMissingFontFileList(fontFileName);
        }
    }

}
