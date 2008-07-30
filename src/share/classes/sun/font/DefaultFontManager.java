package sun.font;

public abstract class DefaultFontManager extends FontManager {

    public synchronized native String getFontPath(boolean noType1Fonts);
}
