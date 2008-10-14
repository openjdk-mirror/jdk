package sun.font;

public abstract class DefaultFontManager extends SunFontManager {

    public synchronized native String getFontPath(boolean noType1Fonts);
}
