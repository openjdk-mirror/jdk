package sun.awt.haiku;

import java.util.Locale;
import java.util.Properties;
import sun.awt.FontProperties;


public class BFontProperties extends FontProperties {
	public BFontProperties() {
		super();
	}
	
    // overrides FontProperties.getFallbackFamilyName
    // REMIND: remove this method and references to it from the next feature release.
    public String getFallbackFamilyName(String fontName, String defaultFallback) {
        // maintain compatibility with old font.properties files, which
        // either had aliases for TimesRoman & Co. or defined mappings for them.
        String compatibilityName = getCompatibilityFamilyName(fontName);
        if (compatibilityName != null) {
            return compatibilityName;
        }
        return defaultFallback;
    }
}
