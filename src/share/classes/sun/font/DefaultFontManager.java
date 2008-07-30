package sun.font;

class DefaultFontManager extends FontManager {

    public synchronized native String getFontPath(boolean noType1Fonts);
}
