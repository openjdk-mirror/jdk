package sun.font;

public abstract class DefaultFontManager extends FontManagerBase {

    public synchronized native String getFontPath(boolean noType1Fonts);
}
