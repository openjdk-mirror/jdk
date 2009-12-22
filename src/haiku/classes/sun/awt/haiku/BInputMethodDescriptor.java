package sun.awt.haiku;

import java.awt.*;
import java.awt.im.*;
import java.awt.im.spi.*;
import java.util.Locale;

class BInputMethodDescriptor implements InputMethodDescriptor {

	private Locale[] locales;

	BInputMethodDescriptor() {
		locales = new Locale[0];
	}

    public Locale[] getAvailableLocales() throws AWTException {
		return locales;
    }
    
    public boolean hasDynamicLocaleList() {
		return false;
    }
    
    public String getInputMethodDisplayName(Locale inputLocale, Locale displayLanguage) {
		return "Haiku";
    }
    
    public Image getInputMethodIcon(Locale inputLocale) {
		return null;
    }
    
    public InputMethod createInputMethod() throws Exception {
		return new BInputMethod();
    }
}
