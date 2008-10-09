

package sun.awt;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import sun.awt.windows.WFontConfiguration;
import sun.font.DefaultFontManager;
import sun.font.FontManager;
import sun.java2d.HeadlessGraphicsEnvironment;
import sun.java2d.SunGraphicsEnvironment;

public class Win32FontManager extends DefaultFontManager {

    // FIXME: Windows build still needs to be abstracted from
    // SunGraphicEnvironment
    
    // please, don't reference sgEnv in any other code in this class
    // use getGraphicsEnvironment.
    @Deprecated
    private static SunGraphicsEnvironment sgEnv = null;
    
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
        if (getTrueTypeFilter().accept(null, fontFileName)) {
            fontFormat = FontManager.FONTFORMAT_TRUETYPE;
        } else if (getType1Filter().accept(null, fontFileName)) {
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

    @Override
    protected FontConfiguration createFontConfiguration() {
        
       FontConfiguration fc = new WFontConfiguration(this);
       fc.init();
       return fc;
    }

    @Override
    public FontConfiguration createFontConfiguration(boolean preferLocaleFonts,
            boolean preferPropFonts) {
        
        return new WFontConfiguration(this,
                                      preferLocaleFonts,preferPropFonts);
    }

    private GraphicsEnvironment getGraphicsEnvironment() {

        if (sgEnv != null)
            return sgEnv;
        
        sgEnv = null;
        
        GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        
        if (!(ge instanceof SunGraphicsEnvironment)) {
            throw new UnsupportedOperationException("Windows build currently " +
                        "only supports SunGraphicsEnvironment");
        }
        
        if (ge instanceof HeadlessGraphicsEnvironment) {
            HeadlessGraphicsEnvironment hgEnv =
                (HeadlessGraphicsEnvironment)ge;
                    sgEnv = (SunGraphicsEnvironment)
                        hgEnv.getSunGraphicsEnvironment();
        } else {
            sgEnv = (SunGraphicsEnvironment)ge;
        }       
        
        return sgEnv;
    }

    protected void
        populateFontFileNameMap(HashMap<String,String> fontToFileMap,
                                HashMap<String,String> fontToFamilyNameMap,
                                HashMap<String,ArrayList<String>>
                                familyToFontListMap,
                                Locale locale) {

	populateFontFileNameMap0(fontToFileMap, fontToFamilyNameMap,
				 familyToFontListMap, locale);

    }

    private static native void
        populateFontFileNameMap0(HashMap<String,String> fontToFileMap,
                                 HashMap<String,String> fontToFamilyNameMap,
                                 HashMap<String,ArrayList<String>>
                                     familyToFontListMap,
                                 Locale locale);

}
