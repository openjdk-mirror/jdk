package sun.font;

import java.awt.Font;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class FontUtilities {

    private static Method getFont2DMethod;
    private static Field font2DHandleField;

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
}
